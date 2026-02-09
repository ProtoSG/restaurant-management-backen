package com.restaurant_management.restaurant_management_backend.mapper;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.dto.TransactionDTO;
import com.restaurant_management.restaurant_management_backend.entity.Transaction;

@Component
public class TransactionMapper {

  public TransactionDTO toDto(Transaction transaction) {
    if (transaction == null) return null;

    return TransactionDTO.builder()
      .id(transaction.getId())
      .orderId(transaction.getOrder() != null ? transaction.getOrder().getId() : null)
      .userId(transaction.getUser() != null ? transaction.getUser().getId() : null)
      .userName(transaction.getUser() != null ? transaction.getUser().getName() : null)
      .total(transaction.getTotal())
      .paymentMethod(transaction.getPaymentMethod())
      .status(transaction.getStatus())
      .transactionDate(transaction.getTransactionDate())
      .build();
  }

  public Transaction toEntity(TransactionDTO transactionDTO) {
    if (transactionDTO == null) return null;

    return Transaction.builder()
      .id(transactionDTO.getId())
      .total(transactionDTO.getTotal())
      .paymentMethod(transactionDTO.getPaymentMethod())
      .status(transactionDTO.getStatus())
      .transactionDate(transactionDTO.getTransactionDate())
      .build();
  }
  
}
