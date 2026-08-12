package com.monokek.catalog.infrastructure;

import com.monokek.catalog.domain.Category;
import com.monokek.catalog.domain.CategoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCategoryRepository extends CategoryRepository, JpaRepository<Category, Long> {
}
