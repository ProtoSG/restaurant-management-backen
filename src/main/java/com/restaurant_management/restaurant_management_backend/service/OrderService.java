package com.restaurant_management.restaurant_management_backend.service;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;

public interface OrderService {
  
  public OrderDTO save(OrderDTO orderDTO);

  public void addProducts(Long orderId, List<OrderItemDTO> orderItems);
  
}
