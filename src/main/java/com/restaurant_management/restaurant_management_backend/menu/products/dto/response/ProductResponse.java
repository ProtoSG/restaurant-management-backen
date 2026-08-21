package com.restaurant_management.restaurant_management_backend.menu.products.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.restaurant_management.restaurant_management_backend.menu.categories.dto.response.CategoryResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.response.ProductVariantResponse;

public record ProductResponse(

  Long id,
  String name,
  BigDecimal price,
  CategoryResponse category,
  Boolean isAvailable,
  List<ProductVariantResponse> variants

) {}
