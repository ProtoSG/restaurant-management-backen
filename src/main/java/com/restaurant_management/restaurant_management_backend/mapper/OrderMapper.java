package com.restaurant_management.restaurant_management_backend.mapper;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.entity.Order;

import lombok.RequiredArgsConstructor;

@Component
public class OrderMapper {

  public OrderDTO toDto(Order order) {
    if ( order == null ) return null;

    return OrderDTO.builder()
      .id(order.getId())
      .orderCode(order.getOrderCode())
      .tableId(order.getTable().getId())
      .type(order.getType())
      .status(order.getStatus())
      .total(order.getTotal())
      .build();
  }

  public Order toEntity(OrderDTO orderDTO) {
    if ( orderDTO == null ) return null;

    return Order.builder()
      .id(orderDTO.getId())
      .orderCode(orderDTO.getOrderCode())
      .type(orderDTO.getType())
      .status(orderDTO.getStatus())
      .total(orderDTO.getTotal())
      .build();
  }
  
}
