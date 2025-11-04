package com.restaurant_management.restaurant_management_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequestDTO {

  @Email(message = "Debe ser un email válido")
  private String email;

  @NotNull(message = "La contraseña es obligatoria")
  private String password;

}
