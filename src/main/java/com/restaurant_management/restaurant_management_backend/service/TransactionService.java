package com.restaurant_management.restaurant_management_backend.service;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.dto.TransactionDTO;
import com.restaurant_management.restaurant_management_backend.dto.TransactionMountGroupByPaymentMethodDTO;

public interface TransactionService {

  public TransactionDTO save(TransactionDTO transactiondto);
  public List<TransactionMountGroupByPaymentMethodDTO> getTotalAmountGroupedByPaymentMethod();

}
