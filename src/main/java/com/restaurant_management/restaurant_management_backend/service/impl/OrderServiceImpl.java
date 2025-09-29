package com.restaurant_management.restaurant_management_backend.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.entity.Order;
import com.restaurant_management.restaurant_management_backend.entity.OrderItem;
import com.restaurant_management.restaurant_management_backend.entity.Product;
import com.restaurant_management.restaurant_management_backend.entity.Table;
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
    
    Order newOrder = orderMapper.toEntity(orderDTO);
    
    Table table = tableRepository.findById(orderDTO.getTableId())
        .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    newOrder.assignToTable(table);
    table.occupy();

    Order savedOrder = orderRepository.save(newOrder);
    tableRepository.save(table);

    return orderMapper.toDto(savedOrder);
  }

  @Transactional
  public void addProducts(Long orderId, List<OrderItemDTO> orderItemsDtos) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    for (OrderItemDTO orderItemDto : orderItemsDtos) {
      Product product = productRepository.findById(orderItemDto.getProductId())
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

      OrderItem orderItem = new OrderItem();
      orderItem.assignProduct(product, orderItemDto.getQuantity());
      orderItem.setOrder(order);

      orderItemRepository.save(orderItem);

      order.addItem(orderItem);
    }
  }

}
