package com.monokek.printing.application;

import com.monokek.printing.domain.PrintQueue;
import com.monokek.printing.domain.PrintQueueRepository;
import com.monokek.printing.domain.Printer;
import com.monokek.printing.web.dto.PrintQueueDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrintQueueServiceTest {

    @Test
    void pendingIsScopedToTheCallersBranchOnly() {
        Printer printer = new Printer();
        printer.setId(5L);
        printer.setBranchId(3L);
        printer.setName("Cuisine");
        printer.setOsPrinterName("cups-kitchen-1");

        PrintQueue job = new PrintQueue();
        job.setId(11L);
        job.setPrinter(printer);
        job.setJobType("kitchen");
        job.setContent("{}");
        job.setStatus("pending");

        PrintQueueRepository printQueueRepository = mock(PrintQueueRepository.class);
        when(printQueueRepository.findByStatusAndPrinter_BranchId("pending", 3L)).thenReturn(List.of(job));

        PrintQueueService service = new PrintQueueService(printQueueRepository);

        List<PrintQueueDto> result = service.pending(3L);

        assertThat(result).hasSize(1);
        PrintQueueDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo(11L);
        assertThat(dto.printerOsPrinterName()).isEqualTo("cups-kitchen-1");
    }
}
