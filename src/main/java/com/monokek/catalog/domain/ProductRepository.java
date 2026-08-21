package com.monokek.catalog.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ProductRepository extends Repository<Product, Long> {

    Product save(Product product);

    Optional<Product> findById(Long id);

    void deleteById(Long id);

    List<Product> findAll();

    List<Product> findByCategoryId(Long categoryId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /** Port of {@code ProductController::index}'s optional {@code category_id}/{@code search} filters — always {@code is_active}.
     * {@code branchId} null means unscoped (owner/super-admin) — see {@code identity.CurrentUser#branchId}. */
    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:branchId IS NULL OR p.category.branchId = :branchId)
            ORDER BY p.name ASC
            """)
    List<Product> searchActive(@Param("categoryId") Long categoryId, @Param("search") String search, @Param("branchId") Long branchId);
}
