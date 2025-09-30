package com.restaurant_management.restaurant_management_backend.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.dto.TableDTO;
import com.restaurant_management.restaurant_management_backend.entity.Table;
import com.restaurant_management.restaurant_management_backend.enums.TableStatus;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.mapper.TableMapper;
import com.restaurant_management.restaurant_management_backend.repository.TableRepository;
import com.restaurant_management.restaurant_management_backend.service.TableService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TableServiceImpl implements TableService {

  private final TableRepository tableRepository;
  private final TableMapper tableMapper;

  public TableDTO save(TableDTO tableDTO) {
    Table newTable = tableMapper.toEntity(tableDTO);

    return tableMapper.toDto(tableRepository.save(newTable));
  }

  public List<TableDTO> findAll() {
    List<Table> tables = tableRepository.findAll();

    return tables.stream()
      .map(tableMapper::toDto)
      .toList();
  }

  public TableDTO findById(Long id) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    return tableMapper.toDto(table);
  }

  public TableDTO update(Long id, TableDTO tableDTO) {
    Table table = tableRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    table.setNumber(tableDTO.getNumber());
    table.setStatus(tableDTO.getStatus());

    return tableMapper.toDto(tableRepository.save(table));
  }

  public void deleteById(Long id) {
    if (!tableRepository.existsById(id)) {
      throw new ResourceNotFoundException("Mesa no encontrada");
    }

    tableRepository.deleteById(id);
  }
}
