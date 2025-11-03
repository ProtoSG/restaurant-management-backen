package com.restaurant_management.restaurant_management_backend.service.impl;

import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.entity.OrderCodeSequence;
import com.restaurant_management.restaurant_management_backend.repository.OrderCodeSequenceRepository;
import com.restaurant_management.restaurant_management_backend.service.OrderCodeService;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderCodeServiceImpl implements OrderCodeService {

  private final OrderCodeSequenceRepository sequenceRepository;

  @PostConstruct
  public void initializeSequence() {
    if (sequenceRepository.count() == 0) {
      OrderCodeSequence sequence = new OrderCodeSequence();
      sequence.setCurrentValue(0L);
      sequenceRepository.save(sequence);
    }
  }

  @Transactional
  @Override
  public String generateNextOrderCode() {
    OrderCodeSequence sequence = sequenceRepository.findSequenceForUpdate();
    if (sequence == null) {
      sequence = new OrderCodeSequence();
      sequence.setCurrentValue(0L);
      sequenceRepository.save(sequence);
    }
    
    return sequence.generateNextCode();
  }
}
