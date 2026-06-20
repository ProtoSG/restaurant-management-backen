package com.restaurant_management.restaurant_management_backend.entity;

import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.TransactionStatus;
import com.restaurant_management.restaurant_management_backend.transactions.entity.Transaction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderEntityTest {

  @Test
  void markAsPaid_changesStatusToPaid() {
    Order order = Order.builder()
      .status(OrderStatus.CREATED)
      .total(BigDecimal.valueOf(100))
      .transactions(new LinkedHashSet<>())
      .build();

    order.markAsPaid();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
  }

  @Test
  void markAsPaid_throwsWhenAlreadyPaid() {
    Order order = Order.builder()
      .status(OrderStatus.PAID)
      .total(BigDecimal.valueOf(100))
      .transactions(new LinkedHashSet<>())
      .build();

    assertThatThrownBy(order::markAsPaid)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void getPaidAmount_sumCompletedTransactions() {
    Transaction t1 = Transaction.builder()
      .total(BigDecimal.valueOf(30))
      .status(TransactionStatus.COMPLETED)
      .transactionDate(LocalDateTime.now())
      .build();

    Transaction t2 = Transaction.builder()
      .total(BigDecimal.valueOf(20))
      .status(TransactionStatus.COMPLETED)
      .transactionDate(LocalDateTime.now())
      .build();

    Set<Transaction> txns = new LinkedHashSet<>();
    txns.add(t1);
    txns.add(t2);

    Order order = Order.builder()
      .status(OrderStatus.CREATED)
      .total(BigDecimal.valueOf(100))
      .transactions(txns)
      .build();

    assertThat(order.getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
  }

  @Test
  void getRemainingAmount_returnsDifference() {
    Transaction t1 = Transaction.builder()
      .total(BigDecimal.valueOf(40))
      .status(TransactionStatus.COMPLETED)
      .transactionDate(LocalDateTime.now())
      .build();

    Set<Transaction> txns = new LinkedHashSet<>();
    txns.add(t1);

    Order order = Order.builder()
      .status(OrderStatus.CREATED)
      .total(BigDecimal.valueOf(100))
      .transactions(txns)
      .build();

    assertThat(order.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(60));
  }

  @Test
  void isFullyPaid_returnsTrueWhenRemainingIsZero() {
    Transaction t1 = Transaction.builder()
      .total(BigDecimal.valueOf(100))
      .status(TransactionStatus.COMPLETED)
      .transactionDate(LocalDateTime.now())
      .build();

    Set<Transaction> txns = new LinkedHashSet<>();
    txns.add(t1);

    Order order = Order.builder()
      .status(OrderStatus.CREATED)
      .total(BigDecimal.valueOf(100))
      .transactions(txns)
      .build();

    assertThat(order.isFullyPaid()).isTrue();
  }

  @Test
  void markAsReady_changesStatusToReady() {
    Order order = Order.builder()
      .status(OrderStatus.IN_PROGRESS)
      .total(BigDecimal.valueOf(100))
      .transactions(new LinkedHashSet<>())
      .build();

    order.markAsReady();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
  }

  @Test
  void markAsReady_throwsWhenNotInProgress() {
    Order order = Order.builder()
      .status(OrderStatus.CREATED)
      .total(BigDecimal.valueOf(50))
      .transactions(new LinkedHashSet<>())
      .build();

    assertThatThrownBy(order::markAsReady)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void assignToTable_throwsWhenNotDineIn() {
    com.restaurant_management.restaurant_management_backend.tables.entity.Table table =
      com.restaurant_management.restaurant_management_backend.tables.entity.Table.builder()
        .status(com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus.FREE)
        .build();

    Order order = Order.builder()
      .type(com.restaurant_management.restaurant_management_backend.shared.enums.OrderType.TAKEAWAY)
      .transactions(new LinkedHashSet<>())
      .build();

    assertThatThrownBy(() -> order.assignToTable(table))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void assignToTable_throwsWhenTableOccupied() {
    com.restaurant_management.restaurant_management_backend.tables.entity.Table table =
      com.restaurant_management.restaurant_management_backend.tables.entity.Table.builder()
        .status(com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus.OCCUPIED)
        .build();

    Order order = Order.builder()
      .type(com.restaurant_management.restaurant_management_backend.shared.enums.OrderType.DINE_IN)
      .transactions(new LinkedHashSet<>())
      .build();

    assertThatThrownBy(() -> order.assignToTable(table))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void assignToTable_occupiesTableAndLinksIt() {
    com.restaurant_management.restaurant_management_backend.tables.entity.Table table =
      com.restaurant_management.restaurant_management_backend.tables.entity.Table.builder()
        .status(com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus.FREE)
        .build();

    Order order = Order.builder()
      .type(com.restaurant_management.restaurant_management_backend.shared.enums.OrderType.DINE_IN)
      .transactions(new LinkedHashSet<>())
      .build();

    order.assignToTable(table);

    assertThat(table.getStatus())
      .isEqualTo(com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus.OCCUPIED);
    assertThat(order.getTable()).isEqualTo(table);
  }

  @Test
  void isPartiallyPaid_returnsTrueWhenSomePaid() {
    Transaction t = Transaction.builder()
      .total(BigDecimal.valueOf(30))
      .status(TransactionStatus.COMPLETED)
      .transactionDate(LocalDateTime.now())
      .build();

    Set<Transaction> txns = new LinkedHashSet<>();
    txns.add(t);

    Order order = Order.builder()
      .status(OrderStatus.PARTIALLY_PAID)
      .total(BigDecimal.valueOf(100))
      .transactions(txns)
      .build();

    assertThat(order.isPartiallyPaid()).isTrue();
  }

  @Test
  void markAsPaid_fromReadyStatusWorks() {
    Order order = Order.builder()
      .status(OrderStatus.READY)
      .total(BigDecimal.valueOf(80))
      .transactions(new LinkedHashSet<>())
      .build();

    order.markAsPaid();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
  }

  @Test
  void markAsPaid_throwsWhenCancelled() {
    Order order = Order.builder()
      .status(OrderStatus.CANCELLED)
      .total(BigDecimal.valueOf(80))
      .transactions(new LinkedHashSet<>())
      .build();

    assertThatThrownBy(order::markAsPaid)
      .isInstanceOf(IllegalStateException.class);
  }
}
