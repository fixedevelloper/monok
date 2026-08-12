package com.monokek.catalog.infrastructure;

import com.monokek.catalog.domain.Modifier;
import com.monokek.catalog.domain.ModifierRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaModifierRepository extends ModifierRepository, JpaRepository<Modifier, Long> {
}
