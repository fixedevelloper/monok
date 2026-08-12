package com.monokek.settings;

/**
 * Read-only slice of the flat settings key-value store, for modules that only
 * care about "what does the receipt header say" — same shape as
 * {@code catalog.ProductCatalog}/{@code floorplan.TableDirectory}: a plain
 * public interface at the module's root package, no {@code @NamedInterface}
 * needed.
 */
public interface StoreSettings {

    StoreInfo current();

    record StoreInfo(String name, String address, String phone, String logoUrl) {
    }
}
