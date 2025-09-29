package com.restaurant_management.restaurant_management_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.dto.TableDTO;
import com.restaurant_management.restaurant_management_backend.service.TableService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tables")
@RequiredArgsConstructor
public class TableController {

  private final TableService tableService;

  @PostMapping
  public ResponseEntity<?> create(
    @RequestBody @Valid TableDTO tableDTO
  ) {
    TableDTO savedTable = tableService.save(tableDTO);

    return ResponseEntity.status(HttpStatus.CREATED)
      .body(savedTable);
  }

  @GetMapping
  public ResponseEntity<?> getAll() {
    List<TableDTO> tables = tableService.findAll();

    return ResponseEntity.status(HttpStatus.OK)
      .body(tables);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(
    @PathVariable Long id
  ) {
    TableDTO table = tableService.findById(id);

    return ResponseEntity.status(HttpStatus.OK)
      .body(table);
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(
    @PathVariable Long id,
    @RequestBody @Valid TableDTO tableDTO
  ) {
    TableDTO updatedTable = tableService.update(id, tableDTO);

    return ResponseEntity.status(HttpStatus.OK)
      .body(updatedTable);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable Long id
  ) {
    tableService.deleteById(id);

    return ResponseEntity.noContent().build();
  }
}
