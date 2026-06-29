package com.restaurant_management.restaurant_management_backend.shared.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.auth.RoleRepository;
import com.restaurant_management.restaurant_management_backend.auth.UserRepository;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.shared.enums.RoleName;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${admin.default.username}")
  private String adminUsername;

  @Value("${admin.default.password}")
  private String adminPassword;

  @Override
  @Transactional
  public void run(String... args) {
    if (!userRepository.existsByUsername(adminUsername)) {
      var adminRole = roleRepository.findByName(RoleName.ADMIN)
          .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado — verificar migrations V14"));
      userRepository.save(User.builder()
          .name("Administrador")
          .username(adminUsername)
          .password(passwordEncoder.encode(adminPassword))
          .role(adminRole)
          .isActive(true)
          .build());
      log.info("Usuario admin '{}' creado desde variables de entorno", adminUsername);
    }
  }
}
