package com.restaurant_management.restaurant_management_backend.menu.categories;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.menu.categories.dto.request.CreateCategoryRequest;
import com.restaurant_management.restaurant_management_backend.menu.categories.dto.response.CategoryResponse;
import com.restaurant_management.restaurant_management_backend.menu.categories.entity.Category;

@Component
public class CategoryMapper {

  public Category toEntity(CreateCategoryRequest req) {
    if (req == null) return null;

    return Category.builder()
      .name(req.name())
      .build();
  }

  public CategoryResponse toReponse(Category category) {
    if (category == null) return null;

    return new CategoryResponse(category.getId(), category.getName());
  }

}
