package com.monokek.floorplan.infrastructure;

import com.monokek.floorplan.domain.Floor;
import com.monokek.floorplan.domain.FloorRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaFloorRepository extends FloorRepository, JpaRepository<Floor, Long> {
}
