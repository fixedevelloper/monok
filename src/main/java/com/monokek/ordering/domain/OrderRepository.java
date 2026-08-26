package com.monokek.ordering.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface OrderRepository extends Repository<Order, Long> {

    Order save(Order order);

    Optional<Order> findById(Long id);

    /** Same row as {@link #findById}, but under {@code SELECT ... FOR UPDATE} — used only by
     * {@code OrderService#resolveOrderForSendRound}'s explicit-{@code orderId} branch, never for
     * plain reads (payment, cancellation, history...), to avoid serializing those unnecessarily
     * behind a lock they don't need. See {@link #findFirstByTableIdAndStatusNotInOrderByIdDesc}
     * for why this lock exists at all.
     *
     * <p>Backed by an explicit {@code @Query} rather than derived from the method name: Spring
     * Data's derivation parses {@code findByIdForUpdate} as a property path ("idForUpdate" on
     * {@code Order}, which doesn't exist) — "ForUpdate" isn't one of its recognized keywords, it
     * only means anything here because of the {@link Lock} annotation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    Optional<Order> findByUuid(UUID uuid);

    Optional<Order> findByReference(String reference);

    List<Order> findByBranchIdAndStatus(Long branchId, String status);

    /** The order currently open on a table, if any — reused by {@code sendRound} instead of creating a duplicate.
     * Must exclude both terminal statuses ("paid" and "cancelled"): excluding only "paid" let a
     * cancelled order get silently picked back up as "the" open order for its table — see
     * {@code Order#assertOpen}.
     *
     * <p>Locked ({@code SELECT ... FOR UPDATE}), restoring the pessimistic locking the class-level
     * javadoc on {@code OrderService} used to call out as intentionally dropped versus the Laravel
     * original: without it, two near-simultaneous {@code sendRound} calls for the same table each
     * read the same round count, both call {@code Order#openRound} with the same computed
     * {@code round_number}, and both commit — a real, observed duplicate (traced live on orders
     * #51/#54: a whole round, items included, silently doubled). The lock is held for the
     * remainder of {@code sendRound}'s transaction, so the second call blocks until the first
     * commits its new round, then sees the up-to-date count.</p> */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findFirstByTableIdAndStatusNotInOrderByIdDesc(Long tableId, List<String> excludedStatuses);

    /** The order the POS shows as "active" for a table — port of {@code OrderController::getActiveOrder}. */
    Optional<Order> findFirstByTableIdAndStatusInOrderByIdDesc(Long tableId, List<String> statuses);

    List<Order> findByIdIn(List<Long> ids);

    /** Every order still "open" (not cancelled/paid) for the branch, regardless of which day it
     * started — a tab opened yesterday and never settled is exactly as active as one opened five
     * minutes ago, so this must NOT filter by {@code createdAt} (it used to, via a now-removed
     * {@code after} param: any order older than "today" silently vanished from the POS sales
     * screen's list even though it was still genuinely open — see {@code OrderService#history}).
     * {@code branchId} null means unscoped (owner/super-admin) — see {@code identity.CurrentUser#branchId}. */
    @Query("""
            SELECT o FROM Order o
            WHERE (:branchId IS NULL OR o.branchId = :branchId)
              AND o.status NOT IN :excludedStatuses
            """)
    List<Order> findActive(@Param("branchId") Long branchId, @Param("excludedStatuses") List<String> excludedStatuses);

    List<Order> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    /** Every order a given coupon was applied to, most recent first — backs the admin "Commandes"
     * view on a coupon's card (see {@code OrderController#ordersByCoupon}). Not filtered by
     * status: a coupon applied then the order cancelled before payment is still a real use worth
     * showing, not just paid orders. */
    List<Order> findByCouponIdOrderByIdDesc(Long couponId);

    /** Port of {@code OrderController::index}/{@code historyAdmin} — every filter is optional,
     * {@code branchId} the same null-means-unscoped convention as {@link #findActive}. */
    @Query("""
            SELECT o FROM Order o
            WHERE (:search IS NULL OR LOWER(o.reference) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR o.status = :status)
              AND (:from IS NULL OR o.createdAt >= :from)
              AND (:to IS NULL OR o.createdAt <= :to)
              AND (:branchId IS NULL OR o.branchId = :branchId)
            ORDER BY o.id DESC
            """)
    Page<Order> search(
            @Param("search") String search,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") Long branchId,
            Pageable pageable);
}
