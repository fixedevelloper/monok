package com.monokek.ordering.application;

import com.monokek.catalog.ProductCatalog;
import com.monokek.identity.UserDirectory;
import com.monokek.ordering.domain.Commission;
import com.monokek.ordering.domain.CommissionRepository;
import com.monokek.ordering.domain.Order;
import com.monokek.ordering.domain.OrderItem;
import com.monokek.ordering.web.dto.CommissionDto;
import com.monokek.ordering.web.dto.CommissionStatsDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service: port of {@code App\Http\Services\CommissionService} +
 * the query logic {@code CommissionController} runs directly against
 * Eloquent in Laravel.
 */
@Service
public class CommissionService {

    private static final BigDecimal GLOBAL_RATE = new BigDecimal("0.02"); // Option A: 2%

    private final CommissionRepository commissionRepository;
    private final ProductCatalog productCatalog;
    private final UserDirectory userDirectory;

    public CommissionService(CommissionRepository commissionRepository, ProductCatalog productCatalog, UserDirectory userDirectory) {
        this.commissionRepository = commissionRepository;
        this.productCatalog = productCatalog;
        this.userDirectory = userDirectory;
    }

    /** Called once per paid order, from {@code OrderService#finalizePayment}. */
    @Transactional
    public void calculateCommissions(Order order) {
        if (commissionRepository.existsByOrderId(order.getId())) {
            return;
        }
        for (OrderItem item : order.allItems()) {
            BigDecimal incentiveAmount = productCatalog.findProduct(item.getProductId())
                    .map(ProductCatalog.ProductSnapshot::incentiveAmount)
                    .orElse(BigDecimal.ZERO);

            if (incentiveAmount != null && incentiveAmount.signum() > 0) {
                // Option C: fixed amount per unit takes priority over the global rate.
                create(order, item, incentiveAmount.multiply(BigDecimal.valueOf(item.getQty())), "incentive", null);
            } else {
                // Option A: global percentage.
                BigDecimal amount = item.getPrice().multiply(BigDecimal.valueOf(item.getQty())).multiply(GLOBAL_RATE);
                create(order, item, amount, "global", GLOBAL_RATE.multiply(BigDecimal.valueOf(100)).floatValue());
            }
        }
    }

    @Transactional
    public int settleWaiterCommissions(Long userId) {
        return commissionRepository.settlePendingForUser(userId);
    }

    @Transactional(readOnly = true)
    public List<CommissionDto> search(Integer month, Integer year) {
        List<Commission> commissions = commissionRepository.search(month, year);
        Map<Long, String> names = userDirectory.namesByIds(
                commissions.stream().map(Commission::getUserId).collect(Collectors.toSet()));
        return commissions.stream().map(c -> toDto(c, names)).toList();
    }

    @Transactional(readOnly = true)
    public CommissionStatsDto stats() {
        BigDecimal pending = commissionRepository.sumByStatus("pending");
        BigDecimal paid = commissionRepository.sumByStatus("paid");
        Long topWaiterId = commissionRepository.topWaiterIdsByTotal(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        String topWaiterName = topWaiterId == null ? null : userDirectory.namesByIds(Set.of(topWaiterId)).get(topWaiterId);
        return new CommissionStatsDto(pending, paid, topWaiterId, topWaiterName);
    }

    private void create(Order order, OrderItem item, BigDecimal amount, String type, Float percentage) {
        Commission commission = new Commission();
        commission.setUserId(order.getUserId());
        commission.setOrder(order);
        commission.setOrderItem(item);
        commission.setAmount(amount);
        commission.setPercentage(percentage);
        commission.setType(type);
        commission.setStatus("pending");
        commissionRepository.save(commission);
    }

    private CommissionDto toDto(Commission c, Map<Long, String> names) {
        String productName = c.getOrderItem() == null ? null
                : productCatalog.findProduct(c.getOrderItem().getProductId()).map(ProductCatalog.ProductSnapshot::name).orElse(null);
        return new CommissionDto(
                c.getId(),
                c.getAmount(),
                c.getPercentage(),
                c.getType(),
                c.getStatus(),
                c.getCreatedAt() == null ? null : c.getCreatedAt().toString(),
                c.getUserId(),
                names.getOrDefault(c.getUserId(), "Inconnu"),
                c.getOrder() == null ? null : c.getOrder().getReference(),
                productName == null ? "Vente Globale" : productName,
                "incentive".equals(c.getType()),
                "paid".equals(c.getStatus()) ? "Payé" : "En attente"
        );
    }
}
