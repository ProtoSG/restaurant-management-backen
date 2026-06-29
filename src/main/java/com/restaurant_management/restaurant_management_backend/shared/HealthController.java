package com.restaurant_management.restaurant_management_backend.shared;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Health check público: GET /api/health → { "status": "ok", "message": "Server running" }. */
@RestController
public class HealthController {

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of(
        "status", "ok",
        "message", "Server running"
    ));
  }
}
