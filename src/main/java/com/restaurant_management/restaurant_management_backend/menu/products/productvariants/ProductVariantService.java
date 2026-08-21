package com.restaurant_management.restaurant_management_backend.menu.products.productvariants;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.request.CreateProductVariantRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.request.UpdateProductVariantRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.response.ProductVariantResponse;

public interface ProductVariantService {
  List<ProductVariantResponse> findByProductId(Long productId);
  List<ProductVariantResponse> findAvailableByProductId(Long productId);
  ProductVariantResponse findById(Long id);
  ProductVariantResponse save(Long productId, CreateProductVariantRequest req);
  ProductVariantResponse update(Long id, UpdateProductVariantRequest req);
  void delete(Long id);
}
