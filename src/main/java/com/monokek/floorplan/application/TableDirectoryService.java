package com.monokek.floorplan.application;

import com.monokek.floorplan.TableDirectory;
import com.monokek.floorplan.domain.RestaurantTable;
import com.monokek.floorplan.domain.RestaurantTableRepository;
import com.monokek.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
class TableDirectoryService implements TableDirectory {

    private final RestaurantTableRepository tableRepository;

    TableDirectoryService(RestaurantTableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TableSnapshot> findTable(Long tableId) {
        return tableRepository.findById(tableId).map(this::toSnapshot);
    }

    @Override
    @Transactional
    public void markOccupied(Long tableId) {
        updateStatus(tableId, "occupied");
    }

    @Override
    @Transactional
    public void markFree(Long tableId) {
        updateStatus(tableId, "free");
    }

    private void updateStatus(Long tableId, String status) {
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> ApiException.notFound("Table introuvable"));
        table.changeStatus(status);
        tableRepository.save(table);
    }

    private TableSnapshot toSnapshot(RestaurantTable table) {
        Long branchId = table.getFloor() == null ? null : table.getFloor().getBranchId();
        Long floorId = table.getFloor() == null ? null : table.getFloor().getId();
        return new TableSnapshot(table.getId(), table.getName(), floorId, branchId, table.getStatus(), table.isVirtual());
    }
}
