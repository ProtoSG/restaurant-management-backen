package com.restaurant_management.restaurant_management_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant_management.restaurant_management_backend.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

  
}
