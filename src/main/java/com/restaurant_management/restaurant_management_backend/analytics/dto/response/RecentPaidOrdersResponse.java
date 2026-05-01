package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.time.LocalDate;
import java.util.List;

public record RecentPaidOrdersResponse(
  LocalDate date,
  Long totalTransactions,
  List<RecentPaidOrderResponse> orders
) {}
