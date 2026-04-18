package com.restaurant_management.restaurant_management_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@jakarta.persistence.Table(name = "table_transfer_audit")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class TableTransferAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", referencedColumnName = "id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "from_table_id", referencedColumnName = "id", nullable = false)
  private Table fromTable;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "to_table_id", referencedColumnName = "id", nullable = false)
  private Table toTable;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
  private User user;

  @Column(name = "transfer_date", nullable = false)
  @CreationTimestamp
  private LocalDateTime transferDate;

  @Column(name = "order_total", nullable = false)
  @Builder.Default
  private BigDecimal orderTotal = BigDecimal.ZERO;

}
