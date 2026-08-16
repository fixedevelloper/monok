package com.monokek.staffing.infrastructure;

import com.monokek.staffing.domain.TimeClockEntry;
import com.monokek.staffing.domain.TimeClockRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaTimeClockRepository extends TimeClockRepository, JpaRepository<TimeClockEntry, Long> {
}
