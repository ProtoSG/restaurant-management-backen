package com.restaurant_management.restaurant_management_backend.orders.entity;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.shared.audit.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "order_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class OrderItem extends AuditableEntity {

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

  @Column(name = "unit_price", precision = 10, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "sub_total", precision = 10, scale = 2)
  @Builder.Default
  private BigDecimal subTotal = BigDecimal.ZERO;

  @Column(name = "notes", length = 255)
  private String notes;

  @Column(name = "is_takeaway")
  @Builder.Default
  private Boolean isTakeaway = false;

  @Column(name = "takeaway_surcharge", precision = 10, scale = 2)
  @Builder.Default
  private BigDecimal takeawaySurcharge = BigDecimal.ZERO;

  @Column(name = "kitchen_printed_quantity")
  @Builder.Default
  private Integer kitchenPrintedQuantity = 0;

  public void assignProduct(Product product, Integer quantity) {
    this.product = product;
    this.quantity = quantity;
    this.unitPrice = product.getPrice();
    BigDecimal surcharge = this.takeawaySurcharge != null ? this.takeawaySurcharge : BigDecimal.ZERO;
    this.subTotal = this.unitPrice
        .multiply(BigDecimal.valueOf(quantity))
        .add(surcharge.multiply(BigDecimal.valueOf(quantity)));
  }

  public void assignProductCustomPrice(Product product, BigDecimal price, Integer quantity) {
    this.product = product;
    this.quantity = quantity;
    this.unitPrice = (price != null) ? price : product.getPrice();
    BigDecimal surcharge = this.takeawaySurcharge != null ? this.takeawaySurcharge : BigDecimal.ZERO;
    this.subTotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity))
        .add(surcharge.multiply(BigDecimal.valueOf(quantity)));
  }

  public void calculateSubTotal() {
    if (this.product != null && this.quantity != null) {
      BigDecimal price = (this.unitPrice != null) ? this.unitPrice : product.getPrice();
      BigDecimal surcharge = this.takeawaySurcharge != null ? this.takeawaySurcharge : BigDecimal.ZERO;
      this.subTotal = price.multiply(BigDecimal.valueOf(quantity))
          .add(surcharge.multiply(BigDecimal.valueOf(quantity)));
    }
  }
}
