package com.restaurant_management.restaurant_management_backend.menu.products.productvariants;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.request.CreateProductVariantRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.request.UpdateProductVariantRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.response.ProductVariantResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products/{productId}/variants")
@RequiredArgsConstructor
public class ProductVariantController {

  private final ProductVariantService variantService;

  @GetMapping
  public ResponseEntity<List<ProductVariantResponse>> getByProductId(@PathVariable Long productId) {
    return ResponseEntity.ok(variantService.findByProductId(productId));
  }

  @GetMapping("/available")
  public ResponseEntity<List<ProductVariantResponse>> getAvailableByProductId(@PathVariable Long productId) {
    return ResponseEntity.ok(variantService.findAvailableByProductId(productId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductVariantResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(variantService.findById(id));
  }

  @PostMapping
  public ResponseEntity<ProductVariantResponse> create(
      @PathVariable Long productId,
      @RequestBody @Valid CreateProductVariantRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(variantService.save(productId, req));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProductVariantResponse> update(
      @PathVariable Long productId,
      @PathVariable Long id,
      @RequestBody @Valid UpdateProductVariantRequest req) {
    return ResponseEntity.ok(variantService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    variantService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
