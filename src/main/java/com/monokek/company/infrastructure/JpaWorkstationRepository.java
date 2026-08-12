package com.monokek.company.infrastructure;

import com.monokek.company.domain.Workstation;
import com.monokek.company.domain.WorkstationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaWorkstationRepository extends WorkstationRepository, JpaRepository<Workstation, Long> {
}
