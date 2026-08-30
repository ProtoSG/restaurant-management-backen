package com.restaurant_management.restaurant_management_backend.auth;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.auth.dto.request.CreateUserRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.SetPinRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.UpdateUserRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.response.UserResponse;
import com.restaurant_management.restaurant_management_backend.auth.entity.Role;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.shared.dto.response.PageResponse;
import com.restaurant_management.restaurant_management_backend.shared.enums.RoleName;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceConflictException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @PostMapping
  @Transactional
  public ResponseEntity<UserResponse> create(
    @RequestBody @Valid CreateUserRequest req
  ) {
    if (userRepository.existsByUsername(req.username())) {
      throw new ResourceConflictException("El nombre de usuario ya existe");
    }

    Role role = roleRepository.findByName(req.role())
      .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

    User user = User.builder()
      .name(req.name())
      .username(req.username())
      .password(passwordEncoder.encode(req.password()))
      .role(role)
      .build();

    User saved = userRepository.save(user);
    return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
      .body(userMapper.toResponse(saved));
  }

  @GetMapping
  public ResponseEntity<PageResponse<UserResponse>> getAll(
    @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
    @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, size);
    Page<User> users = userRepository.findAllWithRole(pageable);
    Page<UserResponse> responses = users.map(userMapper::toResponse);
    return ResponseEntity.ok(new PageResponse<>(
      responses.getContent(),
      responses.getNumber(),
      responses.getSize(),
      responses.getTotalElements(),
      responses.getTotalPages()
    ));
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
    User user = findUser(id);
    return ResponseEntity.ok(userMapper.toResponse(user));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<UserResponse> update(
    @PathVariable Long id,
    @RequestBody @Valid UpdateUserRequest req,
    Authentication authentication
  ) {
    User user = findUser(id);

    User caller = (User) authentication.getPrincipal();
    if (caller.getId().equals(id) && !req.role().name().equals(user.getRole().getName().name())) {
      throw new BadRequestException("No puedes cambiar tu propio rol");
    }

    Role role = roleRepository.findByName(req.role())
      .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

    user.setName(req.name());
    user.setRole(role);
    return ResponseEntity.ok(userMapper.toResponse(userRepository.save(user)));
  }

  @PatchMapping("/{id}/toggle")
  @Transactional
  public ResponseEntity<UserResponse> toggleActive(
    @PathVariable Long id,
    Authentication authentication
  ) {
    User caller = (User) authentication.getPrincipal();
    if (caller.getId().equals(id)) {
      throw new BadRequestException("No puedes desactivarte a ti mismo");
    }

    User user = findUser(id);
    user.setIsActive(!user.getIsActive());
    return ResponseEntity.ok(userMapper.toResponse(userRepository.save(user)));
  }

  // PIN eligibility is enforced here too, not just at login — an ADMIN who somehow tries to
  // set a PIN on an ADMIN/CHEF account gets rejected outright, not silently accepted and then
  // just never usable (that would look like a bug, not a boundary).
  @PatchMapping("/{id}/pin")
  @Transactional
  public ResponseEntity<UserResponse> setPin(
    @PathVariable Long id,
    @RequestBody @Valid SetPinRequest req
  ) {
    User user = findUser(id);
    requirePinEligibleRole(user);

    user.setPinHash(passwordEncoder.encode(req.pin()));
    user.setFailedPinAttempts(0);
    user.setPinLockedUntil(null);
    return ResponseEntity.ok(userMapper.toResponse(userRepository.save(user)));
  }

  @DeleteMapping("/{id}/pin")
  @Transactional
  public ResponseEntity<UserResponse> deletePin(@PathVariable Long id) {
    User user = findUser(id);
    user.setPinHash(null);
    user.setFailedPinAttempts(0);
    user.setPinLockedUntil(null);
    return ResponseEntity.ok(userMapper.toResponse(userRepository.save(user)));
  }

  private void requirePinEligibleRole(User user) {
    RoleName role = user.getRole().getName();
    if (role != RoleName.WAITER && role != RoleName.CASHIER) {
      throw new BadRequestException("Solo mesero o cajero pueden tener PIN de acceso");
    }
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<Void> delete(
    @PathVariable Long id,
    Authentication authentication
  ) {
    User caller = (User) authentication.getPrincipal();
    if (caller.getId().equals(id)) {
      throw new BadRequestException("No puedes eliminarte a ti mismo");
    }

    if (!userRepository.existsById(id)) {
      throw new ResourceNotFoundException("Usuario no encontrado");
    }

    userRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  private User findUser(Long id) {
    return userRepository.findByIdWithRole(id)
      .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
  }
}
