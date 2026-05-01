package com.restaurant_management.restaurant_management_backend.menu.categories;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.menu.categories.dto.request.CreateCategoryRequest;
import com.restaurant_management.restaurant_management_backend.menu.categories.dto.request.UpdateCategoryRequest;
import com.restaurant_management.restaurant_management_backend.menu.categories.dto.response.CategoryResponse;

public interface CategoryService {

  public CategoryResponse save(CreateCategoryRequest req);
  public CategoryResponse findById(Long id);
  public List<CategoryResponse> findAll();
  public CategoryResponse update(Long id, UpdateCategoryRequest req);
  public void delete(Long id);

}
