package com.monokek.floorplan.application;

import com.monokek.company.domain.event.BranchCreatedEvent;
import com.monokek.floorplan.domain.Floor;
import com.monokek.floorplan.domain.FloorRepository;
import com.monokek.floorplan.domain.RestaurantTable;
import com.monokek.floorplan.domain.RestaurantTableRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * A branch with no free table can't take a walk-in — without a fallback,
 * the floor being full means the POS simply can't open an order. This
 * provisions one default table automatically so staff never have to set
 * it up by hand (see {@code V7__add_default_takeaway_table.sql} for the
 * one-time backfill on branches that already existed).
 */
@Component
class DefaultTableProvisioner {

    static final String DEFAULT_TABLE_PREFIX = "Emporter — ";

    private final FloorRepository floorRepository;
    private final RestaurantTableRepository tableRepository;

    DefaultTableProvisioner(FloorRepository floorRepository, RestaurantTableRepository tableRepository) {
        this.floorRepository = floorRepository;
        this.tableRepository = tableRepository;
    }

    @ApplicationModuleListener
    void on(BranchCreatedEvent event) {
        String label = DEFAULT_TABLE_PREFIX + event.branchName();

        Floor floor = new Floor();
        floor.setBranchId(event.branchId());
        floor.setName(label);
        floor = floorRepository.save(floor);

        RestaurantTable table = new RestaurantTable();
        table.setFloor(floor);
        table.setName(label);
        table.setSeats(1);
        table.setVirtual(true);
        tableRepository.save(table);
    }
}
