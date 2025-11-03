package com.restaurant_management.restaurant_management_backend.service;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.dto.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderWithOrderItemsDTO;
import com.restaurant_management.restaurant_management_backend.dto.TableDTO;
import com.restaurant_management.restaurant_management_backend.dto.UpdateOrderItemRequest;

public interface TableService {

  public TableDTO save(TableDTO tableDTO);
  public List<TableDTO> findAll();
  public TableDTO findById(Long id);
  public TableDTO update(Long id, TableDTO tableDTO);
  public void deleteById(Long id);
  public OrderWithOrderItemsDTO saveOrder(Long id);
  public OrderWithOrderItemsDTO getActiveOrder(Long id);
  public void addProducts(Long id, OrderItemsDTO items);
  public OrderItemDTO addOrderItem(Long tableId, AddOrderItemRequest request);
  public OrderItemDTO updateOrderItem(Long tableId, Long itemId, UpdateOrderItemRequest request);
  public void removeOrderItem(Long tableId, Long itemId);
}
