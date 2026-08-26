package com.monokek.catalog.web;

import com.monokek.catalog.application.ProductService;
import com.monokek.catalog.web.dto.*;
import com.monokek.identity.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Port of {@code App\Http\Controllers\Api\Pos\ProductController}. Not
 * ported: {@code updateStock}/{@code toggleStatus} (no route reaches either
 * in the Laravel source). {@code image} is a plain URL/path string in
 * {@code CreateProductRequest}/{@code UpdateProductRequest}, not a
 * multipart file upload — no file storage/serving layer was ported.
 */
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/pos/products")
    public List<ProductDto> posIndex(
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String search,
            @AuthenticationPrincipal CurrentUser principal) {
        return productService.index(categoryId, search, principal.branchId());
    }

    @GetMapping("/api/admin/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<ProductDto> index(
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String search,
            @AuthenticationPrincipal CurrentUser principal) {
        return productService.index(categoryId, search, principal.branchId());
    }

    /** Backs the admin menu's infinite-scroll grid — one independently-paginated request per
     * branch section. {@code branchId} is client-supplied (unlike {@link #index}'s implicit
     * "my own branch" scoping) since an unscoped owner picks which branch's section it's
     * loading more of; a branch-scoped manager can't override it with someone else's branch. */
    @GetMapping("/api/admin/products/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Page<ProductDto> indexPage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal CurrentUser principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Long effectiveBranchId = principal.branchId() != null ? principal.branchId() : branchId;
        return productService.indexPage(categoryId, search, effectiveBranchId, pageable);
    }

    @GetMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto show(@PathVariable Long id) {
        return productService.show(id);
    }

    @PostMapping("/api/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_products'))")
    public ProductDto store(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_products'))")
    public ProductDto update(@PathVariable Long id, @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_products'))")
    public Map<String, String> destroy(@PathVariable Long id) {
        productService.delete(id);
        return Map.of("message", "Produit supprimé");
    }

    @PostMapping("/api/admin/products/bulk-import")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_products'))")
    public Map<String, String> bulkImport(@Valid @RequestBody BulkImportRequest request) {
        int count = productService.bulkImport(request);
        return Map.of("message", "Succès ! %d produits ont été importés.".formatted(count));
    }

    @PostMapping("/api/admin/products/{id}/sync-modifiers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_products'))")
    public Map<String, String> syncModifiers(@PathVariable Long id, @RequestBody SyncModifiersRequest request) {
        productService.syncModifiers(id, request.modifierIds());
        return Map.of("message", "Modificateurs synchronisés");
    }

    /** The only way left to move a product's {@code stockCount} — the edit form's old direct field
     * is gone (see {@code UpdateProductRequest}). Same guard as {@code IngredientController#adjustStock}:
     * an Admin always can, a Manager only with the {@code manage_stock} authority. */
    @PostMapping("/api/admin/products/{id}/stock/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_stock'))")
    public ProductDto adjustStock(
            @PathVariable Long id, @Valid @RequestBody AdjustProductStockRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return productService.adjustStock(id, request, principal.id());
    }

    @GetMapping("/api/admin/products/{id}/stock/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_stock'))")
    public List<ProductStockMovementDto> stockMovements(@PathVariable Long id) {
        return productService.listStockMovements(id);
    }
}
