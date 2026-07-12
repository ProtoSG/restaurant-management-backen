package com.restaurant_management.restaurant_management_backend.tables;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.tables.dto.request.CreateTableRequest;
import com.restaurant_management.restaurant_management_backend.tables.dto.request.UpdateTableRequest;
import com.restaurant_management.restaurant_management_backend.tables.dto.response.TableResponse;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEvent;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TableServiceImpl implements TableService {

  private final TableRepository tableRepository;
  private final TableMapper tableMapper;
  private final OrderRepository orderRepository;
  private final OrderEventPublisher orderEventPublisher;

  @Override
  public TableResponse save(CreateTableRequest req) {
    Table table = tableMapper.toEntity(req);

    return tableMapper.toResponse(tableRepository.save(table));
  }

  @Override
  public List<TableResponse> findAll() {
    List<Table> tables = tableRepository.findAllOrderedByNumberNumeric();

    return tables.stream()
      .map(tableMapper::toResponse)
      .toList();
  }

  @Override
  public TableResponse findById(Long id) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    return tableMapper.toResponse(table);
  }

  @Override
  public TableResponse update(Long id, UpdateTableRequest req) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    table.setNumber(req.number());

    return tableMapper.toResponse(tableRepository.save(table));
  }

  @Override
  public void deleteById(Long id) {
    if (!tableRepository.existsById(id)) {
      throw new ResourceNotFoundException("Mesa no encontrada");
    }

    tableRepository.deleteById(id);
  }

  @Override
  @Transactional
  public TableResponse release(Long id) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    List<Order> activeOrders = orderRepository.findActiveOrdersByTableId(id);

    boolean hasUnpaidOrder = activeOrders.stream()
      .anyMatch(order -> order.getStatus() != OrderStatus.PAID);
    if (hasUnpaidOrder) {
      throw new BadRequestException(
        "No se puede liberar la mesa: tiene un pedido sin pagar. Cancelalo o cobralo primero.");
    }

    List<Order> stuckPaidOrders = activeOrders.stream()
      .filter(order -> order.getStatus() == OrderStatus.PAID)
      .toList();

    for (Order order : stuckPaidOrders) {
      order.markAsFinalized();
      orderRepository.save(order);
      orderEventPublisher.publish(OrderEvent.Type.FINALIZED, order.getId(), table.getId());
    }

    table.forceFree();

    return tableMapper.toResponse(tableRepository.save(table));
  }

}
