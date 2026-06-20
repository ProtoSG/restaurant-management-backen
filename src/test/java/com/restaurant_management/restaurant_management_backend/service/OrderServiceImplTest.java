package com.restaurant_management.restaurant_management_backend.service;

import com.restaurant_management.restaurant_management_backend.auth.UserRepository;
import com.restaurant_management.restaurant_management_backend.menu.categories.CategoryMapper;
import com.restaurant_management.restaurant_management_backend.menu.categories.entity.Category;
import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.orders.OrderCodeService;
import com.restaurant_management.restaurant_management_backend.orders.OrderItemRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderMapper;
import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderServiceImpl;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.PartialPaymentRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.orders.entity.OrderItem;
import com.restaurant_management.restaurant_management_backend.shared.config.SystemConfigRepository;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.tables.TableRepository;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;
import com.restaurant_management.restaurant_management_backend.transactions.TransactionMapper;
import com.restaurant_management.restaurant_management_backend.transactions.TransactionRepository;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEventPublisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock OrderRepository orderRepository;
  @Mock TableRepository tableRepository;
  @Mock OrderItemRepository orderItemRepository;
  @Mock ProductRepository productRepository;
  @Mock TransactionRepository transactionRepository;
  @Mock OrderCodeService orderCodeService;
  @Mock OrderMapper orderMapper;
  @Mock CategoryMapper categoryMapper;
  @Mock OrderEventPublisher orderEventPublisher;
  @Mock TransactionMapper transactionMapper;
  @Mock SystemConfigRepository systemConfigRepository;
  @Mock UserRepository userRepository;

  @InjectMocks
  OrderServiceImpl orderService;

  // ── helpers ─────────────────────────────────────────────────────────────────

  private Order createdOrder() {
    return Order.builder()
      .id(1L)
      .status(OrderStatus.CREATED)
      .type(OrderType.DINE_IN)
      .total(BigDecimal.valueOf(100))
      .items(new LinkedHashSet<>())
      .transactions(new LinkedHashSet<>())
      .build();
  }

  private Product food(long id, BigDecimal price) {
    Category cat = Category.builder().id(1L).name("platos").build();
    return Product.builder().id(id).name("Producto " + id).price(price).category(cat).build();
  }

  private Product drink(long id, BigDecimal price) {
    Category cat = Category.builder().id(2L).name("bebidas").build();
    return Product.builder().id(id).name("Bebida " + id).price(price).category(cat).build();
  }

  // ── addOrderItem ─────────────────────────────────────────────────────────────

  @Test
  void addOrderItem_throwsWhenOrderCancelled() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CANCELLED).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() ->
      orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 1, null, false)))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  void addOrderItem_throwsWhenOrderPaid() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.PAID).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() ->
      orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 1, null, false)))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  void addOrderItem_transitionsCreatedToInProgress() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();
    Product product = food(5L, BigDecimal.valueOf(20));

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findByIdWithCategory(5L)).thenReturn(Optional.of(product));

    orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 2, null, false));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
    verify(orderRepository).save(order);
  }

  @Test
  void addOrderItem_mergesExistingItemQuantity() {
    Product product = food(5L, BigDecimal.valueOf(10));

    OrderItem existing = new OrderItem();
    existing.setProduct(product);
    existing.setQuantity(2);
    existing.setUnitPrice(BigDecimal.valueOf(10));
    existing.setSubTotal(BigDecimal.valueOf(20));
    existing.setIsTakeaway(false);
    existing.setTakeawaySurcharge(BigDecimal.ZERO);

    Set<OrderItem> items = new LinkedHashSet<>();
    items.add(existing);

    Order order = Order.builder()
      .id(1L).status(OrderStatus.IN_PROGRESS).type(OrderType.DINE_IN)
      .items(items).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findByIdWithCategory(5L)).thenReturn(Optional.of(product));

    orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 3, null, false));

    assertThat(existing.getQuantity()).isEqualTo(5); // 2 + 3
    verify(orderItemRepository).save(existing);
  }

  @Test
  void addOrderItem_zeroSurchargeForBebidasEvenWhenTakeaway() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();
    Product drinkProduct = drink(3L, BigDecimal.valueOf(5));

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findByIdWithCategory(3L)).thenReturn(Optional.of(drinkProduct));

    orderService.addOrderItem(1L, new AddOrderItemRequest(3L, 1, null, true));

    OrderItem created = order.getItems().iterator().next();
    assertThat(created.getTakeawaySurcharge()).isEqualByComparingTo(BigDecimal.ZERO);
    verify(systemConfigRepository, never()).findById(any());
  }

  @Test
  void addOrderItem_appliesSurchargeForNonBebidasTakeaway() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();
    Product foodProduct = food(7L, BigDecimal.valueOf(20));

    com.restaurant_management.restaurant_management_backend.shared.config.SystemConfig cfg =
      new com.restaurant_management.restaurant_management_backend.shared.config.SystemConfig();
    cfg.setValue("2");

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findByIdWithCategory(7L)).thenReturn(Optional.of(foodProduct));
    when(systemConfigRepository.findById("takeaway_surcharge")).thenReturn(Optional.of(cfg));

    orderService.addOrderItem(1L, new AddOrderItemRequest(7L, 1, null, true));

    OrderItem created = order.getItems().iterator().next();
    assertThat(created.getTakeawaySurcharge()).isEqualByComparingTo(BigDecimal.valueOf(2));
  }

  // ── removeOrderItem ──────────────────────────────────────────────────────────

  @Test
  void removeOrderItem_revertsInProgressToCreatedWhenLastItemRemoved() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.IN_PROGRESS).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    OrderItem item = new OrderItem();
    item.setId(10L);
    item.setSubTotal(BigDecimal.valueOf(20));
    item.setTakeawaySurcharge(BigDecimal.ZERO);
    item.setOrder(order);
    order.getItems().add(item);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

    orderService.removeOrderItemByOrderId(1L, 10L);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(order.getItems()).isEmpty();
  }

  @Test
  void removeOrderItem_throwsWhenItemBelongsToDifferentOrder() {
    Order order1 = Order.builder()
      .id(1L).status(OrderStatus.IN_PROGRESS).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    Order order2 = Order.builder()
      .id(2L).status(OrderStatus.IN_PROGRESS).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    OrderItem item = new OrderItem();
    item.setId(10L);
    item.setOrder(order2); // belongs to order 2, not order 1

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
    when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

    assertThatThrownBy(() -> orderService.removeOrderItemByOrderId(1L, 10L))
      .isInstanceOf(BadRequestException.class);
  }

  // ── cancelOrder ──────────────────────────────────────────────────────────────

  @Test
  void cancelOrder_freesTableForDineIn() {
    Table table = Table.builder().id(2L).status(TableStatus.OCCUPIED).build();
    Order order = Order.builder()
      .id(1L).status(OrderStatus.IN_PROGRESS).type(OrderType.DINE_IN)
      .table(table).items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    orderService.cancelOrder(1L);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(table.getStatus()).isEqualTo(TableStatus.FREE);
    verify(tableRepository).save(table);
    verify(orderRepository).save(order);
  }

  @Test
  void cancelOrder_doesNotTouchTableForTakeaway() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.TAKEAWAY)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    orderService.cancelOrder(1L);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    verify(tableRepository, never()).save(any());
  }

  // ── payOrder ─────────────────────────────────────────────────────────────────

  @Test
  void payOrder_throwsWhenOrderIsPartiallyPaid() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.PARTIALLY_PAID).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.payOrder(1L, PaymentMethodType.CASH))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("parciales");
  }

  // ── payPartialOrder ──────────────────────────────────────────────────────────

  @Test
  void payPartialOrder_throwsWhenAmountExceedsRemaining() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .total(BigDecimal.valueOf(100))
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() ->
      orderService.payPartialOrder(1L, new PartialPaymentRequest(
        BigDecimal.valueOf(150), PaymentMethodType.CASH)))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void payPartialOrder_setsPartiallyPaidWhenAmountLessThanTotal() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .total(BigDecimal.valueOf(100))
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
    when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(orderMapper.toResponse(any())).thenReturn(mock(OrderResponse.class));

    orderService.payPartialOrder(1L, new PartialPaymentRequest(
      BigDecimal.valueOf(60), PaymentMethodType.CASH));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_PAID);
    verify(transactionRepository).save(any());
  }

  @Test
  void payPartialOrder_setsPaidAndFreesTableWhenAmountCoversTotal() {
    Table table = Table.builder().id(2L).status(TableStatus.OCCUPIED).build();
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .total(BigDecimal.valueOf(100))
      .table(table)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
    when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(orderMapper.toResponse(any())).thenReturn(mock(OrderResponse.class));

    orderService.payPartialOrder(1L, new PartialPaymentRequest(
      BigDecimal.valueOf(100), PaymentMethodType.CASH));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    assertThat(table.getStatus()).isEqualTo(TableStatus.FREE);
    verify(tableRepository).save(table);
  }

  @Test
  void payPartialOrder_throwsWhenOrderNotPayable() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CANCELLED).type(OrderType.DINE_IN)
      .total(BigDecimal.valueOf(100))
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() ->
      orderService.payPartialOrder(1L, new PartialPaymentRequest(
        BigDecimal.valueOf(50), PaymentMethodType.CASH)))
      .isInstanceOf(IllegalStateException.class);
  }
}
