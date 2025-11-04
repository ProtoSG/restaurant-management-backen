package com.restaurant_management.restaurant_management_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.exceptions.UnauthorizedException;
import com.restaurant_management.restaurant_management_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

  private final UserRepository userRepository;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
  
  @Bean
  public UserDetailsService userDetailsService() {
    return username -> {
      final User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

      return org.springframework.security.core.userdetails.User.builder()
        .username(user.getEmail())
        .password(user.getPassword())
        .roles(user.getRole().name())
        .build();
    };
  }

  @Bean
  public AuthenticationProvider authenticationProvider(
      UserDetailsService uds, PasswordEncoder encoder
  ) {
    var provider = new DaoAuthenticationProvider(uds);
    provider.setPasswordEncoder(encoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}
