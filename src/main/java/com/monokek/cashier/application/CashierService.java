package com.monokek.cashier.application;

import com.monokek.cashier.domain.*;
import com.monokek.cashier.domain.event.CashSessionReportReadyEvent;
import com.monokek.cashier.web.dto.*;
import com.monokek.common.ApiException;
import com.monokek.identity.UserDirectory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Pos\CashController}.
 */
@Service
public class CashierService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CashRegisterRepository cashRegisterRepository;
    private final CashSessionRepository cashSessionRepository;
    private final PaymentRepository paymentRepository;
    private final UserDirectory userDirectory;
    private final ApplicationEventPublisher events;

    public CashierService(
            CashRegisterRepository cashRegisterRepository,
            CashSessionRepository cashSessionRepository,
            PaymentRepository paymentRepository,
            UserDirectory userDirectory,
            ApplicationEventPublisher events) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.paymentRepository = paymentRepository;
        this.userDirectory = userDirectory;
        this.events = events;
    }

    /** {@code branchId} null means unscoped (admin/owner with no assigned branch) — everyone
     * else only ever sees their own branch's registers, same rule as {@code floorplan.FloorService#list}. */
    @Transactional(readOnly = true)
    public List<CashRegisterDto> listRegisters(Long branchId) {
        List<CashRegister> registers = branchId == null ? cashRegisterRepository.findAll() : cashRegisterRepository.findByBranchId(branchId);

        // One namesByIds call for the whole list, not one per register (toRegisterDto's own
        // call is fine for the single-register call sites below, but here it would mean one
        // HTTP round-trip to monokek-identity per open register — see HttpUserDirectory).
        List<CashSession> openSessions = cashSessionRepository.findByRegisterIdInAndClosedAtIsNull(
                registers.stream().map(CashRegister::getId).toList());
        Map<Long, CashSession> openSessionByRegisterId = openSessions.stream()
                .collect(Collectors.toMap(session -> session.getRegister().getId(), session -> session));
        Map<Long, String> namesByUserId = userDirectory.namesByIds(
                openSessions.stream().map(CashSession::getUserId).collect(Collectors.toSet()));

        return registers.stream()
                .map(register -> {
                    CashSession session = openSessionByRegisterId.get(register.getId());
                    return session == null
                            ? new CashRegisterDto(register.getId(), register.getBranchId(), register.getName(), false, null)
                            : new CashRegisterDto(register.getId(), register.getBranchId(), register.getName(),
                                    true, namesByUserId.get(session.getUserId()));
                })
                .toList();
    }

    /** {@code callerBranchId} null means the caller has no assigned branch (owner/super-admin) —
     * only then is {@code request.branchId()} trusted; a branch manager can only ever create a
     * register for their own branch, not pick another one from the dropdown. */
    @Transactional
    public CashRegisterDto storeRegister(StoreRegisterRequest request, Long callerBranchId) {
        CashRegister register = new CashRegister();
        register.setBranchId(callerBranchId != null ? callerBranchId : request.branchId());
        register.setName(request.name());
        register = cashRegisterRepository.save(register);
        return toRegisterDto(register);
    }

    @Transactional
    public CashRegisterDto updateRegister(Long id, UpdateRegisterRequest request) {
        CashRegister register = cashRegisterRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Caisse introuvable"));
        if (request.name() != null) register.setName(request.name());
        if (request.branchId() != null) register.setBranchId(request.branchId());
        register = cashRegisterRepository.save(register);
        return toRegisterDto(register);
    }

    /** Whether {@code register} currently has an open session, and by whom — see {@link #open} for why this matters. */
    private CashRegisterDto toRegisterDto(CashRegister register) {
        return cashSessionRepository.findFirstByRegisterIdAndClosedAtIsNull(register.getId())
                .map(session -> new CashRegisterDto(register.getId(), register.getBranchId(), register.getName(),
                        true, userDirectory.namesByIds(Set.of(session.getUserId())).get(session.getUserId())))
                .orElseGet(() -> new CashRegisterDto(register.getId(), register.getBranchId(), register.getName(), false, null));
    }

    /**
     * cash_sessions.register_id is ON DELETE CASCADE (see V1__init_schema.sql) — deleting a
     * register that has ever been opened would silently wipe its financial history along with
     * it, unlike deleting an unused floor/table/station. Refused outright rather than risking that.
     */
    @Transactional
    public void deleteRegister(Long id) {
        cashRegisterRepository.findById(id).orElseThrow(() -> ApiException.notFound("Caisse introuvable"));
        if (cashSessionRepository.existsByRegisterId(id)) {
            throw ApiException.conflict("Impossible de supprimer une caisse ayant déjà des sessions enregistrées.");
        }
        cashRegisterRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CashStatusDto status(Long userId) {
        return cashSessionRepository.findFirstByUserIdAndClosedAtIsNull(userId)
                .map(session -> new CashStatusDto(true, toDto(session)))
                .orElseGet(() -> new CashStatusDto(false, null));
    }

    @Transactional
    public CashSessionDto open(OpenSessionRequest request, Long userId) {
        if (cashSessionRepository.findFirstByUserIdAndClosedAtIsNull(userId).isPresent()) {
            throw ApiException.badRequest("Une caisse est déjà ouverte");
        }
        CashRegister register = cashRegisterRepository.findById(request.registerId())
                .orElseThrow(() -> ApiException.badRequest("Caisse introuvable"));

        // The check above only stops the CURRENT user from double-opening — nothing stopped a
        // second cashier from opening the same physical register at the same time (findById
        // has no ownership/locking concept at all), splitting its sales across two concurrent
        // sessions with no way to reconcile which payment belongs to which. registerId was
        // already unique per open session in intent (CashSessionRepository even had
        // findFirstByRegisterIdAndClosedAtIsNull defined for this) but nothing ever called it.
        cashSessionRepository.findFirstByRegisterIdAndClosedAtIsNull(register.getId()).ifPresent(existing -> {
            String openedBy = userDirectory.namesByIds(Set.of(existing.getUserId())).get(existing.getUserId());
            throw ApiException.conflict("Cette caisse est déjà ouverte par %s.".formatted(openedBy != null ? openedBy : "un autre utilisateur"));
        });

        CashSession session = CashSession.open(register, userId, request.openingAmount());
        return toDto(cashSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public CashSummaryDto currentSummary(Long userId) {
        CashSession session = openSessionOrThrow(userId, "Caisse fermée");
        List<Payment> payments = paymentRepository.findByCashSessionId(session.getId());
        List<PaymentBreakdownDto> breakdown = groupByMethod(payments);
        BigDecimal total = breakdown.stream().map(PaymentBreakdownDto::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CashSummaryDto(toDto(session), breakdown, total);
    }

    @Transactional
    public CloseReportDto close(CloseSessionRequest request, Long userId) {
        CashSession session = openSessionOrThrow(userId, "Aucune session active trouvée");

        List<Payment> payments = paymentRepository.findByCashSessionId(session.getId());
        BigDecimal totalPayments = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedAmount = session.getOpeningAmount().add(totalPayments);
        List<PaymentBreakdownDto> breakdown = groupByMethod(payments);

        session.close(request.closingAmount(), expectedAmount, request.note());
        cashSessionRepository.save(session);

        BigDecimal difference = request.closingAmount().subtract(expectedAmount);
        CloseReportDto.Report report = new CloseReportDto.Report(
                format(session.getOpenedAt()),
                format(session.getClosedAt()),
                session.getOpeningAmount(),
                totalPayments,
                expectedAmount,
                request.closingAmount(),
                difference,
                request.note(),
                breakdown
        );

        events.publishEvent(new CashSessionReportReadyEvent(
                session.getId(), session.getRegister().getBranchId(), userId,
                session.getOpenedAt(), session.getClosedAt(),
                session.getOpeningAmount(), totalPayments, expectedAmount, request.closingAmount(), difference,
                request.note(), toEventBreakdown(breakdown)));

        return new CloseReportDto("Caisse fermée avec succès", report);
    }

    private List<CashSessionReportReadyEvent.PaymentBreakdown> toEventBreakdown(List<PaymentBreakdownDto> breakdown) {
        return breakdown.stream().map(b -> new CashSessionReportReadyEvent.PaymentBreakdown(b.method(), b.total())).toList();
    }

    private CashSession openSessionOrThrow(Long userId, String message) {
        return cashSessionRepository.findFirstByUserIdAndClosedAtIsNull(userId)
                .orElseThrow(() -> ApiException.notFound(message));
    }

    private List<PaymentBreakdownDto> groupByMethod(List<Payment> payments) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Payment payment : payments) {
            totals.merge(payment.getPaymentMethod().getName(), payment.getAmount(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(e -> new PaymentBreakdownDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PaymentBreakdownDto::method))
                .toList();
    }

    private CashSessionDto toDto(CashSession session) {
        return new CashSessionDto(
                session.getId(),
                session.getRegister().getId(),
                session.getRegister().getName(),
                session.getOpeningAmount(),
                session.getClosingAmount(),
                format(session.getOpenedAt()),
                format(session.getClosedAt()),
                session.getExpectedAmount(),
                session.getNote()
        );
    }

    private String format(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME);
    }
}
