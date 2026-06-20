package com.restaurant_management.restaurant_management_backend.integration;

import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.orders.OrderItemRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.orders.entity.OrderItem;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus;
import com.restaurant_management.restaurant_management_backend.tables.TableRepository;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OrderRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired TableRepository tableRepository;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;

    private Product anyProduct;
    private Table testTable;

    @BeforeEach
    void setUp() {
        anyProduct = productRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("V15 seed data missing — no products in DB"));

        testTable = tableRepository.save(
            Table.builder().number("T99").capacity(4).status(TableStatus.FREE).isActive(true).build()
        );
    }

    // ── findActiveOrder ──────────────────────────────────────────────────────

    @Test
    void findActiveOrder_returnsCreatedAndInProgressOrders() {
        Order active = orderRepository.save(order(OrderStatus.CREATED));
        orderRepository.save(order(OrderStatus.PAID));
        orderRepository.save(order(OrderStatus.CANCELLED));

        List<Order> result = orderRepository.findActiveOrder();

        assertThat(result).extracting(Order::getId).contains(active.getId());
        assertThat(result).noneMatch(o -> o.getStatus() == OrderStatus.PAID);
        assertThat(result).noneMatch(o -> o.getStatus() == OrderStatus.CANCELLED);
    }

    @Test
    void findActiveOrder_includesPartiallyPaidAndReady() {
        Order partiallyPaid = orderRepository.save(order(OrderStatus.PARTIALLY_PAID));
        Order ready        = orderRepository.save(order(OrderStatus.READY));

        List<Order> result = orderRepository.findActiveOrder();

        assertThat(result).extracting(Order::getId)
            .contains(partiallyPaid.getId(), ready.getId());
    }

    // ── findByIdWithDetails ──────────────────────────────────────────────────

    @Test
    void findByIdWithDetails_loadsItemsAndProductWithoutLazyException() {
        Order saved = orderRepository.save(order(OrderStatus.IN_PROGRESS));

        OrderItem item = new OrderItem();
        item.setOrder(saved);
        item.setProduct(anyProduct);
        item.setQuantity(2);
        item.setUnitPrice(anyProduct.getPrice());
        item.setSubTotal(anyProduct.getPrice().multiply(BigDecimal.valueOf(2)));
        item.setTakeawaySurcharge(BigDecimal.ZERO);
        item.setIsTakeaway(false);
        // Order.items has no CascadeType — save item directly
        orderItemRepository.save(item);

        // Flush writes to DB then clear evicts all entities from first-level cache,
        // so the next JPQL query re-fetches from DB (not from cache).
        em.flush();
        em.clear();

        Optional<Order> found = orderRepository.findByIdWithDetails(saved.getId());

        assertThat(found).isPresent();
        // Access lazy collections — no LazyInitializationException expected
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().iterator().next().getProduct().getName()).isNotBlank();
        assertThat(found.get().getItems().iterator().next().getProduct().getCategory()).isNotNull();
    }

    @Test
    void findByIdWithDetails_returnsEmptyForUnknownId() {
        Optional<Order> result = orderRepository.findByIdWithDetails(Long.MAX_VALUE);
        assertThat(result).isEmpty();
    }

    // ── findActiveOrderByTableId ─────────────────────────────────────────────

    @Test
    void findActiveOrderByTableId_returnsActiveOrderForTable() {
        Order active = orderRepository.save(
            Order.builder()
                .orderCode("INT-T99-001")
                .type(OrderType.DINE_IN)
                .status(OrderStatus.IN_PROGRESS)
                .table(testTable)
                .total(BigDecimal.valueOf(50))
                .items(new LinkedHashSet<>())
                .transactions(new LinkedHashSet<>())
                .build()
        );

        Optional<Order> found = orderRepository.findActiveOrderByTableId(testTable.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(active.getId());
    }

    @Test
    void findActiveOrderByTableId_returnsEmptyWhenOnlyPaidOrderExists() {
        orderRepository.save(
            Order.builder()
                .orderCode("INT-T99-PAID")
                .type(OrderType.DINE_IN)
                .status(OrderStatus.PAID)
                .table(testTable)
                .total(BigDecimal.valueOf(50))
                .items(new LinkedHashSet<>())
                .transactions(new LinkedHashSet<>())
                .build()
        );

        Optional<Order> found = orderRepository.findActiveOrderByTableId(testTable.getId());

        assertThat(found).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static int seq = 0;

    private Order order(OrderStatus status) {
        return Order.builder()
            .orderCode("INT-TEST-" + (++seq))
            .type(OrderType.TAKEAWAY)
            .status(status)
            .total(BigDecimal.valueOf(100))
            .items(new LinkedHashSet<>())
            .transactions(new LinkedHashSet<>())
            .build();
    }
}
