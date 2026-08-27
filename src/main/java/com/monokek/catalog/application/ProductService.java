package com.monokek.catalog.application;

import com.monokek.common.ApiException;
import com.monokek.catalog.ProductStockReceiver;
import com.monokek.catalog.domain.*;
import com.monokek.catalog.web.dto.*;
import com.monokek.identity.UserDirectory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Pos\ProductController}.
 * Not ported: {@code updateStock}/{@code toggleStatus} — real methods on the
 * controller, but no route in {@code routes/api.php} calls either, so
 * there's nothing to reach them in the source app.
 */
@Service
public class ProductService implements ProductStockReceiver {

    private static final List<String> VALID_TYPES = List.of("storable", "consumable", "service");
    private static final List<String> VALID_MOVEMENT_TYPES = List.of("in", "out", "adjust");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModifierRepository modifierRepository;
    private final ModifierProductRepository modifierProductRepository;
    private final ProductStockMovementRepository productStockMovementRepository;
    private final UserDirectory userDirectory;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ModifierRepository modifierRepository,
            ModifierProductRepository modifierProductRepository,
            ProductStockMovementRepository productStockMovementRepository,
            UserDirectory userDirectory) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.modifierRepository = modifierRepository;
        this.modifierProductRepository = modifierProductRepository;
        this.productStockMovementRepository = productStockMovementRepository;
        this.userDirectory = userDirectory;
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

    /** The only path left that can move {@link Product#getStockCount()} — {@code
     * UpdateProductRequest} no longer accepts it, so every change from here on is traced by a
     * {@link ProductStockMovement}. "in"/"out" move {@code qty} units; "adjust" treats {@code qty}
     * as the new absolute count from a physical stock take, recording whatever delta that implies. */
    @Transactional
    public ProductDto adjustStock(Long productId, AdjustProductStockRequest request, Long authorId) {
        if (!VALID_MOVEMENT_TYPES.contains(request.type())) {
            throw ApiException.badRequest("Type de mouvement inconnu : " + request.type());
        }
        Product product = findOrThrow(productId);
        int before = product.getStockCount();
        int delta = switch (request.type()) {
            case "in" -> request.qty();
            case "out" -> -request.qty();
            default -> request.qty() - before; // "adjust"
        };
        if (before + delta < 0) {
            throw ApiException.badRequest("Stock insuffisant pour cette sortie.");
        }
        product.setStockCount(before + delta);
        productRepository.save(product);

        ProductStockMovement movement = new ProductStockMovement();
        movement.setProduct(product);
        movement.setType(request.type());
        movement.setQty(delta);
        movement.setReason(request.reason() == null || request.reason().isBlank() ? "Ajustement manuel" : request.reason());
        movement.setAuthorId(authorId);
        productStockMovementRepository.save(movement);

        return toDto(product);
    }

    /** {@link ProductStockReceiver#receivePurchase} — called by {@code inventory.PurchaseOrderService}
     * when a purchase order line references a {@code Product} (a resellable item, e.g. a crate of
     * soda) instead of a recipe {@code Ingredient}. Always an "in" movement; unlike {@link
     * #adjustStock} there's no negative-stock check to make (a purchase only ever adds stock). */
    @Override
    @Transactional
    public void receivePurchase(Long productId, int qty, BigDecimal unitPrice, String reason, Long authorId) {
        Product product = findOrThrow(productId);
        product.setStockCount(product.getStockCount() + qty);
        if (unitPrice != null) {
            product.setPurchasePrice(unitPrice);
        }
        productRepository.save(product);

        ProductStockMovement movement = new ProductStockMovement();
        movement.setProduct(product);
        movement.setType("in");
        movement.setQty(qty);
        movement.setReason(reason);
        movement.setAuthorId(authorId);
        productStockMovementRepository.save(movement);
    }

    /** {@link ProductStockReceiver#deductForSale} — called by {@code ordering.application.
     * ProductStockDeductionListener} when a POS sale is paid. Mirrors {@link #adjustStock}'s "out"
     * branch (same movement bookkeeping), except it never throws on insufficient stock: by the time
     * this runs the order is already paid and committed, so there's nothing left to roll back.
     * Instead it clamps at zero and says so in the movement's reason, which is a real signal (stock
     * count is untrustworthy) rather than a swallowed failure. A no-op for a non-"storable" product
     * (consumable/service never carry a real stock count) — deliberately keyed off {@code type}
     * rather than {@link Product#isTrackStock()}: that flag is never actually set from the admin
     * UI (neither {@code AddProductModal} nor {@code EditProductModal} send it), so every real
     * product has it stuck at {@code false} — gating on it here silently no-op'd every sale. {@code
     * type == "storable"} is the field the admin UI actually drives (it's what decides whether
     * {@code stockCount} is even shown/sent), and it's the same "just act on stockCount, no extra
     * gate" convention {@link #adjustStock}/{@link #receivePurchase} already follow. */
    @Override
    @Transactional
    public void deductForSale(Long productId, int qty, Long orderId, Long cashierUserId) {
        Product product = findOrThrow(productId);
        if (!"storable".equals(product.getType())) {
            return;
        }
        int before = product.getStockCount();
        int after = before - qty;
        String reason = "Vente POS #" + orderId;
        if (after < 0) {
            reason += " (stock insuffisant : ajusté à 0)";
            after = 0;
        }
        product.setStockCount(after);
        productRepository.save(product);

        ProductStockMovement movement = new ProductStockMovement();
        movement.setProduct(product);
        movement.setType("out");
        movement.setQty(after - before);
        movement.setReason(reason);
        movement.setAuthorId(cashierUserId);
        productStockMovementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public List<ProductStockMovementDto> listStockMovements(Long productId) {
        findOrThrow(productId);
        List<ProductStockMovement> movements = productStockMovementRepository.findByProductIdOrderByIdDesc(productId);
        Map<Long, String> authorNames = userDirectory.namesByIds(movements.stream().map(ProductStockMovement::getAuthorId).toList());

        return movements.stream()
                .map(m -> new ProductStockMovementDto(
                        m.getId(), productId, m.getType(), m.getQty(),
                        m.getReason() == null ? "Aucune note" : m.getReason(),
                        m.getAuthorId(), authorNames.getOrDefault(m.getAuthorId(), "Utilisateur inconnu"),
                        m.getCreatedAt() == null ? null : m.getCreatedAt().format(DATE)))
                .toList();
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
                .map(m -> new ModifierDto(m.getId(), m.getName(), m.getType(), m.isRequired(), m.getMinSelect(), m.getMaxSelect(),
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
