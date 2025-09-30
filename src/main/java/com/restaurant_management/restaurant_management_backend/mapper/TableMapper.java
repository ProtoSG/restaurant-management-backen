package com.restaurant_management.restaurant_management_backend.mapper;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.dto.TableDTO;
import com.restaurant_management.restaurant_management_backend.entity.Table;

@Component
public class TableMapper {

  public Table toEntity(TableDTO tableDTO) {
    if (tableDTO == null) return null;

    return Table.builder()
      .id(tableDTO.getId())
      .number(tableDTO.getNumber())
      .status(tableDTO.getStatus())
      .build();
  }

  public TableDTO toDto(Table table) {
    if (table == null) return null;

    return TableDTO.builder()
      .id(table.getId())
      .number(table.getNumber())
      .status(table.getStatus())
      .build();
  }
}
