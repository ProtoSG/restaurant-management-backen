package com.restaurant_management.restaurant_management_backend.shared.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_config")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class SystemConfig {

  @Id
  @Column(name = "config_key", length = 100)
  private String key;

  @Column(name = "config_value", nullable = false, length = 255)
  private String value;
}
