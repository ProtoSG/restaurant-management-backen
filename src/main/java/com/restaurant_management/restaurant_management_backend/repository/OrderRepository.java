package com.restaurant_management.restaurant_management_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurant_management.restaurant_management_backend.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

  @Query("SELECT o FROM Order o WHERE o.table.id = :tableId AND o.status IN ('CREATED', 'IN_PROGRESS', 'READY') ORDER BY o.id DESC")
  Optional<Order> findActiveOrderByTableId(@Param("tableId") Long tableId);
  
}
