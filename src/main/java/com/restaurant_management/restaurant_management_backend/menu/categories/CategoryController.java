package com.restaurant_management.restaurant_management_backend.menu.categories;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.menu.categories.dto.request.CreateCategoryRequest;
import com.restaurant_management.restaurant_management_backend.menu.categories.dto.request.UpdateCategoryRequest;
import com.restaurant_management.restaurant_management_backend.menu.categories.dto.response.CategoryResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  @GetMapping
  public ResponseEntity<List<CategoryResponse>> getAll() {
    List<CategoryResponse> categories = categoryService.findAll();

    return ResponseEntity.status(HttpStatus.OK).body(categories);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable Long id) {
    CategoryResponse category = categoryService.findById(id);

    return ResponseEntity.status(HttpStatus.OK).body(category);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<?> create(@RequestBody @Valid CreateCategoryRequest req) {
    CategoryResponse savedCategory = categoryService.save(req);

    return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public ResponseEntity<?> update(
    @PathVariable Long id,
    @RequestBody @Valid UpdateCategoryRequest req
  ) {
    CategoryResponse updatedCategory = categoryService.update(id, req);

    return ResponseEntity.status(HttpStatus.OK).body(updatedCategory);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable Long id) {
    categoryService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
