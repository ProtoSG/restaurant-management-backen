package com.restaurant_management.restaurant_management_backend.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.dto.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderWithOrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.TableDTO;
import com.restaurant_management.restaurant_management_backend.dto.UpdateOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.entity.Order;
import com.restaurant_management.restaurant_management_backend.entity.OrderItem;
import com.restaurant_management.restaurant_management_backend.entity.Product;
import com.restaurant_management.restaurant_management_backend.entity.Table;
import com.restaurant_management.restaurant_management_backend.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.enums.TableStatus;
import com.restaurant_management.restaurant_management_backend.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.mapper.ProductMapper;
import com.restaurant_management.restaurant_management_backend.mapper.TableMapper;
import com.restaurant_management.restaurant_management_backend.repository.OrderItemRepository;
import com.restaurant_management.restaurant_management_backend.repository.OrderRepository;
import com.restaurant_management.restaurant_management_backend.repository.ProductRepository;
import com.restaurant_management.restaurant_management_backend.repository.TableRepository;
import com.restaurant_management.restaurant_management_backend.service.OrderCodeService;
import com.restaurant_management.restaurant_management_backend.service.TableService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TableServiceImpl implements TableService {

  private final TableRepository tableRepository;
  private final TableMapper tableMapper;
  private final OrderRepository orderRepository;
  private final OrderCodeService orderCodeService;
  private final ProductRepository productRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductMapper productMapper;

  public TableDTO save(TableDTO tableDTO) {
    Table newTable = tableMapper.toEntity(tableDTO);

    return tableMapper.toDto(tableRepository.save(newTable));
  }

  public List<TableDTO> findAll() {
    List<Table> tables = tableRepository.findAll();

    return tables.stream()
      .map(tableMapper::toDto)
      .toList();
  }

  public TableDTO findById(Long id) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    return tableMapper.toDto(table);
  }

  public TableDTO update(Long id, TableDTO tableDTO) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    table.setNumber(tableDTO.getNumber());

    return tableMapper.toDto(tableRepository.save(table));
  }

  public void deleteById(Long id) {
    if (!tableRepository.existsById(id)) {
      throw new ResourceNotFoundException("Mesa no encontrada");
    }

    tableRepository.deleteById(id);
  }

  public OrderWithOrderItemsDTO getActiveOrder(Long id) {
    tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));
    
    Order activeOrder = orderRepository.findActiveOrderByTableId(id)
      .orElseThrow(() -> new ResourceNotFoundException("No hay orden activa para esta mesa"));

    List<OrderItemDTO> items = new ArrayList<>();

    if (activeOrder.getItems() != null) {
      items = activeOrder.getItems().stream()
        .map(orderItem -> OrderItemDTO.builder()
          .id(orderItem.getId())
          .quantity(orderItem.getQuantity())
          .subTotal(orderItem.getSubTotal())
          .productId(orderItem.getProduct().getId())
          .product(productMapper.toDto(orderItem.getProduct()))
          .build())
        .toList();
    }
    
    return OrderWithOrderItemsDTO.builder()
      .id(activeOrder.getId())
      .orderCode(activeOrder.getOrderCode())
      .status(activeOrder.getStatus())
      .type(activeOrder.getType())
      .total(activeOrder.getTotal())
      .items(items)
      .build();
  }

  @Transactional
  public OrderWithOrderItemsDTO saveOrder(Long id) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));
  
    if (table.getStatus() != TableStatus.FREE) {
      throw new BadRequestException("La mesa no puede guardar la orden");
    }

    Order newOrder = Order.builder().build();
    newOrder.setOrderCode(orderCodeService.generateNextOrderCode());
    newOrder.assignToTable(table);
    newOrder.setType(OrderType.DINE_IN);

    Order savedOrder = orderRepository.save(newOrder);

    List<OrderItemDTO> items = new ArrayList<>();

    return OrderWithOrderItemsDTO.builder()
      .id(savedOrder.getId())
      .orderCode(savedOrder.getOrderCode())
      .status(savedOrder.getStatus())
      .type(savedOrder.getType())
      .total(savedOrder.getTotal())
      .items(items)
      .build();
  }

  @Transactional
  public void addProducts(Long id, OrderItemsDTO items) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    Optional<Order> activeOrderOpt = orderRepository.findActiveOrderByTableId(id);

    if (activeOrderOpt.isPresent()) {
      Order activeOrder = activeOrderOpt.get();
      for (OrderItemDTO orderItemDTO: items.getItems()) {
        Product product = productRepository.findById(orderItemDTO.getProductId())
          .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        OrderItem orderItem = new OrderItem();
        orderItem.assignProductCustomPrice(product, orderItemDTO.getPrice(), orderItemDTO.getQuantity());
        orderItem.setOrder(activeOrder);

        orderItemRepository.save(orderItem);

        activeOrder.addItem(orderItem);
      }
      orderRepository.save(activeOrder);
      return;
    }

    Order newOrder = Order.builder().build();
    newOrder.setOrderCode(orderCodeService.generateNextOrderCode());
    newOrder.assignToTable(table);
    newOrder.setType(OrderType.DINE_IN);

    Order savedOrder = orderRepository.save(newOrder);

    for (OrderItemDTO orderItemDTO: items.getItems()) {
      Product product = productRepository.findById(orderItemDTO.getProductId())
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

      OrderItem orderItem = new OrderItem();
      orderItem.assignProductCustomPrice(product, orderItemDTO.getPrice(), orderItemDTO.getQuantity());
      orderItem.setOrder(savedOrder);

      orderItemRepository.save(orderItem);

      savedOrder.addItem(orderItem);
    }
    savedOrder.setStatus(OrderStatus.READY);
    orderRepository.save(savedOrder);
  }

  @Transactional
  public OrderItemDTO addOrderItem(Long tableId, AddOrderItemRequest request) {
    Table table = tableRepository.findById(tableId)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    Order activeOrder = orderRepository.findActiveOrderByTableId(tableId)
      .orElseGet(() -> {
        Order newOrder = Order.builder().build();
        newOrder.setOrderCode(orderCodeService.generateNextOrderCode());
        newOrder.assignToTable(table);
        newOrder.setType(OrderType.DINE_IN);
        return orderRepository.save(newOrder);
      });

    Product product = productRepository.findById(request.getProductId())
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    OrderItem orderItem = new OrderItem();
    orderItem.assignProductCustomPrice(product, null, request.getQuantity());
    orderItem.setOrder(activeOrder);

    OrderItem savedOrderItem = orderItemRepository.save(orderItem);

    activeOrder.addItem(savedOrderItem);
    orderRepository.save(activeOrder);

    return OrderItemDTO.builder()
      .id(savedOrderItem.getId())
      .quantity(savedOrderItem.getQuantity())
      .subTotal(savedOrderItem.getSubTotal())
      .productId(savedOrderItem.getProduct().getId())
      .product(productMapper.toDto(savedOrderItem.getProduct()))
      .build();
  }

  @Transactional
  public OrderItemDTO updateOrderItem(Long tableId, Long itemId, UpdateOrderItemRequest request) {
    tableRepository.findById(tableId)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    OrderItem orderItem = orderItemRepository.findById(itemId)
      .orElseThrow(() -> new ResourceNotFoundException("Item de orden no encontrado"));

    if (!orderItem.getOrder().getTable().getId().equals(tableId)) {
      throw new BadRequestException("El item no pertenece a la mesa especificada");
    }

    if (orderItem.getOrder().getStatus() != OrderStatus.CREATED && 
        orderItem.getOrder().getStatus() != OrderStatus.PENDING) {
      throw new BadRequestException("No se puede modificar una orden que no está en estado CREATED o PENDING");
    }

    orderItem.setQuantity(request.getQuantity());
    orderItem.calculateSubTotal();

    OrderItem updatedOrderItem = orderItemRepository.save(orderItem);

    Order order = updatedOrderItem.getOrder();
    order.calculateTotal();
    orderRepository.save(order);

    return OrderItemDTO.builder()
      .id(updatedOrderItem.getId())
      .quantity(updatedOrderItem.getQuantity())
      .subTotal(updatedOrderItem.getSubTotal())
      .productId(updatedOrderItem.getProduct().getId())
      .product(productMapper.toDto(updatedOrderItem.getProduct()))
      .build();
  }

  @Transactional
  public void removeOrderItem(Long tableId, Long itemId) {
    tableRepository.findById(tableId)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    OrderItem orderItem = orderItemRepository.findById(itemId)
      .orElseThrow(() -> new ResourceNotFoundException("Item de orden no encontrado"));

    if (!orderItem.getOrder().getTable().getId().equals(tableId)) {
      throw new BadRequestException("El item no pertenece a la mesa especificada");
    }

    if (orderItem.getOrder().getStatus() != OrderStatus.CREATED && 
        orderItem.getOrder().getStatus() != OrderStatus.PENDING) {
      throw new BadRequestException("No se puede modificar una orden que no está en estado CREATED o PENDING");
    }

    Order order = orderItem.getOrder();
    order.removeItem(orderItem);
    
    orderItemRepository.delete(orderItem);
    
    order.calculateTotal();
    orderRepository.save(order);
  }
}
