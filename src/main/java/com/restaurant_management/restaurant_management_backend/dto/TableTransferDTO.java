package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class TableTransferDTO {

  private Long id;
  private Long orderId;
  private String orderCode;
  private TransferTableInfoDTO fromTable;
  private TransferTableInfoDTO toTable;
  private LocalDateTime transferDate;
  private BigDecimal orderAmount;
  private TransferUserInfoDTO user;

}
