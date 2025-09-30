package com.restaurant_management.restaurant_management_backend.service;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.dto.TableDTO;

public interface TableService {

  public TableDTO save(TableDTO tableDTO);
  public List<TableDTO> findAll();
  public TableDTO findById(Long id);
  public TableDTO update(Long id, TableDTO tableDTO);
  public void deleteById(Long id);
  
}
