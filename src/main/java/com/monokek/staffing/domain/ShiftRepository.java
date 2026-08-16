package com.monokek.staffing.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ShiftRepository extends Repository<Shift, Long> {

    Shift save(Shift shift);

    Optional<Shift> findById(Long id);

    void deleteById(Long id);

    List<Shift> findByBranchIdAndStartsAtBetween(Long branchId, LocalDateTime from, LocalDateTime to);

    List<Shift> findByUserIdAndStartsAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
}
