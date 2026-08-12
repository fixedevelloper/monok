package com.monokek.kitchen.infrastructure;

import com.monokek.kitchen.domain.KitchenStation;
import com.monokek.kitchen.domain.KitchenStationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaKitchenStationRepository extends KitchenStationRepository, JpaRepository<KitchenStation, Long> {
}
