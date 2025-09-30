package com.restaurant_management.restaurant_management_backend.service;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.dto.CategoryDTO;

public interface CategoryService {

  public CategoryDTO save(CategoryDTO categoryDTO);
  public CategoryDTO findById(Long id);
  public List<CategoryDTO> findAll();
  public CategoryDTO update(Long id, CategoryDTO categoryDTO);
  public void delete(Long id);

}
