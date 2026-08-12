package com.monokek.floorplan;

import java.util.Optional;

/**
 * Published interface: the slice of floor-plan state {@code ordering} needs
 * — resolving a table's branch (via its floor) and flipping its occupancy —
 * without reaching into {@code floorplan.domain}.
 */
public interface TableDirectory {

    Optional<TableSnapshot> findTable(Long tableId);

    void markOccupied(Long tableId);

    void markFree(Long tableId);

    record TableSnapshot(Long id, String name, Long floorId, Long branchId, String status, boolean virtual) {
    }
}
