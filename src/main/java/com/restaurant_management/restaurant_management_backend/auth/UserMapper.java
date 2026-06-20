package com.restaurant_management.restaurant_management_backend.auth;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.auth.dto.response.UserResponse;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;

@Component
public class UserMapper {

  public UserResponse toResponse(User user) {
    return new UserResponse(
      user.getId(),
      user.getName(),
      user.getUsername(),
      user.getRole().getName().name(),
      user.getIsActive()
    );
  }
}
