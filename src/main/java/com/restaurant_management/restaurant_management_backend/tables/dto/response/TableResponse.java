package com.restaurant_management.restaurant_management_backend.tables.dto.response;

import com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus;

public record TableResponse(

  Long id,
  String number,
  TableStatus status

) {}
