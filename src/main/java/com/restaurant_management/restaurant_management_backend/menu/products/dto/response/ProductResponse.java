package com.restaurant_management.restaurant_management_backend.menu.products.dto.response;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.menu.categories.dto.response.CategoryResponse;

public record ProductResponse(

  Long id,
  String name,
  BigDecimal price,
  CategoryResponse category

) {}
