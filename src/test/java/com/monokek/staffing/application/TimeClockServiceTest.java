package com.monokek.staffing.application;

import com.monokek.common.ApiException;
import com.monokek.identity.UserDirectory;
import com.monokek.staffing.domain.ShiftRepository;
import com.monokek.staffing.domain.TimeClockEntry;
import com.monokek.staffing.domain.TimeClockRepository;
import com.monokek.staffing.web.dto.TimeClockEntryDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeClockServiceTest {

    private final TimeClockRepository timeClockRepository = mock(TimeClockRepository.class);
    private final ShiftRepository shiftRepository = mock(ShiftRepository.class);
    private final UserDirectory userDirectory = mock(UserDirectory.class);
    private final TimeClockService service = new TimeClockService(timeClockRepository, shiftRepository, userDirectory);

    @Test
    void clockInFailsWhenTheEmployeeIsAlreadyClockedIn() {
        when(timeClockRepository.findFirstByUserIdAndClockOutAtIsNull(7L)).thenReturn(Optional.of(new TimeClockEntry()));

        assertThatThrownBy(() -> service.clockIn(7L, 3L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("déjà pointé");
    }

    @Test
    void clockInSucceedsAndLinksTodaysShiftWhenOneExists() {
        when(timeClockRepository.findFirstByUserIdAndClockOutAtIsNull(7L)).thenReturn(Optional.empty());
        when(shiftRepository.findByUserIdAndStartsAtBetween(eq(7L), any(), any())).thenReturn(List.of());
        when(userDirectory.namesByIds(List.of(7L))).thenReturn(Map.of(7L, "Awa"));
        when(timeClockRepository.save(any(TimeClockEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        TimeClockEntryDto dto = service.clockIn(7L, 3L);

        assertThat(dto.userId()).isEqualTo(7L);
        assertThat(dto.userName()).isEqualTo("Awa");
        assertThat(dto.clockOutAt()).isNull();
    }

    @Test
    void clockOutFailsWhenTheEmployeeIsNotClockedIn() {
        when(timeClockRepository.findFirstByUserIdAndClockOutAtIsNull(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clockOut(7L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("n'est pas pointé");
    }

    @Test
    void clockOutClosesTheOpenEntryAndComputesWorkedMinutes() {
        TimeClockEntry entry = TimeClockEntry.clockIn(7L, 3L, null);
        when(timeClockRepository.findFirstByUserIdAndClockOutAtIsNull(7L)).thenReturn(Optional.of(entry));
        when(timeClockRepository.save(any(TimeClockEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userDirectory.namesByIds(any())).thenReturn(Map.of());

        TimeClockEntryDto dto = service.clockOut(7L);

        assertThat(entry.isOpen()).isFalse();
        assertThat(dto.clockOutAt()).isNotNull();
        assertThat(dto.workedMinutes()).isGreaterThanOrEqualTo(0);
    }
}
