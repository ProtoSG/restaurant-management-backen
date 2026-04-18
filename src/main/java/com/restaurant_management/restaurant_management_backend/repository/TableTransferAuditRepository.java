package com.restaurant_management.restaurant_management_backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurant_management.restaurant_management_backend.entity.TableTransferAudit;

public interface TableTransferAuditRepository extends JpaRepository<TableTransferAudit, Long> {

  @Query("SELECT t FROM TableTransferAudit t WHERE " +
         "t.transferDate >= :startDate AND " +
         "t.transferDate < :endDate " +
         "ORDER BY t.transferDate DESC")
  List<TableTransferAudit> findByTransferDateBetween(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
  );

  @Query("SELECT COUNT(t) FROM TableTransferAudit t WHERE " +
         "t.transferDate >= :startDate AND " +
         "t.transferDate < :endDate")
  Long countTransfersByDate(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
  );

}
