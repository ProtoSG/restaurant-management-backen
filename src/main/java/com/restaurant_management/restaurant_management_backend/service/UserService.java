package com.restaurant_management.restaurant_management_backend.service;

import com.restaurant_management.restaurant_management_backend.dto.UserResponseDTO;
import com.restaurant_management.restaurant_management_backend.entity.User;

public interface UserService {
  public UserResponseDTO findByUser(User user);
}
