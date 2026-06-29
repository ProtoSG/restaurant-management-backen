package com.restaurant_management.restaurant_management_backend.entity;

import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.orders.entity.OrderItem;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemEntityTest {

  @Test
  void calculateSubTotal_withoutSurcharge() {
    Product product = Product.builder().price(BigDecimal.valueOf(10)).build();

    OrderItem item = OrderItem.builder()
      .product(product)
      .unitPrice(BigDecimal.valueOf(10))
      .quantity(3)
      .takeawaySurcharge(BigDecimal.ZERO)
      .build();

    item.calculateSubTotal();

    assertThat(item.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(30));
  }

  @Test
  void calculateSubTotal_addsSurchargePerUnit() {
    Product product = Product.builder().price(BigDecimal.valueOf(10)).build();

    OrderItem item = OrderItem.builder()
      .product(product)
      .unitPrice(BigDecimal.valueOf(10))
      .quantity(2)
      .takeawaySurcharge(BigDecimal.valueOf(1))
      .build();

    item.calculateSubTotal();

    // (10 + 1) * 2 = 22
    assertThat(item.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(22));
  }

  @Test
  void assignProduct_setsSubTotalFromProductPrice() {
    Product product = Product.builder().price(BigDecimal.valueOf(15)).build();

    OrderItem item = new OrderItem();
    item.setTakeawaySurcharge(BigDecimal.ZERO);

    item.assignProduct(product, 2);

    assertThat(item.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(30));
    assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(15));
    assertThat(item.getQuantity()).isEqualTo(2);
  }

  @Test
  void assignProductCustomPrice_usesOverridePrice() {
    Product product = Product.builder().price(BigDecimal.valueOf(15)).build();

    OrderItem item = new OrderItem();
    item.setTakeawaySurcharge(BigDecimal.ZERO);

    item.assignProductCustomPrice(product, BigDecimal.valueOf(20), 3);

    // 20 * 3 = 60, NOT 15 * 3
    assertThat(item.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(60));
    assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(20));
  }

  @Test
  void assignProductCustomPrice_fallsBackToProductPriceWhenNullOverride() {
    Product product = Product.builder().price(BigDecimal.valueOf(12)).build();

    OrderItem item = new OrderItem();
    item.setTakeawaySurcharge(BigDecimal.ZERO);

    item.assignProductCustomPrice(product, null, 4);

    assertThat(item.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(48));
    assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(12));
  }

  @Test
  void calculateSubTotal_whenSurchargeIsNull_treatsAsZero() {
    Product product = Product.builder().price(BigDecimal.valueOf(10)).build();

    OrderItem item = OrderItem.builder()
      .product(product)
      .unitPrice(BigDecimal.valueOf(10))
      .quantity(2)
      .takeawaySurcharge(null)
      .build();

    item.calculateSubTotal();

    assertThat(item.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(20));
  }
}
