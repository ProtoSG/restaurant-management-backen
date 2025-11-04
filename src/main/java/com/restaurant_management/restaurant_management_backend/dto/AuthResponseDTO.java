package com.restaurant_management.restaurant_management_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDTO {

  private String email;
  private String token;

  @JsonIgnore
  private String refreshToken;

}
