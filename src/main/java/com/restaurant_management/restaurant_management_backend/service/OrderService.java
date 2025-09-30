package com.restaurant_management.restaurant_management_backend.service;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderTypeDTO;

public interface OrderService {
  
  public OrderDTO save(OrderDTO orderDTO);
  public OrderDTO findById(Long id);
  public List<OrderDTO> findAll();
  public void delete(Long id);

  public OrderDTO changeTable(Long orderId, Long tableId);
  public OrderDTO changeType(Long id, OrderTypeDTO type);
  public OrderDTO cancelOrder(Long id);
  public void addProducts(Long orderId, OrderItemsDTO orderItems);

}
