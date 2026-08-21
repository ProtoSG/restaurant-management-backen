package com.restaurant_management.restaurant_management_backend.service;

import com.restaurant_management.restaurant_management_backend.auth.UserRepository;
import com.restaurant_management.restaurant_management_backend.menu.categories.CategoryMapper;
import com.restaurant_management.restaurant_management_backend.menu.categories.entity.Category;
import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.ProductVariantRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.entity.ProductVariant;
import com.restaurant_management.restaurant_management_backend.orders.OrderCodeService;
import com.restaurant_management.restaurant_management_backend.orders.OrderItemMapper;
import com.restaurant_management.restaurant_management_backend.orders.OrderItemRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderMapper;
import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderServiceImpl;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.KitchenLineRef;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.PartialPaymentRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.UpdatedOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.orders.entity.OrderItem;
import com.restaurant_management.restaurant_management_backend.shared.config.SystemConfigRepository;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceConflictException;
import com.restaurant_management.restaurant_management_backend.tables.TableRepository;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;
import com.restaurant_management.restaurant_management_backend.transactions.TransactionMapper;
import com.restaurant_management.restaurant_management_backend.transactions.TransactionRepository;
import com.restaurant_management.restaurant_management_backend.transactions.entity.Transaction;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEventPublisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock OrderRepository orderRepository;
  @Mock TableRepository tableRepository;
  @Mock OrderItemRepository orderItemRepository;
  @Mock ProductRepository productRepository;
  @Mock ProductVariantRepository productVariantRepository;
  @Mock TransactionRepository transactionRepository;
  @Mock OrderCodeService orderCodeService;
  @Mock OrderMapper orderMapper;
  @Mock OrderItemMapper orderItemMapper;
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
      orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 1, null, false, null)))
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
      orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 1, null, false, null)))
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

    orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 2, null, false, null));

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

    orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 3, null, false, null));

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

    orderService.addOrderItem(1L, new AddOrderItemRequest(3L, 1, null, true, null));

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

    orderService.addOrderItem(1L, new AddOrderItemRequest(7L, 1, null, true, null));

    OrderItem created = order.getItems().iterator().next();
    assertThat(created.getTakeawaySurcharge()).isEqualByComparingTo(BigDecimal.valueOf(2));
  }

  // ── addOrderItem: validación de selectedPrice ───────────────────────────────

  @Test
  void addOrderItem_acceptsSelectedPriceMatchingRealVariantPrice() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();
    Product product = food(5L, BigDecimal.valueOf(20));
    ProductVariant variant = ProductVariant.builder().id(1L).product(product).name("Grande").price(BigDecimal.valueOf(28)).build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findByIdWithCategory(5L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(5L)).thenReturn(List.of(variant));

    orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 1, null, false, BigDecimal.valueOf(28)));

    OrderItem created = order.getItems().iterator().next();
    assertThat(created.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(28));
    verify(orderItemRepository).save(any());
  }

  @Test
  void addOrderItem_acceptsSelectedPriceMatchingInitialPriceEvenWhenVariantsExist() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();
    Product product = food(5L, BigDecimal.valueOf(20));
    ProductVariant variant = ProductVariant.builder().id(1L).product(product).name("Grande").price(BigDecimal.valueOf(28)).build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findByIdWithCategory(5L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(5L)).thenReturn(List.of(variant));

    // El precio inicial (20) sigue siendo una opción válida aunque el producto
    // tenga variantes (28): no son mutuamente excluyentes.
    orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 1, null, false, BigDecimal.valueOf(20)));

    OrderItem created = order.getItems().iterator().next();
    assertThat(created.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(20));
    verify(orderItemRepository).save(any());
  }

  @Test
  void addOrderItem_throwsWhenSelectedPriceDoesNotMatchProductOrAnyVariant() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();
    Product product = food(5L, BigDecimal.valueOf(20));
    ProductVariant variant = ProductVariant.builder().id(1L).product(product).name("Grande").price(BigDecimal.valueOf(28)).build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findByIdWithCategory(5L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(5L)).thenReturn(List.of(variant));

    // Precio manipulado: no coincide ni con el inicial (20) ni con la variante (28).
    assertThatThrownBy(() ->
      orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 1, null, false, BigDecimal.valueOf(1))))
      .isInstanceOf(BadRequestException.class);

    verify(orderItemRepository, never()).save(any());
  }

  @Test
  void addOrderItem_throwsWhenProductHasVariantsAndSelectedPriceIsOmitted() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();
    Product product = food(5L, BigDecimal.valueOf(20));
    ProductVariant variant = ProductVariant.builder().id(1L).product(product).name("Grande").price(BigDecimal.valueOf(28)).build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findByIdWithCategory(5L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(5L)).thenReturn(List.of(variant));

    // Con más de una opción disponible (inicial + variante), hay que elegir una explícitamente.
    assertThatThrownBy(() ->
      orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 1, null, false, null)))
      .isInstanceOf(BadRequestException.class);

    verify(orderItemRepository, never()).save(any());
  }

  @Test
  void addOrderItem_acceptsSelectedPriceMatchingInitialPriceWhenProductHasNoVariants() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();
    Product product = food(5L, BigDecimal.valueOf(20));

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findByIdWithCategory(5L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(5L)).thenReturn(Collections.emptyList());

    orderService.addOrderItem(1L, new AddOrderItemRequest(5L, 1, null, false, BigDecimal.valueOf(20)));

    OrderItem created = order.getItems().iterator().next();
    assertThat(created.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(20));
  }

  // ── updateOrderItem ──────────────────────────────────────────────────────────

  @Test
  void updateOrderItem_allowedWhenOrderIsReadyAndQuantityDecreases() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.READY).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    OrderItem item = new OrderItem();
    item.setId(10L);
    item.setQuantity(5);
    item.setSubTotal(BigDecimal.valueOf(50));
    item.setTakeawaySurcharge(BigDecimal.ZERO);
    item.setOrder(order);
    order.getItems().add(item);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

    orderService.updateOrderItem(1L, 10L, new UpdatedOrderItemRequest(3, null, null));

    assertThat(item.getQuantity()).isEqualTo(3);
    // Bajar la cantidad no requiere cocinar más — se queda READY.
    assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
  }

  @Test
  void updateOrderItem_revertsReadyToInProgressWhenQuantityIncreases() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.READY).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    OrderItem item = new OrderItem();
    item.setId(10L);
    item.setQuantity(1);
    item.setSubTotal(BigDecimal.valueOf(10));
    item.setTakeawaySurcharge(BigDecimal.ZERO);
    item.setOrder(order);
    order.getItems().add(item);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

    orderService.updateOrderItem(1L, 10L, new UpdatedOrderItemRequest(2, null, null));

    assertThat(item.getQuantity()).isEqualTo(2);
    // Subir la cantidad significa cocinar más — vuelve a IN_PROGRESS.
    assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
  }

  @Test
  void updateOrderItem_throwsWhenOrderIsPaid() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.PAID).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    OrderItem item = new OrderItem();
    item.setId(10L);
    item.setOrder(order);
    order.getItems().add(item);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(orderItemRepository.findById(10L)).thenReturn(Optional.of(item));

    assertThatThrownBy(() -> orderService.updateOrderItem(1L, 10L, new UpdatedOrderItemRequest(3, null, null)))
      .isInstanceOf(BadRequestException.class);
  }

  // ── removeOrderItem ──────────────────────────────────────────────────────────

  @Test
  void removeOrderItem_revertsReadyToCreatedWhenLastItemRemoved() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.READY).type(OrderType.DINE_IN)
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

  // ── delete ───────────────────────────────────────────────────────────────────

  @Test
  void delete_freesTableForDineIn() {
    Table table = Table.builder().id(2L).status(TableStatus.OCCUPIED).build();
    Order order = Order.builder()
      .id(1L).status(OrderStatus.IN_PROGRESS).type(OrderType.DINE_IN)
      .table(table).items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    orderService.delete(1L);

    assertThat(table.getStatus()).isEqualTo(TableStatus.FREE);
    verify(orderRepository).delete(order);
  }

  @Test
  void delete_throwsWhenOrderIsPaid() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.PAID).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.delete(1L))
      .isInstanceOf(BadRequestException.class);
    verify(orderRepository, never()).delete(any());
  }

  @Test
  void delete_throwsWhenOrderIsFinalizado() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.FINALIZADO).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.delete(1L))
      .isInstanceOf(BadRequestException.class);
    verify(orderRepository, never()).delete(any());
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

  @Test
  void cancelOrder_throwsWhenOrderIsPaid() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.PAID).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.cancelOrder(1L))
      .isInstanceOf(BadRequestException.class);
    verify(orderRepository, never()).save(any());
  }

  @Test
  void cancelOrder_throwsWhenOrderIsFinalizado() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.FINALIZADO).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.cancelOrder(1L))
      .isInstanceOf(BadRequestException.class);
    verify(orderRepository, never()).save(any());
  }

  // ── finalizeOrder ────────────────────────────────────────────────────────────

  @Test
  void finalizeOrder_throwsWhenOrderNotPaid() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.READY).type(OrderType.DINE_IN)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.finalizeOrder(1L))
      .isInstanceOf(IllegalStateException.class);
    verify(tableRepository, never()).save(any());
  }

  @Test
  void finalizeOrder_freesTableForDineIn() {
    Table table = Table.builder().id(2L).status(TableStatus.OCCUPIED).build();
    Order order = Order.builder()
      .id(1L).status(OrderStatus.PAID).type(OrderType.DINE_IN)
      .table(table)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
    when(orderMapper.toResponse(any())).thenReturn(mock(OrderResponse.class));

    orderService.finalizeOrder(1L);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.FINALIZADO);
    assertThat(table.getStatus()).isEqualTo(TableStatus.FREE);
    verify(tableRepository).save(table);
  }

  @Test
  void finalizeOrder_doesNotTouchTableForTakeaway() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.PAID).type(OrderType.TAKEAWAY)
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
    when(orderMapper.toResponse(any())).thenReturn(mock(OrderResponse.class));

    orderService.finalizeOrder(1L);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.FINALIZADO);
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

    assertThatThrownBy(() -> orderService.payOrder(1L, PaymentMethodType.CASH, null))
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
        BigDecimal.valueOf(150), PaymentMethodType.CASH, null)))
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
      BigDecimal.valueOf(60), PaymentMethodType.CASH, null));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_PAID);
    verify(transactionRepository).save(any());
  }

  @Test
  void payPartialOrder_setsPaidButDoesNotFreeTableWhenAmountCoversTotal() {
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
      BigDecimal.valueOf(100), PaymentMethodType.CASH, null));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    // Pago adelantado: la mesa se libera recién al finalizar el pedido, no al pagar
    assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
    verify(tableRepository, never()).save(table);
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
        BigDecimal.valueOf(50), PaymentMethodType.CASH, null)))
      .isInstanceOf(IllegalStateException.class);
  }

  // ── payPartialOrder: idempotencia ───────────────────────────────────────────

  @Test
  void payPartialOrder_sameIdempotencyKeyOnRetryCreatesOnlyOneTransaction() {
    Order order = Order.builder()
      .id(1L).status(OrderStatus.CREATED).type(OrderType.DINE_IN)
      .total(BigDecimal.valueOf(100))
      .items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    Transaction savedTransaction = Transaction.builder()
      .id(9L).order(order).total(BigDecimal.valueOf(60))
      .paymentMethod(PaymentMethodType.CASH)
      .idempotencyKey("retry-key")
      .build();

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
    // Primer intento: no hay transacción previa con esta clave. Segundo intento
    // (el reintento): ya existe — simula que el primer pago sí se persistió.
    when(transactionRepository.findByOrder_IdAndIdempotencyKey(1L, "retry-key"))
      .thenReturn(Optional.empty(), Optional.of(savedTransaction));
    when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(orderMapper.toResponse(any())).thenReturn(mock(OrderResponse.class));

    orderService.payPartialOrder(1L, new PartialPaymentRequest(
      BigDecimal.valueOf(60), PaymentMethodType.CASH, "retry-key"));
    orderService.payPartialOrder(1L, new PartialPaymentRequest(
      BigDecimal.valueOf(60), PaymentMethodType.CASH, "retry-key"));

    verify(transactionRepository, times(1)).save(any());
  }

  // ── kitchen: lock advisory contra envíos concurrentes ───────────────────────

  @Test
  void getKitchenPending_throwsWhenLockIsFresh() {
    Order order = createdOrder();
    // Lock tomado hace 5s: sigue vigente dentro del TTL de 30s.
    order.setKitchenSendLockedAt(LocalDateTime.now().minusSeconds(5));

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.getKitchenPending(1L))
      .isInstanceOf(ResourceConflictException.class);

    verify(orderRepository, never()).save(any());
  }

  @Test
  void getKitchenPending_succeedsWhenLockIsStale() {
    Order order = createdOrder();
    // Lock tomado hace 45s: ya venció el TTL de 30s (intento previo abandonado).
    order.setKitchenSendLockedAt(LocalDateTime.now().minusSeconds(45));

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
    when(orderMapper.toResponse(any())).thenReturn(mock(OrderResponse.class));

    orderService.getKitchenPending(1L);

    // El intento vencido no bloquea: se toma un lock nuevo y se procede.
    assertThat(order.getKitchenSendLockedAt()).isNotNull();
    verify(orderRepository).save(order);
  }

  @Test
  void confirmKitchen_clearsLockSoNextAttemptProceedsNormally() {
    Order order = createdOrder();
    order.setKitchenSendLockedAt(LocalDateTime.now()); // tomado por el intento en curso

    OrderItem item = new OrderItem();
    item.setId(10L);
    item.setQuantity(3);
    item.setKitchenPrintedQuantity(0);
    item.setOrder(order);
    order.getItems().add(item);

    when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

    orderService.confirmKitchen(1L, List.of(new KitchenLineRef(10L, 3)));

    assertThat(order.getKitchenSendLockedAt()).isNull();
    assertThat(item.getKitchenPrintedQuantity()).isEqualTo(3);
    verify(orderRepository).save(order);

    // Con el lock ya liberado, el próximo envío a cocina no debería chocar.
    when(orderMapper.toResponse(any())).thenReturn(mock(OrderResponse.class));
    orderService.getKitchenPending(1L);
    assertThat(order.getKitchenSendLockedAt()).isNotNull();
  }
}
