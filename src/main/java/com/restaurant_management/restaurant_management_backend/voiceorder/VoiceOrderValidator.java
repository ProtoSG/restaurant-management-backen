package com.restaurant_management.restaurant_management_backend.voiceorder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.ProductVariantRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.entity.ProductVariant;
import com.restaurant_management.restaurant_management_backend.tables.TableRepository;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderExtraction;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderItemExtraction;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderItemStatus;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderPreview;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderPreviewItem;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderTableStatus;

import lombok.RequiredArgsConstructor;

/**
 * Pure deterministic validation of an LLM extraction against the real catalog. No LLM calls, no
 * writes — this is the "code decides, not AI" half of the voice-order design principle. The LLM
 * never decides a price: it only reports what it heard (a spoken price number, OR a spoken
 * variant label like "mediano", OR neither) — this class is the only place a real price is ever
 * looked up and assigned.
 *
 * <p>Three ways an item's price resolves, in this order:
 * <ol>
 *   <li>A price number was spoken ({@code selectedPrice} present) — matched exactly against the
 *       product's base price or its variants' real prices.</li>
 *   <li>A variant label was spoken instead ({@code variantName} present, e.g. "mediano",
 *       "pescado") — matched by name (case-insensitive) against the product's real variants.</li>
 *   <li>Neither was spoken — safe to auto-resolve ONLY if the product has no variants at all
 *       (a single real price, so there's nothing to disambiguate). If it has variants and the
 *       mesero named neither a price nor a variant, the item is ambiguous and must not be
 *       guessed.</li>
 * </ol>
 *
 * <p>The base price is always a valid, orderable option — never a special case of "has no
 * variants" (confirmed against {@code ListProducts.tsx} on the frontend: "Ninguno es un caso
 * especial del otro"; and against real prod data: "Trio marisco" sells at both its base price of
 * 25 and its "Pescado" variant price of 30).
 *
 * <p>Also resolves the dictated table number against a real, active {@link Table} — {@code
 * Table.number} is a String in the schema (supports non-numeric codes like "BAR"), a mesero
 * always dictates a plain number, so lookup is by {@code Integer.toString(n)}. {@code
 * allResolved} requires the table to resolve too — an order can't be created for a table that
 * was never understood.
 */
@Component
@RequiredArgsConstructor
public class VoiceOrderValidator {

  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;
  private final TableRepository tableRepository;

  private record PriceResolution(VoiceOrderItemStatus status, BigDecimal price) {}

  // Package-private (not private): VoiceOrderConfirmService re-validates a table/item pair at
  // confirm time using the exact same logic, and needs to read these results.
  record TableResolution(VoiceOrderTableStatus status, Long tableId) {}
  public record ItemPriceValidation(VoiceOrderItemStatus status, String productName, BigDecimal price) {}

  public VoiceOrderPreview validate(VoiceOrderExtraction extraction) {
    List<VoiceOrderPreviewItem> previewItems = extraction.items().stream()
      .map(this::validateItem)
      .toList();

    TableResolution tableResolution = resolveTable(extraction.tableNumber());

    boolean allResolved = tableResolution.status() == VoiceOrderTableStatus.RESOLVED
      && previewItems.stream().allMatch(item -> item.status() == VoiceOrderItemStatus.RESOLVED);

    return new VoiceOrderPreview(
      extraction.tableNumber().orElse(null),
      tableResolution.tableId(),
      tableResolution.status(),
      previewItems,
      allResolved
    );
  }

  /**
   * Re-resolves a table number in isolation (not part of an extraction) — used by
   * {@code VoiceOrderConfirmService} to check the table again at confirm time.
   */
  TableResolution resolveTableNumber(Integer tableNumber) {
    return resolveTable(Optional.ofNullable(tableNumber));
  }

  /**
   * Re-validates a single (productId, selectedPrice) pair against the real catalog, independent
   * of any LLM extraction — used by {@code VoiceOrderConfirmService} to check every item again at
   * confirm time. Never trust a client-submitted preview as-is, even one this same validator
   * built moments earlier: the catalog can change in between (a product disabled, a price moved).
   */
  public ItemPriceValidation validateProductPrice(Long productId, BigDecimal selectedPrice) {
    if (productId == null || selectedPrice == null) {
      return new ItemPriceValidation(VoiceOrderItemStatus.NOT_FOUND, null, null);
    }

    Optional<Product> productOpt = productRepository.findByIdWithCategory(productId);
    if (productOpt.isEmpty()) {
      return new ItemPriceValidation(VoiceOrderItemStatus.NOT_FOUND, null, null);
    }

    Product product = productOpt.get();
    if (!Boolean.TRUE.equals(product.getIsAvailable())) {
      return new ItemPriceValidation(VoiceOrderItemStatus.NOT_AVAILABLE, product.getName(), null);
    }

    PriceResolution resolution = resolveBySpokenPrice(product, selectedPrice);
    return new ItemPriceValidation(resolution.status(), product.getName(), resolution.price());
  }

  private TableResolution resolveTable(Optional<Integer> tableNumber) {
    if (tableNumber.isEmpty()) {
      return new TableResolution(VoiceOrderTableStatus.MISSING, null);
    }

    Optional<Table> table = tableRepository.findByNumber(String.valueOf(tableNumber.get()))
      .filter(t -> Boolean.TRUE.equals(t.getIsActive()));

    return table
      .map(t -> new TableResolution(VoiceOrderTableStatus.RESOLVED, t.getId()))
      .orElseGet(() -> new TableResolution(VoiceOrderTableStatus.NOT_FOUND, null));
  }

  private VoiceOrderPreviewItem validateItem(VoiceOrderItemExtraction item) {
    Long productId = item.productId().orElse(null);
    if (productId == null) {
      return toPreviewItem(item, VoiceOrderItemStatus.NOT_FOUND, null, item.selectedPrice().orElse(null));
    }

    Optional<Product> productOpt = productRepository.findByIdWithCategory(productId);
    if (productOpt.isEmpty()) {
      return toPreviewItem(item, VoiceOrderItemStatus.NOT_FOUND, null, item.selectedPrice().orElse(null));
    }

    Product product = productOpt.get();
    if (!Boolean.TRUE.equals(product.getIsAvailable())) {
      return toPreviewItem(item, VoiceOrderItemStatus.NOT_AVAILABLE, product.getName(), item.selectedPrice().orElse(null));
    }

    PriceResolution resolution = resolvePrice(product, item);
    return toPreviewItem(item, resolution.status(), product.getName(), resolution.price());
  }

  private PriceResolution resolvePrice(Product product, VoiceOrderItemExtraction item) {
    if (item.selectedPrice().isPresent()) {
      return resolveBySpokenPrice(product, item.selectedPrice().get());
    }

    if (item.variantName().isPresent()) {
      return resolveByVariantName(product, item.variantName().get());
    }

    // Neither a price nor a variant label was spoken. Only safe to auto-resolve when the
    // product has no variants — a single real price means there's nothing to disambiguate
    // (e.g. "una coca de un litro", where nobody states the price out loud).
    List<ProductVariant> allVariants = productVariantRepository.findByProductId(product.getId());
    if (allVariants.isEmpty()) {
      return new PriceResolution(VoiceOrderItemStatus.RESOLVED, product.getPrice());
    }
    return new PriceResolution(VoiceOrderItemStatus.PRICE_MISMATCH, null);
  }

  private PriceResolution resolveBySpokenPrice(Product product, BigDecimal selectedPrice) {
    if (product.getPrice() != null && selectedPrice.compareTo(product.getPrice()) == 0) {
      return new PriceResolution(VoiceOrderItemStatus.RESOLVED, product.getPrice());
    }

    List<ProductVariant> allVariants = productVariantRepository.findByProductId(product.getId());

    Optional<ProductVariant> availableMatch = allVariants.stream()
      .filter(v -> Boolean.TRUE.equals(v.getIsAvailable()))
      .filter(v -> v.getPrice().compareTo(selectedPrice) == 0)
      .findFirst();
    if (availableMatch.isPresent()) {
      return new PriceResolution(VoiceOrderItemStatus.RESOLVED, availableMatch.get().getPrice());
    }

    Optional<ProductVariant> disabledMatch = allVariants.stream()
      .filter(v -> !Boolean.TRUE.equals(v.getIsAvailable()))
      .filter(v -> v.getPrice().compareTo(selectedPrice) == 0)
      .findFirst();
    if (disabledMatch.isPresent()) {
      return new PriceResolution(VoiceOrderItemStatus.NOT_AVAILABLE, disabledMatch.get().getPrice());
    }

    return new PriceResolution(VoiceOrderItemStatus.PRICE_MISMATCH, selectedPrice);
  }

  private PriceResolution resolveByVariantName(Product product, String variantName) {
    List<ProductVariant> allVariants = productVariantRepository.findByProductId(product.getId());

    Optional<ProductVariant> match = allVariants.stream()
      .filter(v -> v.getName().equalsIgnoreCase(variantName.trim()))
      .findFirst();

    if (match.isEmpty()) {
      return new PriceResolution(VoiceOrderItemStatus.PRICE_MISMATCH, null);
    }

    ProductVariant variant = match.get();
    VoiceOrderItemStatus status = Boolean.TRUE.equals(variant.getIsAvailable())
      ? VoiceOrderItemStatus.RESOLVED
      : VoiceOrderItemStatus.NOT_AVAILABLE;
    return new PriceResolution(status, variant.getPrice());
  }

  private VoiceOrderPreviewItem toPreviewItem(
      VoiceOrderItemExtraction item, VoiceOrderItemStatus status, String productName, BigDecimal price) {
    return new VoiceOrderPreviewItem(
      status,
      item.rawText(),
      item.productId().orElse(null),
      productName,
      price,
      item.quantity(),
      item.notes()
    );
  }
}
