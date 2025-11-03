package com.restaurant_management.restaurant_management_backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", referencedColumnName = "id", nullable = false)
  private Order order;

  @Column(name = "quantity")
  @Builder.Default
  private Integer quantity = 0;

  @Column(name = "sub_total")
  @Builder.Default
  private BigDecimal subTotal = BigDecimal.ZERO;

  public void assignProduct(Product product, Integer quantity) {
    this.product = product;
    this.quantity = quantity;
    this.subTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
  }

  public void assignProductCustomPrice(Product product, BigDecimal price, Integer quantity) {
    this.product = product;
    this.quantity = quantity;
    BigDecimal effectivePrice = (price != null) ? price : product.getPrice();
    this.subTotal = effectivePrice.multiply(BigDecimal.valueOf(quantity));
  }

  public void calculateSubTotal() {
    if (this.product != null && this.quantity != null) {
      this.subTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
  }
}
