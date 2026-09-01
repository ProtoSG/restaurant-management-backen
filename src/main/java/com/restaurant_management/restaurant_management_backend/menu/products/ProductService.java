package com.restaurant_management.restaurant_management_backend.menu.products;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.CreateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.UpdateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;

public interface ProductService {

  public ProductResponse save(CreateProductRequest req);
  public ProductResponse findById(Long id);
  public List<ProductResponse> findAll();
  public Page<ProductResponse> findAll(Pageable pageable);
  public List<ProductResponse> findAllAvailable();
  public Page<ProductResponse> findAllAvailable(Pageable pageable);
  public List<ProductResponse> findByCategory(Long categoryId);
  public Page<ProductResponse> findByCategory(Long categoryId, Pageable pageable);
  public ProductResponse update(Long id, UpdateProductRequest req);
  public ProductResponse toggleAvailable(Long id);
  public void delete(Long id);
  public ProductResponse uploadImage(Long id, MultipartFile image);
  public ProductResponse deleteImage(Long id);

}
