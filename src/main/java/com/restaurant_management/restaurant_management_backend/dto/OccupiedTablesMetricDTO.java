package com.restaurant_management.restaurant_management_backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class OccupiedTablesMetricDTO {

  private Integer occupied;
  private Integer total;
  private Double percentageOccupancy;
  private List<Integer> sparklineData;

}
