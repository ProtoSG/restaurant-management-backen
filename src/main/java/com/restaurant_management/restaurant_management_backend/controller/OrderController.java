package com.restaurant_management.restaurant_management_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.dto.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderTypeDTO;
import com.restaurant_management.restaurant_management_backend.dto.PartialPaymentDTO;
import com.restaurant_management.restaurant_management_backend.dto.TransactionDTO;
import com.restaurant_management.restaurant_management_backend.dto.UpdateOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.service.OrderService;
import com.restaurant_management.restaurant_management_backend.service.ThermalPrinterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;
  private final ThermalPrinterService thermalPrinterService;

  @GetMapping
  public ResponseEntity<?> getAll() {
    List<OrderDTO> orders = orderService.findAll();

    return ResponseEntity.status(HttpStatus.OK).body(orders);
  }

  @GetMapping("/active")
  public ResponseEntity<?> getActive() {
    List<OrderDTO> orders = orderService.findActiveOrder();

    return ResponseEntity.status(HttpStatus.OK).body(orders);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable Long id) {
    OrderDTO order = orderService.findById(id);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @PostMapping
  public ResponseEntity<?> create(@RequestBody @Valid OrderDTO orderDTO) {
    OrderDTO order = orderService.save(orderDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(order);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable Long id) {
    orderService.delete(id);

    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @PutMapping("/{orderId}/table/{tableId}")
  public ResponseEntity<?> changeTable(
    @PathVariable Long orderId,
    @PathVariable Long tableId,
    @AuthenticationPrincipal User user
  ) {
    OrderDTO order = orderService.changeTable(orderId, tableId, user);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
  @PutMapping("/{id}/type")
  public ResponseEntity<?> changeType(
    @PathVariable Long id,
    @RequestBody @Valid OrderTypeDTO type
  ) {
    OrderDTO order = orderService.changeType(id, type);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
  @PostMapping("/{id}/cancel")
  public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
    OrderDTO order = orderService.cancelOrder(id);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @PostMapping("/{id}/products")
  public ResponseEntity<?> addProducts(
    @PathVariable Long id,
    @RequestBody @Valid OrderItemsDTO orderItemsDTO
  ) {
    orderService.addProducts(id, orderItemsDTO);

    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @PostMapping("/{orderId}/items")
  public ResponseEntity<?> addOrderItem(
    @PathVariable Long orderId,
    @RequestBody @Valid AddOrderItemRequest request
  ) {
    OrderItemDTO orderItem = orderService.addOrderItemByOrderId(orderId, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(orderItem);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @PutMapping("/{orderId}/items/{itemId}")
  public ResponseEntity<?> updateOrderItem(
    @PathVariable Long orderId,
    @PathVariable Long itemId,
    @RequestBody @Valid UpdateOrderItemRequest request
  ) {
    OrderItemDTO orderItem = orderService.updateOrderItemByOrderId(orderId, itemId, request);

    return ResponseEntity.status(HttpStatus.OK).body(orderItem);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @DeleteMapping("/{orderId}/items/{itemId}")
  public ResponseEntity<Void> removeOrderItem(
    @PathVariable Long orderId,
    @PathVariable Long itemId
  ) {
    orderService.removeOrderItemByOrderId(orderId, itemId);

    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CHEF')")
  @PostMapping("/{id}/ready")
  public ResponseEntity<?> markAsReady(@PathVariable Long id) {
    OrderDTO orderDTO = orderService.markAsReady(id);
    return ResponseEntity.status(HttpStatus.OK).body(orderDTO);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
  @PostMapping("/{id}/pay/{paymentMethod}")
  public ResponseEntity<?> payOrder(
      @PathVariable Long id,
      @PathVariable PaymentMethodType paymentMethod,
      @AuthenticationPrincipal User user
  ) {
    OrderDTO orderDTO = orderService.payOrder(id, paymentMethod, user);

    return ResponseEntity.status(HttpStatus.OK).body(orderDTO);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
  @PostMapping("/{id}/pay-partial")
  public ResponseEntity<?> payPartialOrder(
      @PathVariable Long id,
      @RequestBody @Valid PartialPaymentDTO paymentDTO,
      @AuthenticationPrincipal User user
  ) {
    OrderDTO orderDTO = orderService.payPartialOrder(id, paymentDTO, user);

    return ResponseEntity.status(HttpStatus.OK).body(orderDTO);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @PostMapping("/{id}/print-thermal")
  public ResponseEntity<?> printThermal(@PathVariable Long id) {
    OrderDTO orderDTO = orderService.findById(id);
    thermalPrinterService.printPrecuenta(orderDTO);
    return ResponseEntity.noContent().build();
  }

}
