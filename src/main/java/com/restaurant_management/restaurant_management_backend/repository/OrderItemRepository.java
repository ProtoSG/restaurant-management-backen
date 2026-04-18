package com.restaurant_management.restaurant_management_backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurant_management.restaurant_management_backend.dto.TopSellingProductDTO;
import com.restaurant_management.restaurant_management_backend.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  Optional<OrderItem> findByProductId(Long productId);

  @Query("SELECT new com.restaurant_management.restaurant_management_backend.dto.TopSellingProductDTO(" +
       "p.id, " +
       "p.name, " +
       "c.name, " +
       "SUM(oi.quantity), " +
       "SUM(oi.subTotal)) " +
       "FROM OrderItem oi " +
       "JOIN oi.product p " +
       "JOIN p.category c " +
       "JOIN oi.order o " +
       "WHERE o.status = 'PAID' " +
       "GROUP BY p.id, p.name, c.name " +
       "ORDER BY SUM(oi.quantity) DESC")
  List<TopSellingProductDTO> findTopSellingProducts(Pageable pageable);

  @Query("SELECT new com.restaurant_management.restaurant_management_backend.dto.TopSellingProductDTO(" +
       "p.id, " +
       "p.name, " +
       "c.name, " +
       "SUM(oi.quantity), " +
       "SUM(oi.subTotal)) " +
       "FROM OrderItem oi " +
       "JOIN oi.product p " +
       "JOIN p.category c " +
       "JOIN oi.order o " +
       "JOIN o.transactions t " +
       "WHERE t.status = 'COMPLETED' " +
       "AND t.transactionDate >= :startDate " +
       "AND t.transactionDate < :endDate " +
       "GROUP BY p.id, p.name, c.name " +
       "ORDER BY SUM(oi.quantity) DESC")
  List<TopSellingProductDTO> findTopSellingProductsByDateRange(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate,
    Pageable pageable
  );

  @Query("SELECT new com.restaurant_management.restaurant_management_backend.dto.TopSellingProductDTO(" +
       "p.id, " +
       "p.name, " +
       "c.name, " +
       "SUM(oi.quantity), " +
       "SUM(oi.subTotal)) " +
       "FROM OrderItem oi " +
       "JOIN oi.product p " +
       "JOIN p.category c " +
       "JOIN oi.order o " +
       "JOIN o.transactions t " +
       "WHERE t.status = 'COMPLETED' " +
       "AND t.transactionDate >= :startDate " +
       "AND t.transactionDate < :endDate " +
       "AND p.category.id = :categoryId " +
       "GROUP BY p.id, p.name, c.name " +
       "ORDER BY SUM(oi.quantity) DESC")
  List<TopSellingProductDTO> findTopSellingProductsByCategoryAndDateRange(
    @Param("categoryId") Long categoryId,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate,
    Pageable pageable
  );

}
