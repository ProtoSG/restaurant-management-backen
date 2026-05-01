package com.restaurant_management.restaurant_management_backend.menu.products;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.CreateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.UpdateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;

public interface ProductService {

  public ProductResponse save(CreateProductRequest req);
  public ProductResponse findById(Long id);
  public List<ProductResponse> findAll();
  public List<ProductResponse> findByCategory(Long categoryId);
  public ProductResponse update(Long id, UpdateProductRequest req);
  public void delete(Long id);

}
