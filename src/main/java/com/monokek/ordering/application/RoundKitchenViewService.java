package com.monokek.ordering.application;

import com.monokek.floorplan.TableDirectory;
import com.monokek.ordering.RoundKitchenView;
import com.monokek.ordering.domain.OrderItem;
import com.monokek.ordering.domain.OrderItemModifier;
import com.monokek.ordering.domain.OrderRound;
import com.monokek.ordering.domain.OrderRoundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
class RoundKitchenViewService implements RoundKitchenView {

    private final OrderRoundRepository orderRoundRepository;
    private final TableDirectory tableDirectory;

    RoundKitchenViewService(OrderRoundRepository orderRoundRepository, TableDirectory tableDirectory) {
        this.orderRoundRepository = orderRoundRepository;
        this.tableDirectory = tableDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoundSnapshot> findRound(Long roundId) {
        return orderRoundRepository.findById(roundId).map(this::toSnapshot);
    }

    private RoundSnapshot toSnapshot(OrderRound round) {
        String tableName = round.getOrder().getTableId() == null ? "Emporté"
                : tableDirectory.findTable(round.getOrder().getTableId()).map(TableDirectory.TableSnapshot::name).orElse("Emporté");

        var items = round.getItems().stream().map(this::toItemSnapshot).toList();

        return new RoundSnapshot(
                round.getId(), round.getRoundNumber(), round.getStatus(),
                round.getOrder().getId(), round.getOrder().getReference(), tableName, items);
    }

    private ItemSnapshot toItemSnapshot(OrderItem item) {
        var modifierItemIds = item.getModifiers().stream().map(OrderItemModifier::getModifierItemId).toList();
        return new ItemSnapshot(item.getId(), item.getProductId(), item.getQty(), modifierItemIds);
    }
}
