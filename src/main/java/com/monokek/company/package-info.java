/**
 * Company / branch / workstation module (multi-site setup). Other modules
 * reference {@code Branch}/{@code Company} only by numeric id — never by JPA
 * association — to keep the module graph acyclic; see the ordering module's
 * {@code Order.branchId} for an example.
 *
 * <p><b>New functionality, not a port:</b> Laravel has the
 * {@code companies}/{@code branches}/{@code workstations} tables, bare
 * models and boilerplate API resources, but no controller for any of them —
 * the only reference anywhere in the source is a hardcoded
 * {@code Branch::first()} inside the (not yet ported) {@code ProductController::bulkImport}.
 * Every module ported so far depends on a real {@code branchId} existing,
 * so admin CRUD for this hierarchy is a genuine gap, not a speculative
 * feature. Renaming/deleting a company, branch or workstation stays plain
 * reference-data maintenance with no event; branch <em>creation</em> is the
 * one exception — {@code domain.event.BranchCreatedEvent} lets
 * {@code floorplan} provision a branch's default fallback table the moment
 * it exists, instead of leaving that to an admin to remember.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Company")
package com.monokek.company;
