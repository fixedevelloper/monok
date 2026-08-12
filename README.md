# monokek-spring

Spring Modulith port of the [MonoKek](../MonoKek) Laravel restaurant POS
(monolith → monolith, not monolith → microservices), DDD-layered and
event-driven throughout. All twelve modules are now implemented end-to-end:
`identity` (auth/staff/RBAC), `catalog` (categories, products, modifiers),
`ordering` (the order lifecycle, reservations, waiter commissions),
`kitchen` (the KDS: stations and tickets), `cashier` (registers, cash
sessions, X/Z reports), `inventory` (ingredients, recipes, purchase orders,
stock deduction on sale), `printing` (printers + the print job queue),
`floorplan` (floors, tables, table transfer), `crm` (customers, coupons,
loyalty points), `company` (companies, branches, workstations), `settings`
(key/value settings + the audit trail) and `reporting` (dashboard KPIs,
sales-by-category, closing reports, admin analytics). `crm` and `company`
are new functionality rather than ports — see below for what that means and
why. `reporting` is architecturally different from the other eleven: it's a
read-only CQRS layer over the shared schema rather than an owner of any
table — see its own section below and `com.monokek.reporting.package-info`
for the full rationale, including two real Laravel queries that referenced
non-existent columns and would have thrown a SQL error.

## Architecture

- **Spring Modulith**, not a generic layered monolith: each top-level package
  under `com.monokek` is an application module declared via
  `package-info.java` + `@ApplicationModule`. `ModularityTests` (in
  `src/test`) runs `ApplicationModules.verify()` on every build to catch
  illegal cross-module access and dependency cycles.
- **Modules mirror the comment blocks in the Laravel migration file**
  (`database/migrations/..._base_migrations.php`): `identity`, `company`,
  `floorplan`, `catalog`, `crm`, `ordering`, `kitchen`, `cashier`,
  `inventory`, `printing`, `settings`, plus an `OPEN` `common` module for the
  response envelope and exceptions shared by everyone. `reporting` is the
  one deliberate exception — it owns no table of its own, so it isn't in
  this list; see its section further down for why.
- **Every module is DDD-layered**, same four packages everywhere:
  - `domain` — aggregate root entities (JPA-mapped directly, no separate
    persistence model) and **repository ports**: plain interfaces extending
    Spring Data's bare `Repository<T, ID>` marker, declaring only the
    methods the domain actually needs (no `flush()`, `getReferenceById()`,
    batch deletes — those are persistence-technology leakage, not domain
    concepts).
  - `infrastructure` — package-private `Jpa<Entity>Repository` interfaces
    that extend both the domain port and `JpaRepository`. Spring Data
    supplies the implementation; application code only ever depends on the
    port type from `domain`, never on `infrastructure`, which is dependency
    inversion in its plainest form.
  - `application` — use-case orchestration. Every module has one, including
    `settings`, whose sole job used to be its one listener class (see below)
    and which now also has real settings CRUD.
  - `web` — REST controllers + DTOs, the module's inbound adapter. Same
    coverage as `application`.
- **Modules that need real data from another module expose a published
  interface at their own root package** (not nested under `domain`), which
  Spring Modulith treats as open API by default — this is the DDD "open host
  service" pattern. `ordering` can't price an order without the catalog, so
  `catalog.ProductCatalog` exists; it can't occupy/free a table without
  `floorplan.TableDirectory`; it can't take a payment without
  `cashier.CashierFacade`; it can't book a walk-in customer without
  `crm.CustomerDirectory`; it can't show a waiter's name without
  `identity.UserDirectory`. `kitchen` can't render a ticket without
  `ordering.RoundKitchenView`, and writes a resolved round status back
  through `ordering.OrderRoundStatusUpdater` — both exposed by `ordering`
  itself, keeping that dependency one-directional (see the event-driven
  section below for why that direction specifically had to hold). Each
  interface is a handful of methods returning plain records
  (`ProductSnapshot`, `TableSnapshot`, ...) — never the module's actual JPA
  entities. `identity.CurrentUser` is the same idea in reverse:
  `AuthenticatedUser` (internal to `identity.infrastructure.security`)
  implements it so every module's controllers can bind
  `@AuthenticationPrincipal CurrentUser` without depending on identity's
  internals.
- **Cross-module foreign keys are plain `Long` fields, not JPA associations.**
  E.g. `ordering.Order.branchId` is a `Long`, not a `@ManyToOne` to
  `company.Branch`. Two reasons:
  1. The domain has a genuine cycle at the data level
     (`catalog.Category.kitchenStationId` → `kitchen` → `kitchen_tickets`
     reference `orders` → `ordering` depends on `catalog` for product ids).
     Spring Modulith rejects cyclic module dependencies; plain ids sidestep
     that entirely instead of forcing an artificial extra module.
  2. It's the idiomatic Spring Modulith style: modules should talk through
     public services/events, never by reaching into another module's
     entity graph. Within a module, real `@ManyToOne`/`@OneToMany`
     associations are used freely.
- **Schema fidelity over `ddl-auto`.** `src/main/resources/db/migration`
  contains hand-translated Flyway SQL (`V1__init_schema.sql`,
  `V2__seed_rbac.sql`) that mirrors the Laravel migrations table-for-table,
  including the generated column on `order_item_modifiers.total`. Hibernate
  runs with `ddl-auto: validate` — it never creates or alters tables.
- **RBAC is simplified from spatie/laravel-permission's polymorphic
  `model_has_roles`/`model_has_permissions` tables** (which support any
  model, but in this app only `User` ever uses them) **to plain
  `user_roles` / `user_permissions` / `role_permissions` join tables.**
  Same capabilities (roles + direct per-user permission overrides, exactly
  what `StaffController::updatePermissions` needs), simpler schema.
- **Sanctum tokens → stateless JWT.** `identity.infrastructure.security.JwtService`
  issues one token per login (`app.jwt.secret` / `JWT_SECRET` env var),
  verified by `JwtAuthenticationFilter` on every request. There is nothing to
  revoke server-side on logout — same limitation Sanctum's `plainTextToken`
  doesn't have, worth revisiting (e.g. a short-lived token + refresh token,
  or a denylist) before this goes to production.
- **Password hashes are bcrypt-compatible both ways.** Spring Security's
  `BCryptPasswordEncoder` reads `$2y$` hashes produced by Laravel's
  `Hash::make()` and produces `$2a$` hashes PHP's `password_verify()` reads
  back fine — no migration/conversion needed if you point this app at the
  existing `mono_kek` database.

## Event-driven: how modules actually talk to each other

Cross-module communication is domain events, published/consumed through
**Spring Modulith's event publication registry** (`spring-modulith-starter-jdbc`
in the POM — a JDBC-backed outbox, auto-initialized via
`spring.modulith.events.jdbc-schema-initialization.enabled=true`, no Flyway
migration needed for it). This replaces the Laravel `app/Events` +
`app/Listeners` pattern (`OrderCreated`, `TicketCreated`, etc.), with one
concrete advantage: publication is transactional and durable — if a listener
throws or the process dies mid-flight, the unfinished entry stays in the
registry and is retried, instead of the event silently vanishing the way an
in-memory `ApplicationListener` would.

**The reference flow, fully wired end to end:**

1. `identity.domain.User` is an aggregate root. State-changing methods
   (`User.register(...)`, `updateDirectPermissions(...)`, `revokeAccess(...)`)
   stage an event; `User` exposes it via `@DomainEvents` /
   `@AfterDomainEventPublication` (Spring Data's hook for entities that can't
   extend `AbstractAggregateRoot` because they already extend `Timestamps`).
   Spring Data publishes the event automatically the moment
   `userRepository.save(user)` succeeds — never before, so `id`/`uuid` are
   always populated by the time listeners see them.
2. Events that don't originate from a `save()` (logging in doesn't mutate
   anything) are published directly from the application service via
   `ApplicationEventPublisher` — `AuthService#login` does this for
   `UserLoggedInEvent`. Spring Modulith tracks it identically either way.
3. `identity.domain.event` is annotated `@NamedInterface("events")` — the one
   deliberate opening in an otherwise closed module, because event types are
   exactly what's supposed to cross module boundaries.
4. `settings.application.ActivityLogListener` consumes all four events
   (`UserLoggedInEvent`, `StaffCreatedEvent`, `StaffPermissionsUpdatedEvent`,
   `StaffAccessRevokedEvent`) via `@ApplicationModuleListener` and writes rows
   to `activity_logs` — a table that existed in the Laravel schema and had a
   model, but that nothing ever actually wrote to. `settings` never imports
   anything from `identity` except those four event types; `identity` doesn't
   know `settings` exists. (By now `settings` also consumes `ordering`'s and
   `cashier`'s events the same way — see below — so the audit trail spans
   four modules that don't know it exists.)

**`ordering` follows the identical shape, plus a second consumer module:**

- `Order` (also a `@DomainEvents` aggregate, for the same "already extends
  `Timestamps`" reason as `User`) stages `OrderCreatedEvent` on
  `Order.openForTable(...)`/`openReservation(...)` and
  `OrderStatusChangedEvent` on every `Order.changeStatus(...)` call
  (payment, service, cancellation, reservation confirmation) —
  `ordering.domain.event` is `@NamedInterface("events")` too.
- `kitchen.application.KitchenTicketListener` consumes
  `KitchenTicketRequestedEvent` (published directly by `OrderService`, not
  staged on the aggregate, because deciding *which* event to raise needs
  `catalog.ProductCatalog` to resolve a product's kitchen station — an
  aggregate must never call another module) and creates the
  `KitchenTicket` row. It also consumes `OrderStatusChangedEvent` to mark a
  completed order's tickets `served` — replacing the
  `$order->kitchenTickets()->update(...)` call Laravel makes straight across
  what would otherwise be a module boundary.
- `settings.application.ActivityLogListener` *also* consumes
  `OrderCreatedEvent`/`OrderStatusChangedEvent`, on top of identity's four
  events — the audit trail spans both modules for free.

**`cashier` follows `identity`'s shape exactly** (a `@DomainEvents` aggregate
with no cross-module reaction needed): `CashSession.open(...)`/`close(...)`
stage `CashSessionOpenedEvent`/`CashSessionClosedEvent`, and
`settings.application.ActivityLogListener` picks up both — a fifth and
sixth event type added to that one listener class without `cashier` or
`ActivityLogListener` needing to know about each other beyond the event
types themselves.

**`inventory` follows `kitchen`'s shape**, reacting to `ordering` rather
than being reacted to: `inventory.application.StockDeductionListener`
consumes `OrderStatusChangedEvent`, and on `newStatus == "paid"` reads the
sold products straight off `event.items()` — one `(productId, qty)` pair
per `OrderItem` — then deducts each sold product's recipe ingredients and
logs a `StockMovement`: the feature Laravel's `StockService::deductFromOrder`
was clearly written for but that nothing in the Laravel source ever actually
calls (`grep -r "StockService::" app/` turns up nothing). This used to be
two dependencies on `ordering` — the event, plus a narrow
`ordering.OrderLineItems` published interface called right after, just to
re-fetch the same order's items — until `OrderStatusChangedEvent` started
carrying the item list itself. One listener method, one import, no
callback: the minimum coupling an async reaction can have. Same
one-directional rule still holds regardless: `ordering` doesn't know
`inventory` exists, so there's no cycle to worry about, unlike `kitchen`'s
round-status write-back.

**`printing` follows `inventory`'s shape exactly** — another pure listener,
no write-back: `printing.application.PrintQueueListener` consumes both
`KitchenTicketRequestedEvent` (queues the kitchen printer's copy of a
ticket) and `OrderStatusChangedEvent` (queues a receipt when an order is
paid), replacing `ordering` calling `PrintService::queueRoundTickets`/
`queueFinalReceipt` directly. Both events needed a `branchId` field added
(they only carried `orderId`/`roundId` before) so `printing` can look up
*which* branch's printer to use — an example of extending a shared event
for a new consumer rather than adding a bespoke one, since every existing
consumer (`kitchen`, `settings`, `inventory`) just ignores the new field.

**`crm` publishes for the same reason `cashier` does — a new consumer for
`settings`, nothing more.** `Customer.earnPoints(...)`/`redeemPoints(...)`
stage `LoyaltyPointsChangedEvent`, and `settings.application.ActivityLogListener`
picks it up. One wrinkle: this event has no staff user attached (a loyalty
change is about a *customer*, not identified by `activity_logs.user_id`,
which is an FK to `users`), so the listener records it with `user_id = null`
and names the customer in the action text instead of forcing a fake user
reference — a small reminder that "log every event the same way" isn't
always literally true; the shared `record(userId, action)` helper already
supported a nullable `userId` (see `StaffAccessRevokedEvent`'s handling)
so no change was needed there.

`ModularityTests#verifiesModularStructure` asserts every one of these
dependencies (`settings`/`kitchen`/`inventory`/`printing` → the producing
module's `events` named interface, or root-level published interface) is
legal on every build.

**`kitchen` is where events stop being the automatic answer.** A kitchen
staff member updating a ticket's status needs the *result* — the recomputed
round status — in the same HTTP response (Laravel returns `round_status`
inline). Publishing an event from `kitchen` and having `ordering` react to
it asynchronously was the first thing tried here, and it does work... right
up until `ModularityTests` fails with a **cycle**: `kitchen` already depends
on `ordering` (for ticket creation), so `ordering` depending back on
`kitchen` (to consume that event) makes the module graph cyclic, which
Spring Modulith's `verify()` rejects outright — regardless of which
sub-packages or named interfaces are involved, the check operates at the
module level. The fix wasn't to route around the checker; it was to notice
that this interaction was never actually a good fit for an event in the
first place. `kitchen.application.KitchenTicketService#updateTicketStatus`
now calls `ordering.OrderRoundStatusUpdater` **synchronously** instead —
same direction as the existing `kitchen → ordering` dependency, so no cycle,
and it matches what the use case actually needs: an answer before the
response goes out, not an eventual one.

**Audit: every remaining synchronous cross-module call was checked against
"could this be an event instead?" and kept only where the answer is no.**
`catalog.ProductCatalog` (pricing an order line), `floorplan.TableDirectory`
(resolving a table's branch, and flipping it occupied/free), `crm.CustomerDirectory`
(find-or-create by phone), `cashier.CashierFacade` (recording a payment and
getting `changeDue` back) and `identity.UserDirectory`/`CurrentUser` all stay
direct method calls, not listeners, because each one either returns a value
the caller needs to finish computing its own response in the same request
(a price, a `changeDue`, a newly created customer's id), or is a write whose
result the caller's own response must reflect immediately — turning any of
these into a fire-and-forget event would mean the HTTP response either goes
out with stale/missing data or the caller blocks on request-response over
the event bus anyway, which is the same coupling with extra ceremony.
`ordering.OrderRoundStatusUpdater` (`kitchen` → `ordering`) stays direct for
the same reason *and* because the reverse can't legally exist anyway —
`ordering` already depends on `kitchen`'s ticket-request flow the other way
via events, so an event-based write-back here would be the exact cycle
described above, not just a style choice. The one dependency that *was*
purely a callback with no request needing an immediate answer —
`inventory` re-fetching `ordering`'s sold items right after being notified
about them — is the `OrderLineItems` removal described just above: same
information, delivered on the event that was already crossing that boundary,
instead of a second interface making the same trip.

**When you port the next module**, follow the same shape: the aggregate
stages events for its own state changes (or the application service
publishes directly, when the event doesn't correspond to a `save()`), other
modules react via `@ApplicationModuleListener` against a `@NamedInterface`
events package — never a direct method call across modules for anything
that isn't itself part of completing the current use case (compare: freeing
a table on payment is a synchronous call through `TableDirectory`, because
the API response should reflect it immediately; creating a kitchen ticket is
an event, because the waiter's response doesn't need to wait on it).

## What's implemented end-to-end

`identity` module — mirrors `AuthController` + `StaffController`:

| Laravel route | Spring endpoint |
|---|---|
| `POST /api/login` | `AuthController#login` |
| `GET /api/me` | `AuthController#me` |
| `POST /api/auth/verify-pin` | `AuthController#verifyPin` |
| `POST /api/auth/update-pin` | `AuthController#updatePin` |
| `POST /api/auth/update-password` | `AuthController#updatePassword` |
| `POST /api/logout` | `AuthController#logout` (no-op: stateless JWT) |
| `GET /api/admin/staff` | `StaffController#index` |
| `POST /api/admin/staff` | `StaffController#store` |
| `PUT/PATCH /api/admin/staff/{uuid}` | `StaffController#update` |
| `DELETE /api/admin/staff/{uuid}` | `StaffController#destroy` (soft delete) |
| `GET /api/admin/staff/roles` | `StaffController#roles` |
| `GET /api/admin/staff/permissions/list` | `StaffController#permissions` |
| `PUT /api/admin/staff/{uuid}/permissions` | `StaffController#updatePermissions` |

`/api/admin/**` requires `ROLE_ADMIN` or `ROLE_MANAGER`, matching the
Laravel `role:admin|manager` middleware group.

`catalog` module — mirrors `ProductController` (Pos) and `ModifierController`
(Admin), both real and genuinely buggy, plus new category CRUD:

| Laravel route | Spring endpoint |
|---|---|
| `GET /api/pos/products`, `/api/admin/products` | `ProductController#index` (both, same as Laravel's duplication) |
| `GET /api/admin/products/{id}` | `ProductController#show` |
| `POST/PATCH/DELETE /api/admin/products` | `ProductController` CRUD |
| `POST /api/admin/products/bulk-import` | `ProductController#bulkImport` |
| `POST /api/admin/products/{id}/sync-modifiers` | `ProductController#syncModifiers` |
| `GET /api/pos/categories` | `CategoryController#posIndex` — **moved**, see below |
| `GET/POST/PUT/DELETE /api/admin/categories` | `CategoryController` — **GET is new, POST/PUT/DELETE are new** |
| `GET/POST/PUT/DELETE /api/admin/modifiers` | `ModifierController` CRUD |
| `POST /api/admin/modifiers/{id}/items` | `ModifierController#addItem` |
| `DELETE /api/admin/modifier-items/{id}` | `ModifierController#destroyItem` |

Category create/update/delete is new functionality (user-confirmed before
building it, same process as `crm`/`company`): Laravel has no route to
manage a category directly, only an implicit `Category::firstOrCreate`
buried inside `bulkImport`. `GET /api/pos/categories` is also new as a
*route* but not as behavior — it's Laravel's own `ProductController::categories()`
method, just exposed outside the `role:admin|manager` gate Laravel put it
behind at `GET /admin/categories`: a waiter can't render the POS product
menu without a category list, and `pos/products` sitting right next to it
only ever required plain auth. Not ported: `updateStock`/`toggleStatus` on
`ProductController` (real methods, no route reaches either in
`routes/api.php`); actual image upload (product images are a plain
URL/path string field — no multipart handling or disk storage layer
exists here); `ProductVariant` (scaffolded domain-only, genuinely dead in
Laravel — no route, controller, resource, or seeder references it, unlike
every other gap in this module).

`ordering` module — mirrors `OrderController` (Pos), `ReservationController`
and `CommissionController` (Admin):

| Laravel route | Spring endpoint |
|---|---|
| `POST /api/pos/orders/send-round` | `OrderController#sendRound` |
| `POST /api/pos/orders/{uuid}/finalize` | `OrderController#finalizePayment` |
| `GET /api/pos/orders` | `OrderController#index` |
| `GET /api/pos/orders/history` | `OrderController#history` |
| `GET /api/admin/orders/history` | `OrderController#historyAdmin` |
| `GET /api/pos/waiter/orders` | `OrderController#waiterOrders` |
| `GET /api/pos/tables/{id}/active-order` | `OrderController#getActiveOrder` |
| `POST /api/pos/orders/{id}/serve` | `OrderController#markAsServed` |
| `PATCH /api/pos/rounds/{id}/items/{id}` | `OrderController#updateRoundItemQty` |
| `POST /api/pos/rounds/{id}/items` | `OrderController#addItemToRound` |
| `GET/POST/PUT/DELETE /api/admin/reservations` | `ReservationController` |
| `POST /api/admin/reservations/{orderId}/pay` | `ReservationController#pay` |
| `GET /api/admin/commissions`, `/stats`, `POST /settle/{id}` | `CommissionController` |

Two deliberate fixes over the Laravel source (documented at the call site,
not silent): reservation item prices are resolved server-side through
`ProductCatalog`, matching the discipline `sendRound` already applies —
Laravel trusted the client-submitted price only in the reservation path;
and reservation line items now attach through a real `OrderRound`, since
Laravel's `$order->items()->create(...)` targets a read-only
`hasManyThrough` relation that can't actually persist anything.

Known simplifications, all called out in the code: no pessimistic row
locking on table/order/round (Laravel's `lockForUpdate()`); `index`/
`historyAdmin` search matches on order `reference` only, not table or
waiter name (a real join across the module boundary); `history` merges
"paid in this session" and "active today" in memory, capped at 50, instead
of true cursor pagination; reservations drop the `search` filter and
hardcode `branch_id = 1`, exactly like the Laravel version does.

`kitchen` module — mirrors `TicketController` (the KDS):

| Laravel route | Spring endpoint |
|---|---|
| `GET /api/kitchen/stations` | `TicketController#getStations` |
| `GET /api/kitchen/tickets?station_id=` | `TicketController#index` |
| `PATCH /api/kitchen/tickets/{id}/status` | `TicketController#updateStatus` |
| `PATCH /api/kitchen/tickets-direct/{id}/status` | `TicketController#updateStatusDirect` |

Both status routes now call the same `KitchenTicketService#updateTicketStatus`
(see the event-driven section above for why the two Laravel methods, which
had quietly diverged, were consolidated into one). `TicketController::updateItemStatus`
was dropped: its route in `routes/api.php` points at a controller method that
doesn't exist in the Laravel source, so there was nothing to port. Ticket
responses show every item of the round, not filtered to the ticket's own
station — a faithful port of what `KitchenTicketResource` actually reads
(`$this->round->items`), not the separate `KitchenTicket::items()` model
relation, which filters on an `order_items.station_id` column that was never
part of the schema and would error if it ever ran.

`cashier` module — mirrors `CashController`:

| Laravel route | Spring endpoint |
|---|---|
| `GET /api/cash/registers` | (inline closure in Laravel) → `CashController#listRegisters` |
| `POST /api/cash/registers` | `CashController#storeRegister` |
| `GET /api/cash/status` | `CashController#status` |
| `POST /api/cash/open` | `CashController#open` |
| `GET /api/cash/current-summary` | `CashController#currentSummary` |
| `POST /api/cash/close` | `CashController#close` |

One deliberate omission, not a silent drop: Laravel's `close()` also builds
a "sold items summary" by joining `order_items` → `order_rounds` → `orders`
→ `products` (a report spanning `ordering` and `catalog`) and queues a
receipt-printer job. Porting either would need `cashier` to depend on
`ordering`/`printing` — but `ordering` already depends on `cashier` (through
`CashierFacade`, to record payments), and `printing` still has no
application layer to call. Adding either dependency would either cycle or
call into nothing; both are left as the natural next step for whoever ports
`printing`, or a small denormalized field passed through `CashierFacade`
if the report is wanted sooner. The payment-method breakdown and totals —
the parts that are 100% `cashier`'s own data — are fully ported.

`inventory` module — mirrors `IngredientController`, `RecipeController` and
`PurchaseOrderController`:

| Laravel route | Spring endpoint |
|---|---|
| `GET /api/admin/ingredients` | `IngredientController#index` |
| `POST /api/admin/ingredients` | `IngredientController#store` |
| `GET /api/admin/units` | `IngredientController#units` |
| `GET /api/admin/stock-movements` | `IngredientController#mouvements` |
| `POST /api/admin/ingredients/{id}/adjust` | `IngredientController#adjustStock` |
| `GET/POST /api/admin/products/{id}/recipe` | `RecipeController#show`/`store` |
| `POST /api/admin/purchase-orders` | `PurchaseOrderController#store` |

Not ported, because there's nothing there to port: `IngredientController::updateAlert`
exists but no route in `routes/api.php` calls it; `Route::apiResource('ingredients', ...)`
and `Route::apiResource('purchase-orders', ...)` each generate `show`/`update`/`destroy`
(and `index` for purchase-orders) routes whose controller methods were never
written, so those routes 500 in the Laravel app today. Also skipped:
`InventoryController` (the *other*, product-level stock controller, routed at
`/api/admin/inventory`) — it references `App\Models\Stock` and
`App\Models\StockLog`, neither of which exist anywhere in the Laravel
codebase; every one of its actions would throw a class-not-found error. The
working, schema-backed stock system is the ingredient/recipe one this module
ports; product-level `stock_count`/`alert_stock` (real columns on
`catalog.Product`) would be a small `catalog` feature if it's wanted later,
not an `inventory` one.

`printing` module — mirrors `PrinterController` fully, plus the inline
print-queue closures from `routes/api.php`:

| Laravel route | Spring endpoint |
|---|---|
| `GET /api/admin/settings/printers` | `PrinterController#index` |
| `POST /api/admin/settings/printers` | `PrinterController#store` |
| `GET /api/admin/settings/printers/{id}` | `PrinterController#show` |
| `PUT/PATCH /api/admin/settings/printers/{id}` | `PrinterController#update` |
| `DELETE /api/admin/settings/printers/{id}` | `PrinterController#destroy` |
| `GET /api/admin/settings/printers/{id}/test` | `PrinterController#testConnection` |
| `GET /api/print-queue/pending` | inline closure → `PrintQueueController#pending` |
| `POST /api/print-queue/{id}/mark-success` | inline closure → `#markSuccess` |
| `POST /api/print-queue/{id}/mark-failed` | inline closure → `#markFailed` |

`testConnection` is a plain TCP reachability probe (`java.net.Socket`, same
as Laravel's `fsockopen`) — no ESC/POS protocol involved, so it ports
directly. What's *not* ported is rendering a receipt: `PrintManagerService`,
`PrintQueueController::dispatchNetwork` and the `ProcessNetworkPrintJob`
queue worker all push raw ESC/POS bytes over a socket using
`mike42/escpos-php`, which has no equivalent dependency here. Laravel's own
`pending`/`mark-success`/`mark-failed` trio suggests the actual production
design is an *external* LAN print worker that polls the server, prints
locally, and reports back — that path needs no printing library on the
server at all, and is the one fully ported. `job.content` is therefore a
lightweight placeholder (ids + a timestamp) rather than Laravel's
deeply-loaded receipt payload, since nothing here renders it yet.

`floorplan` module — mirrors `TableController`, a real and genuinely buggy
controller (unlike `crm`/`company`, which had almost nothing):

| Laravel route | Spring endpoint |
|---|---|
| `GET/POST /api/pos/floors`, `PUT /api/pos/floors/{id}` | `FloorController` |
| `GET /api/pos/tables` | `TableController#posIndex` |
| `PATCH /api/pos/tables/{id}/status` | `TableController#updateStatus` |
| `GET/POST/PUT/DELETE /api/admin/tables` | `TableController` (admin CRUD) |
| `POST /api/pos/tables/transfer` | `ordering.web.OrderController#transferTable` |

Three deviations, each real and each documented at its call site (see
`floorplan`'s package-info for the full reasoning): table status is
validated against `free`/`occupied`/`billing`/`maintenance`, not Laravel's
`available`/`occupied`/`billing`/`maintenance` — `available` conflicts with
the seeded/default status and with what `ordering.TableDirectory` already
writes, so one table could never be set back to its own default through
this endpoint; `floors()` drops the `currentOrder`/`total` field it
computed in Laravel, since building it here would need `floorplan` to
depend on `ordering`, which already depends on `floorplan` — the same data
is one call away through `ordering`'s existing `GET /pos/tables/{id}/active-order`;
and `transfer` — a real, useful feature whose Laravel implementation calls
a `$fromTable->activeOrder()` method that exists nowhere in the codebase
(dead code, would 500) — is implemented correctly, but lives in
`ordering.web.OrderController`/`OrderService#transferTable` rather than
here, for the same one-directional-dependency reason as `floors()`, since
transferring mutates `Order.tableId`. The Laravel admin route also had
`apiResource`'s `PATCH /admin/tables/{id}` (→ `update`) silently shadowed
by an explicit `Route::patch('tables/{table}', 'updateStatus')` registered
on the identical path — resolved by giving them different verbs (`PUT` for
a full edit, `PATCH` for status-only) instead of picking a winner.
`show`/`destroy` for `/admin/tables/{id}` are implemented for real, too:
`apiResource` routed to them, but the Laravel controller never defined
either method.

`crm` module — **new functionality, not a port** (see `crm`'s package-info
for the full story: Laravel has a `Customer`/`Coupon`/`LoyaltyTransaction`
schema and boilerplate API resources, but no route or controller for any of
it beyond one inline `Customer::firstOrCreate`, already covered by
`CustomerDirectory`):

| Endpoint | What it does |
|---|---|
| `GET/POST/PUT/DELETE /api/admin/customers` | Admin customer CRUD |
| `GET /api/admin/coupons`, `POST /api/admin/coupons` | List / create coupons |
| `GET /api/admin/coupons/validate?code=` | Check a code is real and not expired |
| `GET /api/admin/customers/{id}/loyalty` | Loyalty ledger for a customer |
| `POST .../loyalty/earn`, `POST .../loyalty/redeem` | Credit/debit points |

`Customer.redeemPoints(...)` enforces the one invariant the schema implies
but nothing enforced before: a balance can't go negative. Coupons have no
"redeemed" flag in the schema, so `validate` only checks existence and
expiry — a valid coupon stays reusable, matching what the table actually
supports rather than inventing a tracking column.

`company` module — **also new functionality, not a port** (Laravel has the
`companies`/`branches`/`workstations` tables and bare models, but zero
controllers — the only reference anywhere in the source is a hardcoded
`Branch::first()` inside the not-yet-ported `ProductController::bulkImport`):

| Endpoint | What it does |
|---|---|
| `GET/POST/PUT/DELETE /api/admin/companies` | Admin company CRUD |
| `GET/POST/PUT/DELETE /api/admin/branches` | Admin branch CRUD (`?companyId=` filter) |
| `GET/POST/PUT/DELETE /api/admin/workstations` | Admin workstation CRUD (`?branchId=` filter) |

No domain events here, unlike `crm`'s `Customer`: renaming a branch or
adding a workstation isn't a state machine or a balance with an invariant
to protect, just reference-data maintenance — plain `@Setter`-based entities
were the right amount of ceremony, not `@DomainEvents`. Worth building at
all because every module ported so far already depends on a real `branchId`
existing (`ordering`, `catalog`, `kitchen`, `cashier`, `printing`); before
this, there was no way to create one through the API — only Flyway's seed
data.

`settings` module — mirrors `SettingsController`:

| Laravel route | Spring endpoint |
|---|---|
| `GET /api/admin/settings` | `SettingsController#index` |
| `POST /api/admin/settings` | `SettingsController#update` |

The interesting part isn't the CRUD, it's the security rule: despite living
under `/admin`, Laravel registered `GET /admin/settings` *outside* its
`role:admin|manager` group — any authenticated user can read settings, only
writing them requires the admin/manager role. `SecurityConfig`'s path-prefix
rule for `/api/admin/**` couldn't express that exception on its own, so a
more specific `GET /api/admin/settings` rule was added ahead of it (Spring
Security evaluates `authorizeHttpRequests` rules in order — first match
wins). `Setting::value` is Eloquent-cast `json` even though the column is
plain `TEXT`; `SettingsService` replicates that with Jackson at the service
boundary rather than teaching the entity about JSON. Not ported:
`SettingsController::printers`/`storePrint`/`edtPrint` — real methods, but
no route reaches any of them (printer CRUD is `PrinterController`'s job,
already ported in `printing`), and `edtPrint` is additionally just a
copy-paste bug that calls `Printer::create()` where an update was clearly
intended. No `SettingSeeder` exists in Laravel either, so `index()` starts
out genuinely empty here too, until something is written through `update()`.

`reporting` module — mirrors `ReportController`, but is architecturally
unlike every other module described above:

| Laravel route | Spring endpoint |
|---|---|
| `GET /api/admin/reports/dashboard` | `ReportController#dashboardStats` |
| `GET /api/admin/reports/categories` | `ReportController#salesByCategory` (`?startDate=&endDate=`, defaults to the current calendar month) |
| `GET /api/admin/reports/closing` | `ReportController#closingReport` |
| `GET /api/admin/analytics` | `ReportController#getAnalytics` (`?startDate=&endDate=`, defaults to the current calendar month) |

Every other module owns its tables and exposes a narrow published interface
for whatever another module needs from it. That pattern doesn't fit a report
that SUM/GROUP BY/JOINs across `orders`, `order_items`, `order_rounds`,
`payments`, `payment_methods`, `products`, `categories` and `users` in one
query — composing that from narrow cross-module method calls would mean
pulling full result sets into Java and aggregating by hand. `reporting` has
no `domain` package; `application.ReportingService` runs read-only native
SQL straight against the shared schema via `NamedParameterJdbcTemplate`,
addressing tables by name rather than importing other modules' JPA entities.
Spring Modulith's cycle detection is based on Java type references, so it
can't see a table read the way it sees a type import — from `ModularityTests`'s
point of view `reporting` has zero dependency on `ordering`/`cashier`/
`catalog` (it does still import `identity.CurrentUser` for `closingReport`'s
"whose shift is this" scoping, so that one dependency is real and
one-directional like everywhere else). This is a deliberate, one-time
exception for a pure CQRS read side with no writes and no domain invariants
at stake — not a precedent for regular business logic, where the
data-through-your-own-domain rule still applies everywhere else in this
codebase.

Two of Laravel's four methods would throw a SQL error as originally
written, and were fixed rather than replicated:

- `dashboardStats()` selected `orders.total_amount` and
  `orders.payment_method` — neither column exists (`orders.total` is the
  real revenue column; payment method is only known per-*payment*, via
  `payments.payment_method_id → payment_methods`). Rebuilt the "cash vs
  mobile" split as a generic per-method breakdown instead of hardcoding
  Laravel's `cash`/`momo`/`orange` bucket names, since this schema's seed
  data has no `orange` row and hardcoded buckets would silently drop
  anything else. It also selected `order_items.quantity`, which doesn't
  exist (`qty` does).
- `salesByCategory()` selected `order_items.subtotal` — doesn't exist
  (`order_items.total` does).
- `closingReport()` had the same `total_amount`/`payment_method` problem as
  `dashboardStats()`. Rebuilt around the same `payments` join, scoped to the
  current user's own completed orders for today — Laravel's evident intent
  was a waiter's/cashier's personal end-of-day summary, distinct from
  `cashier.CashSession`'s Z-report, which is scoped by cash session rather
  than by user-and-calendar-day.
- `getAnalytics()` was already almost entirely correct against the real
  schema — ported with only the column names double-checked, no structural
  changes needed. Its hardcoded `food_cost => 32` is carried over as-is;
  Laravel's own comment already flagged it as a stub for later.

## What's left

Every table in the schema has an aggregate root; every module with Laravel
routes worth porting — including `ReportController`, the last one — now has
a full `application`/`web` layer. Nothing from the original Laravel route
list is left deliberately unported except what's called out in
"Explicitly deferred" below.

## Explicitly deferred (per project scope)

- Realtime (Laravel Reverb / WebSocket broadcasting of `OrderCreated`,
  `TicketCreated`, etc.) — `ordering`/`kitchen` now publish the real domain
  events (`OrderCreatedEvent`, `OrderStatusChangedEvent`,
  `KitchenTicketRequestedEvent`), so bridging them to a WebSocket/STOMP topic
  is now a thin adapter subscribing to those same events, not a redesign.
- Rendering an actual receipt (ESC/POS over a socket) — see the `printing`
  section above for exactly what is and isn't ported and why.
  `OrderController#reprint` was dropped for the same reason: nothing here
  can render what it would re-print.
- Excel export (`maatwebsite/excel`) and PDF (`barryvdh/laravel-dompdf`)
  equivalents for reports.
- Two routes that are **public with no auth in the Laravel app itself**
  (`/api/print-queue/*`, `/api/sales/*/reprint`) were carried over as
  `permitAll()` in `SecurityConfig` for fidelity — worth revisiting since
  that looks like an oversight in the source app, not an intentional design.

## Running it

```bash
# against the existing mono_kek MySQL database (same one MonoKek/.env points at)
export DB_HOST=127.0.0.1 DB_PORT=3306 DB_DATABASE=mono_kek DB_USERNAME=base DB_PASSWORD=ba001133
export JWT_SECRET=$(openssl rand -base64 32)

mvn spring-boot:run
```

Flyway runs automatically on startup and seeds (`V2__seed_rbac.sql`) the same
roles/permissions/users as Laravel's `DatabaseSeeder`:

| email | password | role |
|---|---|---|
| admin@resto.com | password | admin |
| cashier@resto.com | password | cashier |
| kitchen@resto.com | password | kitchen |
| waiter@resto.com | password | waiter |

```bash
curl -s localhost:8080/api/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@resto.com","password":"password"}'
```

Logging in publishes `UserLoggedInEvent`; check the `activity_logs` table
afterwards to see `ActivityLogListener` having reacted to it asynchronously.

Create a category and a product — Flyway already seeds some (`ProductSeeder`'s
equivalent), so `tableId`/`productId` `1` below work out of the box, but here's
how a fresh one gets created:

```bash
curl -s -X POST localhost:8080/api/admin/categories -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"branchId": 1, "name": "Grillades", "icon": "Flame"}'
curl -s -X POST localhost:8080/api/admin/products -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"categoryId": 1, "name": "Poisson Braisé", "price": 5000, "type": "storable"}'
```

Sending a round (against a seeded table/product) does the same for
`OrderCreatedEvent`, and also lands a row in `kitchen_tickets` if the
product's category has a `kitchen_station_id`:

```bash
curl -s localhost:8080/api/pos/orders/send-round -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"tableId": 1, "items": [{"productId": 1, "qty": 2}]}'
```

Then, as kitchen staff, list what landed on a station and move a ticket
along — the response's `round_status` comes straight back from `ordering`
through `OrderRoundStatusUpdater`, synchronously:

```bash
curl -s "localhost:8080/api/kitchen/tickets?stationId=1" -H "Authorization: Bearer $TOKEN"
curl -s -X PATCH localhost:8080/api/kitchen/tickets/1/status -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"status": "ready"}'
```

A cashier needs an open session before `finalize` will accept a payment —
open one, take the payment, then close out the shift (`activity_logs` gets
both the open and the close):

```bash
curl -s localhost:8080/api/cash/open -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"registerId": 1, "openingAmount": 20000}'
curl -s localhost:8080/api/pos/orders/$ORDER_UUID/finalize -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"paymentMethod": "cash", "amountReceived": 5000}'
curl -s -X POST localhost:8080/api/cash/close -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"closingAmount": 25000}'
```

Give a product a recipe, then watch its ingredients get deducted automatically
the moment an order containing it is paid (`GET .../stock-movements` afterwards
shows a `"reason": "Vente #<orderId>"` row per ingredient, with no explicit
inventory call in the payment flow above):

```bash
curl -s -X POST localhost:8080/api/admin/products/1/recipe -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"items": [{"ingredientId": 1, "qty": 0.2}]}'
```

Register a printer for the branch, then send a round again — a job shows up
in the (unauthenticated, by design) print queue for a worker to pick up:

```bash
curl -s -X POST localhost:8080/api/admin/settings/printers -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"branchId": 1, "name": "Cuisine", "type": "escpos", "connection": "network", "ip": "192.168.1.50", "location": "kitchen"}'
curl -s localhost:8080/api/print-queue/pending
curl -s -X POST localhost:8080/api/print-queue/1/mark-success
```

Credit a customer some loyalty points, then try to redeem more than their
balance — `activity_logs` gets the credit with `user_id = null` either way:

```bash
curl -s -X POST localhost:8080/api/admin/customers -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name": "Awa", "phone": "677000000"}'
curl -s -X POST localhost:8080/api/admin/customers/1/loyalty/earn -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"points": 50}'
curl -s -X POST localhost:8080/api/admin/customers/1/loyalty/redeem -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"points": 999}'   # 409: solde de points insuffisant
```

Set up a new site end to end — company, then branch, then workstation —
the same hierarchy Flyway's seed data creates, now creatable through the API:

```bash
curl -s -X POST localhost:8080/api/admin/companies -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name": "RestoPro Group", "phone": "+237000000000", "email": "contact@restopro.com"}'
curl -s -X POST localhost:8080/api/admin/branches -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"companyId": 1, "name": "Douala Centre", "address": "Akwa, Douala"}'
curl -s -X POST localhost:8080/api/admin/workstations -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"branchId": 1, "name": "Caisse 1", "type": "pos"}'
```

Add a floor and a table to that branch, seat a walk-in, then move them to
another free table — the transfer endpoint lives in `ordering`, not
`floorplan`, but the URL is exactly where Laravel put it:

```bash
curl -s -X POST localhost:8080/api/pos/floors -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"branchId": 1, "name": "Terrasse"}'
curl -s -X POST localhost:8080/api/admin/tables -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"floorId": 1, "name": "T-01", "seats": 4}'
curl -s -X POST localhost:8080/api/admin/tables -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"floorId": 1, "name": "T-02", "seats": 2}'
curl -s -X POST localhost:8080/api/pos/tables/transfer -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"fromTableId": 1, "toTableId": 2}'
```

Set the store's display name, then read it back as the *waiter* seeded
above, not an admin — proving `GET` really is open to any authenticated
user while `POST` isn't:

```bash
curl -s -X POST localhost:8080/api/admin/settings -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"settings": {"store_name": "RestoPro Douala", "store_phone": "+237000000000"}}'
curl -s localhost:8080/api/admin/settings -H "Authorization: Bearer $WAITER_TOKEN"
```

Pull today's dashboard, last month's category split, an admin analytics
window, and the current admin's own closing report — none of these touch
any table `reporting` owns, because it doesn't own any:

```bash
curl -s localhost:8080/api/admin/reports/dashboard -H "Authorization: Bearer $TOKEN"
curl -s "localhost:8080/api/admin/reports/categories?startDate=2026-07-01&endDate=2026-07-31" -H "Authorization: Bearer $TOKEN"
curl -s "localhost:8080/api/admin/analytics?startDate=2026-07-01&endDate=2026-07-31" -H "Authorization: Bearer $TOKEN"
curl -s localhost:8080/api/admin/reports/closing -H "Authorization: Bearer $TOKEN"
```

## Verifying the module structure

```bash
mvn test -Dtest=ModularityTests
```

This runs `ApplicationModules.of(MonokekApplication.class).verify()` (no
database needed — it's static analysis of the compiled classes) and dumps a
PlantUML diagram per module under `target/spring-modulith-docs/`.
