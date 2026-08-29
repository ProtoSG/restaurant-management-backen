package com.restaurant_management.restaurant_management_backend.voiceorder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderService;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.CreateOrderRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.voiceorder.VoiceOrderValidator.ItemPriceValidation;
import com.restaurant_management.restaurant_management_backend.voiceorder.VoiceOrderValidator.TableResolution;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderConfirmItem;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderConfirmRequest;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderItemStatus;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderTableStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceOrderConfirmServiceTest {

  @Mock VoiceOrderValidator voiceOrderValidator;
  @Mock OrderRepository orderRepository;
  @Mock OrderService orderService;

  @InjectMocks
  VoiceOrderConfirmService confirmService;

  private static VoiceOrderConfirmItem item(Long productId, BigDecimal price, int quantity, String notes) {
    return new VoiceOrderConfirmItem(productId, price, quantity, notes, false);
  }

  private static VoiceOrderConfirmItem takeawayItem(Long productId, BigDecimal price, int quantity, String notes) {
    return new VoiceOrderConfirmItem(productId, price, quantity, notes, true);
  }

  @Test
  void confirm_createsNewOrderAndAddsItems_whenTableHasNoActiveOrder() {
    when(voiceOrderValidator.resolveTableNumber(8))
      .thenReturn(new TableResolution(VoiceOrderTableStatus.RESOLVED, 100L));
    when(voiceOrderValidator.validateProductPrice(1L, new BigDecimal("25")))
      .thenReturn(new ItemPriceValidation(VoiceOrderItemStatus.RESOLVED, "Trio marisco", new BigDecimal("25.00")));

    when(orderRepository.findActiveOrdersByTableId(100L)).thenReturn(Collections.emptyList());
    OrderResponse created = mock(OrderResponse.class);
    when(created.id()).thenReturn(500L);
    when(orderService.save(any(CreateOrderRequest.class))).thenReturn(created);

    OrderResponse finalResponse = mock(OrderResponse.class);
    when(orderService.findById(500L)).thenReturn(finalResponse);

    VoiceOrderConfirmRequest request = new VoiceOrderConfirmRequest(
      8, false, List.of(item(1L, new BigDecimal("25"), 1, "sin ají")));

    OrderResponse result = confirmService.confirm(request);

    assertThat(result).isSameAs(finalResponse);
    verify(orderService).save(eq(new CreateOrderRequest(100L, com.restaurant_management.restaurant_management_backend.shared.enums.OrderType.DINE_IN, null)));
    verify(orderService).addOrderItem(eq(500L),
      eq(new AddOrderItemRequest(1L, 1, "sin ají", false, new BigDecimal("25.00"))));
  }

  @Test
  void confirm_reusesExistingActiveOrder_insteadOfCreatingASecondOne() {
    when(voiceOrderValidator.resolveTableNumber(8))
      .thenReturn(new TableResolution(VoiceOrderTableStatus.RESOLVED, 100L));
    when(voiceOrderValidator.validateProductPrice(1L, new BigDecimal("25")))
      .thenReturn(new ItemPriceValidation(VoiceOrderItemStatus.RESOLVED, "Trio marisco", new BigDecimal("25.00")));

    Order existingOrder = mock(Order.class);
    when(existingOrder.getId()).thenReturn(777L);
    when(orderRepository.findActiveOrdersByTableId(100L)).thenReturn(List.of(existingOrder));

    OrderResponse finalResponse = mock(OrderResponse.class);
    when(orderService.findById(777L)).thenReturn(finalResponse);

    VoiceOrderConfirmRequest request = new VoiceOrderConfirmRequest(
      8, false, List.of(item(1L, new BigDecimal("25"), 1, null)));

    confirmService.confirm(request);

    verify(orderService, never()).save(any());
    verify(orderService).addOrderItem(eq(777L), any(AddOrderItemRequest.class));
  }

  @Test
  void confirm_rejectsWithoutWritingAnything_whenTableDoesNotResolve() {
    when(voiceOrderValidator.resolveTableNumber(99))
      .thenReturn(new TableResolution(VoiceOrderTableStatus.NOT_FOUND, null));

    VoiceOrderConfirmRequest request = new VoiceOrderConfirmRequest(
      99, false, List.of(item(1L, new BigDecimal("25"), 1, null)));

    assertThatThrownBy(() -> confirmService.confirm(request))
      .isInstanceOf(BadRequestException.class);

    verifyNoInteractions(orderRepository, orderService);
  }

  @Test
  void confirm_rejectsWithoutWritingAnything_whenOneItemFailsRevalidation() {
    // The critical guarantee: one bad item must not result in a partially-created order with
    // only the valid items — reject the whole confirm before any write happens.
    when(voiceOrderValidator.resolveTableNumber(8))
      .thenReturn(new TableResolution(VoiceOrderTableStatus.RESOLVED, 100L));
    when(voiceOrderValidator.validateProductPrice(1L, new BigDecimal("25")))
      .thenReturn(new ItemPriceValidation(VoiceOrderItemStatus.RESOLVED, "Trio marisco", new BigDecimal("25.00")));
    when(voiceOrderValidator.validateProductPrice(2L, new BigDecimal("99")))
      .thenReturn(new ItemPriceValidation(VoiceOrderItemStatus.PRICE_MISMATCH, "Trio chaufa", null));

    VoiceOrderConfirmRequest request = new VoiceOrderConfirmRequest(8, false, List.of(
      item(1L, new BigDecimal("25"), 1, null),
      item(2L, new BigDecimal("99"), 1, null)
    ));

    assertThatThrownBy(() -> confirmService.confirm(request))
      .isInstanceOf(BadRequestException.class);

    verifyNoInteractions(orderRepository, orderService);
  }

  @Test
  void confirm_usesTheRevalidatedPrice_notTheClientSubmittedOne() {
    // Even if the client sends a price, the order is written with what VoiceOrderValidator
    // resolved server-side — never a client-controlled number.
    when(voiceOrderValidator.resolveTableNumber(8))
      .thenReturn(new TableResolution(VoiceOrderTableStatus.RESOLVED, 100L));
    when(voiceOrderValidator.validateProductPrice(1L, new BigDecimal("25")))
      .thenReturn(new ItemPriceValidation(VoiceOrderItemStatus.RESOLVED, "Trio marisco", new BigDecimal("25.00")));

    when(orderRepository.findActiveOrdersByTableId(100L)).thenReturn(Collections.emptyList());
    OrderResponse created = mock(OrderResponse.class);
    when(created.id()).thenReturn(500L);
    when(orderService.save(any())).thenReturn(created);
    when(orderService.findById(500L)).thenReturn(mock(OrderResponse.class));

    VoiceOrderConfirmRequest request = new VoiceOrderConfirmRequest(
      8, false, List.of(item(1L, new BigDecimal("25"), 3, null)));

    confirmService.confirm(request);

    verify(orderService, times(1)).addOrderItem(eq(500L),
      eq(new AddOrderItemRequest(1L, 3, null, false, new BigDecimal("25.00"))));
  }

  // ── Takeaway (whole order) ──────────────────────────────────────────────

  @Test
  void confirm_createsFreshTakeawayOrder_withNoTableAndNoDedup() {
    when(voiceOrderValidator.validateProductPrice(1L, new BigDecimal("18")))
      .thenReturn(new ItemPriceValidation(VoiceOrderItemStatus.RESOLVED, "Coca", new BigDecimal("18.00")));

    OrderResponse created = mock(OrderResponse.class);
    when(created.id()).thenReturn(600L);
    when(orderService.save(any(CreateOrderRequest.class))).thenReturn(created);

    OrderResponse finalResponse = mock(OrderResponse.class);
    when(orderService.findById(600L)).thenReturn(finalResponse);

    VoiceOrderConfirmRequest request = new VoiceOrderConfirmRequest(
      null, true, List.of(item(1L, new BigDecimal("18"), 1, null)));

    OrderResponse result = confirmService.confirm(request);

    assertThat(result).isSameAs(finalResponse);
    verify(orderService).save(eq(new CreateOrderRequest(null,
      com.restaurant_management.restaurant_management_backend.shared.enums.OrderType.TAKEAWAY, null)));
    verify(orderService).addOrderItem(eq(600L),
      eq(new AddOrderItemRequest(1L, 1, null, false, new BigDecimal("18.00"))));
    // A takeaway order is never deduped against an existing one, unlike a table order.
    verifyNoInteractions(orderRepository);
    verify(voiceOrderValidator, never()).resolveTableNumber(any());
  }

  @Test
  void confirm_rejectsWithoutWritingAnything_whenTableNumberMissingAndNotTakeawayOrder() {
    // @NotNull was removed from tableNumber (nullable to support takeaway orders) — this service
    // must enforce "required unless isTakeawayOrder" manually now that bean validation can't.
    VoiceOrderConfirmRequest request = new VoiceOrderConfirmRequest(
      null, false, List.of(item(1L, new BigDecimal("18"), 1, null)));

    assertThatThrownBy(() -> confirmService.confirm(request))
      .isInstanceOf(BadRequestException.class);

    verifyNoInteractions(voiceOrderValidator, orderRepository, orderService);
  }

  @Test
  void confirm_rejectsTakeawayOrderWithoutWritingAnything_whenOneItemFailsRevalidation() {
    when(voiceOrderValidator.validateProductPrice(1L, new BigDecimal("18")))
      .thenReturn(new ItemPriceValidation(VoiceOrderItemStatus.RESOLVED, "Coca", new BigDecimal("18.00")));
    when(voiceOrderValidator.validateProductPrice(2L, new BigDecimal("99")))
      .thenReturn(new ItemPriceValidation(VoiceOrderItemStatus.PRICE_MISMATCH, "Fantasma", null));

    VoiceOrderConfirmRequest request = new VoiceOrderConfirmRequest(null, true, List.of(
      item(1L, new BigDecimal("18"), 1, null),
      item(2L, new BigDecimal("99"), 1, null)
    ));

    assertThatThrownBy(() -> confirmService.confirm(request))
      .isInstanceOf(BadRequestException.class);

    // The critical guarantee still holds for takeaway: no order gets created before every item
    // is known valid.
    verifyNoInteractions(orderRepository, orderService);
  }

  @Test
  void confirm_perItemTakeawayFlag_flowsIntoTheRealAddOrderItemRequest() {
    when(voiceOrderValidator.resolveTableNumber(8))
      .thenReturn(new TableResolution(VoiceOrderTableStatus.RESOLVED, 100L));
    when(voiceOrderValidator.validateProductPrice(1L, new BigDecimal("18")))
      .thenReturn(new ItemPriceValidation(VoiceOrderItemStatus.RESOLVED, "Coca", new BigDecimal("18.00")));

    when(orderRepository.findActiveOrdersByTableId(100L)).thenReturn(Collections.emptyList());
    OrderResponse created = mock(OrderResponse.class);
    when(created.id()).thenReturn(500L);
    when(orderService.save(any(CreateOrderRequest.class))).thenReturn(created);
    when(orderService.findById(500L)).thenReturn(mock(OrderResponse.class));

    // Whole order stays dine-in (isTakeawayOrder=false), but this one item is takeaway.
    VoiceOrderConfirmRequest request = new VoiceOrderConfirmRequest(
      8, false, List.of(takeawayItem(1L, new BigDecimal("18"), 1, null)));

    confirmService.confirm(request);

    verify(orderService).addOrderItem(eq(500L),
      eq(new AddOrderItemRequest(1L, 1, null, true, new BigDecimal("18.00"))));
  }
}
