package com.restaurant_management.restaurant_management_backend.shared.dto.response;

import java.util.List;

public record PageResponse<T>(
  List<T> content,
  int page,
  int size,
  long totalElements,
  int totalPages
) {}
