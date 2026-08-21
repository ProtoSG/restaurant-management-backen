package com.restaurant_management.restaurant_management_backend.menu.products.productvariants;

import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.request.CreateProductVariantRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.request.UpdateProductVariantRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.response.ProductVariantResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.entity.ProductVariant;

import org.springframework.stereotype.Component;

@Component
public class ProductVariantMapper {

  public ProductVariant toEntity(CreateProductVariantRequest req, Product product) {
    if (req == null) return null;
    return ProductVariant.builder()
      .name(req.name())
      .price(req.price())
      .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
      .product(product)
      .build();
  }

  public ProductVariantResponse toResponse(ProductVariant variant) {
    if (variant == null) return null;
    return new ProductVariantResponse(
      variant.getId(),
      variant.getName(),
      variant.getPrice(),
      variant.getIsAvailable(),
      variant.getSortOrder()
    );
  }
}
