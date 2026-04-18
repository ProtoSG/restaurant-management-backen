package com.restaurant_management.restaurant_management_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class WeekComparisonDTO {

  private Double salesChange;
  private Double ordersChange;

}
