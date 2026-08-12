package com.monokek.printing.infrastructure;

import com.monokek.printing.domain.Printer;
import com.monokek.printing.domain.PrinterRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaPrinterRepository extends PrinterRepository, JpaRepository<Printer, Long> {
}
