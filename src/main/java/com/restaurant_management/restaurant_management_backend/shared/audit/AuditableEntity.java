package com.restaurant_management.restaurant_management_backend.shared.audit;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class AuditableEntity {

  @CreatedDate
  @Column(name = "date_created", updatable = false)
  private LocalDateTime dateCreated;

  @LastModifiedDate
  @Column(name = "date_updated")
  private LocalDateTime dateUpdated;

  @CreatedBy
  @Column(name = "user_created", updatable = false, length = 100)
  private String userCreated;

  @LastModifiedBy
  @Column(name = "user_updated", length = 100)
  private String userUpdated;
}
