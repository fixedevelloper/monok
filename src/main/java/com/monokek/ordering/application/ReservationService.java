package com.monokek.ordering.application;

import com.monokek.catalog.ProductCatalog;
import com.monokek.common.ApiException;
import com.monokek.crm.CustomerDirectory;
import com.monokek.ordering.domain.*;
import com.monokek.ordering.web.dto.CreateReservationRequest;
import com.monokek.ordering.web.dto.ReservationDto;
import com.monokek.ordering.web.dto.UpdateReservationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Admin\ReservationController}.
 *
 * <p>Two deliberate deviations from the Laravel source, both fixes rather
 * than simplifications: (1) item prices are resolved server-side via
 * {@link ProductCatalog}, matching the discipline {@code OrderService}
 * already applies — Laravel trusted the client-submitted price here, which
 * {@code sendRound} explicitly does <em>not</em> do; (2) items need an
 * {@code OrderRound} to attach to (the schema requires one), which Laravel's
 * {@code $order->items()->create(...)} — a read-only {@code hasManyThrough}
 * relation — could not actually have produced at runtime.
 */
@Service
public class ReservationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** Laravel hardcodes {@code branch_id => 1} here too — there's no manager-branch concept in the schema yet. */
    private static final Long DEFAULT_BRANCH_ID = 1L;

    private final ReservationRepository reservationRepository;
    private final OrderRepository orderRepository;
    private final CustomerDirectory customerDirectory;
    private final ProductCatalog productCatalog;
    private final OrderService orderService;

    public ReservationService(
            ReservationRepository reservationRepository,
            OrderRepository orderRepository,
            CustomerDirectory customerDirectory,
            ProductCatalog productCatalog,
            OrderService orderService) {
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
        this.customerDirectory = customerDirectory;
        this.productCatalog = productCatalog;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public Page<ReservationDto> index(String filter, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Reservation> page = switch (filter == null ? "" : filter) {
            case "today" -> reservationRepository.findByPickupDateBetweenOrderByPickupDateAsc(
                    now.toLocalDate().atStartOfDay(), now.toLocalDate().atTime(23, 59, 59), pageable);
            case "upcoming" -> reservationRepository.findByPickupDateAfterOrderByPickupDateAsc(now, pageable);
            case "past" -> reservationRepository.findByPickupDateBeforeOrderByPickupDateAsc(now, pageable);
            default -> reservationRepository.findAllByOrderByPickupDateAsc(pageable);
        };
        return page.map(this::toDto);
    }

    @Transactional
    public ReservationDto create(CreateReservationRequest request, Long managerUserId) {
        CustomerDirectory.CustomerSnapshot customer =
                customerDirectory.findOrCreateByPhone(request.customerPhone(), request.customerName());

        Order order = Order.openReservation(DEFAULT_BRANCH_ID, customer.id(), managerUserId);
        OrderRound round = order.openRound(null);

        for (CreateReservationRequest.ItemLine line : request.items()) {
            ProductCatalog.ProductSnapshot product = productCatalog.findProduct(line.productId())
                    .orElseThrow(() -> ApiException.badRequest("Produit introuvable : " + line.productId()));
            round.addItem(product.id(), null, line.quantity(), product.price());
        }
        order.refreshTotals();
        order = orderRepository.save(order);

        Reservation reservation = new Reservation();
        reservation.setOrder(order);
        reservation.setCustomerId(customer.id());
        reservation.setPickupDate(request.pickupDate());
        reservation.setGuestsCount(request.guestsCount() == null ? 1 : request.guestsCount());
        reservation.setManagerNotes(request.managerNotes());
        reservation.setReservationStatus("confirmed");

        return toDto(reservationRepository.save(reservation));
    }

    @Transactional
    public void pay(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Commande introuvable."));
        if (order.isPaid()) {
            throw ApiException.badRequest("Cette commande est déjà réglée");
        }
        order.changeStatus("pending_payment", userId);
        orderRepository.save(order);
    }

    @Transactional
    public ReservationDto update(Long reservationId, UpdateReservationRequest request) {
        Reservation reservation = findOrThrow(reservationId);

        CustomerDirectory.CustomerSnapshot customer =
                customerDirectory.findOrCreateByPhone(request.customerPhone(), request.customerName());
        reservation.setCustomerId(customer.id());

        Order order = reservation.getOrder();
        // Replace the line items: clear every existing round and rebuild a single fresh one,
        // same intent as Laravel's `$order->items()->delete()` followed by re-creation.
        order.getRounds().clear();
        OrderRound round = order.openRound(null);
        for (CreateReservationRequest.ItemLine line : request.items()) {
            ProductCatalog.ProductSnapshot product = productCatalog.findProduct(line.productId())
                    .orElseThrow(() -> ApiException.badRequest("Produit introuvable : " + line.productId()));
            round.addItem(product.id(), null, line.quantity(), product.price());
        }
        order.refreshTotals();
        orderRepository.save(order);

        reservation.setPickupDate(request.pickupDate());
        reservation.setGuestsCount(request.guestsCount() == null ? 1 : request.guestsCount());
        reservation.setManagerNotes(request.managerNotes());

        return toDto(reservationRepository.save(reservation));
    }

    @Transactional
    public void destroy(Long reservationId) {
        Reservation reservation = findOrThrow(reservationId);
        Order order = reservation.getOrder();
        if (order != null) {
            order.changeStatus("cancelled", null);
            orderRepository.save(order);
        }
        reservationRepository.deleteById(reservationId);
    }

    private Reservation findOrThrow(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> ApiException.notFound("Réservation introuvable."));
    }

    private ReservationDto toDto(Reservation reservation) {
        ReservationDto.CustomerRef customer = customerDirectory.findById(reservation.getCustomerId())
                .map(c -> new ReservationDto.CustomerRef(c.id(), c.name(), c.phone()))
                .orElse(new ReservationDto.CustomerRef(reservation.getCustomerId(), null, null));
        return new ReservationDto(
                reservation.getId(),
                reservation.getPickupDate() == null ? null : reservation.getPickupDate().format(DATE_TIME),
                reservation.getGuestsCount(),
                reservation.getManagerNotes(),
                reservation.getReservationStatus(),
                customer,
                orderService.toDto(reservation.getOrder()),
                reservation.getCreatedAt() == null ? null : reservation.getCreatedAt().format(DATE_TIME),
                reservation.getUpdatedAt() == null ? null : reservation.getUpdatedAt().format(DATE_TIME)
        );
    }
}
