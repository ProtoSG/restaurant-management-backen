package com.restaurant_management.restaurant_management_backend.mapper;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.dto.ProductDTO;
import com.restaurant_management.restaurant_management_backend.entity.Product;

@Component
public class ProductMapper {

  public Product toEntity(ProductDTO productDTO) {
    if (productDTO == null) return null;

    return Product.builder()
      .id(productDTO.getId())
      .name(productDTO.getName())
      .price(productDTO.getPrice())
      .build();
  }

  public ProductDTO toDto(Product product) {
    if (product == null) return null;

    return ProductDTO.builder()
      .id(product.getId())
      .name(product.getName())
      .price(product.getPrice())
      .categoryId(product.getCategory().getId())
      .build();
  }
}
