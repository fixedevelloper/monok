package com.monokek.inventory.application;

import com.monokek.common.ApiException;
import com.monokek.inventory.domain.*;
import com.monokek.inventory.web.dto.AdjustStockRequest;
import com.monokek.inventory.web.dto.CreateIngredientRequest;
import com.monokek.inventory.web.dto.IngredientDto;
import com.monokek.inventory.web.dto.StockMovementDto;
import com.monokek.inventory.web.dto.UnitDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Admin\IngredientController}.
 * {@code updateAlert} isn't ported: it exists in the Laravel controller but
 * no route in {@code routes/api.php} calls it. Same for the {@code show}/
 * {@code update}/{@code destroy} legs of {@code Route::apiResource('ingredients', ...)}
 * — the controller never defines those methods, so those routes would 500
 * in the source app.
 */
@Service
public class IngredientService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final IngredientRepository ingredientRepository;
    private final UnitRepository unitRepository;
    private final StockMovementRepository stockMovementRepository;

    public IngredientService(
            IngredientRepository ingredientRepository, UnitRepository unitRepository, StockMovementRepository stockMovementRepository) {
        this.ingredientRepository = ingredientRepository;
        this.unitRepository = unitRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<IngredientDto> list() {
        return ingredientRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<UnitDto> listUnits() {
        return unitRepository.findAll().stream().map(u -> new UnitDto(u.getId(), u.getName())).toList();
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> listMovements(Pageable pageable) {
        return stockMovementRepository.findAllByOrderByIdDesc(pageable).map(this::toDto);
    }

    @Transactional
    public IngredientDto create(CreateIngredientRequest request) {
        if (ingredientRepository.existsByName(request.name())) {
            throw ApiException.conflict("Un ingrédient porte déjà ce nom.");
        }
        Unit unit = unitRepository.findById(request.unitId()).orElseThrow(() -> ApiException.badRequest("Unité introuvable"));

        Ingredient ingredient = new Ingredient();
        ingredient.setUnit(unit);
        ingredient.setName(request.name());
        ingredient.setStock(request.stock());
        ingredient.setAlertQty(request.alertQty());
        ingredient = ingredientRepository.save(ingredient);

        if (request.stock().signum() > 0) {
            recordMovement(ingredient, "in", request.stock(), "Stock initial à la création");
        }
        return toDto(ingredient);
    }

    @Transactional
    public BigDecimal adjustStock(Long ingredientId, AdjustStockRequest request) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> ApiException.notFound("Ingrédient introuvable"));

        ingredient.applyMovement(request.type(), request.qty());
        ingredientRepository.save(ingredient);
        recordMovement(ingredient, request.type(), request.qty(), request.reason() == null ? "Ajustement manuel" : request.reason());

        return ingredient.getStock();
    }

    private void recordMovement(Ingredient ingredient, String type, BigDecimal qty, String reason) {
        StockMovement movement = new StockMovement();
        movement.setIngredient(ingredient);
        movement.setType(type);
        movement.setQty(qty);
        movement.setReason(reason);
        stockMovementRepository.save(movement);
    }

    private IngredientDto toDto(Ingredient ingredient) {
        return new IngredientDto(
                ingredient.getId(), ingredient.getName(), ingredient.getStock(), ingredient.getAlertQty(),
                ingredient.getUnit().getName(), ingredient.isLowStock());
    }

    private StockMovementDto toDto(StockMovement movement) {
        return new StockMovementDto(
                movement.getId(),
                movement.getIngredient().getId(),
                movement.getIngredient().getName(),
                movement.getType(),
                movement.getQty(),
                movement.getReason() == null ? "Aucune note" : movement.getReason(),
                movement.getCreatedAt() == null ? null : movement.getCreatedAt().format(DATE)
        );
    }
}
