package com.restaurant_management.restaurant_management_backend.entity;

import com.restaurant_management.restaurant_management_backend.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.enums.TableStatus;
import com.restaurant_management.restaurant_management_backend.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@jakarta.persistence.Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "order_code", unique = true, nullable = false)
  private String orderCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "table_id", referencedColumnName = "id", nullable = true)
  private Table table;

  @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, orphanRemoval = true)
  @OrderBy("id ASC")
  @Builder.Default
  private Set<OrderItem> items = new LinkedHashSet<>();

  @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, orphanRemoval = true)
  @Builder.Default
  private Set<Transaction> transactions = new LinkedHashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  @Builder.Default
  private OrderType type = OrderType.DINE_IN;

  @Column(name = "customer_name")
  private String customerName;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private OrderStatus status = OrderStatus.CREATED;

  @Column(name = "total")
  @Builder.Default
  private BigDecimal total = BigDecimal.ZERO;

  @Column(name = "created_at")
  @CreationTimestamp
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private LocalDateTime updatedAt;

  public void assignToTable(Table table) {
    if (this.type != OrderType.DINE_IN) {
      throw new IllegalStateException("Sólo las órdenes de 'DINE_IN' se pueden asignar a una mesa");
    }
    if (table.getStatus() == TableStatus.OCCUPIED) {
      throw new IllegalStateException("La mesa esta ocupada");
    }
    if (table.getStatus() == TableStatus.RESERVED) {
      throw new IllegalStateException("La mesa esta reservada");
    }

    table.occupy();
    this.table = table;
  }

  public void addItem(OrderItem item) {
    this.items.add(item);
    item.setOrder(this);
    recalculateTotal();
  }

  public void removeItem(OrderItem item) {
    this.items.remove(item);
    recalculateTotal();
  }

  private void recalculateTotal() {
    this.total = items.stream()
      .map(OrderItem::getSubTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public void calculateTotal() {
    recalculateTotal();
  }

  public void markAsPaid() {
    if (this.status != OrderStatus.CREATED && this.status != OrderStatus.READY
        && this.status != OrderStatus.PARTIALLY_PAID) {
      throw new IllegalStateException("Order no puede ser pagado en este estado: " + this.status);
    }
    this.status = OrderStatus.PAID;
  }

  public void markAsReady() {
    if (this.status != OrderStatus.IN_PROGRESS) {
      throw new IllegalStateException("Solo órdenes IN_PROGRESS pueden marcarse como listas");
    }
    this.status = OrderStatus.READY;
  }

  public void markAsPending() {
    if (this.status != OrderStatus.CREATED && this.status != OrderStatus.IN_PROGRESS) {
      throw new IllegalStateException("Solo órdenes CREATED o IN_PROGRESS pueden marcarse como pendientes");
    }
    this.status = OrderStatus.PENDING;
  }

  // Métodos para pagos parciales
  public BigDecimal getPaidAmount() {
    return transactions.stream()
        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
        .map(Transaction::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public BigDecimal getRemainingAmount() {
    return this.total.subtract(getPaidAmount());
  }

  public boolean isFullyPaid() {
    return getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0;
  }

  public boolean isPartiallyPaid() {
    BigDecimal paid = getPaidAmount();
    return paid.compareTo(BigDecimal.ZERO) > 0 && 
           paid.compareTo(this.total) < 0;
  }

}
