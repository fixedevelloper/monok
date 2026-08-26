/**
 * Product catalog: categories, products, variants and modifiers (options).
 * Implemented end-to-end, porting the real (and real-buggy)
 * {@code App\Http\Controllers\Api\Pos\ProductController} and
 * {@code App\Http\Controllers\Api\Admin\ModifierController}. Exposes
 * {@link ProductCatalog} at its root package for {@code ordering}/
 * {@code inventory} — see those modules' package-info for the one-directional
 * dependency this keeps. {@link ProductStockReceiver}, at the same root
 * package, is a second and deliberately separate published interface for
 * {@code inventory}: unlike the read-only {@code ProductCatalog}, it lets a
 * received purchase order line increment a storable product's stock —
 * implemented by the same {@code ProductService} that backs the admin
 * "Ajuster le stock" modal, since receiving a purchase and a manual stock
 * adjustment are the same underlying operation (traced movement + stock
 * update), not two separate concerns.
 *
 * <p>Deliberate deviations, each documented at its call site too:
 * <ul>
 *   <li><b>Category CRUD is new functionality.</b> Laravel has no route to
 *       create/update/delete a category directly — only an implicit
 *       {@code Category::firstOrCreate} inside {@code ProductController::bulkImport}.
 *       Since {@code category_id} is required to create a product, this was
 *       a real gap, not a speculative feature (user-confirmed before
 *       building it, same as {@code crm}/{@code company}).</li>
 *   <li>{@code GET /api/pos/categories} moved out from under the
 *       {@code role:admin|manager} gate Laravel put {@code categories()}
 *       behind at {@code GET /admin/categories} — a waiter needs the
 *       category list to render the POS menu at all, and {@code pos/products}
 *       right next to it only requires plain auth.</li>
 *   <li>{@code updateStock}/{@code toggleStatus} on {@code ProductController}
 *       are real methods with no route anywhere in {@code routes/api.php} —
 *       not ported, nothing to reach them.</li>
 *   <li>Product images are a plain URL/path string field, not a multipart
 *       upload — no file storage/serving layer exists in this project.</li>
 *   <li>{@code bulkImport} requires an explicit {@code branchId} instead of
 *       Laravel's hardcoded {@code Branch::first()} — same fix as
 *       {@code floorplan}'s {@code storeFloor}.</li>
 *   <li>{@code ProductVariant} stays domain-only (already scaffolded, no
 *       application/web layer): no route, controller, resource, or seeder
 *       anywhere in the Laravel source references it — it's a dead/unused
 *       model, unlike the other gaps in this module.</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(displayName = "Catalog")
package com.monokek.catalog;
