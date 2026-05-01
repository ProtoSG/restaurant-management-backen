package com.restaurant_management.restaurant_management_backend.tables;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.tables.dto.request.CreateTableRequest;
import com.restaurant_management.restaurant_management_backend.tables.dto.response.TableResponse;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;

@Component
public class TableMapper {

  public Table toEntity(CreateTableRequest req) {
    if (req == null) return null;

    return Table.builder()
      .number(req.number())
      .build();
  }

  public TableResponse toResponse(Table table) {
    if (table == null) return null;

    return new TableResponse(
      table.getId(), 
      table.getNumber(),
      table.getStatus()
    );
  }

}
