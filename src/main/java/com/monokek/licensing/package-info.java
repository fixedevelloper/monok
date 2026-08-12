/**
 * Licensing module: verifies signed license keys entirely offline, with no
 * external license server. Each key is a JWT whose own {@code exp} claim IS
 * the license's expiry — minted out-of-band by {@link com.monokek.licensing.LicenseKeyGeneratorTool}
 * and checked here against {@code app.license.signing-key}. Auto-expiration
 * falls out for free: the moment {@code now() > exp}, {@code status()}
 * reports {@code expired=true} without any scheduled job touching the
 * database.
 *
 * <p>{@code GET /api/license/status} and {@code POST /api/license/activate}
 * match the frontend's {@code useLicense.ts}/{@code LicenseGuard.tsx}
 * contract field-for-field (including the 403-means-tampered-key convention
 * {@code LicenseGuard} already checks for). Both routes are in {@code SecurityConfig}'s
 * public allow-list, since the license gate has to be checkable before a user
 * is ever authenticated — it wraps the whole app in the frontend's root layout.
 *
 * <p>Standalone module: no dependency on any other module.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Licensing")
package com.monokek.licensing;
