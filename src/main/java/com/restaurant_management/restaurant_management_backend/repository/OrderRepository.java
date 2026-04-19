package com.restaurant_management.restaurant_management_backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurant_management.restaurant_management_backend.dto.OrderStatusCountDTO;
import com.restaurant_management.restaurant_management_backend.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

  @Query("SELECT DISTINCT o FROM Order o " +
         "LEFT JOIN FETCH o.table " +
         "LEFT JOIN FETCH o.items i " +
         "LEFT JOIN FETCH i.product p " +
         "LEFT JOIN FETCH p.category " +
         "LEFT JOIN FETCH o.transactions t " +
         "WHERE o.id = :id")
  Optional<Order> findByIdWithDetails(@Param("id") Long id);

  @Query("SELECT o FROM Order o WHERE o.table.id = :tableId AND o.status IN ('CREATED', 'IN_PROGRESS', 'READY', 'PARTIALLY_PAID') ORDER BY o.id DESC")
  Optional<Order> findActiveOrderByTableId(@Param("tableId") Long tableId);

  @Query("SELECT o FROM Order o Where " +
         "o.createdAt >= :startDate AND " +
         "o.createdAt < :endDate")
  List<Order> findOrdersByDate(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
  );

  @Query("SELECT new com.restaurant_management.restaurant_management_backend.dto.OrderStatusCountDTO(" +
       "o.status, COUNT(o)) " +
       "FROM Order o " +
       "WHERE o.createdAt >= :startDate AND " +
       "o.createdAt < :endDate " +
       "GROUP BY o.status")
  List<OrderStatusCountDTO> countOrdersByStatusAndDate(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  @Query("SELECT DISTINCT o FROM Order o " +
         "LEFT JOIN FETCH o.table " +
         "LEFT JOIN FETCH o.items i " +
         "LEFT JOIN FETCH i.product p " +
         "LEFT JOIN FETCH p.category " +
         "LEFT JOIN FETCH o.transactions " +
         "WHERE o.status IN ('CREATED', 'IN_PROGRESS', 'READY', 'PARTIALLY_PAID') " +
         "ORDER BY o.id DESC")
  List<Order> findActiveOrder();

  @Query("SELECT COUNT(DISTINCT o.id) FROM Order o " +
         "JOIN o.transactions t " +
         "WHERE t.transactionDate >= :startDate " +
         "AND t.transactionDate < :endDate " +
         "AND t.status = 'COMPLETED'")
  Long countPaidOrdersByDate(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
  );

}
