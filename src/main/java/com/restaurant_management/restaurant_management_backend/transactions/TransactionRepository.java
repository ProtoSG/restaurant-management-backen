package com.restaurant_management.restaurant_management_backend.transactions;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurant_management.restaurant_management_backend.shared.enums.TransactionStatus;
import com.restaurant_management.restaurant_management_backend.transactions.dto.response.TransactionMountGroupByPaymentMethodResponse;
import com.restaurant_management.restaurant_management_backend.transactions.dto.response.TrendTransactionsWeekResponse;
import com.restaurant_management.restaurant_management_backend.transactions.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  @Query("SELECT new com.restaurant_management.restaurant_management_backend.transactions.dto.response.TransactionMountGroupByPaymentMethodResponse(" +
            "t.paymentMethod, " +
            "CAST(COUNT(t) AS integer), " +
            "SUM(t.total)) " +
            "FROM Transaction t " +
            "WHERE t.status = 'COMPLETED' " +
            "GROUP BY t.paymentMethod")
  List<TransactionMountGroupByPaymentMethodResponse> getTotalAmountGroupedByPaymentMethod(); 

  List<Transaction> findByTransactionDateBetweenAndStatus(
    LocalDateTime startDate,
    LocalDateTime endDate,
    TransactionStatus status
  );

  @Query(value = "SELECT " +
        "CAST(transaction_date AS DATE) as transactionDate, " +
        "SUM(total) as totalSum " +
        "FROM transactions " +
        "WHERE status = 'COMPLETED' " +
        "GROUP BY CAST(transaction_date AS DATE) " +
        "ORDER BY CAST(transaction_date AS DATE) DESC " +
        "LIMIT 7", 
        nativeQuery = true)
  List<TrendTransactionsWeekResponse> getTotalAmountWeekGroupedByTransactionDate();

  @Query("SELECT new com.restaurant_management.restaurant_management_backend.transactions.dto.response.TransactionMountGroupByPaymentMethodResponse(" +
            "t.paymentMethod, " +
            "CAST(COUNT(t) AS integer), " +
            "SUM(t.total)) " +
            "FROM Transaction t " +
            "WHERE t.status = 'COMPLETED' " +
            "AND t.transactionDate >= :startDate " +
            "AND t.transactionDate < :endDate " +
            "GROUP BY t.paymentMethod")
  List<TransactionMountGroupByPaymentMethodResponse> getTotalAmountGroupedByPaymentMethodAndDate(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
  );

  @Query("SELECT t FROM Transaction t " +
         "WHERE t.transactionDate >= :startDate " +
         "AND t.transactionDate < :endDate " +
         "AND t.status = 'COMPLETED' " +
         "ORDER BY t.transactionDate ASC")
  List<Transaction> findCompletedTransactionsByDateRange(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
  );

  @Query("SELECT t FROM Transaction t " +
         "LEFT JOIN FETCH t.order o " +
         "LEFT JOIN FETCH o.table " +
         "LEFT JOIN FETCH t.user " +
         "WHERE t.transactionDate >= :startDate " +
         "AND t.transactionDate < :endDate " +
         "AND t.status = 'COMPLETED' " +
         "ORDER BY t.transactionDate DESC")
  List<Transaction> findRecentCompletedTransactions(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
  );

}
