package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.restaurant_management.restaurant_management_backend.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.enums.OrderType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class OrderDTO {

  private Long id;

  private String orderCode;

  private Long tableId;

  private String tableNumber;

  private OrderType type;

  private String customerName;

  private OrderStatus status;

  private BigDecimal total;

  // Campos para pagos parciales
  private BigDecimal paidAmount;
  
  private BigDecimal remainingAmount;

  private List<OrderItemDTO> items;

}
