package com.restaurant_management.restaurant_management_backend.transactions;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.transactions.dto.response.TransactionResponse;
import com.restaurant_management.restaurant_management_backend.transactions.entity.Transaction;

@Component
public class TransactionMapper {

  public TransactionResponse toDto(Transaction transaction) {
    if (transaction == null) return null;

    return new TransactionResponse(
        transaction.getId(), 
        transaction.getOrder() != null ? transaction.getOrder().getId() : null,
        transaction.getUser() != null ? transaction.getUser().getId() : null,
        transaction.getUser() != null ? transaction.getUser().getName() : null,
        transaction.getTotal(),
        transaction.getPaymentMethod(),
        transaction.getStatus(),
        transaction.getTransactionDate()
    );

  }

}
