package com.restaurant_management.restaurant_management_backend.menu.products;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

  @Query("SELECT p FROM Product p JOIN FETCH p.category")
  List<Product> findAllWithCategory();

  @Query(value = "SELECT p FROM Product p JOIN FETCH p.category",
      countQuery = "SELECT COUNT(p) FROM Product p")
  Page<Product> findAllWithCategory(Pageable pageable);

  @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :categoryId")
  List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

  @Query(value = "SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :categoryId",
      countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
  Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

  @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
  Optional<Product> findByIdWithCategory(@Param("id") Long id);

  @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.isAvailable = true")
  List<Product> findAllAvailableWithCategory();

  @Query(value = "SELECT p FROM Product p JOIN FETCH p.category WHERE p.isAvailable = true",
      countQuery = "SELECT COUNT(p) FROM Product p WHERE p.isAvailable = true")
  Page<Product> findAllAvailableWithCategory(Pageable pageable);

}
