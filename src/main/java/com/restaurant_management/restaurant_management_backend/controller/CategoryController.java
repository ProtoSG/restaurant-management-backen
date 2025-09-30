package com.restaurant_management.restaurant_management_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.dto.CategoryDTO;
import com.restaurant_management.restaurant_management_backend.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  @GetMapping
  public ResponseEntity<?> getAll() {
    List<CategoryDTO> categories = categoryService.findAll();

    return ResponseEntity.status(HttpStatus.OK).body(categories);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable Long id) {
    CategoryDTO category = categoryService.findById(id);

    return ResponseEntity.status(HttpStatus.OK).body(category);
  }
  
  @PostMapping
  public ResponseEntity<?> create(@RequestBody @Valid CategoryDTO categoryDTO) {
    CategoryDTO savedCategory = categoryService.save(categoryDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(
    @PathVariable Long id, 
    @RequestBody @Valid CategoryDTO categoryDTO
  ) {
    CategoryDTO updatedCategory = categoryService.update(id, categoryDTO);

    return ResponseEntity.status(HttpStatus.OK).body(updatedCategory);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable Long id) {
    categoryService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
