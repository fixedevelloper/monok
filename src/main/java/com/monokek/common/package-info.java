/**
 * Shared kernel: response envelope, exceptions and cross-cutting concerns used
 * by every other module. Declared {@code OPEN} so any module may depend on it
 * without that dependency showing up as a violation during
 * {@code ApplicationModules.verify()}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Common",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.monokek.common;
