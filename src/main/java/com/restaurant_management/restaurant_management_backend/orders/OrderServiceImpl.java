package com.restaurant_management.restaurant_management_backend.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.auth.UserRepository;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.menu.categories.CategoryMapper;
import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.CreateOrderRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.PartialPaymentRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.UpdatedOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.ActiveOrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderItemResponse;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.orders.entity.OrderItem;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.shared.enums.TransactionStatus;
import com.restaurant_management.restaurant_management_backend.tables.TableRepository;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;
import com.restaurant_management.restaurant_management_backend.transactions.TransactionMapper;
import com.restaurant_management.restaurant_management_backend.transactions.TransactionRepository;
import com.restaurant_management.restaurant_management_backend.transactions.dto.response.TransactionResponse;
import com.restaurant_management.restaurant_management_backend.transactions.entity.Transaction;
import com.restaurant_management.restaurant_management_backend.shared.config.SystemConfigRepository;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEventPublisher;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEvent;

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
  private final OrderMapper orderMapper;
  private final CategoryMapper categoryMapper;
  private final OrderEventPublisher orderEventPublisher;
  private final TransactionMapper transactionMapper;
  private final SystemConfigRepository systemConfigRepository;
  private final UserRepository userRepository;

  @Transactional
  public OrderResponse save(CreateOrderRequest req) {
    String orderCode = orderCodeService.generateNextOrderCode();
    
    Order newOrder = orderMapper.toEntity(req);
    newOrder.setOrderCode(orderCode);

    if (req.type() == OrderType.DINE_IN) {
      if (req.tableId() == null) {
        throw new IllegalArgumentException("La mesa es obligatoria para pedidos en mesa");
      }
      Table table = tableRepository.findById(req.tableId())
          .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));
      newOrder.assignToTable(table);
    }

    OrderResponse saved = orderMapper.toResponse(orderRepository.save(newOrder));
    Long tableId = newOrder.getTable() != null ? newOrder.getTable().getId() : null;
    orderEventPublisher.publish(OrderEvent.Type.CREATED, saved.id(), tableId);
    return saved;
  }

  @Transactional(readOnly = true)
  public OrderResponse findById(Long id) {
    Order order = orderRepository.findByIdWithDetails(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    return orderMapper.toResponse(order);
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> findAll() {
    List<Order> orders = orderRepository.findAll();

    return orders.stream()
      .map(orderMapper::toResponse)
      .toList();
  }

  @Transactional
  public void delete(Long id) {
    Order order = orderRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    Table table = order.getTable();
    if (table != null) {
      table.free();
    }

    orderRepository.delete(order);
  }

  @Transactional
  public OrderResponse changeTable(Long orderId, Long tableId) {
    Order order = orderRepository.findByIdWithDetails(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    if (order.getType() != OrderType.DINE_IN) {
      throw new BadRequestException("Solo se puede cambiar mesa en órdenes de tipo DINE_IN");
    }

    Table oldTable = order.getTable();
    if (oldTable != null) {
      oldTable.free();
    }

    Table newTable = tableRepository.findById(tableId)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    order.assignToTable(newTable);
    orderRepository.save(order);

    OrderResponse result = orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    orderEventPublisher.publish(OrderEvent.Type.TABLE_CHANGED, result.id(), newTable.getId());
    return result;
  }

  @Transactional
  public void cancelOrder(Long id) {
    Order order = orderRepository.findByIdWithDetails(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    order.setStatus(OrderStatus.CANCELLED);

    if (order.getType() == OrderType.DINE_IN && order.getTable() != null) {
      Table table = order.getTable();
      table.free();
      tableRepository.save(table);
    }

    Long tableId = order.getTable() != null ? order.getTable().getId() : null;
    orderRepository.save(order);
    orderEventPublisher.publish(OrderEvent.Type.CANCELLED, id, tableId);
  }

  @Override
  @Transactional
  public OrderResponse markAsReady(Long orderId) {
    Order order = orderRepository.findByIdWithDetails(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    order.markAsReady();
    orderRepository.save(order);
    OrderResponse result = orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    Long tableId = order.getTable() != null ? order.getTable().getId() : null;
    orderEventPublisher.publish(OrderEvent.Type.READY, result.id(), tableId);
    return result;
  }

  @Override
  @Transactional
  public OrderResponse payOrder(Long orderId, PaymentMethodType paymentMethodType) {
    Order order = orderRepository.findByIdWithDetails(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    if (order.getStatus() == OrderStatus.PARTIALLY_PAID) {
        throw new IllegalStateException(
            "Esta orden tiene pagos parciales previos. Use el endpoint de pago parcial para completar el pago."
        );
    }

    order.markAsPaid();

    Transaction transaction = Transaction.builder()
        .order(order)
        .user(getAuthenticatedUser())
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
    
    Long tableId = order.getType() == OrderType.DINE_IN && order.getTable() != null ? order.getTable().getId() : null;
    orderRepository.save(order);
    OrderResponse result = orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    orderEventPublisher.publish(OrderEvent.Type.PAID, result.id(), tableId);
    return result;
  }

  @Override
  @Transactional
  public OrderResponse payPartialOrder(Long orderId, PartialPaymentRequest paymentDTO) {
    Order order = orderRepository.findByIdWithDetails(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.READY
        && order.getStatus() != OrderStatus.PARTIALLY_PAID) {
      throw new IllegalStateException("La orden no puede recibir pagos en este estado: " + order.getStatus());
    }

    BigDecimal remainingAmount = order.getRemainingAmount();
    if (paymentDTO.amount().compareTo(remainingAmount) > 0) {
        throw new IllegalArgumentException("El monto a pagar (" + paymentDTO.amount() +
            ") excede el monto restante (" + remainingAmount + ")");
    }

    Transaction transaction = Transaction.builder()
        .order(order)
        .user(getAuthenticatedUser())
        .total(paymentDTO.amount())
        .paymentMethod(paymentDTO.paymentMethod())
        .status(TransactionStatus.COMPLETED)
        .transactionDate(LocalDateTime.now())
        .build();
    
    transactionRepository.save(transaction);
    
    // Calcular si la orden está completamente pagada sumando el nuevo pago
    BigDecimal totalPaid = order.getPaidAmount().add(paymentDTO.amount());
    boolean isFullyPaid  = totalPaid.compareTo(order.getTotal()) >= 0;
    
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
    
    Long tableId = order.getType() == OrderType.DINE_IN && order.getTable() != null ? order.getTable().getId() : null;
    orderRepository.save(order);
    OrderResponse result = orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    orderEventPublisher.publish(OrderEvent.Type.PAID, result.id(), tableId);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrderResponse> findActiveOrder() {
    List<Order> orders = orderRepository.findActiveOrder();

    return orders.stream()
      .map(orderMapper::toResponse)
      .toList();
  }

  @Override
  @Transactional
  public void addOrderItem(Long orderId, AddOrderItemRequest request) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.IN_PROGRESS
        && order.getStatus() != OrderStatus.READY) {
      throw new BadRequestException("No se puede agregar items a un pedido que no está en estado CREATED, IN_PROGRESS o READY");
    }

    Product product = productRepository.findByIdWithCategory(request.productId())
      .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    if (!Boolean.TRUE.equals(product.getIsAvailable())) {
      throw new BadRequestException("El producto no está disponible");
    }

    boolean isTakeaway = Boolean.TRUE.equals(request.isTakeaway()) || order.getType() == OrderType.TAKEAWAY;
    BigDecimal surchargePerUnit = resolveSurcharge(isTakeaway, product);

    Optional<OrderItem> existingOrderItem = order.getItems().stream()
      .filter(item -> item.getProduct().getId().equals(request.productId())
          && Boolean.TRUE.equals(item.getIsTakeaway()) == isTakeaway)
      .findFirst();

    OrderItem orderItem;
    if (existingOrderItem.isPresent()) {
      orderItem = existingOrderItem.get();
      orderItem.setQuantity(orderItem.getQuantity() + request.quantity());
      orderItem.calculateSubTotal();
      if (request.notes() != null) {
        orderItem.setNotes(request.notes());
      }
    } else {
      orderItem = new OrderItem();
      orderItem.setIsTakeaway(isTakeaway);
      orderItem.setTakeawaySurcharge(surchargePerUnit);
      orderItem.assignProductCustomPrice(product, null, request.quantity());
      orderItem.setNotes(request.notes());
      orderItem.setOrder(order);
      order.addItem(orderItem);
    }

    orderItemRepository.save(orderItem);
    order.calculateTotal();
    if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.READY) {
      order.setStatus(OrderStatus.IN_PROGRESS);
    }
    Long tableId = order.getTable() != null ? order.getTable().getId() : null;
    orderRepository.save(order);
    orderEventPublisher.publish(OrderEvent.Type.ITEM_ADDED, orderId, tableId);
  }

  @Override
  @Transactional
  public void updateOrderItem(Long orderId, Long itemId, UpdatedOrderItemRequest request) {
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

    if (request.isTakeaway() != null) {
      boolean newIsTakeaway = Boolean.TRUE.equals(request.isTakeaway()) || order.getType() == OrderType.TAKEAWAY;
      orderItem.setIsTakeaway(newIsTakeaway);
      orderItem.setTakeawaySurcharge(resolveSurcharge(newIsTakeaway, orderItem.getProduct()));
    }
    orderItem.setQuantity(request.quantity());
    if (request.notes() != null) {
      orderItem.setNotes(request.notes());
    }
    orderItem.calculateSubTotal();

    orderItemRepository.save(orderItem);
    order.calculateTotal();
    Long tableId = order.getTable() != null ? order.getTable().getId() : null;
    orderRepository.save(order);
    orderEventPublisher.publish(OrderEvent.Type.ITEM_UPDATED, orderId, tableId);
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

    Long tableId = order.getTable() != null ? order.getTable().getId() : null;
    order.removeItem(orderItem);
    orderItemRepository.delete(orderItem);
    order.calculateTotal();
    if (order.getStatus() == OrderStatus.IN_PROGRESS && order.getItems().isEmpty()) {
      order.setStatus(OrderStatus.CREATED);
    }
    orderRepository.save(order);
    orderEventPublisher.publish(OrderEvent.Type.ITEM_REMOVED, orderId, tableId);
  }

  @Override
  @Transactional
  public ActiveOrderResponse findActiveOrderByTable(Long tableId) {
    tableRepository.findById(tableId)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    Order activeOrder = orderRepository.findActiveOrderByTableId(tableId)
      .orElseThrow(() -> new ResourceNotFoundException("No hay orden activa para esta mesa"));

    List<OrderItemResponse> items = new ArrayList<>();

    if (activeOrder.getItems() != null) {
      items = activeOrder.getItems().stream()
        .map(orderItem -> {
          ProductResponse product = new ProductResponse(
              orderItem.getProduct().getId(),
              orderItem.getProduct().getName(),
              orderItem.getProduct().getPrice(),
              categoryMapper.toResponse(orderItem.getProduct().getCategory()),
              orderItem.getProduct().getIsAvailable()
            );

          return new OrderItemResponse(
            orderItem.getId(),
            orderItem.getQuantity(),
            orderItem.getSubTotal(),
            product,
            orderItem.getNotes(),
            orderItem.getIsTakeaway(),
            orderItem.getTakeawaySurcharge()
          );
        })
        .toList();
    }
    
    // Obtener transacciones completadas
    List<TransactionResponse> transactions = new ArrayList<>();
    if (activeOrder.getTransactions() != null) {
      transactions = activeOrder.getTransactions().stream()
        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
        .map(transactionMapper::toDto)
        .toList();
    }

    return new ActiveOrderResponse(
        activeOrder.getId(),
        activeOrder.getOrderCode(),
        activeOrder.getStatus(),
        activeOrder.getType(),
        activeOrder.getTotal(),
        items,
        activeOrder.getPaidAmount(),
        activeOrder.getRemainingAmount(),
        transactions
    );

  }

  private BigDecimal resolveSurcharge(boolean isTakeaway, Product product) {
    if (!isTakeaway) return BigDecimal.ZERO;
    String categoryName = product.getCategory() != null ? product.getCategory().getName() : "";
    if ("bebidas".equalsIgnoreCase(categoryName)) return BigDecimal.ZERO;
    return systemConfigRepository.findById("takeaway_surcharge")
        .map(c -> new BigDecimal(c.getValue()))
        .orElse(BigDecimal.ONE);
  }

  private User getAuthenticatedUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
      return null;
    }
    return userRepository.findByUsername(auth.getName()).orElse(null);
  }

}
