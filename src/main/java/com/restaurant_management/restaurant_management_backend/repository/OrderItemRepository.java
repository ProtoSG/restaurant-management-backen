package com.restaurant_management.restaurant_management_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant_management.restaurant_management_backend.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  
}
