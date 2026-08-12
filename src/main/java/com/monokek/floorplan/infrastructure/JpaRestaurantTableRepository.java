package com.monokek.floorplan.infrastructure;

import com.monokek.floorplan.domain.RestaurantTable;
import com.monokek.floorplan.domain.RestaurantTableRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaRestaurantTableRepository extends RestaurantTableRepository, JpaRepository<RestaurantTable, Long> {
}
