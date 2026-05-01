package com.restaurant_management.restaurant_management_backend.menu.products;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

  @Query("SELECT p FROM Product p JOIN FETCH p.category")
  List<Product> findAllWithCategory();

  @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :categoryId")
  List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

  @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
  Optional<Product> findByIdWithCategory(@Param("id") Long id);

}
