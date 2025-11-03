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

import com.restaurant_management.restaurant_management_backend.dto.ProductDTO;
import com.restaurant_management.restaurant_management_backend.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @PostMapping
  public ResponseEntity<?> create(
    @RequestBody @Valid ProductDTO productDTO
  ) {
    ProductDTO savedProduct = productService.save(productDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
  }

  @GetMapping
  public ResponseEntity<?> getAll() {
    List<ProductDTO> products = productService.findAll();

    return ResponseEntity.status(HttpStatus.OK).body(products);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(
    @PathVariable Long id
  ) {
    ProductDTO productDTO = productService.findById(id);

    return ResponseEntity.status(HttpStatus.OK).body(productDTO);
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(
      @PathVariable Long id,
      @RequestBody @Valid ProductDTO productDTO
  ) {
    ProductDTO updatedProduct = productService.update(id, productDTO);

    return ResponseEntity.status(HttpStatus.OK).body(updatedProduct);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(
    @PathVariable Long id
  ) {
    productService.delete(id);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/categories/{id}")
  public ResponseEntity<?> getByCategoryId(@PathVariable Long id) {
    List<ProductDTO> products = productService.findByCategoryId(id);

    return ResponseEntity.status(HttpStatus.OK).body(products);
  }
}
