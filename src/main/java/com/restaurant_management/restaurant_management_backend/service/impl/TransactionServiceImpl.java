package com.restaurant_management.restaurant_management_backend.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.dto.TransactionDTO;
import com.restaurant_management.restaurant_management_backend.entity.Order;
import com.restaurant_management.restaurant_management_backend.entity.Transaction;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.exceptions.UnauthorizedException;
import com.restaurant_management.restaurant_management_backend.mapper.TransactionMapper;
import com.restaurant_management.restaurant_management_backend.repository.OrderRepository;
import com.restaurant_management.restaurant_management_backend.repository.TransactionRepository;
import com.restaurant_management.restaurant_management_backend.repository.UserRepository;
import com.restaurant_management.restaurant_management_backend.service.TransactionServide;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionServide {

  private final TransactionRepository transactionRepository;
  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final TransactionMapper transactionMapper;

  @Override
  @Transactional
  public TransactionDTO save(TransactionDTO transactionDTO) {
    // Get current authenticated user
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new UnauthorizedException("Usuario no autenticado");
    }
    
    String userEmail = authentication.getName();
    User user = userRepository.findByEmail(userEmail)
      .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

    // Get order
    Order order = orderRepository.findById(transactionDTO.getOrderId())
      .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

    // Build transaction
    Transaction transaction = Transaction.builder()
      .order(order)
      .user(user)
      .total(transactionDTO.getTotal())
      .paymentMethod(transactionDTO.getPaymentMethod())
      .status(transactionDTO.getStatus())
      .transactionDate(transactionDTO.getTransactionDate() != null ? 
        transactionDTO.getTransactionDate() : LocalDateTime.now())
      .build();

    Transaction savedTransaction = transactionRepository.save(transaction);
    
    return transactionMapper.toDto(savedTransaction);
  }
  
}
