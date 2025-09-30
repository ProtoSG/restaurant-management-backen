package com.restaurant_management.restaurant_management_backend.service;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.dto.ProductDTO;

public interface ProductService {

  public ProductDTO save(ProductDTO productDTO);
  public ProductDTO findById(Long id);
  public List<ProductDTO> findAll();
  public ProductDTO update(Long id, ProductDTO productDTO);
  public void delete(Long id);

}
