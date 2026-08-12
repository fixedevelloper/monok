package com.monokek.catalog.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CategoryRepository extends Repository<Category, Long> {

    Category save(Category category);

    Optional<Category> findById(Long id);

    void deleteById(Long id);

    List<Category> findAll();

    List<Category> findByBranchId(Long branchId);

    List<Category> findByActiveTrueOrderByNameAsc();

    List<Category> findByBranchIdAndActiveTrueOrderByNameAsc(Long branchId);

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
