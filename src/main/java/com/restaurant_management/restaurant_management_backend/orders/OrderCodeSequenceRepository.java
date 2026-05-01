package com.restaurant_management.restaurant_management_backend.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.restaurant_management.restaurant_management_backend.orders.entity.OrderCodeSequence;

import jakarta.persistence.LockModeType;

public interface OrderCodeSequenceRepository extends JpaRepository<OrderCodeSequence, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM OrderCodeSequence s WHERE s.id = 1")
  OrderCodeSequence findSequenceForUpdate();

}
