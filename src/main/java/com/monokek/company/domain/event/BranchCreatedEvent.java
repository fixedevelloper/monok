package com.monokek.company.domain.event;

/** Raised once a branch is created — {@code floorplan} reacts by provisioning its default fallback table. */
public record BranchCreatedEvent(Long branchId, String branchName) {
}
