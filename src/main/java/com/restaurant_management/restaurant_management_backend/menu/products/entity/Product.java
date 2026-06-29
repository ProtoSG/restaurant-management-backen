package com.restaurant_management.restaurant_management_backend.menu.products.entity;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.menu.categories.entity.Category;
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
@Table(name = "products")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Product extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "price", precision = 10, scale = 2)
  @Builder.Default
  private BigDecimal price = BigDecimal.ZERO;

  @Column(name = "cost", precision = 10, scale = 2)
  private BigDecimal cost;

  @Column(name = "is_available", nullable = false)
  @Builder.Default
  private Boolean isAvailable = true;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
  private Category category;
}
