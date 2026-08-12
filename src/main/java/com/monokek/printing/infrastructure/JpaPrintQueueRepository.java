package com.monokek.printing.infrastructure;

import com.monokek.printing.domain.PrintQueue;
import com.monokek.printing.domain.PrintQueueRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaPrintQueueRepository extends PrintQueueRepository, JpaRepository<PrintQueue, Long> {
}
