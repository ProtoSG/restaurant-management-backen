package com.restaurant_management.restaurant_management_backend.dto;

import com.restaurant_management.restaurant_management_backend.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateDTO {

  @NotBlank(message = "El nombre el obligatoria")
  private String name;
  
  @Email(message = "Debe ser un email válido")
  private String email;

  @NotBlank(message = "La contraseña es obligatoria")
  private String password;

  @NotNull(message = "El rol es obligatorio")
  private Role role;
}
