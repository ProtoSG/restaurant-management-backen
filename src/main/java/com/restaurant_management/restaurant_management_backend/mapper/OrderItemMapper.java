package com.restaurant_management.restaurant_management_backend.mapper;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.dto.CategoryDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.ProductDTO;
import com.restaurant_management.restaurant_management_backend.entity.OrderItem;

@Component
public class OrderItemMapper {

  public OrderItem toEntity(OrderItemDTO orderItemDTO) {
    if (orderItemDTO == null) return null;

    return OrderItem.builder()
      .id(orderItemDTO.getId())
      .quantity(orderItemDTO.getQuantity())
      .subTotal(orderItemDTO.getSubTotal())
      .build();
  }

  public OrderItemDTO toDto(OrderItem orderItem) {
    if (orderItem == null) return null;

    ProductDTO productDTO = ProductDTO.builder()
      .id(orderItem.getProduct().getId())
      .name(orderItem.getProduct().getName())
      .price(orderItem.getProduct().getPrice())
      .categoryId(orderItem.getProduct().getCategory().getId())
      .category(CategoryDTO.builder()
          .id(orderItem.getProduct().getCategory().getId())
          .name(orderItem.getProduct().getCategory().getName())
          .build())
      .build();

    return OrderItemDTO.builder()
      .id(orderItem.getId())
      .productId(orderItem.getProduct().getId())
      .quantity(orderItem.getQuantity())
      .subTotal(orderItem.getSubTotal())
      .product(productDTO)
      .build();
  }

  public List<OrderItemDTO> toDtoList(Collection<OrderItem> orderItems) {
    return orderItems.stream()
      .map(this::toDto)
      .toList();
  }
}
