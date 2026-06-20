package com.restaurant_management.restaurant_management_backend.shared.config;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class SystemConfigController {

  private static final String TAKEAWAY_SURCHARGE_KEY = "takeaway_surcharge";
  private static final String QUICK_ADD_KEY = "quick_add_product_ids";

  private final SystemConfigRepository systemConfigRepository;
  private final ObjectMapper objectMapper;

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

  @GetMapping("/quick-add-products")
  public ResponseEntity<List<Long>> getQuickAddProducts() {
    return systemConfigRepository.findById(QUICK_ADD_KEY)
        .map(c -> {
          try {
            return ResponseEntity.ok(objectMapper.readValue(c.getValue(), new TypeReference<List<Long>>() {}));
          } catch (Exception e) {
            return ResponseEntity.ok(Collections.<Long>emptyList());
          }
        })
        .orElse(ResponseEntity.ok(Collections.emptyList()));
  }

  @PutMapping("/quick-add-products")
  public ResponseEntity<List<Long>> updateQuickAddProducts(@RequestBody List<Long> ids) {
    try {
      SystemConfig config = systemConfigRepository.findById(QUICK_ADD_KEY)
          .orElse(new SystemConfig(QUICK_ADD_KEY, "[]"));
      config.setValue(objectMapper.writeValueAsString(ids));
      systemConfigRepository.save(config);
      return ResponseEntity.ok(ids);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
