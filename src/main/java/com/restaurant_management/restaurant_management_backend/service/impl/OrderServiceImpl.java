package com.restaurant_management.restaurant_management_backend.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderTypeDTO;
import com.restaurant_management.restaurant_management_backend.entity.Order;
import com.restaurant_management.restaurant_management_backend.entity.OrderItem;
import com.restaurant_management.restaurant_management_backend.entity.Product;
import com.restaurant_management.restaurant_management_backend.entity.Table;
import com.restaurant_management.restaurant_management_backend.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.mapper.OrderMapper;
import com.restaurant_management.restaurant_management_backend.repository.OrderItemRepository;
import com.restaurant_management.restaurant_management_backend.repository.OrderRepository;
import com.restaurant_management.restaurant_management_backend.repository.ProductRepository;
import com.restaurant_management.restaurant_management_backend.repository.TableRepository;
import com.restaurant_management.restaurant_management_backend.service.OrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final TableRepository tableRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;
  
  private final OrderMapper orderMapper;

  @Transactional
  public OrderDTO save(OrderDTO orderDTO) {
    Order newOrder = Order.builder().build();

    Table table = tableRepository.findById(orderDTO.getTableId())
        .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    newOrder.assignToTable(table);

    return orderMapper.toDto(orderRepository.save(newOrder));
  }

  @Transactional(readOnly = true)
  public OrderDTO findById(Long id) {
    Order order = orderRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    return orderMapper.toDto(order);
  }

  @Transactional(readOnly = true)
  public List<OrderDTO> findAll() {
    List<Order> orders = orderRepository.findAll();

    return orders.stream()
      .map(orderMapper::toDto)
      .toList();
  }

  @Transactional
  public void delete(Long id) {
    Order order = orderRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    Table table = order.getTable();
    table.free();

    orderRepository.delete(order);
  }

  @Transactional
  public OrderDTO changeTable(Long orderId, Long tableId) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    Table oldTable = order.getTable();
    oldTable.free();

    Table table = tableRepository.findById(tableId)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    order.assignToTable(table);

    return orderMapper.toDto(orderRepository.save(order));
  }

  @Transactional
  public OrderDTO changeType(Long id, OrderTypeDTO orderTypeDTO) {
    Order order = orderRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    order.setType(orderTypeDTO.getType());

    return orderMapper.toDto(orderRepository.save(order));
  }

  @Transactional
  public OrderDTO cancelOrder(Long id) {
    Order order = orderRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    order.setStatus(OrderStatus.CANCELLED);

    return orderMapper.toDto(orderRepository.save(order));
  }

  @Transactional
  public void addProducts(Long orderId, OrderItemsDTO orderItemsDTO) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    for (OrderItemDTO orderItemDto : orderItemsDTO.getItems()) {
      Product product = productRepository.findById(orderItemDto.getProductId())
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

      OrderItem orderItem = new OrderItem();
      orderItem.assignProduct(product, orderItemDto.getQuantity());
      orderItem.setOrder(order);

      orderItemRepository.save(orderItem);

      order.addItem(orderItem);
    }

    orderRepository.save(order);
  }

}
