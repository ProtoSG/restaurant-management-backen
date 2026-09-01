package com.restaurant_management.restaurant_management_backend.menu.products;

import java.util.Collections;
import java.util.List;

import com.restaurant_management.restaurant_management_backend.menu.categories.dto.response.CategoryResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.CreateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.ProductVariantRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.response.ProductVariantResponse;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductMapper {

  private final ProductVariantRepository variantRepository;

  public Product toEntity(CreateProductRequest req) {
    if (req == null) return null;

    return Product.builder()
      .name(req.name())
      .price(req.price())
      .build();
  }

  public ProductResponse toResponse(Product product) {
    if (product == null) return null;

    CategoryResponse category = new CategoryResponse(
      product.getCategory().getId(),
      product.getCategory().getName()
    );

    List<ProductVariantResponse> variants = variantRepository != null
      ? variantRepository.findByProductId(product.getId()).stream()
          .map(v -> new ProductVariantResponse(v.getId(), v.getName(), v.getPrice(), v.getIsAvailable(), v.getSortOrder()))
          .toList()
      : Collections.emptyList();

    return new ProductResponse(
      product.getId(),
      product.getName(),
      product.getPrice(),
      category,
      product.getIsAvailable(),
      variants,
      product.getImageUrl()
    );
  }

}
