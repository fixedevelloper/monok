package com.monokek.catalog.application;

import com.monokek.common.ApiException;
import com.monokek.catalog.domain.Category;
import com.monokek.catalog.domain.CategoryRepository;
import com.monokek.catalog.web.dto.CategoryDto;
import com.monokek.catalog.web.dto.CreateCategoryRequest;
import com.monokek.catalog.web.dto.UpdateCategoryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * New functionality (category creation) plus a port of
 * {@code ProductController::categories} (the read-only listing). See the
 * module's package-info for why Laravel had no route to create a category
 * directly — only an implicit {@code firstOrCreate} inside {@code bulkImport}.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** {@code branchId} null means unscoped (admin/owner with no assigned branch) — everyone
     * else only ever sees their own branch's categories, same rule as {@code floorplan.FloorService#list}. */
    @Transactional(readOnly = true)
    public List<CategoryDto> list(Long branchId) {
        List<Category> categories = branchId == null ? categoryRepository.findAll() : categoryRepository.findByBranchId(branchId);
        return categories.stream().map(this::toDto).toList();
    }

    /** Port of {@code ProductController::categories} — active categories only, for the POS menu. */
    @Transactional(readOnly = true)
    public List<CategoryDto> listActive(Long branchId) {
        List<Category> categories = branchId == null
                ? categoryRepository.findByActiveTrueOrderByNameAsc()
                : categoryRepository.findByBranchIdAndActiveTrueOrderByNameAsc(branchId);
        return categories.stream().map(this::toDto).toList();
    }

    /** {@code callerBranchId} null means the caller has no assigned branch (owner/super-admin) —
     * only then is {@code request.branchId()} trusted; a branch manager can only ever create a
     * category for their own branch, not pick another one from the dropdown. */
    @Transactional
    public CategoryDto create(CreateCategoryRequest request, Long callerBranchId) {
        String slug = slugify(request.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw ApiException.conflict("Une catégorie porte déjà ce nom.");
        }

        Category category = new Category();
        category.setBranchId(callerBranchId != null ? callerBranchId : request.branchId());
        category.setKitchenStationId(request.kitchenStationId());
        category.setName(request.name());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setIcon(request.icon());
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto update(Long id, UpdateCategoryRequest request) {
        Category category = findOrThrow(id);

        if (request.name() != null) {
            String slug = slugify(request.name());
            if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
                throw ApiException.conflict("Une catégorie porte déjà ce nom.");
            }
            category.setName(request.name());
            category.setSlug(slug);
        }
        if (request.description() != null) category.setDescription(request.description());
        if (request.icon() != null) category.setIcon(request.icon());
        if (request.kitchenStationId() != null) category.setKitchenStationId(request.kitchenStationId());
        if (request.active() != null) category.setActive(request.active());

        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        // products.category_id has no ON DELETE CASCADE in the schema — deleting a category
        // with existing products fails at the database's foreign-key constraint, same as Laravel would.
        categoryRepository.deleteById(id);
    }

    Category findOrThrow(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Catégorie introuvable"));
    }

    /** Port of {@code Category::boot()}'s {@code Str::slug($name)} on create/rename. */
    private String slugify(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return normalized.isEmpty() ? "categorie" : normalized;
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getBranchId(), category.getKitchenStationId(),
                category.getName(), category.getSlug(), category.getDescription(), category.getIcon(), category.isActive());
    }
}
