package com.restaurant_management.restaurant_management_backend.orders;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.CreateOrderRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.PartialPaymentRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.UpdatedOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.ActiveOrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @GetMapping
  public ResponseEntity<List<OrderResponse>> getAll() {
    List<OrderResponse> orders = orderService.findAll();

    return ResponseEntity.status(HttpStatus.OK).body(orders);
  }

  @GetMapping("/active")
  public ResponseEntity<List<OrderResponse>> getActive() {
    List<OrderResponse> orders = orderService.findActiveOrder();

    return ResponseEntity.status(HttpStatus.OK).body(orders);
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
    OrderResponse order = orderService.findById(id);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PostMapping
  public ResponseEntity<OrderResponse> create(@RequestBody @Valid CreateOrderRequest req) {
    OrderResponse order = orderService.save(req);

    return ResponseEntity.status(HttpStatus.CREATED).body(order);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    orderService.delete(id);

    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{orderId}/table/{tableId}")
  public ResponseEntity<OrderResponse> changeTable(
    @PathVariable Long orderId,
    @PathVariable Long tableId
  ) {
    OrderResponse order = orderService.changeTable(orderId, tableId);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
    orderService.cancelOrder(id);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{orderId}/items")
  public ResponseEntity<Void> addOrderItem(
    @PathVariable Long orderId,
    @RequestBody @Valid AddOrderItemRequest request
  ) {
    orderService.addOrderItem(orderId, request);

    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{orderId}/items/{itemId}")
  public ResponseEntity<Void> updateOrderItem(
    @PathVariable Long orderId,
    @PathVariable Long itemId,
    @RequestBody @Valid UpdatedOrderItemRequest request
  ) {
    orderService.updateOrderItem(orderId, itemId, request);

    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{orderId}/items/{itemId}")
  public ResponseEntity<Void> removeOrderItem(
    @PathVariable Long orderId,
    @PathVariable Long itemId
  ) {
    orderService.removeOrderItemByOrderId(orderId, itemId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/ready")
  public ResponseEntity<OrderResponse> markAsReady(@PathVariable Long id) {
    OrderResponse orderDTO = orderService.markAsReady(id);
    return ResponseEntity.status(HttpStatus.OK).body(orderDTO);
  }

  @PostMapping("/{id}/pay/{paymentMethod}")
  public ResponseEntity<OrderResponse> payOrder(
      @PathVariable Long id,
      @PathVariable PaymentMethodType paymentMethod
  ) {
    OrderResponse orderDTO = orderService.payOrder(id, paymentMethod);

    return ResponseEntity.status(HttpStatus.OK).body(orderDTO);
  }

  @PostMapping("/{id}/pay-partial")
  public ResponseEntity<OrderResponse> payPartialOrder(
      @PathVariable Long id,
      @RequestBody @Valid PartialPaymentRequest paymentDTO
  ) {
    OrderResponse orderDTO = orderService.payPartialOrder(id, paymentDTO);

    return ResponseEntity.status(HttpStatus.OK).body(orderDTO);
  }

  @GetMapping("/active/tables/{id}")
  public ResponseEntity<ActiveOrderResponse> getActiveOrder(@PathVariable Long id) {
    ActiveOrderResponse activeOrder = orderService.findActiveOrderByTable(id);

    return ResponseEntity.status(HttpStatus.OK).body(activeOrder);
  }

}
