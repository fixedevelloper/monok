package com.monokek.company.infrastructure;

import com.monokek.company.domain.Branch;
import com.monokek.company.domain.BranchRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaBranchRepository extends BranchRepository, JpaRepository<Branch, Long> {
}
