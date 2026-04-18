package com.restaurant_management.restaurant_management_backend.service;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.dto.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderTypeDTO;
import com.restaurant_management.restaurant_management_backend.dto.PartialPaymentDTO;
import com.restaurant_management.restaurant_management_backend.dto.UpdateOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.enums.PaymentMethodType;

public interface OrderService {
  
  public OrderDTO save(OrderDTO orderDTO);
  public OrderDTO findById(Long id);
  public List<OrderDTO> findAll();
  public void delete(Long id);

  public OrderDTO changeTable(Long orderId, Long tableId, User user);
  public OrderDTO changeType(Long id, OrderTypeDTO type);
  public OrderDTO cancelOrder(Long id);
  public void addProducts(Long orderId, OrderItemsDTO orderItems);

  public OrderItemDTO addOrderItemByOrderId(Long orderId, AddOrderItemRequest request);
  public OrderItemDTO updateOrderItemByOrderId(Long orderId, Long itemId, UpdateOrderItemRequest request);
  public void removeOrderItemByOrderId(Long orderId, Long itemId);

  public OrderDTO markAsReady(Long orderId);
  public OrderDTO payOrder(Long orderId, PaymentMethodType paymentMethodType, User user);
  public OrderDTO payPartialOrder(Long orderId, PartialPaymentDTO paymentDTO, User user);

  public List<OrderDTO> findActiveOrder();
}
