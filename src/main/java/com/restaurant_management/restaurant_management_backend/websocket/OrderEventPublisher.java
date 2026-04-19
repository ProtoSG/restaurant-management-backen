package com.restaurant_management.restaurant_management_backend.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public void publish(OrderEvent.Type type, Long orderId) {
    messagingTemplate.convertAndSend("/topic/orders", new OrderEvent(type, orderId));
  }
}
