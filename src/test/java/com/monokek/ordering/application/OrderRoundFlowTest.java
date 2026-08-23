package com.monokek.ordering.application;

import com.monokek.cashier.domain.CashRegister;
import com.monokek.cashier.domain.CashSession;
import com.monokek.catalog.domain.Category;
import com.monokek.catalog.domain.Product;
import com.monokek.floorplan.domain.Floor;
import com.monokek.floorplan.domain.RestaurantTable;
import com.monokek.ordering.OrderRoundStatusUpdater;
import com.monokek.ordering.domain.Order;
import com.monokek.ordering.domain.OrderRepository;
import com.monokek.ordering.web.dto.OrderDto;
import com.monokek.ordering.web.dto.SendRoundRequest;
import com.monokek.ordering.web.dto.SendRoundResult;
import io.minio.MinioClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration bout-en-bout (contexte Spring complet, base H2 en mémoire — voir
 * {@code application-test.yml}) du scénario : sélection de table -> round 1 -> round 2 immédiat
 * -> visible dans Ventes -> tous les rounds servis -> commande COMPLETED.
 *
 * <p>Ne passe volontairement pas par la couche HTTP/sécurité (pas de token OAuth2 à fabriquer) :
 * appelle {@link OrderService} et {@link OrderRoundStatusUpdater} directement, comme le ferait le
 * contrôleur. {@link OrderRoundStatusUpdater} est l'interface publiée que {@code kitchen} utilise
 * réellement pour signaler "ce round est prêt" — l'utiliser ici simule fidèlement l'effet d'un
 * ticket cuisine marqué "servi" sans avoir à recréer les fixtures {@code KitchenStation}/
 * {@code KitchenTicket} ni dépendre du timing asynchrone de {@code @ApplicationModuleListener}.
 *
 * <p>Ce scénario couvre, en régression, les bugs corrigés cette session sur ce flux :
 * <ul>
 *   <li>une commande annulée ne doit plus jamais réabsorber un nouveau round (cf. {@code
 *   Order#assertOpen})</li>
 *   <li>le statut ne doit pas rester bloqué sur {@code completed} dès qu'un round est ajouté après
 *   coup (cf. {@code Order#openRound})</li>
 *   <li>deux {@code sendRound} concurrents sur la même commande ne doivent jamais produire de
 *   {@code round_number} en double (cf. {@code OrderRepository}, verrou pessimiste)</li>
 *   <li>un round ajouté à une commande déjà existante ne doit jamais s'insérer deux fois — bug
 *   trouvé EN ÉCRIVANT ce test (jamais atteint par un appel séquentiel unique auparavant, donc
 *   invisible à {@code OrderTest}, purement domaine) : {@code OrderService#sendRound} appelait à la
 *   fois {@code orderRepository.save(order)} (merge, qui cascade quand même l'insert du round
 *   fraîchement ajouté, contrairement à ce que son commentaire supposait) et
 *   {@code orderRoundRepository.save(round)} juste après — deux inserts pour un seul round dès le
 *   2e round d'une commande</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class OrderRoundFlowTest {

    /** {@code MinioConfig}'s {@code ApplicationRunner} touches a real MinIO bucket at context
     * startup (unrelated to anything this test exercises) — mocked out so it no-ops instead of
     * failing the whole context because no MinIO is reachable/configured for this test. */
    @MockBean
    private MinioClient minioClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderRoundStatusUpdater orderRoundStatusUpdater;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long tableId;
    private Long productId;
    private Long cashierUserId;
    private Long branchId;

    /**
     * Volontairement PAS {@code @Transactional} sur la classe/méthode de test : les tests
     * ci-dessous ont besoin que {@code sendRound} (lui-même {@code @Transactional}) commite
     * réellement — en particulier le test de concurrence, où plusieurs threads doivent voir les
     * écritures des autres. On commite donc les fixtures explicitement ici via un
     * {@link TransactionTemplate} plutôt que de compter sur le rollback-par-défaut d'un
     * {@code @Transactional} de test (qui aurait de toute façon annulé ces données AVANT même le
     * début du test, puisque {@code @BeforeEach} et {@code @Test} n'ouvrent pas la même transaction).
     */
    @BeforeEach
    void seedFixtures() {
        branchId = 1L;
        cashierUserId = 2L;

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seedEntities());
    }

    private void seedEntities() {
        Floor floor = new Floor();
        floor.setBranchId(branchId);
        floor.setName("Salle Test");
        entityManager.persist(floor);

        RestaurantTable table = new RestaurantTable();
        table.setFloor(floor);
        table.setName("T-TEST");
        table.setSeats(4);
        table.setVirtual(false);
        entityManager.persist(table);
        tableId = table.getId();

        Category category = new Category();
        category.setBranchId(branchId);
        category.setKitchenStationId(1L); // pas besoin d'une vraie KitchenStation pour sendRound
        category.setName("Boissons");
        category.setSlug("boissons");
        category.setActive(true);
        entityManager.persist(category);

        Product product = new Product();
        product.setCategory(category);
        product.setName("Bière Test");
        product.setPrice(new BigDecimal("1000.00"));
        product.setActive(true);
        entityManager.persist(product);
        productId = product.getId();

        CashRegister register = new CashRegister();
        register.setBranchId(branchId);
        register.setName("Caisse Test");
        entityManager.persist(register);

        CashSession session = CashSession.open(register, cashierUserId, BigDecimal.ZERO);
        entityManager.persist(session);

        entityManager.flush();
    }

    @Test
    void multiRoundOrderReachesCompletedOnlyOnceEveryRoundIsServed() {
        // --- 1 & 2. Round 1, statut de commande "pending" ---
        SendRoundResult round1Result = orderService.sendRound(sendRoundRequest(null, 1), cashierUserId, cashierUserId);
        OrderDto afterRound1 = round1Result.order();
        Long orderId = afterRound1.id();

        assertThat(afterRound1.status()).isEqualTo("pending");
        assertThat(afterRound1.rounds()).hasSize(1);
        Long round1Id = afterRound1.rounds().get(0).id();

        // --- 3. Round 2 immédiat, même commande ---
        SendRoundResult round2Result = orderService.sendRound(sendRoundRequest(orderId, 2), cashierUserId, cashierUserId);
        OrderDto afterRound2 = round2Result.order();

        assertThat(afterRound2.id()).isEqualTo(orderId);
        assertThat(afterRound2.rounds()).hasSize(2);
        assertThat(afterRound2.status()).isEqualTo("pending");
        List<Integer> roundNumbers = afterRound2.rounds().stream().map(OrderDto.RoundDto::roundNumber).toList();
        assertThat(roundNumbers).doesNotHaveDuplicates();
        Long round2Id = afterRound2.rounds().stream().filter(r -> !r.id().equals(round1Id)).findFirst().orElseThrow().id();

        // --- 4. Vérification dans "Ventes" (historyPos) ---
        List<OrderDto> history = orderService.history(cashierUserId, branchId);
        OrderDto inHistory = history.stream().filter(o -> o.id().equals(orderId)).findFirst().orElse(null);
        assertThat(inHistory).as("la commande doit apparaître dans Ventes").isNotNull();
        assertThat(inHistory.status()).isEqualTo("pending");
        assertThat(inHistory.rounds()).hasSize(2);

        // --- 5a. Round 1 servi : la commande NE DOIT PAS encore se compléter ---
        orderRoundStatusUpdater.applyKitchenRoundStatus(orderId, round1Id, "served");
        Order afterRound1Served = orderRepository.findById(orderId).orElseThrow();
        assertThat(afterRound1Served.getStatus())
                .as("round 2 encore non résolu : la commande ne doit pas passer à completed")
                .isEqualTo("pending");

        // --- 5b. Round 2 servi à son tour ---
        orderRoundStatusUpdater.applyKitchenRoundStatus(orderId, round2Id, "served");

        // --- 6. Vérification finale ---
        // Lu dans une transaction explicite : order_rounds/order_status_histories sont des
        // collections LAZY, illisibles une fois la session d'origine (celle de findById) fermée.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Order finalOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(finalOrder.getStatus()).isEqualTo("completed");
            assertThat(finalOrder.getRounds()).allSatisfy(r -> assertThat(r.getStatus()).isEqualTo("served"));

            // Preuve que la transition a bien republié un événement (pas silencieusement avalée par
            // la garde de Order#completeIfAllRoundsResolved — c'est exactement ce que l'ancien bug cassait) :
            assertThat(finalOrder.getStatusHistories())
                    .filteredOn(h -> "completed".equals(h.getStatus()))
                    .as("une transition vers completed doit être journalisée à chaque round résolu, pas seulement au premier")
                    .hasSize(1); // ici un seul passage à completed puisque round 1 seul ne suffisait pas (voir 5a)
        });
    }

    /**
     * Régression du bug de concurrence : deux sendRound quasi simultanés sur la même commande ne
     * doivent jamais produire deux rounds avec le même round_number (voir OrderRepository#
     * findFirstByTableIdAndStatusNotInOrderByIdDesc / findByIdForUpdate, verrou pessimiste).
     * Dépend du support par H2 du verrouillage de ligne (SELECT ... FOR UPDATE) — comportement
     * par défaut de H2 en mode non-MVCC, cohérent avec MySQL/InnoDB en production.
     */
    @Test
    void concurrentSendRoundsNeverProduceDuplicateRoundNumbers() throws InterruptedException {
        SendRoundResult firstRound = orderService.sendRound(sendRoundRequest(null, 1), cashierUserId, cashierUserId);
        Long orderId = firstRound.order().id();

        int concurrentRounds = 5;
        ExecutorService pool = Executors.newFixedThreadPool(concurrentRounds);
        CountDownLatch ready = new CountDownLatch(concurrentRounds);
        CountDownLatch start = new CountDownLatch(1);

        List<Runnable> tasks = List.of(
                () -> orderService.sendRound(sendRoundRequest(orderId, 1), cashierUserId, cashierUserId),
                () -> orderService.sendRound(sendRoundRequest(orderId, 1), cashierUserId, cashierUserId),
                () -> orderService.sendRound(sendRoundRequest(orderId, 1), cashierUserId, cashierUserId),
                () -> orderService.sendRound(sendRoundRequest(orderId, 1), cashierUserId, cashierUserId),
                () -> orderService.sendRound(sendRoundRequest(orderId, 1), cashierUserId, cashierUserId)
        );

        for (Runnable task : tasks) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown(); // relâche les 5 threads en même temps
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).as("threads terminés à temps").isTrue();

        List<Integer> roundNumbers = new TransactionTemplate(transactionManager).execute(status -> {
            Order finalOrder = orderRepository.findById(orderId).orElseThrow();
            return finalOrder.getRounds().stream().map(r -> r.getRoundNumber()).toList();
        });
        assertThat(roundNumbers)
                .as("round_number : %s", roundNumbers)
                .doesNotHaveDuplicates()
                .hasSize(1 + concurrentRounds);
    }

    private SendRoundRequest sendRoundRequest(Long orderId, int qty) {
        return new SendRoundRequest(
                orderId, tableId,
                List.of(new SendRoundRequest.ItemLine(productId, qty, null)),
                null);
    }
}
