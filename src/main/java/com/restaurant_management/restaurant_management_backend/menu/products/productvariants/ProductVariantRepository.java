package com.restaurant_management.restaurant_management_backend.menu.products.productvariants;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.entity.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

  @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId ORDER BY pv.sortOrder ASC, pv.id ASC")
  List<ProductVariant> findByProductId(@Param("productId") Long productId);

  @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.isAvailable = true ORDER BY pv.sortOrder ASC, pv.id ASC")
  List<ProductVariant> findAvailableByProductId(@Param("productId") Long productId);

  @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id")
  Optional<ProductVariant> findById(@Param("id") Long id);

  void deleteByProductId(Long productId);
}
