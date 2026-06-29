package com.restaurant_management.restaurant_management_backend.tables;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.restaurant_management.restaurant_management_backend.tables.entity.Table;

public interface TableRepository extends JpaRepository<Table, Long> {

  // Numeric codes (e.g. "1","10") sort as integers; letter codes (e.g. "B","BAR") sort after, alphabetically.
  @Query(value = "SELECT * FROM tables ORDER BY CASE WHEN number ~ '^\\d+$' THEN CAST(number AS INTEGER) ELSE NULL END NULLS LAST, number ASC", nativeQuery = true)
  List<Table> findAllOrderedByNumberNumeric();

  @Query("SELECT COUNT(t) FROM Table t WHERE t.status = 'OCCUPIED'")
  Long countOccupiedTables();

}
