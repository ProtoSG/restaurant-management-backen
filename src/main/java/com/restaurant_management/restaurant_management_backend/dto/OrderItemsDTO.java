package com.restaurant_management.restaurant_management_backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class OrderItemsDTO {

  @NotEmpty(message = "La lista no puede estar vacia")
  @Valid
  private List<OrderItemDTO> items;
  
}
