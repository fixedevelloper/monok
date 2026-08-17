package com.monokek.staffing.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ShiftRepository extends Repository<Shift, Long> {

    Shift save(Shift shift);

    Optional<Shift> findById(Long id);

    void deleteById(Long id);

    /** {@code branchId} null means the caller is unscoped (see {@link com.monokek.identity.CurrentUser#branchId()})
     * and must see every branch's shifts — a plain {@code branch_id = :branchId} equality would instead match
     * nothing at all once the parameter is bound to SQL NULL. */
    @Query("SELECT s FROM Shift s WHERE (:branchId IS NULL OR s.branchId = :branchId) AND s.startsAt BETWEEN :from AND :to")
    List<Shift> findByBranchIdAndStartsAtBetween(@Param("branchId") Long branchId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Shift> findByUserIdAndStartsAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
}
