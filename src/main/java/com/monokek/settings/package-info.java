/**
 * Cross-cutting operational module: key/value settings, offline-sync logs,
 * and the audit trail. {@code application.ActivityLogListener} is the
 * reference example of consuming another module's domain events (see
 * {@code com.monokek.identity.domain.event}) via
 * {@code @ApplicationModuleListener} instead of a direct call.
 *
 * <p>{@code web.SettingsController} ports {@code SettingsController::index}/
 * {@code update} — the only two of its five methods any route actually
 * reaches ({@code printers}/{@code storePrint}/{@code edtPrint} are dead:
 * printer CRUD lives in {@code PrinterController}, ported in {@code printing}).
 * {@code Setting::value} is Eloquent-cast {@code json} despite the column
 * being plain {@code TEXT}; {@code SettingsService} replicates that with
 * Jackson at the service boundary. No {@code SettingSeeder} exists in
 * Laravel, so there's nothing seeded here either — {@code index()} starts
 * out empty until something is written through {@code update()}.
 *
 * <p>{@code StoreSettings} (root package, implemented by {@code application.StoreSettingsImpl})
 * is a thin read-only published interface — same shape as {@code catalog.ProductCatalog}/
 * {@code floorplan.TableDirectory} — so {@code printing} can resolve the receipt
 * header (store name/address/phone) without depending on the raw key-value shape.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Settings")
package com.monokek.settings;
