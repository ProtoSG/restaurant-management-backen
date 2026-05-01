package com.restaurant_management.restaurant_management_backend.tables;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.tables.dto.request.CreateTableRequest;
import com.restaurant_management.restaurant_management_backend.tables.dto.request.UpdateTableRequest;
import com.restaurant_management.restaurant_management_backend.tables.dto.response.TableResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tables")
@RequiredArgsConstructor
public class TableController {

  private final TableService tableService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<TableResponse> create(
    @RequestBody @Valid CreateTableRequest req
  ) {
    TableResponse savedTable = tableService.save(req);

    return ResponseEntity.status(HttpStatus.CREATED)
      .body(savedTable);
  }

  @GetMapping
  public ResponseEntity<List<TableResponse>> getAll() {
    List<TableResponse> tables = tableService.findAll();

    return ResponseEntity.status(HttpStatus.OK)
      .body(tables);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TableResponse> getById(
    @PathVariable Long id
  ) {
    TableResponse table = tableService.findById(id);

    return ResponseEntity.status(HttpStatus.OK)
      .body(table);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public ResponseEntity<TableResponse> update(
    @PathVariable Long id,
    @RequestBody @Valid UpdateTableRequest req
  ) {
    TableResponse updatedTable = tableService.update(id, req);

    return ResponseEntity.status(HttpStatus.OK)
      .body(updatedTable);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable Long id
  ) {
    tableService.deleteById(id);

    return ResponseEntity.noContent().build();
  }

}
