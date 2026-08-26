package com.monokek.catalog.application;

import com.monokek.catalog.ProductCatalog;
import com.monokek.catalog.domain.ModifierItem;
import com.monokek.catalog.domain.ModifierItemRepository;
import com.monokek.catalog.domain.ModifierProduct;
import com.monokek.catalog.domain.ModifierProductRepository;
import com.monokek.catalog.domain.Product;
import com.monokek.catalog.domain.ProductRepository;
import com.monokek.catalog.domain.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
class ProductCatalogService implements ProductCatalog {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ModifierItemRepository modifierItemRepository;
    private final ModifierProductRepository modifierProductRepository;

    ProductCatalogService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ModifierItemRepository modifierItemRepository,
            ModifierProductRepository modifierProductRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.modifierItemRepository = modifierItemRepository;
        this.modifierProductRepository = modifierProductRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSnapshot> findProduct(Long productId) {
        return productRepository.findById(productId).map(this::toSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VariantSnapshot> findVariant(Long variantId) {
        return productVariantRepository.findById(variantId)
                .map(v -> new VariantSnapshot(v.getId(), v.getName(), v.getPrice()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModifierItemSnapshot> findModifierItem(Long modifierItemId) {
        return modifierItemRepository.findById(modifierItemId)
                .map(m -> new ModifierItemSnapshot(m.getId(), m.getName(), m.getPrice(), m.getModifier().getId()));
    }

    private ProductSnapshot toSnapshot(Product product) {
        Long kitchenStationId = product.getCategory() == null ? null : product.getCategory().getKitchenStationId();
        Long branchId = product.getCategory() == null ? null : product.getCategory().getBranchId();
        var modifierGroups = modifierProductRepository.findByProductId(product.getId()).stream()
                .map(ModifierProduct::getModifier)
                .map(m -> new ModifierGroupSnapshot(m.getId(), m.getName(), m.getType(), m.isRequired(), m.getMinSelect(), m.getMaxSelect()))
                .toList();
        return new ProductSnapshot(
                product.getId(), product.getName(), product.getPrice(), product.getIncentiveAmount(), kitchenStationId, branchId,
                modifierGroups);
    }
}
