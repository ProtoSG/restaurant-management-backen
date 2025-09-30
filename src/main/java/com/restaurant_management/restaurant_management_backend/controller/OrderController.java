package com.restaurant_management.restaurant_management_backend.controller;

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

import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderTypeDTO;
import com.restaurant_management.restaurant_management_backend.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @GetMapping
  public ResponseEntity<?> getAll() {
    List<OrderDTO> orders = orderService.findAll();

    return ResponseEntity.status(HttpStatus.OK).body(orders);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable Long id) {
    OrderDTO order = orderService.findById(id);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody @Valid OrderDTO orderDTO) {
    OrderDTO order = orderService.save(orderDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(order);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable Long id) {
    orderService.delete(id);

    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{orderId}/table/{tableId}")
  public ResponseEntity<?> changeTable(
    @PathVariable Long orderId,
    @PathVariable Long tableId
  ) {
    OrderDTO order = orderService.changeTable(orderId, tableId);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PutMapping("/{id}/type")
  public ResponseEntity<?> changeType(
    @PathVariable Long id,
    @RequestBody @Valid OrderTypeDTO type
  ) {
    OrderDTO order = orderService.changeType(id, type);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
    OrderDTO order = orderService.cancelOrder(id);

    return ResponseEntity.status(HttpStatus.OK).body(order);
  }

  @PostMapping("/{id}/products")
  public ResponseEntity<?> addProducts(
    @PathVariable Long id,
    @RequestBody @Valid OrderItemsDTO orderItemsDTO
  ) {
    orderService.addProducts(id, orderItemsDTO);

    return ResponseEntity.noContent().build();
  }
}
