package com.restaurant_management.restaurant_management_backend.menu.products;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
  private final ProductImageStorageService productImageStorageService;

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
  public Page<ProductResponse> findAll(Pageable pageable) {
    return productRepository.findAllWithCategory(pageable)
      .map(productMapper::toResponse);
  }

  @Override
  public List<ProductResponse> findAllAvailable() {
    return productRepository.findAllAvailableWithCategory().stream()
      .map(productMapper::toResponse)
      .toList();
  }

  @Override
  public Page<ProductResponse> findAllAvailable(Pageable pageable) {
    return productRepository.findAllAvailableWithCategory(pageable)
      .map(productMapper::toResponse);
  }

  @Override
  public List<ProductResponse> findByCategory(Long categoryId) {
    return productRepository.findByCategoryId(categoryId).stream()
      .map(productMapper::toResponse)
      .toList();
  }

  @Override
  public Page<ProductResponse> findByCategory(Long categoryId, Pageable pageable) {
    return productRepository.findByCategoryId(categoryId, pageable)
      .map(productMapper::toResponse);
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
  @Transactional
  public ProductResponse toggleAvailable(Long id) {
    Product product = productRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    product.setIsAvailable(!product.getIsAvailable());
    return productMapper.toResponse(productRepository.save(product));
  }

  @Override
  public void delete(Long id) {
    if (!productRepository.existsById(id)) {
      throw new ResourceNotFoundException("Producto no existe");
    }

    productRepository.deleteById(id);
  }

  @Override
  @Transactional
  public ProductResponse uploadImage(Long id, MultipartFile image) {
    Product product = productRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    String previousImageUrl = product.getImageUrl();
    String newImageUrl = productImageStorageService.upload(id, image);
    product.setImageUrl(newImageUrl);
    ProductResponse response = productMapper.toResponse(productRepository.save(product));

    // Solo después de guardar la referencia nueva — si el borrado del objeto viejo falla, la
    // foto del producto sigue siendo la nueva de todos modos (best-effort, ver el javadoc del
    // método: nunca debe tumbar esta operación).
    productImageStorageService.deleteIfManaged(previousImageUrl);

    return response;
  }

  @Override
  @Transactional
  public ProductResponse deleteImage(Long id) {
    Product product = productRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    productImageStorageService.deleteIfManaged(product.getImageUrl());
    product.setImageUrl(null);

    return productMapper.toResponse(productRepository.save(product));
  }

}
