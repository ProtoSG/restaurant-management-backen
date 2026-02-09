package com.restaurant_management.restaurant_management_backend.service.impl;

import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.dto.UserResponseDTO;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.mapper.UserMapper;
import com.restaurant_management.restaurant_management_backend.repository.UserRepository;
import com.restaurant_management.restaurant_management_backend.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  public UserResponseDTO findByUser(User user) {
    System.out.println("====> ID:" + user.getId());
    User userFind = userRepository.findById(user.getId())
      .orElseThrow( () -> new ResourceNotFoundException("User not found"));

    return userMapper.toDto(userFind); 
  }

}
