package com.monokek.catalog.web;

import com.monokek.catalog.application.ProductService;
import com.monokek.catalog.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public List<ProductDto> posIndex(@RequestParam(required = false) Long categoryId, @RequestParam(required = false) String search) {
        return productService.index(categoryId, search);
    }

    @GetMapping("/api/admin/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<ProductDto> index(@RequestParam(required = false) Long categoryId, @RequestParam(required = false) String search) {
        return productService.index(categoryId, search);
    }

    @GetMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto show(@PathVariable Long id) {
        return productService.show(id);
    }

    @PostMapping("/api/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto store(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto update(@PathVariable Long id, @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, String> destroy(@PathVariable Long id) {
        productService.delete(id);
        return Map.of("message", "Produit supprimé");
    }

    @PostMapping("/api/admin/products/bulk-import")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, String> bulkImport(@Valid @RequestBody BulkImportRequest request) {
        int count = productService.bulkImport(request);
        return Map.of("message", "Succès ! %d produits ont été importés.".formatted(count));
    }

    @PostMapping("/api/admin/products/{id}/sync-modifiers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, String> syncModifiers(@PathVariable Long id, @RequestBody SyncModifiersRequest request) {
        productService.syncModifiers(id, request.modifierIds());
        return Map.of("message", "Modificateurs synchronisés");
    }
}
