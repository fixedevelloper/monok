package com.monokek.staffing.application;

import com.monokek.common.ApiException;
import com.monokek.identity.UserDirectory;
import com.monokek.staffing.domain.Shift;
import com.monokek.staffing.domain.ShiftRepository;
import com.monokek.staffing.web.dto.CreateShiftRequest;
import com.monokek.staffing.web.dto.ShiftDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShiftServiceTest {

    private final ShiftRepository shiftRepository = mock(ShiftRepository.class);
    private final UserDirectory userDirectory = mock(UserDirectory.class);
    private final ShiftService service = new ShiftService(shiftRepository, userDirectory);

    @Test
    void createRejectsAShiftThatEndsBeforeItStarts() {
        CreateShiftRequest request = new CreateShiftRequest(
                7L, 3L, LocalDateTime.of(2026, 8, 20, 18, 0), LocalDateTime.of(2026, 8, 20, 10, 0), null);

        assertThatThrownBy(() -> service.create(request, 3L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("précéder");
    }

    @Test
    void createUsesTheCallersBranchOverTheRequestedOneWhenTheCallerIsScopedToOne() {
        CreateShiftRequest request = new CreateShiftRequest(
                7L, 99L, LocalDateTime.of(2026, 8, 20, 10, 0), LocalDateTime.of(2026, 8, 20, 18, 0), "Ouverture");
        when(userDirectory.namesByIds(any())).thenReturn(Map.of(7L, "Jean"));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(inv -> {
            Shift shift = inv.getArgument(0);
            shift.setId(1L);
            return shift;
        });

        ShiftDto dto = service.create(request, 3L);

        assertThat(dto.branchId()).isEqualTo(3L);
        assertThat(dto.userName()).isEqualTo("Jean");
    }

    @Test
    void deleteThrowsWhenTheShiftDoesNotExist() {
        when(shiftRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(ApiException.class);
    }
}
