package com.restaurant_management.restaurant_management_backend.integration;

import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderService;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.CreateOrderRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.PartialPaymentRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus;
import com.restaurant_management.restaurant_management_backend.tables.TableRepository;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;
import com.restaurant_management.restaurant_management_backend.transactions.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired TableRepository tableRepository;
    @Autowired ProductRepository productRepository;
    @Autowired TransactionRepository transactionRepository;

    private Table table;
    private Product product;

    @BeforeEach
    void setUp() {
        table = tableRepository.save(
            Table.builder().number("S99").capacity(4).status(TableStatus.FREE).isActive(true).build()
        );
        product = productRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("V15 seed missing"));
    }

    // ── save (create order) ──────────────────────────────────────────────────

    @Test
    void save_dineIn_occupiesTableAndPersistsOrder() {
        CreateOrderRequest req = new CreateOrderRequest(
            table.getId(), OrderType.DINE_IN, null
        );

        OrderResponse response = orderService.save(req);

        assertThat(response.id()).isPositive();
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);

        Table updated = tableRepository.findById(table.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void save_takeaway_doesNotRequireTable() {
        CreateOrderRequest req = new CreateOrderRequest(
            null, OrderType.TAKEAWAY, "Cliente X"
        );

        OrderResponse response = orderService.save(req);

        assertThat(response.id()).isPositive();
        assertThat(response.type()).isEqualTo(OrderType.TAKEAWAY);
    }

    // ── addOrderItem ─────────────────────────────────────────────────────────

    @Test
    void addOrderItem_transitionsCreatedToInProgressAndCalculatesTotal() {
        CreateOrderRequest req = new CreateOrderRequest(null, OrderType.TAKEAWAY, null);
        OrderResponse created = orderService.save(req);

        orderService.addOrderItem(created.id(),
            new AddOrderItemRequest(product.getId(), 2, null, false));

        Order updated = orderRepository.findByIdWithDetails(created.id()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getTotal()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void addOrderItem_mergesDuplicateProductQuantity() {
        CreateOrderRequest req = new CreateOrderRequest(null, OrderType.TAKEAWAY, null);
        OrderResponse created = orderService.save(req);
        Long orderId = created.id();

        orderService.addOrderItem(orderId, new AddOrderItemRequest(product.getId(), 1, null, false));
        orderService.addOrderItem(orderId, new AddOrderItemRequest(product.getId(), 2, null, false));

        Order updated = orderRepository.findByIdWithDetails(orderId).orElseThrow();
        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().iterator().next().getQuantity()).isEqualTo(3);
    }

    // ── cancelOrder ──────────────────────────────────────────────────────────

    @Test
    void cancelOrder_setsCancelledStatusAndFreesTable() {
        CreateOrderRequest req = new CreateOrderRequest(table.getId(), OrderType.DINE_IN, null);
        OrderResponse created = orderService.save(req);

        orderService.cancelOrder(created.id());

        Order cancelled = orderRepository.findById(created.id()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Table freed = tableRepository.findById(table.getId()).orElseThrow();
        assertThat(freed.getStatus()).isEqualTo(TableStatus.FREE);
    }

    // ── payOrder ─────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin")
    void payOrder_setsPaidAndCreatesTransactionAndFreesTable() {
        CreateOrderRequest req = new CreateOrderRequest(table.getId(), OrderType.DINE_IN, null);
        OrderResponse created = orderService.save(req);
        Long orderId = created.id();

        // Add an item so the order has a non-zero total
        orderService.addOrderItem(orderId, new AddOrderItemRequest(product.getId(), 1, null, false));

        // Mark as ready first — payOrder works from CREATED/READY
        orderService.markAsReady(orderId);

        orderService.payOrder(orderId, PaymentMethodType.CASH);

        Order paid = orderRepository.findByIdWithDetails(orderId).orElseThrow();
        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);

        Table freed = tableRepository.findById(table.getId()).orElseThrow();
        assertThat(freed.getStatus()).isEqualTo(TableStatus.FREE);

        assertThat(transactionRepository.findAll())
            .anyMatch(t -> t.getOrder().getId().equals(orderId));
    }

    // ── payPartialOrder ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin")
    void payPartialOrder_setsPartiallyPaidThenPaidOnFinalPayment() {
        CreateOrderRequest req = new CreateOrderRequest(null, OrderType.TAKEAWAY, null);
        OrderResponse created = orderService.save(req);
        Long orderId = created.id();

        orderService.addOrderItem(orderId, new AddOrderItemRequest(product.getId(), 2, null, false));
        orderService.markAsReady(orderId);

        Order withItems = orderRepository.findByIdWithDetails(orderId).orElseThrow();
        BigDecimal total = withItems.getTotal();
        BigDecimal half  = total.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.DOWN);

        // First payment — partial
        orderService.payPartialOrder(orderId,
            new PartialPaymentRequest(half, PaymentMethodType.CASH));

        Order afterFirst = orderRepository.findByIdWithDetails(orderId).orElseThrow();
        assertThat(afterFirst.getStatus()).isEqualTo(OrderStatus.PARTIALLY_PAID);

        // Second payment — covers remainder
        BigDecimal remaining = afterFirst.getRemainingAmount();
        orderService.payPartialOrder(orderId,
            new PartialPaymentRequest(remaining, PaymentMethodType.CREDITCARD));

        Order afterSecond = orderRepository.findByIdWithDetails(orderId).orElseThrow();
        assertThat(afterSecond.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @WithMockUser(username = "admin")
    void payPartialOrder_throwsWhenAmountExceedsRemaining() {
        CreateOrderRequest req = new CreateOrderRequest(null, OrderType.TAKEAWAY, null);
        OrderResponse created = orderService.save(req);
        Long orderId = created.id();

        orderService.addOrderItem(orderId, new AddOrderItemRequest(product.getId(), 1, null, false));
        orderService.markAsReady(orderId);

        Order withItems = orderRepository.findByIdWithDetails(orderId).orElseThrow();
        BigDecimal overAmount = withItems.getTotal().add(BigDecimal.valueOf(999));

        assertThatThrownBy(() ->
            orderService.payPartialOrder(orderId,
                new PartialPaymentRequest(overAmount, PaymentMethodType.CASH)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
