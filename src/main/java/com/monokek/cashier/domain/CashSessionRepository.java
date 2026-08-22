package com.monokek.cashier.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CashSessionRepository extends Repository<CashSession, Long> {

    CashSession save(CashSession session);

    Optional<CashSession> findById(Long id);

    Optional<CashSession> findFirstByRegisterIdAndClosedAtIsNull(Long registerId);

    /** Batches the open-session lookup for CashierService#listRegisters — one query for every register, not one per. */
    List<CashSession> findByRegisterIdInAndClosedAtIsNull(Collection<Long> registerIds);

    Optional<CashSession> findFirstByUserIdAndClosedAtIsNull(Long userId);

    List<CashSession> findByUserId(Long userId);

    /** Guards register deletion — cash_sessions.register_id is ON DELETE CASCADE, which would silently wipe financial history otherwise. */
    boolean existsByRegisterId(Long registerId);

    /** Every shift (open or closed) for a branch's registers, most recent first — backs the
     * admin "browse shifts to export" screen. {@code branchId} null means unscoped (owner/super-admin). */
    @Query("""
            SELECT s FROM CashSession s
            WHERE (:branchId IS NULL OR s.register.branchId = :branchId)
            ORDER BY s.openedAt DESC
            """)
    Page<CashSession> findAllForBranch(@Param("branchId") Long branchId, Pageable pageable);
}
