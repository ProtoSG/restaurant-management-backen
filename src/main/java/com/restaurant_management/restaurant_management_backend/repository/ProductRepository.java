package com.restaurant_management.restaurant_management_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant_management.restaurant_management_backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

  
}
