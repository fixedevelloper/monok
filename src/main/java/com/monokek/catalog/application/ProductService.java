package com.monokek.catalog.application;

import com.monokek.common.ApiException;
import com.monokek.catalog.domain.*;
import com.monokek.catalog.web.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Pos\ProductController}.
 * Not ported: {@code updateStock}/{@code toggleStatus} — real methods on the
 * controller, but no route in {@code routes/api.php} calls either, so
 * there's nothing to reach them in the source app.
 */
@Service
public class ProductService {

    private static final List<String> VALID_TYPES = List.of("storable", "consumable", "service");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModifierRepository modifierRepository;
    private final ModifierProductRepository modifierProductRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ModifierRepository modifierRepository,
            ModifierProductRepository modifierProductRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.modifierRepository = modifierRepository;
        this.modifierProductRepository = modifierProductRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> index(Long categoryId, String search, Long branchId) {
        return productRepository.searchActive(categoryId, blankToNull(search), branchId).stream().map(this::toDto).toList();
    }

    /** Backs the admin menu's infinite-scroll grid — one page request per branch section. */
    @Transactional(readOnly = true)
    public Page<ProductDto> indexPage(Long categoryId, String search, Long branchId, Pageable pageable) {
        return productRepository.searchActivePage(categoryId, blankToNull(search), branchId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public ProductDto create(CreateProductRequest request) {
        validateType(request.type());
        if (request.sku() != null && productRepository.existsBySku(request.sku())) {
            throw ApiException.conflict("Ce SKU est déjà utilisé.");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.badRequest("Catégorie introuvable"));

        Product product = new Product();
        product.setCategory(category);
        product.setName(request.name());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setPurchasePrice(request.purchasePrice());
        product.setIncentiveAmount(request.incentiveAmount() == null ? BigDecimal.ZERO : request.incentiveAmount());
        product.setStockCount(request.stockCount() == null ? 0 : request.stockCount());
        product.setAlertStock(request.alertStock() == null ? 0 : request.alertStock());
        product.setType(request.type());
        product.setTrackStock(Boolean.TRUE.equals(request.trackStock()));
        product.setImage(request.image());
        product.setDescription(request.description());

        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto update(Long id, UpdateProductRequest request) {
        Product product = findOrThrow(id);

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> ApiException.badRequest("Catégorie introuvable"));
            product.setCategory(category);
        }
        if (request.name() != null) product.setName(request.name());
        if (request.sku() != null) {
            if (productRepository.existsBySkuAndIdNot(request.sku(), id)) {
                throw ApiException.conflict("Ce SKU est déjà utilisé.");
            }
            product.setSku(request.sku());
        }
        if (request.price() != null) product.setPrice(request.price());
        if (request.purchasePrice() != null) product.setPurchasePrice(request.purchasePrice());
        if (request.incentiveAmount() != null) product.setIncentiveAmount(request.incentiveAmount());
        if (request.stockCount() != null) product.setStockCount(request.stockCount());
        if (request.alertStock() != null) product.setAlertStock(request.alertStock());
        if (request.type() != null) {
            validateType(request.type());
            product.setType(request.type());
        }
        if (request.trackStock() != null) product.setTrackStock(request.trackStock());
        if (request.active() != null) product.setActive(request.active());
        if (request.image() != null) product.setImage(request.image());
        if (request.description() != null) product.setDescription(request.description());

        return toDto(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = findOrThrow(id);
        product.softDelete();
        productRepository.save(product);
    }

    @Transactional
    public void syncModifiers(Long productId, List<Long> modifierIds) {
        Product product = findOrThrow(productId);
        List<Long> ids = modifierIds == null ? List.of() : modifierIds;

        List<ModifierProduct> existing = modifierProductRepository.findByProductId(productId);
        Set<Long> existingModifierIds = existing.stream().map(mp -> mp.getModifier().getId()).collect(Collectors.toSet());

        for (ModifierProduct link : existing) {
            if (!ids.contains(link.getModifier().getId())) {
                modifierProductRepository.deleteById(link.getId());
            }
        }

        int sortOrder = 0;
        for (Long modifierId : ids) {
            if (!existingModifierIds.contains(modifierId)) {
                Modifier modifier = modifierRepository.findById(modifierId)
                        .orElseThrow(() -> ApiException.badRequest("Groupe de modificateurs introuvable : " + modifierId));
                modifierProductRepository.save(ModifierProduct.of(product, modifier, sortOrder));
            }
            sortOrder++;
        }
    }

    /** Port of {@code ProductController::bulkImport}. */
    @Transactional
    public int bulkImport(BulkImportRequest request) {
        int imported = 0;
        for (BulkImportRequest.Line line : request.items()) {
            String categoryName = line.category().trim();
            Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElseGet(() -> {
                Category created = new Category();
                created.setBranchId(request.branchId());
                created.setName(categoryName);
                created.setSlug(slugify(categoryName));
                return categoryRepository.save(created);
            });

            Product product = new Product();
            product.setCategory(category);
            product.setName(line.name());
            product.setPrice(line.price());
            product.setActive(true);
            productRepository.save(product);
            imported++;
        }
        return imported;
    }

    private void validateType(String type) {
        if (!VALID_TYPES.contains(type)) {
            throw ApiException.badRequest("Type de produit inconnu : " + type);
        }
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> ApiException.notFound("Produit introuvable"));
    }

    private String slugify(String name) {
        String normalized = name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return normalized.isEmpty() ? "categorie" : normalized;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private ProductDto toDto(Product product) {
        List<ModifierDto> modifiers = modifierProductRepository.findByProductId(product.getId()).stream()
                .map(ModifierProduct::getModifier)
                .map(m -> new ModifierDto(m.getId(), m.getName(),
                        m.getItems().stream().map(i -> new ModifierDto.Item(i.getId(), i.getName(), i.getPrice())).toList()))
                .toList();

        return new ProductDto(
                product.getId(), product.getCategory().getId(), product.getCategory().getName(),
                product.getCategory().getBranchId(), product.getSku(),
                product.getName(), product.getDescription(), product.getPrice(), product.getPurchasePrice(),
                product.getIncentiveAmount(),
                formatFcfa(product.getPrice()), product.isActive(), product.isTrackStock(), product.getStockCount(),
                product.getAlertStock(), product.getType(), product.getImage(), modifiers
        );
    }

    private String formatFcfa(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(amount == null ? BigDecimal.ZERO : amount) + " FCFA";
    }
}
