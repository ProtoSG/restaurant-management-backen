package com.restaurant_management.restaurant_management_backend.mapper;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.entity.OrderItem;

@Component
public class OrderItemMapper {

  public OrderItem toEntity(OrderItemDTO orderItemDTO) {
    if (orderItemDTO == null) return null;

    return OrderItem.builder()
      .id(orderItemDTO.getId())
      .quantity(orderItemDTO.getQuantity())
      .subTotal(orderItemDTO.getSubTotal())
      .build();
  }

  public OrderItemDTO toDto(OrderItem orderItem) {
    if (orderItem == null) return null;

    return OrderItemDTO.builder()
      .id(orderItem.getId())
      .productId(orderItem.getProduct().getId())
      .orderId(orderItem.getOrder().getId())
      .quantity(orderItem.getQuantity())
      .subTotal(orderItem.getSubTotal())
      .build();
  }
}
