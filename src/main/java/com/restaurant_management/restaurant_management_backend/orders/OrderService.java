package com.restaurant_management.restaurant_management_backend.orders;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.CreateOrderRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.PartialPaymenRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.UpdatedOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.ActiveOrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;

public interface OrderService {
  
  public OrderResponse save(CreateOrderRequest req);
  public OrderResponse findById(Long id);
  public List<OrderResponse> findAll();
  public void delete(Long id);

  public OrderResponse changeTable(Long orderId, Long tableId, User user);
  public ActiveOrderResponse findActiveOrderByTable(Long tableId);

  // NOTE: Actualmente no se usa en el frontend
  // public OrderResponse changeType(Long id, OrderTypeDTO type);
  public void cancelOrder(Long id);
  public void addOrderItem(Long orderId, AddOrderItemRequest req);
  public void updateOrderItem(Long orderId, Long itemId, UpdatedOrderItemRequest req);
  public void removeOrderItemByOrderId(Long orderId, Long itemId);

  // NOTE: verificar si es necesario retornar algo
  public OrderResponse markAsReady(Long orderId);
  public OrderResponse markAsPending(Long orderId);
  public OrderResponse payOrder(Long orderId, PaymentMethodType paymentMethodType, User user);
  public OrderResponse payPartialOrder(Long orderId, PartialPaymenRequest paymentDTO, User user);

  public List<OrderResponse> findActiveOrder();

}
