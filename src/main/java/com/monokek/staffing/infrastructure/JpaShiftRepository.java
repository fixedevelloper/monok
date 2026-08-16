package com.monokek.staffing.infrastructure;

import com.monokek.staffing.domain.Shift;
import com.monokek.staffing.domain.ShiftRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaShiftRepository extends ShiftRepository, JpaRepository<Shift, Long> {
}
