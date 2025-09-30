package com.restaurant_management.restaurant_management_backend.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.dto.CategoryDTO;
import com.restaurant_management.restaurant_management_backend.entity.Category;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.mapper.CategoryMapper;
import com.restaurant_management.restaurant_management_backend.repository.CategoryRepository;
import com.restaurant_management.restaurant_management_backend.service.CategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;

  public CategoryDTO save(CategoryDTO categoryDTO) {
    Category newCategory = categoryMapper.toEntity(categoryDTO);

    return categoryMapper.toDto(categoryRepository.save(newCategory));
  }

  public CategoryDTO findById(Long id) {
    Category category = categoryRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

    return categoryMapper.toDto(category);
  }

  public List<CategoryDTO> findAll() {
    List<Category> categories = categoryRepository.findAll();

    return categories.stream()
      .map(categoryMapper::toDto)
      .toList();
  }

  public CategoryDTO update(Long id, CategoryDTO categoryDTO) {
    Category category = categoryRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

    category.setName(categoryDTO.getName());

    return categoryMapper.toDto(categoryRepository.save(category));
  }

  public void delete(Long id) {
    if (!categoryRepository.existsById(id)) {
      throw new ResourceNotFoundException("Categoría no encontrada");
    }

    categoryRepository.deleteById(id);
  }
  
}
