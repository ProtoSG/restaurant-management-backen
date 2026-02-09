package com.restaurant_management.restaurant_management_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.restaurant_management.restaurant_management_backend.entity.Table;

public interface TableRepository extends JpaRepository<Table, Long> {

  @Query("SELECT t FROM Table t ORDER BY CAST(t.number AS int) ASC")
  List<Table> findAllOrderedByNumberNumeric();

}
