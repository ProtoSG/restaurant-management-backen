package com.restaurant_management.restaurant_management_backend.voiceorder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderService;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.AddOrderItemRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.request.CreateOrderRequest;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;
import com.restaurant_management.restaurant_management_backend.voiceorder.VoiceOrderValidator.ItemPriceValidation;
import com.restaurant_management.restaurant_management_backend.voiceorder.VoiceOrderValidator.TableResolution;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderConfirmItem;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderConfirmRequest;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderItemStatus;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderTableStatus;

import lombok.RequiredArgsConstructor;

/**
 * Turns a mesero-confirmed voice-order preview into a real order. This is the ONLY place in the
 * voiceorder module that writes anything — everything upstream (extraction, preview validation)
 * is read-only by design.
 *
 * <p><b>Trust nothing from the client, not even its own preview.</b> Every item and the table
 * are re-validated here from scratch via {@link VoiceOrderValidator}, using the exact same
 * deterministic logic the preview used — a client could submit a hand-edited payload, and the
 * catalog itself could have changed between preview and confirm (a product disabled, moved).
 * If ANY item or the table fails re-validation, the whole confirm is rejected before a single
 * write happens — no partial order gets created with only the valid items.
 *
 * <p>Writing itself reuses the real, already-tested {@link OrderService} — this class does not
 * duplicate order-creation or item-pricing logic, it only orchestrates: resolve/create the
 * order (a table's already-open order if one exists, same as the manual tablet flow — or a
 * brand-new {@code TAKEAWAY} order when {@code isTakeawayOrder}, since takeaway orders have no
 * table identity to dedupe against, matching {@code Tables.tsx}'s own "Para llevar" flow), then
 * add each validated item through the same path the tactile UI uses.
 */
@Service
@RequiredArgsConstructor
public class VoiceOrderConfirmService {

  private final VoiceOrderValidator voiceOrderValidator;
  private final OrderRepository orderRepository;
  private final OrderService orderService;

  private record ValidatedItem(Long productId, BigDecimal price, Integer quantity, String notes, boolean isTakeaway) {}

  @Transactional
  public OrderResponse confirm(VoiceOrderConfirmRequest request) {
    // Both checks below are pure reads — no write happens until AFTER the whole confirm is known
    // to be valid, matching the "reject before a single write" guarantee.
    TableResolution tableResolution = request.isTakeawayOrder() ? null : requireResolvedTable(request.tableNumber());
    List<ValidatedItem> validatedItems = validateAllItems(request.items());

    Long orderId = request.isTakeawayOrder()
      ? resolveOrCreateTakeawayOrder()
      : resolveOrCreateDineInOrder(tableResolution);

    for (ValidatedItem item : validatedItems) {
      orderService.addOrderItem(orderId,
        new AddOrderItemRequest(item.productId(), item.quantity(), item.notes(), item.isTakeaway(), item.price()));
    }

    return orderService.findById(orderId);
  }

  private TableResolution requireResolvedTable(Integer tableNumber) {
    // @NotNull was removed from VoiceOrderConfirmRequest.tableNumber (nullable to support
    // takeaway orders) — bean validation can no longer express "required unless isTakeawayOrder",
    // so this is the one place that combination is enforced.
    if (tableNumber == null) {
      throw new BadRequestException("El número de mesa es obligatorio para un pedido que no es para llevar");
    }

    TableResolution tableResolution = voiceOrderValidator.resolveTableNumber(tableNumber);
    if (tableResolution.status() != VoiceOrderTableStatus.RESOLVED) {
      throw new BadRequestException(
        "La mesa %s no es válida (%s) — no se puede confirmar el pedido"
          .formatted(tableNumber, tableResolution.status()));
    }
    return tableResolution;
  }

  // Re-validate every item BEFORE writing anything — reject the whole confirm on the first
  // invalid item rather than partially creating an order with only the valid ones.
  private List<ValidatedItem> validateAllItems(List<VoiceOrderConfirmItem> items) {
    List<ValidatedItem> validatedItems = new ArrayList<>();
    for (VoiceOrderConfirmItem item : items) {
      ItemPriceValidation validation =
        voiceOrderValidator.validateProductPrice(item.productId(), item.selectedPrice());

      if (validation.status() != VoiceOrderItemStatus.RESOLVED) {
        throw new BadRequestException(
          "Ítem no válido (%s): producto %d a S/ %s"
            .formatted(validation.status(), item.productId(), item.selectedPrice()));
      }

      validatedItems.add(new ValidatedItem(
        item.productId(), validation.price(), item.quantity(), item.notes(), item.isTakeaway()));
    }
    return validatedItems;
  }

  // Mirrors the manual tablet flow (ListProducts.tsx): reuse the table's already-open order if
  // one exists, instead of creating a second concurrent order for the same table.
  private Long resolveOrCreateDineInOrder(TableResolution tableResolution) {
    List<Order> activeOrders = orderRepository.findActiveOrdersByTableId(tableResolution.tableId());
    if (!activeOrders.isEmpty()) {
      return activeOrders.get(0).getId();
    }
    return orderService.save(new CreateOrderRequest(tableResolution.tableId(), OrderType.DINE_IN, null)).id();
  }

  // Unlike a table, a takeaway order has no natural identity to dedupe against — multiple
  // simultaneous takeaway orders are normal (different customers, all "para llevar"). Always
  // creates a new order, same as Tables.tsx's own "Para llevar" FAB (takeawayModal.openCreate).
  private Long resolveOrCreateTakeawayOrder() {
    return orderService.save(new CreateOrderRequest(null, OrderType.TAKEAWAY, null)).id();
  }
}
