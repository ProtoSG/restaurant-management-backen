package com.restaurant_management.restaurant_management_backend.mapper;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.dto.CategoryDTO;
import com.restaurant_management.restaurant_management_backend.entity.Category;

@Component
public class CategoryMapper {

  public CategoryDTO toDto(Category category) {
    if (category == null) return null;

    return CategoryDTO.builder()
      .id(category.getId())
      .name(category.getName())
      .build();
  }

  public Category toEntity(CategoryDTO categoryDTO) {
    if (categoryDTO == null) return null;

    return Category.builder()
      .id(categoryDTO.getId())
      .name(categoryDTO.getName())
      .build();
  }
  
}
