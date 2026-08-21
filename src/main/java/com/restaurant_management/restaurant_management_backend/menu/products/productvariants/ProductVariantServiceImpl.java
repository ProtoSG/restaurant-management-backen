package com.restaurant_management.restaurant_management_backend.menu.products.productvariants;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.request.CreateProductVariantRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.request.UpdateProductVariantRequest;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.response.ProductVariantResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.entity.ProductVariant;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

  private final ProductVariantRepository variantRepository;
  private final ProductRepository productRepository;
  private final ProductVariantMapper variantMapper;

  @Override
  public List<ProductVariantResponse> findByProductId(Long productId) {
    return variantRepository.findByProductId(productId).stream()
      .map(variantMapper::toResponse)
      .collect(Collectors.toList());
  }

  @Override
  public List<ProductVariantResponse> findAvailableByProductId(Long productId) {
    return variantRepository.findAvailableByProductId(productId).stream()
      .map(variantMapper::toResponse)
      .collect(Collectors.toList());
  }

  @Override
  public ProductVariantResponse findById(Long id) {
    ProductVariant variant = variantRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));
    return variantMapper.toResponse(variant);
  }

  @Override
  @Transactional
  public ProductVariantResponse save(Long productId, CreateProductVariantRequest req) {
    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    ProductVariant variant = variantMapper.toEntity(req, product);
    return variantMapper.toResponse(variantRepository.save(variant));
  }

  @Override
  @Transactional
  public ProductVariantResponse update(Long id, UpdateProductVariantRequest req) {
    ProductVariant variant = variantRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));
    variant.setName(req.name());
    variant.setPrice(req.price());
    variant.setSortOrder(req.sortOrder() != null ? req.sortOrder() : variant.getSortOrder());
    return variantMapper.toResponse(variantRepository.save(variant));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    if (!variantRepository.existsById(id)) {
      throw new ResourceNotFoundException("Variante no encontrada");
    }
    variantRepository.deleteById(id);
  }
}
