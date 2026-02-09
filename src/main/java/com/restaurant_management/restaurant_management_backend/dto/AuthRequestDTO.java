package com.restaurant_management.restaurant_management_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequestDTO {

  @NotBlank(message = "El username es obligatorio")
  private String username;

  @NotNull(message = "La contraseña es obligatoria")
  private String password;

}
