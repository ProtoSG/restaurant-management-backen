package com.restaurant_management.restaurant_management_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.dto.ActiveOrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderWithOrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.TableDTO;
import com.restaurant_management.restaurant_management_backend.dto.UpdateOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.service.TableService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tables")
@RequiredArgsConstructor
public class TableController {

  private final TableService tableService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<?> create(
    @RequestBody @Valid TableDTO tableDTO
  ) {
    TableDTO savedTable = tableService.save(tableDTO);

    return ResponseEntity.status(HttpStatus.CREATED)
      .body(savedTable);
  }

  @GetMapping
  public ResponseEntity<?> getAll() {
    List<TableDTO> tables = tableService.findAll();

    return ResponseEntity.status(HttpStatus.OK)
      .body(tables);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(
    @PathVariable Long id
  ) {
    TableDTO table = tableService.findById(id);

    return ResponseEntity.status(HttpStatus.OK)
      .body(table);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public ResponseEntity<?> update(
    @PathVariable Long id,
    @RequestBody @Valid TableDTO tableDTO
  ) {
    TableDTO updatedTable = tableService.update(id, tableDTO);

    return ResponseEntity.status(HttpStatus.OK)
      .body(updatedTable);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable Long id
  ) {
    tableService.deleteById(id);

    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @PostMapping("/{id}/orders")
  public ResponseEntity<?> createOrder(@PathVariable Long id) {
    OrderWithOrderItemsDTO order = tableService.saveOrder(id);

    return ResponseEntity.status(HttpStatus.CREATED).body(order);
  }

  @GetMapping("/{id}/orders/active")
  public ResponseEntity<?> getActiveOrder(@PathVariable Long id) {
    ActiveOrderDTO activeOrder = tableService.getActiveOrder(id);

    return ResponseEntity.status(HttpStatus.OK).body(activeOrder);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @PostMapping("/{id}/orders/items")
  public ResponseEntity<?> addOrderItem(
    @PathVariable Long id,
    @RequestBody @Valid AddOrderItemRequest request
  ) {
    OrderItemDTO orderItem = tableService.addOrderItem(id, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(orderItem);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @PutMapping("/{id}/orders/items/{itemId}")
  public ResponseEntity<?> updateOrderItem(
    @PathVariable Long id,
    @PathVariable Long itemId,
    @RequestBody @Valid UpdateOrderItemRequest request
  ) {
    OrderItemDTO orderItem = tableService.updateOrderItem(id, itemId, request);

    return ResponseEntity.status(HttpStatus.OK).body(orderItem);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
  @DeleteMapping("/{id}/orders/items/{itemId}")
  public ResponseEntity<Void> removeOrderItem(
    @PathVariable Long id,
    @PathVariable Long itemId
  ) {
    tableService.removeOrderItem(id, itemId);

    return ResponseEntity.noContent().build();
  }
}
