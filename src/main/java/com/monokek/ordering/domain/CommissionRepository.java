package com.monokek.ordering.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CommissionRepository extends Repository<Commission, Long> {

    Commission save(Commission commission);

    Optional<Commission> findById(Long id);

    List<Commission> findByUserId(Long userId);

    boolean existsByOrderId(Long orderId);

    @Query("""
            SELECT c FROM Commission c
            WHERE (:month IS NULL OR MONTH(c.createdAt) = :month)
              AND (:year IS NULL OR YEAR(c.createdAt) = :year)
            ORDER BY c.id DESC
            """)
    List<Commission> search(@Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Commission c WHERE c.status = :status")
    BigDecimal sumByStatus(@Param("status") String status);

    @Query("SELECT c.userId FROM Commission c GROUP BY c.userId ORDER BY SUM(c.amount) DESC")
    List<Long> topWaiterIdsByTotal(Pageable pageable);

    @Modifying
    @Query("UPDATE Commission c SET c.status = 'paid', c.paidAt = CURRENT_TIMESTAMP WHERE c.userId = :userId AND c.status = 'pending'")
    int settlePendingForUser(@Param("userId") Long userId);
}
