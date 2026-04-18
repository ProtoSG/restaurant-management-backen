package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class TopSellingProductDTO {

  private Long productId;
  private String productName;
  private String categoryName;
  private Long quantitySold;
  private BigDecimal totalRevenue;

}
