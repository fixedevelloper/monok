package com.monokek.catalog.infrastructure;

import com.monokek.catalog.domain.ModifierItem;
import com.monokek.catalog.domain.ModifierItemRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaModifierItemRepository extends ModifierItemRepository, JpaRepository<ModifierItem, Long> {
}
