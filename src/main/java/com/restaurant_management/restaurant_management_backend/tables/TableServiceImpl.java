package com.restaurant_management.restaurant_management_backend.tables;

import java.util.List;

import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.tables.dto.request.CreateTableRequest;
import com.restaurant_management.restaurant_management_backend.tables.dto.request.UpdateTableRequest;
import com.restaurant_management.restaurant_management_backend.tables.dto.response.TableResponse;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TableServiceImpl implements TableService {

  private final TableRepository tableRepository;
  private final TableMapper tableMapper;

  @Override
  public TableResponse save(CreateTableRequest req) {
    Table table = tableMapper.toEntity(req);

    return tableMapper.toResponse(tableRepository.save(table));
  }

  @Override
  public List<TableResponse> findAll() {
    List<Table> tables = tableRepository.findAllOrderedByNumberNumeric();

    return tables.stream()
      .map(tableMapper::toResponse)
      .toList();
  }

  @Override
  public TableResponse findById(Long id) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    return tableMapper.toResponse(table);
  }

  @Override
  public TableResponse update(Long id, UpdateTableRequest req) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    table.setNumber(req.number());

    return tableMapper.toResponse(tableRepository.save(table));
  }

  @Override
  public void deleteById(Long id) {
    if (!tableRepository.existsById(id)) {
      throw new ResourceNotFoundException("Mesa no encontrada");
    }

    tableRepository.deleteById(id);
  }

}
