package com.restaurant_management.restaurant_management_backend.shared.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class SystemConfigController {

  private final SystemConfigService systemConfigService;

  @GetMapping("/takeaway-surcharge")
  public ResponseEntity<BigDecimal> getTakeawaySurcharge() {
    return ResponseEntity.ok(systemConfigService.getTakeawaySurcharge());
  }

  @PutMapping("/takeaway-surcharge")
  public ResponseEntity<BigDecimal> updateTakeawaySurcharge(@RequestBody BigDecimal amount) {
    return ResponseEntity.ok(systemConfigService.updateTakeawaySurcharge(amount));
  }

  @GetMapping("/quick-add-products")
  public ResponseEntity<List<Long>> getQuickAddProducts() {
    return ResponseEntity.ok(systemConfigService.getQuickAddProducts());
  }

  @PutMapping("/quick-add-products")
  public ResponseEntity<List<Long>> updateQuickAddProducts(@RequestBody List<Long> ids) {
    return ResponseEntity.ok(systemConfigService.updateQuickAddProducts(ids));
  }
}
