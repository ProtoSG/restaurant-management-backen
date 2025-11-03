package com.restaurant_management.restaurant_management_backend.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.dto.ProductDTO;
import com.restaurant_management.restaurant_management_backend.entity.Category;
import com.restaurant_management.restaurant_management_backend.entity.Product;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.mapper.ProductMapper;
import com.restaurant_management.restaurant_management_backend.repository.CategoryRepository;
import com.restaurant_management.restaurant_management_backend.repository.ProductRepository;
import com.restaurant_management.restaurant_management_backend.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final ProductMapper productMapper;

  @Transactional
  public ProductDTO save(ProductDTO productDTO) {
    Category category = categoryRepository.findById(productDTO.getCategoryId())
      .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

    Product newProduct = productMapper.toEntity(productDTO);
    newProduct.setCategory(category);

    return productMapper.toDto(productRepository.save(newProduct));
  }

  public ProductDTO findById(Long id) {
    Product product = productRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    return productMapper.toDto(product);
  }

  public List<ProductDTO> findAll() {
    List<Product> products = productRepository.findAll();

    return products.stream()
      .map(productMapper::toDto)
      .toList();
  }

  @Transactional
  public ProductDTO update(Long id, ProductDTO productDTO) {
    Product product = productRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    Category category = categoryRepository.findById(productDTO.getCategoryId())
      .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

    product.setCategory(category);
    product.setName(productDTO.getName());
    product.setPrice(productDTO.getPrice());

    return productMapper.toDto(productRepository.save(product));
  }

  public void delete(Long id) {
    if (!productRepository.existsById(id)) {
      throw new ResourceNotFoundException("Producto no existe");
    }

    productRepository.deleteById(id);
  }

  public List<ProductDTO> findByCategoryId(Long id) {
    Category category = categoryRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

    List<Product> products = productRepository.findByCategoryId(category.getId());

    return products.stream()
      .map(productMapper::toDto)
      .toList();
  }
}
