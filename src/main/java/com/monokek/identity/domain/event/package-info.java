/**
 * Domain events published by the {@code identity} module. Declared as a
 * {@code @NamedInterface} so other modules are allowed to depend on these
 * event types (and only these types) without violating module encapsulation
 * — this is the one deliberate hole in an otherwise closed module, exactly
 * because events are the sanctioned way for modules to talk to each other.
 */
@org.springframework.modulith.NamedInterface("events")
package com.monokek.identity.domain.event;
