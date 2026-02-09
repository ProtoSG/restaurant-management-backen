package com.restaurant_management.restaurant_management_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.restaurant_management.restaurant_management_backend.dto.TransactionMountGroupByPaymentMethodDTO;
import com.restaurant_management.restaurant_management_backend.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  @Query("SELECT new com.restaurant_management.restaurant_management_backend.dto.TransactionMountGroupByPaymentMethodDTO(" +
            "t.paymentMethod, " +
            "CAST(COUNT(t) AS integer), " +
            "SUM(t.total)) " +
            "FROM Transaction t " +
            "WHERE t.status = 'COMPLETED' " +
            "GROUP BY t.paymentMethod")
  List<TransactionMountGroupByPaymentMethodDTO> getTotalAmountGroupedByPaymentMethod(); 

}
