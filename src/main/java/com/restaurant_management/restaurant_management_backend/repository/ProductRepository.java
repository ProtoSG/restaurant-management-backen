package com.restaurant_management.restaurant_management_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant_management.restaurant_management_backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

  List<Product> findByCategoryId(Long id);

}
