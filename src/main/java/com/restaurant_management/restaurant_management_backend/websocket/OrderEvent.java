package com.restaurant_management.restaurant_management_backend.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderEvent {

  public enum Type {
    CREATED, UPDATED, CANCELLED, READY, PAID, ITEM_ADDED, ITEM_UPDATED, ITEM_REMOVED, TABLE_CHANGED
  }

  private final Type type;
  private final Long orderId;
}
