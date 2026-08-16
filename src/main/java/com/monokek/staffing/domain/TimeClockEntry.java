package com.monokek.staffing.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * A single clock-in/clock-out punch — the actual log, as opposed to
 * {@link Shift} (the plan). {@code shiftId} is a best-effort same-day match,
 * never required: an employee can punch in with no prior planned shift.
 */
@Entity
@Table(name = "time_clock_entries")
@Getter
@NoArgsConstructor
public class TimeClockEntry extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References identity.User by id only — see module package-info. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** References company.Branch by id only — see module package-info. */
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "clock_in_at", nullable = false)
    private LocalDateTime clockInAt;

    @Column(name = "clock_out_at")
    private LocalDateTime clockOutAt;

    public static TimeClockEntry clockIn(Long userId, Long branchId, Long shiftId) {
        TimeClockEntry entry = new TimeClockEntry();
        entry.userId = userId;
        entry.branchId = branchId;
        entry.shiftId = shiftId;
        entry.clockInAt = LocalDateTime.now();
        return entry;
    }

    public void clockOut() {
        this.clockOutAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return clockOutAt == null;
    }

    public long workedMinutes() {
        return Duration.between(clockInAt, clockOutAt != null ? clockOutAt : LocalDateTime.now()).toMinutes();
    }
}
