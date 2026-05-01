package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TableTransferResponse(
  Long id,
  Long orderId,
  String orderCode,
  TransferTableInfoResponse fromTable,
  TransferTableInfoResponse toTable,
  LocalDateTime transferDate,
  BigDecimal orderAmount,
  TransferUserInfoResponse user
) {}
