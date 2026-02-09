package com.restaurant_management.restaurant_management_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.dto.TransactionMountGroupByPaymentMethodDTO;
import com.restaurant_management.restaurant_management_backend.service.TransactionService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
  private final TransactionService transactionService;
  
  @GetMapping("/grouped-by-payment-method")
  public ResponseEntity<List<TransactionMountGroupByPaymentMethodDTO>> getTotalAmountGroupedByPaymentMethod() {
    List<TransactionMountGroupByPaymentMethodDTO> result = transactionService.getTotalAmountGroupedByPaymentMethod();

    return ResponseEntity.ok(result);
  }

}
