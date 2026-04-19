package com.restaurant_management.restaurant_management_backend.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.dto.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderTypeDTO;
import com.restaurant_management.restaurant_management_backend.dto.PartialPaymentDTO;
import com.restaurant_management.restaurant_management_backend.dto.TransactionDTO;
import com.restaurant_management.restaurant_management_backend.dto.UpdateOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.entity.Order;
import com.restaurant_management.restaurant_management_backend.entity.OrderItem;
import com.restaurant_management.restaurant_management_backend.entity.Product;
import com.restaurant_management.restaurant_management_backend.entity.Table;
import com.restaurant_management.restaurant_management_backend.entity.TableTransferAudit;
import com.restaurant_management.restaurant_management_backend.entity.Transaction;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.enums.TransactionStatus;
import com.restaurant_management.restaurant_management_backend.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.mapper.OrderItemMapper;
import com.restaurant_management.restaurant_management_backend.mapper.OrderMapper;
import com.restaurant_management.restaurant_management_backend.mapper.ProductMapper;
import com.restaurant_management.restaurant_management_backend.repository.OrderItemRepository;
import com.restaurant_management.restaurant_management_backend.repository.OrderRepository;
import com.restaurant_management.restaurant_management_backend.repository.ProductRepository;
import com.restaurant_management.restaurant_management_backend.repository.TableRepository;
import com.restaurant_management.restaurant_management_backend.repository.TableTransferAuditRepository;
import com.restaurant_management.restaurant_management_backend.repository.TransactionRepository;
import com.restaurant_management.restaurant_management_backend.repository.UserRepository;
import com.restaurant_management.restaurant_management_backend.service.OrderCodeService;
import com.restaurant_management.restaurant_management_backend.service.OrderService;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEvent;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final TableRepository tableRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;
  private final TransactionRepository transactionRepository;
  private final OrderCodeService orderCodeService;
  private final UserRepository userRepository;
  private final TableTransferAuditRepository tableTransferAuditRepository;

  private final OrderMapper orderMapper;
  private final OrderItemMapper orderItemMapper;
  private final ProductMapper productMapper;
  private final OrderEventPublisher orderEventPublisher;

  @Transactional
  public OrderDTO save(OrderDTO orderDTO) {
    if (orderDTO.getType() == null) {
      throw new IllegalArgumentException("El tipo de orden es obligatorio");
    }

    String orderCode = orderCodeService.generateNextOrderCode();
    
    Order newOrder = Order.builder()
        .orderCode(orderCode)
        .type(orderDTO.getType())
        .customerName(orderDTO.getCustomerName())
        .build();

    if (orderDTO.getType() == OrderType.DINE_IN) {
      if (orderDTO.getTableId() == null) {
        throw new IllegalArgumentException("La mesa es obligatoria para pedidos en mesa");
      }
      Table table = tableRepository.findById(orderDTO.getTableId())
          .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));
      newOrder.assignToTable(table);
    }

    OrderDTO saved = orderMapper.toDto(orderRepository.save(newOrder));
    orderEventPublisher.publish(OrderEvent.Type.CREATED, saved.getId());
    return saved;
  }

  @Transactional(readOnly = true)
  public OrderDTO findById(Long id) {
    Order order = orderRepository.findByIdWithDetails(id)
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
  public OrderDTO changeTable(Long orderId, Long tableId, User user) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    Table oldTable = order.getTable();
    oldTable.free();

    Table newTable = tableRepository.findById(tableId)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    order.assignToTable(newTable);

    TableTransferAudit audit = TableTransferAudit.builder()
      .order(order)
      .fromTable(oldTable)
      .toTable(newTable)
      .user(user)
      .orderTotal(order.getTotal())
      .transferDate(LocalDateTime.now())
      .build();

    tableTransferAuditRepository.save(audit);

    OrderDTO result = orderMapper.toDto(orderRepository.save(order));
    orderEventPublisher.publish(OrderEvent.Type.TABLE_CHANGED, result.getId());
    return result;
  }

  @Transactional
  public OrderDTO changeType(Long id, OrderTypeDTO orderTypeDTO) {
    Order order = orderRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    order.setType(orderTypeDTO.getType());

    OrderDTO result = orderMapper.toDto(orderRepository.save(order));
    orderEventPublisher.publish(OrderEvent.Type.UPDATED, result.getId());
    return result;
  }

  @Transactional
  public OrderDTO cancelOrder(Long id) {
    Order order = orderRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    order.setStatus(OrderStatus.CANCELLED);

    if (order.getType() == OrderType.DINE_IN && order.getTable() != null) {
      Table table = order.getTable();
      table.free();
      tableRepository.save(table);
    }

    OrderDTO result = orderMapper.toDto(orderRepository.save(order));
    orderEventPublisher.publish(OrderEvent.Type.CANCELLED, result.getId());
    return result;
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

  @Override
  @Transactional
  public OrderDTO markAsReady(Long orderId) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    order.markAsReady();
    OrderDTO result = orderMapper.toDto(orderRepository.save(order));
    orderEventPublisher.publish(OrderEvent.Type.READY, result.getId());
    return result;
  }

  @Override
  @Transactional
  public OrderDTO markAsPending(Long orderId) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    order.markAsPending();
    return orderMapper.toDto(orderRepository.save(order));
  }

  @Override
  @Transactional
  public OrderDTO payOrder(Long orderId, PaymentMethodType paymentMethodType, User user) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    
    // Validar que no haya pagos previos - si ya está PARTIALLY_PAID debe usar el endpoint de pago parcial
    if (order.getStatus() == OrderStatus.PARTIALLY_PAID) {
        throw new IllegalStateException(
            "Esta orden tiene pagos parciales previos. Use el endpoint de pago parcial para completar el pago."
        );
    }
    
    order.markAsPaid();
    
    User currentUser = userRepository.findById(user.getId())
      .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Transaction transaction = Transaction.builder()
        .order(order)
        .user(currentUser)
        .total(order.getTotal())
        .paymentMethod(paymentMethodType)
        .status(TransactionStatus.COMPLETED)
        .transactionDate(LocalDateTime.now())
        .build();
    
    transactionRepository.save(transaction);
    
    if (order.getType() == OrderType.DINE_IN) {
        Table table = order.getTable();
        table.free();
        tableRepository.save(table);
    }
    
    OrderDTO result = orderMapper.toDto(orderRepository.save(order));
    orderEventPublisher.publish(OrderEvent.Type.PAID, result.getId());
    return result;
  }

  @Override
  @Transactional
  public OrderDTO payPartialOrder(Long orderId, PartialPaymentDTO paymentDTO, User user) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    
    // Validar estado de la orden
    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.READY
        && order.getStatus() != OrderStatus.PARTIALLY_PAID) {
      throw new IllegalStateException("La orden no puede recibir pagos en este estado: " + order.getStatus());
    }
    
    // Validar que el monto no exceda el resto por pagar
    BigDecimal remainingAmount = order.getRemainingAmount();
    if (paymentDTO.getAmount().compareTo(remainingAmount) > 0) {
        throw new IllegalArgumentException("El monto a pagar (" + paymentDTO.getAmount() + 
            ") excede el monto restante (" + remainingAmount + ")");
    }
    
    // Crear la transacción
    User currentUser = userRepository.findById(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

    Transaction transaction = Transaction.builder()
        .order(order)
        .user(currentUser)
        .total(paymentDTO.getAmount())
        .paymentMethod(paymentDTO.getPaymentMethod())
        .status(TransactionStatus.COMPLETED)
        .transactionDate(LocalDateTime.now())
        .build();
    
    transactionRepository.save(transaction);
    
    // Calcular si la orden está completamente pagada sumando el nuevo pago
    BigDecimal totalPaid = order.getPaidAmount().add(paymentDTO.getAmount());
    boolean isFullyPaid = totalPaid.compareTo(order.getTotal()) >= 0;
    
    // Actualizar el estado de la orden
    if (isFullyPaid) {
        order.setStatus(OrderStatus.PAID);
        
        // Liberar mesa si está completamente pagado y es DINE_IN
        if (order.getType() == OrderType.DINE_IN) {
            Table table = order.getTable();
            table.free();
            tableRepository.save(table);
        }
    } else {
        order.setStatus(OrderStatus.PARTIALLY_PAID);
    }
    
    OrderDTO result = orderMapper.toDto(orderRepository.save(order));
    orderEventPublisher.publish(OrderEvent.Type.PAID, result.getId());
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrderDTO> findActiveOrder() {
    List<Order> orders = orderRepository.findActiveOrder();

    return orders.stream()
      .map(orderMapper::toDto)
      .toList();
  }

  @Override
  @Transactional
  public OrderItemDTO addOrderItemByOrderId(Long orderId, AddOrderItemRequest request) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.IN_PROGRESS
        && order.getStatus() != OrderStatus.READY) {
      throw new BadRequestException("No se puede agregar items a un pedido que no está en estado CREATED, IN_PROGRESS o READY");
    }

    Product product = productRepository.findById(request.getProductId())
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    Optional<OrderItem> existingOrderItem = order.getItems().stream()
      .filter(item -> item.getProduct().getId().equals(request.getProductId()))
      .findFirst();

    OrderItem orderItem;
    if (existingOrderItem.isPresent()) {
      orderItem = existingOrderItem.get();
      orderItem.setQuantity(orderItem.getQuantity() + request.getQuantity());
      orderItem.calculateSubTotal();
      if (request.getNotes() != null) {
        orderItem.setNotes(request.getNotes());
      }
    } else {
      orderItem = new OrderItem();
      orderItem.assignProductCustomPrice(product, null, request.getQuantity());
      orderItem.setNotes(request.getNotes());
      orderItem.setOrder(order);
      order.addItem(orderItem);
    }

    OrderItem savedOrderItem = orderItemRepository.save(orderItem);
    order.calculateTotal();
    if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.READY) {
      order.setStatus(OrderStatus.IN_PROGRESS);
    }
    orderRepository.save(order);
    orderEventPublisher.publish(OrderEvent.Type.ITEM_ADDED, orderId);

    return OrderItemDTO.builder()
      .id(savedOrderItem.getId())
      .quantity(savedOrderItem.getQuantity())
      .subTotal(savedOrderItem.getSubTotal())
      .productId(savedOrderItem.getProduct().getId())
      .product(productMapper.toDto(savedOrderItem.getProduct()))
      .notes(savedOrderItem.getNotes())
      .build();
  }

  @Override
  @Transactional
  public OrderItemDTO updateOrderItemByOrderId(Long orderId, Long itemId, UpdateOrderItemRequest request) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    OrderItem orderItem = orderItemRepository.findById(itemId)
      .orElseThrow(() -> new ResourceNotFoundException("Item de orden no encontrado"));

    if (!orderItem.getOrder().getId().equals(orderId)) {
      throw new BadRequestException("El item no pertenece al pedido especificado");
    }

    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.IN_PROGRESS) {
      throw new BadRequestException("No se puede modificar un pedido que no está en estado CREATED o IN_PROGRESS");
    }

    orderItem.setQuantity(request.getQuantity());
    if (request.getNotes() != null) {
      orderItem.setNotes(request.getNotes());
    }
    orderItem.calculateSubTotal();

    OrderItem updatedOrderItem = orderItemRepository.save(orderItem);
    order.calculateTotal();
    orderRepository.save(order);
    orderEventPublisher.publish(OrderEvent.Type.ITEM_UPDATED, orderId);

    return OrderItemDTO.builder()
      .id(updatedOrderItem.getId())
      .quantity(updatedOrderItem.getQuantity())
      .subTotal(updatedOrderItem.getSubTotal())
      .productId(updatedOrderItem.getProduct().getId())
      .product(productMapper.toDto(updatedOrderItem.getProduct()))
      .notes(updatedOrderItem.getNotes())
      .build();
  }

  @Override
  @Transactional
  public void removeOrderItemByOrderId(Long orderId, Long itemId) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    OrderItem orderItem = orderItemRepository.findById(itemId)
      .orElseThrow(() -> new ResourceNotFoundException("Item de orden no encontrado"));

    if (!orderItem.getOrder().getId().equals(orderId)) {
      throw new BadRequestException("El item no pertenece al pedido especificado");
    }

    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.IN_PROGRESS) {
      throw new BadRequestException("No se puede eliminar items de un pedido que no está en estado CREATED o IN_PROGRESS");
    }

    order.removeItem(orderItem);
    orderItemRepository.delete(orderItem);
    order.calculateTotal();
    if (order.getStatus() == OrderStatus.IN_PROGRESS && order.getItems().isEmpty()) {
      order.setStatus(OrderStatus.CREATED);
    }
    orderRepository.save(order);
    orderEventPublisher.publish(OrderEvent.Type.ITEM_REMOVED, orderId);
  }

}
