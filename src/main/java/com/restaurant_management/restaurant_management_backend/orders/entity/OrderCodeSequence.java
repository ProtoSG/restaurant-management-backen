package com.restaurant_management.restaurant_management_backend.orders.entity;

import com.restaurant_management.restaurant_management_backend.shared.audit.AuditableEntity;

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
public class OrderCodeSequence extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "current_value", nullable = false)
  private Long currentValue;

  public void increment() {
    this.currentValue++;
  }
}
