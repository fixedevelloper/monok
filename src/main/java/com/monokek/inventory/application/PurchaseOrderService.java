package com.monokek.inventory.application;

import com.monokek.catalog.ProductCatalog;
import com.monokek.catalog.ProductStockReceiver;
import com.monokek.common.ApiException;
import com.monokek.inventory.domain.*;
import com.monokek.inventory.web.dto.CreatePurchaseOrderRequest;
import com.monokek.inventory.web.dto.PurchaseOrderDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Admin\PurchaseOrderController}.
 * Only {@code store} was ever ported — the {@code index}/{@code show}/{@code update}/
 * {@code destroy} legs of {@code Route::apiResource('purchase-orders', ...)}
 * have no matching controller method in the Laravel source. {@code
 * listBySupplier} is new functionality (not a Laravel port): the admin
 * supplier detail page needs a purchase history, and since a purchase order
 * is always "received" immediately (see {@code PurchaseOrder#receive}) one
 * list call doubles as both index and detail — no separate {@code show}.
 */
@Service
public class PurchaseOrderService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final IngredientRepository ingredientRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductCatalog productCatalog;
    private final ProductStockReceiver productStockReceiver;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierRepository supplierRepository,
            IngredientRepository ingredientRepository,
            StockMovementRepository stockMovementRepository,
            ProductCatalog productCatalog,
            ProductStockReceiver productStockReceiver) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.ingredientRepository = ingredientRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productCatalog = productCatalog;
        this.productStockReceiver = productStockReceiver;
    }

    /** {@code authorId} is who validated the purchase — only actually used for lines that touch a
     * {@code Product} (its {@code ProductStockMovement} needs one); an ingredient's {@code
     * StockMovement} has no author column at all today. */
    @Transactional
    public void store(CreatePurchaseOrderRequest request, Long authorId) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> ApiException.badRequest("Fournisseur introuvable"));

        // Saved once empty first so the id exists for the "Achat #<id>" stock-movement reason below,
        // exactly like Laravel needs $po->id from the initial insert before looping the items.
        PurchaseOrder purchaseOrder = purchaseOrderRepository.save(PurchaseOrder.receive(supplier));

        for (CreatePurchaseOrderRequest.Line line : request.items()) {
            boolean hasIngredient = line.ingredientId() != null;
            boolean hasProduct = line.productId() != null;
            if (hasIngredient == hasProduct) {
                throw ApiException.badRequest("Chaque article doit référencer soit un ingrédient, soit un produit.");
            }

            if (hasIngredient) {
                receiveIngredientLine(purchaseOrder, line);
            } else {
                receiveProductLine(purchaseOrder, line, authorId);
            }
        }

        purchaseOrderRepository.save(purchaseOrder);
    }

    private void receiveIngredientLine(PurchaseOrder purchaseOrder, CreatePurchaseOrderRequest.Line line) {
        Ingredient ingredient = ingredientRepository.findById(line.ingredientId())
                .orElseThrow(() -> ApiException.badRequest("Ingrédient introuvable : " + line.ingredientId()));

        purchaseOrder.addItem(ingredient, line.qty(), line.price());
        ingredient.applyMovement("in", line.qty());
        ingredientRepository.save(ingredient);

        StockMovement movement = new StockMovement();
        movement.setIngredient(ingredient);
        movement.setType("in");
        movement.setQty(line.qty());
        movement.setReason("Achat #" + purchaseOrder.getId());
        stockMovementRepository.save(movement);
    }

    private void receiveProductLine(PurchaseOrder purchaseOrder, CreatePurchaseOrderRequest.Line line, Long authorId) {
        if (!productCatalog.findProduct(line.productId()).isPresent()) {
            throw ApiException.badRequest("Produit introuvable : " + line.productId());
        }
        int qty;
        try {
            qty = line.qty().intValueExact();
        } catch (ArithmeticException e) {
            throw ApiException.badRequest("La quantité doit être un nombre entier pour un produit.");
        }

        purchaseOrder.addItem(line.productId(), line.qty(), line.price());
        productStockReceiver.receivePurchase(line.productId(), qty, line.price(), "Achat fournisseur #" + purchaseOrder.getId(), authorId);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> listBySupplier(Long supplierId) {
        if (!supplierRepository.findById(supplierId).isPresent()) {
            throw ApiException.notFound("Fournisseur introuvable");
        }
        return purchaseOrderRepository.findBySupplierIdOrderByIdDesc(supplierId).stream().map(this::toDto).toList();
    }

    private PurchaseOrderDto toDto(PurchaseOrder purchaseOrder) {
        List<PurchaseOrderDto.Line> lines = purchaseOrder.getItems().stream()
                .map(item -> new PurchaseOrderDto.Line(
                        item.getId(),
                        item.getIngredient() == null ? null : item.getIngredient().getId(),
                        item.getIngredient() == null ? null : item.getIngredient().getName(),
                        item.getProductId(),
                        item.getProductId() == null ? null : productCatalog.findProduct(item.getProductId())
                                .map(ProductCatalog.ProductSnapshot::name).orElse("Produit supprimé"),
                        item.getQty(), item.getPrice()))
                .toList();

        return new PurchaseOrderDto(
                purchaseOrder.getId(), purchaseOrder.getSupplier().getId(), purchaseOrder.getSupplier().getName(),
                purchaseOrder.getStatus(), purchaseOrder.getTotal(),
                purchaseOrder.getCreatedAt() == null ? null : purchaseOrder.getCreatedAt().format(DATE_TIME),
                lines);
    }
}
