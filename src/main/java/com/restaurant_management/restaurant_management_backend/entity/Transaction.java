package com.restaurant_management.restaurant_management_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.restaurant_management.restaurant_management_backend.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.enums.TransactionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Transactions")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "total", nullable = false, precision = 10, scale = 2)
  private BigDecimal total;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  private PaymentMethodType paymentMethod;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private TransactionStatus status = TransactionStatus.PENDING;

  @Column(name = "transaction_date", nullable = false)
  private LocalDateTime transactionDate;

}
