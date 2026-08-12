package com.monokek.identity;

import java.util.Collection;
import java.util.Map;

/**
 * Published interface: other modules only ever need a user's display name
 * (e.g. {@code ordering} showing the waiter/cashier on an order) — never the
 * full {@code identity.domain.User} aggregate, its password hash, or its
 * roles.
 */
public interface UserDirectory {

    Map<Long, String> namesByIds(Collection<Long> ids);
}
