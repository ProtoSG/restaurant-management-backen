package com.restaurant_management.restaurant_management_backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

  @GetMapping
  public ResponseEntity<?> checkHealth() {
    Map<String, String> message = new HashMap<>();
    message.put("message", "Server active");
    message.put("status", "OK");
    
    return ResponseEntity.status(HttpStatus.OK).body(message);
  }
}
