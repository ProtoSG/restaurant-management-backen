package com.restaurant_management.restaurant_management_backend.auth;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurant_management.restaurant_management_backend.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

  @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
  Optional<User> findByUsername(@Param("username") String username);

  @Query("SELECT u FROM User u JOIN FETCH u.role ORDER BY u.id ASC")
  java.util.List<User> findAllWithRole();

  @Query(value = "SELECT u FROM User u JOIN FETCH u.role ORDER BY u.id ASC",
      countQuery = "SELECT COUNT(u) FROM User u")
  Page<User> findAllWithRole(Pageable pageable);

  @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id = :id")
  Optional<User> findByIdWithRole(@Param("id") Long id);

  Boolean existsByUsername(String username);

}
