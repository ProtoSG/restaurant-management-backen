package com.restaurant_management.restaurant_management_backend.integration;

import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderService;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.CreateOrderRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.tables.TableRepository;
import com.restaurant_management.restaurant_management_backend.tables.TableService;
import com.restaurant_management.restaurant_management_backend.tables.dto.response.TableResponse;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TableServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired TableService tableService;
    @Autowired TableRepository tableRepository;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired ProductRepository productRepository;

    private Table table;
    private Product product;

    @BeforeEach
    void setUp() {
        table = tableRepository.save(
            Table.builder().number("R99").capacity(4).status(TableStatus.FREE).isActive(true).build()
        );
        product = productRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("V15 seed missing"));
    }

    // ── release ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin")
    void release_finalizesStuckPaidOrderAndFreesTable() {
        CreateOrderRequest req = new CreateOrderRequest(table.getId(), OrderType.DINE_IN, null);
        OrderResponse created = orderService.save(req);
        Long orderId = created.id();

        orderService.addOrderItem(orderId, new AddOrderItemRequest(product.getId(), 1, null, false));
        orderService.markAsReady(orderId);
        orderService.payOrder(orderId, PaymentMethodType.CASH);

        // Simulate the bug: order left PAID, nobody finalizes it, table stuck OCCUPIED
        Table stuck = tableRepository.findById(table.getId()).orElseThrow();
        assertThat(stuck.getStatus()).isEqualTo(TableStatus.OCCUPIED);

        TableResponse released = tableService.release(table.getId());

        assertThat(released.status()).isEqualTo(TableStatus.FREE);

        Table freed = tableRepository.findById(table.getId()).orElseThrow();
        assertThat(freed.getStatus()).isEqualTo(TableStatus.FREE);

        Order finalized = orderRepository.findById(orderId).orElseThrow();
        assertThat(finalized.getStatus()).isEqualTo(OrderStatus.FINALIZADO);
    }

    @Test
    @WithMockUser(username = "admin")
    void release_freesTableWithNoPaidOrdersWithoutErrors() {
        // Table has no orders at all; release must be a no-op besides forcing FREE.
        assertThatCode(() -> tableService.release(table.getId())).doesNotThrowAnyException();

        Table freed = tableRepository.findById(table.getId()).orElseThrow();
        assertThat(freed.getStatus()).isEqualTo(TableStatus.FREE);
    }

    @Test
    @WithMockUser(username = "admin")
    void release_rejectsWhenAnUnpaidOrderIsStillActive() {
        CreateOrderRequest req = new CreateOrderRequest(table.getId(), OrderType.DINE_IN, null);
        OrderResponse created = orderService.save(req);
        orderService.addOrderItem(created.id(), new AddOrderItemRequest(product.getId(), 1, null, false));

        assertThatThrownBy(() -> tableService.release(table.getId()))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("pedido sin pagar");

        Table stillOccupied = tableRepository.findById(table.getId()).orElseThrow();
        assertThat(stillOccupied.getStatus()).isEqualTo(TableStatus.OCCUPIED);

        Order untouched = orderRepository.findById(created.id()).orElseThrow();
        assertThat(untouched.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
    }
}
