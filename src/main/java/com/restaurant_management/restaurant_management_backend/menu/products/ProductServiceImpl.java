package com.restaurant_management.restaurant_management_backend.menu.products;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.menu.categories.CategoryRepository;
import com.restaurant_management.restaurant_management_backend.menu.categories.entity.Category;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.CreateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.request.UpdateProductRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final ProductMapper productMapper;

  @Override
  public ProductResponse save(CreateProductRequest req) {
    Category category = categoryRepository.findById(req.categoryId())
      .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

    Product newProduct = productMapper.toEntity(req);
    newProduct.setCategory(category);

    return productMapper.toResponse(productRepository.save(newProduct));
  }

  @Override
  public ProductResponse findById(Long id) {
    Product product = productRepository.findByIdWithCategory(id)
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    return productMapper.toResponse(product);
  }

  @Override
  public List<ProductResponse> findAll() {
    return productRepository.findAllWithCategory().stream()
      .map(productMapper::toResponse)
      .toList();
  }

  @Override
  public List<ProductResponse> findByCategory(Long categoryId) {
    return productRepository.findByCategoryId(categoryId).stream()
      .map(productMapper::toResponse)
      .toList();
  }

  @Override
  @Transactional
  public ProductResponse update(Long id, UpdateProductRequest req) {
    Product product = productRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    Category category = categoryRepository.findById(req.categoryId())
      .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

    product.setCategory(category);
    product.setName(req.name());
    product.setPrice(req.price());

    return productMapper.toResponse(productRepository.save(product));
  }

  @Override
  public void delete(Long id) {
    if (!productRepository.existsById(id)) {
      throw new ResourceNotFoundException("Producto no existe");
    }

    productRepository.deleteById(id);
  }

}
