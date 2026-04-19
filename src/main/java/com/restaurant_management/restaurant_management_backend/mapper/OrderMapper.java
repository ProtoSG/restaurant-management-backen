package com.restaurant_management.restaurant_management_backend.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.entity.Order;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderMapper {

  private final OrderItemMapper orderItemMapper;

  public OrderDTO toDto(Order order) {
    if ( order == null ) return null;

    return OrderDTO.builder()
      .id(order.getId())
      .orderCode(order.getOrderCode())
      .tableId(order.getTable() != null ? order.getTable().getId() : null)
      .tableNumber(order.getTable() != null ? order.getTable().getNumber() : null)
      .type(order.getType())
      .customerName(order.getCustomerName())
      .status(order.getStatus())
      .total(order.getTotal())
      .paidAmount(order.getPaidAmount())
      .remainingAmount(order.getRemainingAmount())
      .items(orderItemMapper.toDtoList(order.getItems()))
      .build();
  }

  public List<OrderDTO> toDtoList(List<Order> orders) {
    return orders.stream()
      .map(this::toDto)
      .toList();
  }

  public Order toEntity(OrderDTO orderDTO) {
    if ( orderDTO == null ) return null;

    return Order.builder()
      .id(orderDTO.getId())
      .orderCode(orderDTO.getOrderCode())
      .type(orderDTO.getType())
      .customerName(orderDTO.getCustomerName())
      .status(orderDTO.getStatus())
      .total(orderDTO.getTotal())
      .build();
  }
  
}
