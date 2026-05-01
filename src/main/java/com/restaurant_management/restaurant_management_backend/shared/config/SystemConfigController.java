package com.restaurant_management.restaurant_management_backend.shared.config;

import java.math.BigDecimal;

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

  private static final String TAKEAWAY_SURCHARGE_KEY = "takeaway_surcharge";

  private final SystemConfigRepository systemConfigRepository;

  @GetMapping("/takeaway-surcharge")
  public ResponseEntity<BigDecimal> getTakeawaySurcharge() {
    BigDecimal value = systemConfigRepository.findById(TAKEAWAY_SURCHARGE_KEY)
        .map(c -> new BigDecimal(c.getValue()))
        .orElse(BigDecimal.ONE);
    return ResponseEntity.ok(value);
  }

  @PutMapping("/takeaway-surcharge")
  public ResponseEntity<BigDecimal> updateTakeawaySurcharge(@RequestBody BigDecimal amount) {
    SystemConfig config = systemConfigRepository.findById(TAKEAWAY_SURCHARGE_KEY)
        .orElse(new SystemConfig(TAKEAWAY_SURCHARGE_KEY, "1.00"));
    config.setValue(amount.toPlainString());
    systemConfigRepository.save(config);
    return ResponseEntity.ok(amount);
  }
}
