package com.monokek.staffing.application;

import com.monokek.common.ApiException;
import com.monokek.identity.UserDirectory;
import com.monokek.staffing.domain.Shift;
import com.monokek.staffing.domain.ShiftRepository;
import com.monokek.staffing.domain.TimeClockEntry;
import com.monokek.staffing.domain.TimeClockRepository;
import com.monokek.staffing.web.dto.TimeClockEntryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Actual clock-in/clock-out log — see the module's package-info. */
@Service
public class TimeClockService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TimeClockRepository timeClockRepository;
    private final ShiftRepository shiftRepository;
    private final UserDirectory userDirectory;

    public TimeClockService(TimeClockRepository timeClockRepository, ShiftRepository shiftRepository, UserDirectory userDirectory) {
        this.timeClockRepository = timeClockRepository;
        this.shiftRepository = shiftRepository;
        this.userDirectory = userDirectory;
    }

    @Transactional
    public TimeClockEntryDto clockIn(Long userId, Long branchId) {
        if (timeClockRepository.findFirstByUserIdAndClockOutAtIsNull(userId).isPresent()) {
            throw ApiException.conflict("Cet employé est déjà pointé.");
        }
        Long shiftId = todaysShift(userId).map(Shift::getId).orElse(null);
        TimeClockEntry entry = TimeClockEntry.clockIn(userId, branchId, shiftId);
        return toDto(timeClockRepository.save(entry));
    }

    @Transactional
    public TimeClockEntryDto clockOut(Long userId) {
        TimeClockEntry entry = timeClockRepository.findFirstByUserIdAndClockOutAtIsNull(userId)
                .orElseThrow(() -> ApiException.badRequest("Cet employé n'est pas pointé."));
        entry.clockOut();
        return toDto(timeClockRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<TimeClockEntryDto> currentlyPresent(Long branchId) {
        return toDtos(timeClockRepository.findByBranchIdAndClockOutAtIsNull(branchId));
    }

    @Transactional(readOnly = true)
    public List<TimeClockEntryDto> historyForUser(Long userId, LocalDateTime from, LocalDateTime to) {
        return toDtos(timeClockRepository.findByUserIdAndClockInAtBetween(userId, from, to));
    }

    @Transactional(readOnly = true)
    public List<TimeClockEntryDto> historyForBranch(Long branchId, LocalDateTime from, LocalDateTime to) {
        return toDtos(timeClockRepository.findByBranchIdAndClockInAtBetween(branchId, from, to));
    }

    /** Best-effort link to today's planned shift, if any — never required (see {@link TimeClockEntry}). */
    private Optional<Shift> todaysShift(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return shiftRepository.findByUserIdAndStartsAtBetween(userId, startOfDay, startOfDay.plusDays(1)).stream().findFirst();
    }

    private List<TimeClockEntryDto> toDtos(List<TimeClockEntry> entries) {
        Map<Long, String> names = userDirectory.namesByIds(entries.stream().map(TimeClockEntry::getUserId).distinct().toList());
        return entries.stream().map(e -> toDto(e, names)).toList();
    }

    private TimeClockEntryDto toDto(TimeClockEntry entry) {
        return toDto(entry, userDirectory.namesByIds(List.of(entry.getUserId())));
    }

    private TimeClockEntryDto toDto(TimeClockEntry entry, Map<Long, String> names) {
        return new TimeClockEntryDto(
                entry.getId(), entry.getUserId(), names.get(entry.getUserId()), entry.getBranchId(), entry.getShiftId(),
                entry.getClockInAt().format(DATE_TIME), entry.getClockOutAt() != null ? entry.getClockOutAt().format(DATE_TIME) : null,
                entry.workedMinutes());
    }
}
