package com.restaurant_management.restaurant_management_backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant_management.restaurant_management_backend.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);
  Boolean existsByUsername(String username);

}
