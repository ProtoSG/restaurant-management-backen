package com.restaurant_management.restaurant_management_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_code_sequence")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class OrderCodeSequence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "current_value", nullable = false)
  private Long currentValue;

  public String generateNextCode() {
    this.currentValue++;
    return String.format("PED-%04d", this.currentValue);
  }
}
