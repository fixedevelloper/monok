package com.monokek.cashier.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

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
}
