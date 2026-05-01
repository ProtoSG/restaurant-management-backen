package com.restaurant_management.restaurant_management_backend.menu.categories;

import java.util.List;

import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.menu.categories.dto.request.CreateCategoryRequest;
import com.restaurant_management.restaurant_management_backend.menu.categories.dto.request.UpdateCategoryRequest;
import com.restaurant_management.restaurant_management_backend.menu.categories.dto.response.CategoryResponse;
import com.restaurant_management.restaurant_management_backend.menu.categories.entity.Category;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;
  
  @Override
  public CategoryResponse save(CreateCategoryRequest req) {
    Category category = categoryMapper.toEntity(req);

    return categoryMapper.toReponse(categoryRepository.save(category));
  }

  @Override
  public CategoryResponse findById(Long id) {
    Category category = categoryRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

    return categoryMapper.toReponse(category);
  }

  @Override
  public List<CategoryResponse> findAll() {
    List<Category> categories = categoryRepository.findAll();

    return categories.stream()
      .map(categoryMapper::toReponse)
      .toList();
  }

  @Override
  public CategoryResponse update(Long id, UpdateCategoryRequest req) {
    Category category = categoryRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

    category.setName(req.name());

    return categoryMapper.toReponse(categoryRepository.save(category));
  }

  @Override
  public void delete(Long id) {
    if (!categoryRepository.existsById(id)) {
      throw new ResourceNotFoundException("Categoría no encontrada");
    }

    categoryRepository.deleteById(id);
  }

}
