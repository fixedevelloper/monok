package com.monokek.inventory.infrastructure;

import com.monokek.inventory.domain.Unit;
import com.monokek.inventory.domain.UnitRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaUnitRepository extends UnitRepository, JpaRepository<Unit, Long> {
}
