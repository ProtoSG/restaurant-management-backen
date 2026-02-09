package com.restaurant_management.restaurant_management_backend.mapper;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.dto.UserResponseDTO;
import com.restaurant_management.restaurant_management_backend.entity.User;

@Component
public class UserMapper {

  public UserResponseDTO toDto(User user) {
    if (user == null) return null;

    return UserResponseDTO.builder()
      .id(user.getId())
      .name(user.getName())
      .username(user.getUsername())
      .role(user.getRole().getName())
      .build();
  }

}
