package com.monokek.crm;

import java.util.Optional;

/**
 * Published interface: {@code ordering} needs to find-or-create a customer
 * by phone number when a manager books a reservation, and to resolve a
 * customer id back to a name/phone for display — without reaching into
 * {@code crm.domain}.
 */
public interface CustomerDirectory {

    CustomerSnapshot findOrCreateByPhone(String phone, String name);

    Optional<CustomerSnapshot> findById(Long id);

    record CustomerSnapshot(Long id, String name, String phone) {
    }
}
