package com.monokek.inventory.application;

import com.monokek.common.ApiException;
import com.monokek.inventory.domain.*;
import com.monokek.inventory.web.dto.CreatePurchaseOrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Admin\PurchaseOrderController}.
 * Only {@code store} exists — the {@code index}/{@code show}/{@code update}/
 * {@code destroy} legs of {@code Route::apiResource('purchase-orders', ...)}
 * have no matching controller method in the Laravel source.
 */
@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final IngredientRepository ingredientRepository;
    private final StockMovementRepository stockMovementRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierRepository supplierRepository,
            IngredientRepository ingredientRepository,
            StockMovementRepository stockMovementRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.ingredientRepository = ingredientRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional
    public void store(CreatePurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> ApiException.badRequest("Fournisseur introuvable"));

        // Saved once empty first so the id exists for the "Achat #<id>" stock-movement reason below,
        // exactly like Laravel needs $po->id from the initial insert before looping the items.
        PurchaseOrder purchaseOrder = purchaseOrderRepository.save(PurchaseOrder.receive(supplier));

        for (CreatePurchaseOrderRequest.Line line : request.items()) {
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

        purchaseOrderRepository.save(purchaseOrder);
    }
}
