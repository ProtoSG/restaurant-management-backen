package com.restaurant_management.restaurant_management_backend.dto;

import com.restaurant_management.restaurant_management_backend.enums.TableStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class TableDTO {

  private Long id;

  @NotBlank(message = "El numero de la mesa es obligatorio")
  private String number;

  private TableStatus status;

}
