package com.restaurant_management.restaurant_management_backend.menu.products;

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

import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.CreateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.UpdateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<ProductResponse> create(
    @RequestBody @Valid CreateProductRequest req
  ) {
    ProductResponse product = productService.save(req);

    return ResponseEntity.status(HttpStatus.CREATED).body(product);
  }

  @GetMapping
  public ResponseEntity<List<ProductResponse>> getAll() {
    List<ProductResponse> products = productService.findAll();

    return ResponseEntity.status(HttpStatus.OK).body(products);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> getById(
    @PathVariable Long id
  ) {
    ProductResponse product = productService.findById(id);

    return ResponseEntity.status(HttpStatus.OK).body(product);
  }

  @GetMapping("/categories/{categoryId}")
  public ResponseEntity<List<ProductResponse>> getByCategory(
    @PathVariable Long categoryId
  ) {
    List<ProductResponse> products = productService.findByCategory(categoryId);

    return ResponseEntity.status(HttpStatus.OK).body(products);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public ResponseEntity<ProductResponse> update(
      @PathVariable Long id,
      @RequestBody @Valid UpdateProductRequest req
  ) {
    ProductResponse product = productService.update(id, req);

    return ResponseEntity.status(HttpStatus.OK).body(product);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
    @PathVariable Long id
  ) {
    productService.delete(id);

    return ResponseEntity.noContent().build();
  }

}
