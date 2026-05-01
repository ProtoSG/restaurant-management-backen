package com.restaurant_management.restaurant_management_backend.menu.products;

import com.restaurant_management.restaurant_management_backend.menu.categories.dto.response.CategoryResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.CreateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;
import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;

@Component
public class ProductMapper {

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

    return new ProductResponse(
      product.getId(),
      product.getName(),
      product.getPrice(),
      category
    );
  }

}
