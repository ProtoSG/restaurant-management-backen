package com.restaurant_management.restaurant_management_backend.dto;

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
public class CategoryDTO {

  private Long id;

  @NotBlank(message = "El nombre es obligatorio")
  private String name;
}
