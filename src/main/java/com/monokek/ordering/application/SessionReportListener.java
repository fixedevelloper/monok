package com.monokek.ordering.application;

import com.monokek.cashier.CashierFacade;
import com.monokek.cashier.domain.event.CashSessionReportReadyEvent;
import com.monokek.catalog.ProductCatalog;
import com.monokek.identity.UserDirectory;
import com.monokek.ordering.domain.Order;
import com.monokek.ordering.domain.OrderItem;
import com.monokek.ordering.domain.OrderRepository;
import com.monokek.ordering.domain.event.SessionReportReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds the one piece of the cash-session Z-report {@code cashier} can't
 * compute itself — the sold-items summary, which needs {@code ordering}'s
 * own order data plus {@code catalog} product names — and the cashier's
 * display name from {@code identity}, then re-publishes a fully-combined
 * event in {@code ordering.domain.event} for {@code printing} to render.
 * {@code ordering} already depends one-directionally on {@code cashier}
 * (via {@link CashierFacade}); this just extends that same edge to also
 * cover {@code cashier}'s published domain events.
 */
@Component
public class SessionReportListener {

    private final CashierFacade cashierFacade;
    private final OrderRepository orderRepository;
    private final ProductCatalog productCatalog;
    private final UserDirectory userDirectory;
    private final ApplicationEventPublisher events;

    public SessionReportListener(
            CashierFacade cashierFacade, OrderRepository orderRepository, ProductCatalog productCatalog,
            UserDirectory userDirectory, ApplicationEventPublisher events) {
        this.cashierFacade = cashierFacade;
        this.orderRepository = orderRepository;
        this.productCatalog = productCatalog;
        this.userDirectory = userDirectory;
        this.events = events;
    }

    @ApplicationModuleListener
    void on(CashSessionReportReadyEvent event) {
        String cashierName = userDirectory.namesByIds(Set.of(event.cashierUserId())).get(event.cashierUserId());

        List<Order> orders = orderRepository.findByIdIn(cashierFacade.orderIdsPaidInSession(event.sessionId()));
        List<SessionReportReadyEvent.SoldItem> soldItems = soldItemsSummary(orders);

        List<SessionReportReadyEvent.PaymentBreakdown> breakdown = event.paymentBreakdown().stream()
                .map(b -> new SessionReportReadyEvent.PaymentBreakdown(b.method(), b.total()))
                .toList();

        events.publishEvent(new SessionReportReadyEvent(
                event.sessionId(), event.branchId(), cashierName, event.openedAt(), event.closedAt(),
                event.openingAmount(), event.totalSales(), event.expectedAmount(), event.actualAmount(), event.difference(),
                event.note(), breakdown, soldItems));
    }

    /** Sums qty/total per product across every order paid in the session — a session-wide summary, not a per-order breakdown. */
    private List<SessionReportReadyEvent.SoldItem> soldItemsSummary(List<Order> orders) {
        record Aggregate(String name, int qty, BigDecimal total) {
            Aggregate plus(int extraQty, BigDecimal extraTotal) {
                return new Aggregate(name, qty + extraQty, total.add(extraTotal));
            }
        }

        Map<Long, Aggregate> byProduct = new LinkedHashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.allItems()) {
                Aggregate current = byProduct.get(item.getProductId());
                if (current == null) {
                    String name = productCatalog.findProduct(item.getProductId())
                            .map(ProductCatalog.ProductSnapshot::name).orElse(null);
                    byProduct.put(item.getProductId(), new Aggregate(name, item.getQty(), item.getTotal()));
                } else {
                    byProduct.put(item.getProductId(), current.plus(item.getQty(), item.getTotal()));
                }
            }
        }

        return byProduct.values().stream()
                .map(a -> new SessionReportReadyEvent.SoldItem(a.name(), a.qty(), a.total()))
                .toList();
    }
}
