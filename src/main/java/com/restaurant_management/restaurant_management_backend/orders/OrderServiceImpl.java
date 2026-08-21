package com.restaurant_management.restaurant_management_backend.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.auth.UserRepository;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceConflictException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.menu.categories.CategoryMapper;
import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.ProductVariantRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.entity.ProductVariant;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.CreateOrderRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.PartialPaymentRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.UpdatedOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.KitchenLineRef;
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

  // TTL del lock advisory de envío a cocina: suficiente para cubrir un ciclo
  // de impresión real por WiFi/LAN (unos pocos segundos) sin dejar el pedido
  // bloqueado mucho tiempo si el intento se abandona (impresión fallida y el
  // cliente nunca llama a confirmKitchen).
  private static final long KITCHEN_SEND_LOCK_TTL_SECONDS = 30;

  private final OrderRepository orderRepository;
  private final TableRepository tableRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;
  private final TransactionRepository transactionRepository;
  private final OrderCodeService orderCodeService;
  private final OrderMapper orderMapper;
  private final OrderItemMapper orderItemMapper;
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
    List<Order> orders = orderRepository.findAllWithDetails();

    return orders.stream()
      .map(orderMapper::toResponse)
      .toList();
  }

  @Transactional
  public void delete(Long id) {
    Order order = orderRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.FINALIZADO) {
      throw new BadRequestException("No se puede eliminar un pedido ya pagado");
    }

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

    if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.FINALIZADO) {
      throw new BadRequestException("No se puede cancelar un pedido ya pagado");
    }

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
  public OrderResponse finalizeOrder(Long orderId) {
    Order order = orderRepository.findByIdWithDetails(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    order.markAsFinalized();

    if (order.getType() == OrderType.DINE_IN) {
      Table table = order.getTable();
      table.free();
      tableRepository.save(table);
    }

    orderRepository.save(order);
    OrderResponse result = orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    Long tableId = order.getTable() != null ? order.getTable().getId() : null;
    orderEventPublisher.publish(OrderEvent.Type.FINALIZED, result.id(), tableId);
    return result;
  }

  @Override
  @Transactional
  public OrderResponse getKitchenPending(Long orderId) {
    Order order = orderRepository.findByIdWithDetails(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    LocalDateTime lockedAt = order.getKitchenSendLockedAt();
    LocalDateTime now = LocalDateTime.now();
    boolean lockStillFresh = lockedAt != null
      && lockedAt.isAfter(now.minusSeconds(KITCHEN_SEND_LOCK_TTL_SECONDS));
    if (lockStillFresh) {
      throw new ResourceConflictException(
        "Ya se está enviando la comanda de este pedido, esperá un momento.");
    }

    // Sin lock vigente (nunca se tomó, o quedó vencido tras un intento previo
    // abandonado): se toma el lock ahora y se procede a calcular el delta.
    order.setKitchenSendLockedAt(now);
    orderRepository.save(order);

    List<OrderItemResponse> deltas = new ArrayList<>();
    for (OrderItem item : order.getItems()) {
      int printed = item.getKitchenPrintedQuantity() != null ? item.getKitchenPrintedQuantity() : 0;
      int delta = item.getQuantity() - printed;
      if (delta > 0) deltas.add(orderItemMapper.toResponse(item, delta));
    }

    OrderResponse full = orderMapper.toResponse(order);
    return new OrderResponse(
      full.id(), full.orderCode(), full.tableId(), full.tableNumber(),
      full.type(), full.customerName(), full.status(), full.total(),
      full.paidAmount(), full.remainingAmount(), deltas
    );
  }

  @Override
  @Transactional
  public void confirmKitchen(Long orderId, List<KitchenLineRef> lines) {
    Order order = orderRepository.findByIdWithDetails(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    Map<Long, OrderItem> byId = order.getItems().stream()
      .collect(Collectors.toMap(OrderItem::getId, i -> i));

    for (KitchenLineRef line : lines) {
      OrderItem item = byId.get(line.itemId());
      if (item == null) continue;
      int printed = item.getKitchenPrintedQuantity() != null ? item.getKitchenPrintedQuantity() : 0;
      int updated = Math.min(item.getQuantity(), printed + line.quantity());
      item.setKitchenPrintedQuantity(updated);
    }

    // Confirmado: se libera el lock advisory tomado en getKitchenPending para
    // que el próximo envío a cocina (parcial o no) pueda proceder.
    order.setKitchenSendLockedAt(null);

    // Imprimir la comanda solo registra lo enviado a cocina. Cocina no usa
    // pantalla, avisa físicamente a caja cuando termina; el estado pasa a
    // READY únicamente vía el endpoint manual POST /orders/{id}/ready.
    orderRepository.save(order);
  }

  @Override
  @Transactional
  public OrderResponse payOrder(Long orderId, PaymentMethodType paymentMethodType, String idempotencyKey) {
    Order order = orderRepository.findByIdWithDetails(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    // Reintento con la misma clave de idempotencia: el primer intento ya se procesó
    // (o está en curso), así que devolvemos el estado actual en vez de cobrar dos veces.
    Optional<Transaction> existingTransaction = findExistingIdempotentTransaction(orderId, idempotencyKey);
    if (existingTransaction.isPresent()) {
      return orderMapper.toResponse(order);
    }

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
        .idempotencyKey(normalizeIdempotencyKey(idempotencyKey))
        .build();

    transactionRepository.save(transaction);

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

    // Reintento con la misma clave de idempotencia: el primer intento ya se procesó
    // (o está en curso), así que devolvemos el estado actual en vez de cobrar dos veces.
    Optional<Transaction> existingTransaction = findExistingIdempotentTransaction(orderId, paymentDTO.idempotencyKey());
    if (existingTransaction.isPresent()) {
      return orderMapper.toResponse(order);
    }

    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.IN_PROGRESS
        && order.getStatus() != OrderStatus.READY && order.getStatus() != OrderStatus.PARTIALLY_PAID) {
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
        .idempotencyKey(normalizeIdempotencyKey(paymentDTO.idempotencyKey()))
        .build();

    transactionRepository.save(transaction);

    // Calcular si la orden está completamente pagada sumando el nuevo pago
    BigDecimal totalPaid = order.getPaidAmount().add(paymentDTO.amount());
    boolean isFullyPaid  = totalPaid.compareTo(order.getTotal()) >= 0;

    // Actualizar el estado de la orden
    if (isFullyPaid) {
        order.setStatus(OrderStatus.PAID);
    } else {
        order.setStatus(OrderStatus.PARTIALLY_PAID);
    }

    Long tableId = order.getType() == OrderType.DINE_IN && order.getTable() != null ? order.getTable().getId() : null;
    orderRepository.save(order);
    OrderResponse result = orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    orderEventPublisher.publish(OrderEvent.Type.PAID, result.id(), tableId);
    return result;
  }

  private Optional<Transaction> findExistingIdempotentTransaction(Long orderId, String idempotencyKey) {
    String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
    if (normalizedKey == null) {
      return Optional.empty();
    }
    return transactionRepository.findByOrder_IdAndIdempotencyKey(orderId, normalizedKey);
  }

  private String normalizeIdempotencyKey(String idempotencyKey) {
    return (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : null;
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

    validateSelectedPrice(product, request.selectedPrice());

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
      orderItem.assignProductWithSelectedPrice(product, request.selectedPrice(), request.quantity());
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

    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.IN_PROGRESS
        && order.getStatus() != OrderStatus.READY) {
      throw new BadRequestException("No se puede modificar un pedido que no está en estado CREATED, IN_PROGRESS o READY");
    }

    int previousQuantity = orderItem.getQuantity();

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
    // Si la cantidad sube en un pedido READY, hay que cocinar más — vuelve a IN_PROGRESS.
    // Si baja (o se mantiene), no hay nada nuevo que preparar, se queda como está.
    if (order.getStatus() == OrderStatus.READY && request.quantity() > previousQuantity) {
      order.setStatus(OrderStatus.IN_PROGRESS);
    }
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

    if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.IN_PROGRESS
        && order.getStatus() != OrderStatus.READY) {
      throw new BadRequestException("No se puede eliminar items de un pedido que no está en estado CREATED, IN_PROGRESS o READY");
    }

    Long tableId = order.getTable() != null ? order.getTable().getId() : null;
    order.removeItem(orderItem);
    orderItemRepository.delete(orderItem);
    order.calculateTotal();
    if ((order.getStatus() == OrderStatus.IN_PROGRESS || order.getStatus() == OrderStatus.READY)
        && order.getItems().isEmpty()) {
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

    List<Order> activeOrders = orderRepository.findActiveOrdersByTableId(tableId);
    if (activeOrders.isEmpty()) {
      throw new ResourceNotFoundException("No hay orden activa para esta mesa");
    }
    Order activeOrder = activeOrders.get(0);

    List<OrderItemResponse> items = new ArrayList<>();

    if (activeOrder.getItems() != null) {
      items = orderItemMapper.toResponseList(activeOrder.getItems());
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
        activeOrder.getTable() != null ? activeOrder.getTable().getId() : null,
        activeOrder.getTable() != null ? activeOrder.getTable().getNumber() : null,
        activeOrder.getStatus(),
        activeOrder.getType(),
        activeOrder.getTotal(),
        items,
        activeOrder.getPaidAmount(),
        activeOrder.getRemainingAmount(),
        transactions
    );

  }

  /**
   * Un producto puede tener varios precios: el precio inicial (con el que se creó)
   * y, opcionalmente, uno o más precios adicionales (sus variantes). Ninguno de
   * los dos es "el especial": el precio inicial es simplemente una opción más,
   * siempre vigente, se hayan agregado variantes o no.
   *
   * El cliente puede enviar un selectedPrice indicando cuál de esas opciones eligió.
   * Nunca se confía en ese valor a ciegas: debe coincidir exactamente con el precio
   * inicial del producto o con el precio de alguna de sus variantes; de lo contrario
   * se trata de un intento de manipulación de precio y se rechaza.
   *
   * Cuando hay más de una opción disponible (1+ variantes), el cliente debe elegir
   * explícitamente una: omitir selectedPrice en ese caso no puede caer en silencio
   * al precio inicial, sería una forma de comprar una variante más cara pagando
   * el precio inicial. Con una sola opción (sin variantes), omitir selectedPrice
   * está bien y se resuelve al precio inicial del producto.
   */
  private void validateSelectedPrice(Product product, BigDecimal selectedPrice) {
    List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());

    if (selectedPrice == null) {
      if (!variants.isEmpty()) {
        throw new BadRequestException(
            "Este producto tiene variantes: debe especificarse el precio elegido");
      }
      return;
    }

    boolean matchesInitialPrice = product.getPrice() != null
        && product.getPrice().compareTo(selectedPrice) == 0;

    boolean matchesVariantPrice = variants.stream()
        .anyMatch(variant -> variant.getPrice() != null && variant.getPrice().compareTo(selectedPrice) == 0);

    if (!matchesInitialPrice && !matchesVariantPrice) {
      throw new BadRequestException(
          "El precio enviado no coincide con el precio inicial del producto ni con ninguna de sus variantes");
    }
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
