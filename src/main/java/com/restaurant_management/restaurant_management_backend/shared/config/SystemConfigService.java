package com.restaurant_management.restaurant_management_backend.shared.config;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

  private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

  private static final String TAKEAWAY_SURCHARGE_KEY = "takeaway_surcharge";
  private static final String QUICK_ADD_KEY = "quick_add_product_ids";

  private final SystemConfigRepository systemConfigRepository;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public BigDecimal getTakeawaySurcharge() {
    return systemConfigRepository.findById(TAKEAWAY_SURCHARGE_KEY)
        .map(c -> new BigDecimal(c.getValue()))
        .orElse(BigDecimal.ONE);
  }

  @Transactional
  public BigDecimal updateTakeawaySurcharge(BigDecimal amount) {
    SystemConfig config = systemConfigRepository.findById(TAKEAWAY_SURCHARGE_KEY)
        .orElse(new SystemConfig(TAKEAWAY_SURCHARGE_KEY, "1.00"));
    config.setValue(amount.toPlainString());
    systemConfigRepository.save(config);
    return amount;
  }

  @Transactional(readOnly = true)
  public List<Long> getQuickAddProducts() {
    return systemConfigRepository.findById(QUICK_ADD_KEY)
        .map(c -> {
          try {
            return objectMapper.readValue(c.getValue(), new TypeReference<List<Long>>() {});
          } catch (JsonProcessingException e) {
            log.warn("Valor inválido en config '{}': {}. Devolviendo lista vacía.", QUICK_ADD_KEY, e.getMessage());
            return Collections.<Long>emptyList();
          }
        })
        .orElseGet(Collections::emptyList);
  }

  @Transactional
  public List<Long> updateQuickAddProducts(List<Long> ids) {
    SystemConfig config = systemConfigRepository.findById(QUICK_ADD_KEY)
        .orElse(new SystemConfig(QUICK_ADD_KEY, "[]"));
    try {
      config.setValue(objectMapper.writeValueAsString(ids));
    } catch (JsonProcessingException e) {
      log.error("No se pudo serializar quick-add product ids: {}", e.getMessage());
      throw new IllegalArgumentException("Lista de productos inválida", e);
    }
    systemConfigRepository.save(config);
    return ids;
  }
}
