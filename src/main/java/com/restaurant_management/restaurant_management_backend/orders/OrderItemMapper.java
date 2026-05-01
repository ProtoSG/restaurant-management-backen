package com.restaurant_management.restaurant_management_backend.orders;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.menu.categories.dto.response.CategoryResponse;
import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderItemResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.OrderItem;

@Component
public class OrderItemMapper {

  public OrderItem toEntity(AddOrderItemRequest req) {
    if (req == null) return null;

    return OrderItem.builder()
      .quantity(req.quantity())
      .build();
  }

  public OrderItemResponse toResponse(OrderItem orderItem) {
    if (orderItem == null) return null;

    CategoryResponse category = new CategoryResponse(
      orderItem.getProduct().getCategory().getId(),
      orderItem.getProduct().getCategory().getName()
    );

    ProductResponse productResponse = new ProductResponse(
      orderItem.getProduct().getId(),
      orderItem.getProduct().getName(),
      orderItem.getProduct().getPrice(),
      category
    );

    return new OrderItemResponse(
      orderItem.getId(),
      orderItem.getQuantity(),
      orderItem.getSubTotal(),
      productResponse,
      orderItem.getNotes(),
      orderItem.getIsTakeaway(),
      orderItem.getTakeawaySurcharge()
    );
  }

  public List<OrderItemResponse> toResponseList(Collection<OrderItem> orderItems) {
    return orderItems.stream()
      .map(this::toResponse)
      .toList();
  }
}
