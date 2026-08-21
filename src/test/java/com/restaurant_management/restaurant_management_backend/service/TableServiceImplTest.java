package com.restaurant_management.restaurant_management_backend.service;

import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.shared.enums.TableStatus;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.tables.TableMapper;
import com.restaurant_management.restaurant_management_backend.tables.TableRepository;
import com.restaurant_management.restaurant_management_backend.tables.TableServiceImpl;
import com.restaurant_management.restaurant_management_backend.tables.dto.response.TableResponse;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEvent;
import com.restaurant_management.restaurant_management_backend.websocket.OrderEventPublisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableServiceImplTest {

  @Mock TableRepository tableRepository;
  @Mock TableMapper tableMapper;
  @Mock OrderRepository orderRepository;
  @Mock OrderEventPublisher orderEventPublisher;

  @InjectMocks
  TableServiceImpl tableService;

  // ── release ──────────────────────────────────────────────────────────────────

  @Test
  void release_finalizesStuckPaidOrderAndFreesTable() {
    Table table = Table.builder().id(2L).status(TableStatus.OCCUPIED).build();
    Order paidOrder = Order.builder()
      .id(10L).status(OrderStatus.PAID).type(OrderType.DINE_IN)
      .table(table).items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(tableRepository.findById(2L)).thenReturn(Optional.of(table));
    when(orderRepository.findActiveOrdersByTableId(2L)).thenReturn(List.of(paidOrder));
    when(tableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(tableMapper.toResponse(any())).thenReturn(mock(TableResponse.class));

    tableService.release(2L);

    assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.FINALIZADO);
    assertThat(table.getStatus()).isEqualTo(TableStatus.FREE);
    verify(orderRepository).save(paidOrder);
    verify(orderEventPublisher).publish(OrderEvent.Type.FINALIZED, 10L, 2L);
    verify(tableRepository).save(table);
  }

  @Test
  void release_freesTableWhenNoPaidOrdersExist() {
    Table table = Table.builder().id(3L).status(TableStatus.OCCUPIED).build();

    when(tableRepository.findById(3L)).thenReturn(Optional.of(table));
    when(orderRepository.findActiveOrdersByTableId(3L)).thenReturn(List.of());
    when(tableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(tableMapper.toResponse(any())).thenReturn(mock(TableResponse.class));

    tableService.release(3L);

    assertThat(table.getStatus()).isEqualTo(TableStatus.FREE);
    verify(orderRepository, never()).save(any());
    verify(orderEventPublisher, never()).publish(any(), any(), any());
    verify(tableRepository).save(table);
  }

  @Test
  void release_rejectsWhenAnUnpaidOrderIsStillActive() {
    Table table = Table.builder().id(4L).status(TableStatus.OCCUPIED).build();
    Order inProgressOrder = Order.builder()
      .id(11L).status(OrderStatus.IN_PROGRESS).type(OrderType.DINE_IN)
      .table(table).items(new LinkedHashSet<>()).transactions(new LinkedHashSet<>())
      .build();

    when(tableRepository.findById(4L)).thenReturn(Optional.of(table));
    when(orderRepository.findActiveOrdersByTableId(4L)).thenReturn(List.of(inProgressOrder));

    assertThatThrownBy(() -> tableService.release(4L))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("pedido sin pagar");

    assertThat(inProgressOrder.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
    assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
    verify(orderRepository, never()).save(any());
    verify(tableRepository, never()).save(any());
  }
}
