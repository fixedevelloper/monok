package com.monokek.printing.application;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.TcpIpOutputStream;
import com.monokek.common.ApiException;
import com.monokek.printing.domain.Printer;
import com.monokek.printing.domain.PrinterRepository;
import com.monokek.printing.infrastructure.NetworkPrinterProbe;
import com.monokek.printing.web.dto.CreatePrinterRequest;
import com.monokek.printing.web.dto.PrinterDto;
import com.monokek.printing.web.dto.TestConnectionResult;
import com.monokek.printing.web.dto.UpdatePrinterRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Admin\PrinterController}'s
 * CRUD, plus {@code testConnection} — upgraded from a plain TCP reachability
 * probe to an actual ESC/POS test ticket via {@code escpos-coffee}, so
 * "online" means a physical receipt was really pushed to the printer, not
 * just that the socket accepted a connection. Real order/kitchen tickets are
 * rendered and dispatched by {@link NetworkPrintDispatcher}, triggered from
 * {@link PrintQueueListener} — see the module's package-info.
 */
@Service
public class PrinterService {

    private static final DateTimeFormatter TEST_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final PrinterRepository printerRepository;
    private final NetworkPrinterProbe networkPrinterProbe;

    public PrinterService(PrinterRepository printerRepository, NetworkPrinterProbe networkPrinterProbe) {
        this.printerRepository = printerRepository;
        this.networkPrinterProbe = networkPrinterProbe;
    }

    @Transactional(readOnly = true)
    public List<PrinterDto> list(Long branchId) {
        List<Printer> printers = branchId == null ? printerRepository.findAll() : printerRepository.findByBranchId(branchId);
        return printers.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PrinterDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    /** {@code callerBranchId} null means the caller has no assigned branch (owner/super-admin) —
     * only then is {@code request.branchId()} trusted; a branch manager can only ever create a
     * printer for their own branch, not pick another one from the dropdown. */
    @Transactional
    public PrinterDto store(CreatePrinterRequest request, Long callerBranchId) {
        Printer printer = new Printer();
        printer.setBranchId(callerBranchId != null ? callerBranchId : request.branchId());
        printer.setName(request.name());
        printer.setType(request.type());
        printer.setConnection(request.connection());
        printer.setIp("usb".equals(request.connection()) ? null : request.ip());
        if (request.port() != null) printer.setPort(request.port());
        if (request.charPerLine() != null) printer.setCharPerLine(request.charPerLine());
        if (request.useBeep() != null) printer.setUseBeep(request.useBeep());
        if (request.paperWidth() != null) printer.setPaperWidth(request.paperWidth());
        printer.setLocation(request.location());
        return toDto(printerRepository.save(printer));
    }

    @Transactional
    public PrinterDto update(Long id, UpdatePrinterRequest request) {
        Printer printer = findOrThrow(id);

        if (request.branchId() != null) printer.setBranchId(request.branchId());
        if (request.name() != null) printer.setName(request.name());
        if (request.type() != null) printer.setType(request.type());
        if (request.connection() != null) printer.setConnection(request.connection());
        if (request.charPerLine() != null) printer.setCharPerLine(request.charPerLine());
        if (request.isActive() != null) printer.setActive(request.isActive());
        if (request.useBeep() != null) printer.setUseBeep(request.useBeep());
        if (request.paperWidth() != null) printer.setPaperWidth(request.paperWidth());
        if (request.location() != null) printer.setLocation(request.location());

        // Port of: switching a printer from network to usb clears its IP.
        if ("usb".equals(printer.getConnection())) {
            printer.setIp(null);
        } else if (request.ip() != null) {
            printer.setIp(request.ip());
        }

        return toDto(printerRepository.save(printer));
    }

    @Transactional
    public void destroy(Long id) {
        findOrThrow(id);
        printerRepository.deleteById(id);
    }

    /**
     * Actually prints a test ticket over the network rather than just probing
     * the socket. {@link TcpIpOutputStream} connects on a background thread
     * and only logs connection failures instead of surfacing them through the
     * stream's write path, so {@link NetworkPrinterProbe} runs first — that's
     * what turns an unreachable printer into "offline" instead of a false
     * "online".
     */
    @Transactional(readOnly = true)
    public TestConnectionResult testConnection(Long id) {
        Printer printer = findOrThrow(id);
        if (!"network".equals(printer.getConnection())) {
            throw ApiException.badRequest("Test non disponible pour USB via API");
        }

        if (!networkPrinterProbe.isReachable(printer.getIp(), printer.getPort())) {
            return new TestConnectionResult("offline", "Impossible de joindre l'imprimante");
        }

        try (TcpIpOutputStream out = new TcpIpOutputStream(printer.getIp(), printer.getPort())) {
            EscPos escpos = new EscPos(out);
            escpos.writeLF("=== TEST D'IMPRESSION ===")
                    .writeLF(printer.getName())
                    .writeLF(LocalDateTime.now().format(TEST_TIMESTAMP_FORMAT))
                    .feed(3)
                    .cut(EscPos.CutMode.FULL);
            escpos.flush();
            return new TestConnectionResult("online", "Ticket de test envoyé avec succès");
        } catch (IOException e) {
            return new TestConnectionResult("offline", "Impossible de joindre l'imprimante");
        }
    }

    private Printer findOrThrow(Long id) {
        return printerRepository.findById(id).orElseThrow(() -> ApiException.notFound("Imprimante introuvable"));
    }

    private PrinterDto toDto(Printer printer) {
        return new PrinterDto(
                printer.getId(), printer.getBranchId(), printer.getName(), printer.getType(), printer.getConnection(),
                printer.getLocation(), printer.getIp(), printer.getPort(), printer.getCharPerLine(), printer.isActive(),
                printer.getPaperWidth(), printer.isUseBeep());
    }
}
