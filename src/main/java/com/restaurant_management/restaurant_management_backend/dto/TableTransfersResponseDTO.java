package com.restaurant_management.restaurant_management_backend.dto;

import java.time.LocalDate;
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
public class TableTransfersResponseDTO {

  private LocalDate date;
  private Long totalTransfers;
  private List<TableTransferDTO> transfers;

}
