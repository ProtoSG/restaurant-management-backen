package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;
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
public class CategoryProductsDTO {

  private Long categoryId;
  private String categoryName;
  private BigDecimal totalSales;
  private Long totalQuantity;
  private List<ProductRankingDTO> products;

}
