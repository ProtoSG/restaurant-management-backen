package com.restaurant_management.restaurant_management_backend.menu.products;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.CreateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.UpdateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;
import com.restaurant_management.restaurant_management_backend.shared.dto.response.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @PostMapping
  public ResponseEntity<ProductResponse> create(
    @RequestBody @Valid CreateProductRequest req
  ) {
    ProductResponse product = productService.save(req);

    return ResponseEntity.status(HttpStatus.CREATED).body(product);
  }

  @GetMapping
  public ResponseEntity<PageResponse<ProductResponse>> getAll(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, size);
    Page<ProductResponse> products = productService.findAll(pageable);

    return ResponseEntity.ok(toPageResponse(products));
  }

  @GetMapping("/available")
  public ResponseEntity<PageResponse<ProductResponse>> getAvailable(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, size);
    Page<ProductResponse> products = productService.findAllAvailable(pageable);

    return ResponseEntity.ok(toPageResponse(products));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> getById(
    @PathVariable Long id
  ) {
    ProductResponse product = productService.findById(id);

    return ResponseEntity.status(HttpStatus.OK).body(product);
  }

  @GetMapping("/categories/{categoryId}")
  public ResponseEntity<PageResponse<ProductResponse>> getByCategory(
    @PathVariable Long categoryId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, size);
    Page<ProductResponse> products = productService.findByCategory(categoryId, pageable);

    return ResponseEntity.ok(toPageResponse(products));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProductResponse> update(
      @PathVariable Long id,
      @RequestBody @Valid UpdateProductRequest req
  ) {
    ProductResponse product = productService.update(id, req);

    return ResponseEntity.status(HttpStatus.OK).body(product);
  }

  @PatchMapping("/{id}/toggle")
  public ResponseEntity<ProductResponse> toggleAvailable(@PathVariable Long id) {
    return ResponseEntity.ok(productService.toggleAvailable(id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
    @PathVariable Long id
  ) {
    productService.delete(id);

    return ResponseEntity.noContent().build();
  }

  private <T> PageResponse<T> toPageResponse(Page<T> page) {
    return new PageResponse<>(
      page.getContent(),
      page.getNumber(),
      page.getSize(),
      page.getTotalElements(),
      page.getTotalPages()
    );
  }

}
