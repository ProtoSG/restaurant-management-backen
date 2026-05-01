package com.restaurant_management.restaurant_management_backend.orders;

import java.util.List;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.orders.dto.request.CreateOrderRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderMapper {

  private final OrderItemMapper orderItemMapper;

  public OrderResponse toResponse(Order order) {
    if ( order == null ) return null;

    var table = order.getTable();
    return new OrderResponse(
      order.getId(),
      order.getOrderCode(),
      table != null ? table.getId() : null,
      table != null ? table.getNumber() : null,
      order.getType(),
      order.getCustomerName(), 
      order.getStatus(), 
      order.getTotal(), 
      order.getPaidAmount(),
      order.getRemainingAmount(), 
      orderItemMapper.toResponseList(order.getItems())
    );
  }

  public List<OrderResponse> toResponseList(List<Order> orders) {
    return orders.stream()
      .map(this::toResponse)
      .toList();
  }

  public Order toEntity(CreateOrderRequest req) {
    if ( req == null ) return null;

    return Order.builder()
      .type(req.type())
      .customerName(req.customerName())
      .build();
  }

}
