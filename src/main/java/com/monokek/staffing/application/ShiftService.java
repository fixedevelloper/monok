package com.monokek.staffing.application;

import com.monokek.common.ApiException;
import com.monokek.identity.UserDirectory;
import com.monokek.staffing.domain.Shift;
import com.monokek.staffing.domain.ShiftRepository;
import com.monokek.staffing.web.dto.CreateShiftRequest;
import com.monokek.staffing.web.dto.ShiftDto;
import com.monokek.staffing.web.dto.UpdateShiftRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Admin management of the planned roster — see the module's package-info. */
@Service
public class ShiftService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ShiftRepository shiftRepository;
    private final UserDirectory userDirectory;

    public ShiftService(ShiftRepository shiftRepository, UserDirectory userDirectory) {
        this.shiftRepository = shiftRepository;
        this.userDirectory = userDirectory;
    }

    @Transactional(readOnly = true)
    public List<ShiftDto> list(Long branchId, LocalDateTime from, LocalDateTime to) {
        return toDtos(shiftRepository.findByBranchIdAndStartsAtBetween(branchId, from, to));
    }

    @Transactional(readOnly = true)
    public List<ShiftDto> listForUser(Long userId, LocalDateTime from, LocalDateTime to) {
        return toDtos(shiftRepository.findByUserIdAndStartsAtBetween(userId, from, to));
    }

    /** {@code callerBranchId} null means the caller has no assigned branch (owner/super-admin) —
     * only then is {@code request.branchId()} trusted, same rule as {@code printing.PrinterService#store}. */
    @Transactional
    public ShiftDto create(CreateShiftRequest request, Long callerBranchId) {
        requireOrdered(request.startsAt(), request.endsAt());
        Shift shift = new Shift();
        shift.setUserId(request.userId());
        shift.setBranchId(callerBranchId != null ? callerBranchId : request.branchId());
        shift.setStartsAt(request.startsAt());
        shift.setEndsAt(request.endsAt());
        shift.setNote(request.note());
        return toDto(shiftRepository.save(shift));
    }

    @Transactional
    public ShiftDto update(Long id, UpdateShiftRequest request) {
        Shift shift = findOrThrow(id);
        if (request.userId() != null) shift.setUserId(request.userId());
        if (request.branchId() != null) shift.setBranchId(request.branchId());
        if (request.startsAt() != null) shift.setStartsAt(request.startsAt());
        if (request.endsAt() != null) shift.setEndsAt(request.endsAt());
        if (request.note() != null) shift.setNote(request.note());
        requireOrdered(shift.getStartsAt(), shift.getEndsAt());
        return toDto(shiftRepository.save(shift));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        shiftRepository.deleteById(id);
    }

    private void requireOrdered(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (!startsAt.isBefore(endsAt)) {
            throw ApiException.badRequest("L'heure de début doit précéder l'heure de fin.");
        }
    }

    private Shift findOrThrow(Long id) {
        return shiftRepository.findById(id).orElseThrow(() -> ApiException.notFound("Shift introuvable"));
    }

    private List<ShiftDto> toDtos(List<Shift> shifts) {
        Map<Long, String> names = userDirectory.namesByIds(shifts.stream().map(Shift::getUserId).distinct().toList());
        return shifts.stream().map(s -> toDto(s, names)).toList();
    }

    private ShiftDto toDto(Shift shift) {
        return toDto(shift, userDirectory.namesByIds(List.of(shift.getUserId())));
    }

    private ShiftDto toDto(Shift shift, Map<Long, String> names) {
        return new ShiftDto(
                shift.getId(), shift.getUserId(), names.get(shift.getUserId()), shift.getBranchId(),
                shift.getStartsAt().format(DATE_TIME), shift.getEndsAt().format(DATE_TIME), shift.getNote());
    }
}
