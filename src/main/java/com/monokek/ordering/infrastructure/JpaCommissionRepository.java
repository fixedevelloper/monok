package com.monokek.ordering.infrastructure;

import com.monokek.ordering.domain.Commission;
import com.monokek.ordering.domain.CommissionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCommissionRepository extends CommissionRepository, JpaRepository<Commission, Long> {
}
