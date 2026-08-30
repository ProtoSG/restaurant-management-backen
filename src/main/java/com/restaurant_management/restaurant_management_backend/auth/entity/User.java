package com.restaurant_management.restaurant_management_backend.auth.entity;

import java.time.LocalDateTime;

import com.restaurant_management.restaurant_management_backend.shared.audit.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class User extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "username", unique = true, nullable = false)
  private String username;

  @Column(name = "password", nullable = false)
  private String password;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false)
  private Role role;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  // PIN login (see V22 migration): opt-in alternative to password, for WAITER/CASHIER on a
  // shared tablet. null pinHash means this user has no PIN configured — PATCH /users/{id}/pin
  // (ADMIN only) is the only way to set one. failedPinAttempts/pinLockedUntil enforce a 5-try
  // lockout, same rationale as RateLimitFilter's IP-based throttle but per-account: a 4-digit
  // PIN has only 10,000 combinations, too few to leave unthrottled even behind a role check.
  @Column(name = "pin_hash")
  private String pinHash;

  @Column(name = "failed_pin_attempts", nullable = false)
  @Builder.Default
  private Integer failedPinAttempts = 0;

  @Column(name = "pin_locked_until")
  private LocalDateTime pinLockedUntil;
}
