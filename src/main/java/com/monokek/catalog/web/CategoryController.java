package com.monokek.catalog.web;

import com.monokek.catalog.application.CategoryService;
import com.monokek.catalog.web.dto.CategoryDto;
import com.monokek.catalog.web.dto.CreateCategoryRequest;
import com.monokek.catalog.web.dto.UpdateCategoryRequest;
import com.monokek.identity.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Port of {@code ProductController::categories} ({@code GET /api/pos/categories}
 * below — moved out from under the admin-role gate Laravel put it behind at
 * {@code GET /admin/categories}, since a waiter needs the category list to
 * even render the POS product menu; {@code pos/products} itself already
 * only requires plain auth) plus new admin CRUD (see the module's
 * package-info) at {@code /api/admin/categories}, guarded by
 * {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/pos/categories")
    public List<CategoryDto> posIndex(@AuthenticationPrincipal CurrentUser principal) {
        return categoryService.listActive(principal.branchId());
    }

    @GetMapping("/api/admin/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<CategoryDto> index(@AuthenticationPrincipal CurrentUser principal) {
        return categoryService.list(principal.branchId());
    }

    @PostMapping("/api/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_categories'))")
    public CategoryDto store(@Valid @RequestBody CreateCategoryRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return categoryService.create(request, principal.branchId());
    }

    @PutMapping("/api/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_categories'))")
    public CategoryDto update(@PathVariable Long id, @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/api/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_categories'))")
    public void destroy(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
