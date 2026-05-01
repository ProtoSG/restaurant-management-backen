package com.restaurant_management.restaurant_management_backend.tables;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.tables.dto.request.CreateTableRequest;
import com.restaurant_management.restaurant_management_backend.tables.dto.request.UpdateTableRequest;
import com.restaurant_management.restaurant_management_backend.tables.dto.response.TableResponse;

public interface TableService {

  public TableResponse save(CreateTableRequest req);
  public List<TableResponse> findAll();
  public TableResponse findById(Long id);
  public TableResponse update(Long id, UpdateTableRequest req);
  public void deleteById(Long id);

}
