package com.restaurant_management.restaurant_management_backend.dto;

import com.restaurant_management.restaurant_management_backend.enums.RoleName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDTO {

  private Long id;
  private String name;
  private String username;
  private RoleName role;

}
