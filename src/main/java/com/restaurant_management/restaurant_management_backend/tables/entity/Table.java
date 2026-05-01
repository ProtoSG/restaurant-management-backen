package com.restaurant_management.restaurant_management_backend.tables.entity;

import com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@jakarta.persistence.Table(name = "tables")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class Table {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "number")
  private String number;

  @Enumerated(EnumType.STRING)
  @Column(name = "state")
  @Builder.Default
  private TableStatus status = TableStatus.FREE;

  public void occupy() {
    if (this.status == TableStatus.OCCUPIED) {
      throw new IllegalStateException("Mesa está ocupada");
    }
    this.status = TableStatus.OCCUPIED;
  }

  public void free() {
    if (this.status != TableStatus.OCCUPIED) {
        throw new IllegalStateException("Solo las mesas ocupadas pueden estar libre");
    }
    this.status = TableStatus.FREE;
  }

  public void reserve() {
    if (this.status != TableStatus.FREE) {
      throw new IllegalStateException("Solo las mesas libres pueden reservarse");
    }
    this.status = TableStatus.RESERVED;
  }

}
