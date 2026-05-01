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
}
