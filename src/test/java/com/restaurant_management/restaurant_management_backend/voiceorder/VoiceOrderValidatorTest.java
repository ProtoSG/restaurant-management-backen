package com.restaurant_management.restaurant_management_backend.voiceorder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderTableStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceOrderValidatorTest {

  @Mock ProductRepository productRepository;
  @Mock ProductVariantRepository productVariantRepository;
  @Mock TableRepository tableRepository;

  @InjectMocks
  VoiceOrderValidator validator;

  private static Table activeTable(long id, String number) {
    return Table.builder().id(id).number(number).isActive(true).build();
  }

  private static Product trioMarino(boolean available) {
    return trioMarino(available, BigDecimal.ZERO);
  }

  private static Product trioMarino(boolean available, BigDecimal basePrice) {
    return Product.builder()
      .id(1L)
      .name("Trio Marino")
      .price(basePrice)
      .isAvailable(available)
      .build();
  }

  private static ProductVariant variant(long id, Product product, String name, BigDecimal price, boolean available) {
    return ProductVariant.builder()
      .id(id)
      .product(product)
      .name(name)
      .price(price)
      .isAvailable(available)
      .build();
  }

  private static VoiceOrderExtraction dineInExtraction(Optional<Integer> tableNumber, List<VoiceOrderItemExtraction> items) {
    return new VoiceOrderExtraction(tableNumber, false, items);
  }

  private static VoiceOrderExtraction takeawayExtraction(List<VoiceOrderItemExtraction> items) {
    return new VoiceOrderExtraction(Optional.empty(), true, items);
  }

  private static VoiceOrderItemExtraction itemWithPrice(String rawText, Long productId, BigDecimal price, int quantity, String notes) {
    return itemWithPrice(rawText, productId, price, quantity, notes, false);
  }

  private static VoiceOrderItemExtraction itemWithPrice(
      String rawText, Long productId, BigDecimal price, int quantity, String notes, boolean isTakeaway) {
    return new VoiceOrderItemExtraction(
      rawText, Optional.ofNullable(productId), Optional.ofNullable(price), Optional.empty(), quantity, notes, isTakeaway);
  }

  private static VoiceOrderItemExtraction itemWithVariantName(String rawText, Long productId, String variantName, int quantity, String notes) {
    return new VoiceOrderItemExtraction(
      rawText, Optional.ofNullable(productId), Optional.empty(), Optional.ofNullable(variantName), quantity, notes, false);
  }

  private static VoiceOrderItemExtraction itemWithNeither(String rawText, Long productId, int quantity, String notes) {
    return new VoiceOrderItemExtraction(
      rawText, Optional.ofNullable(productId), Optional.empty(), Optional.empty(), quantity, notes, false);
  }

  // ── RESOLVED — spoken price ─────────────────────────────────────────────

  @Test
  void validate_resolvesItemWhenPriceMatchesAvailableVariant() {
    Product product = trioMarino(true);
    ProductVariant variant25 = variant(10L, product, "variante", new BigDecimal("25.00"), true);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(variant25));
    when(tableRepository.findByNumber("8")).thenReturn(Optional.of(activeTable(100L, "8")));

    VoiceOrderItemExtraction item = itemWithPrice("un trio marino de 25", 1L, new BigDecimal("25"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.of(8), List.of(item)));

    assertThat(preview.items()).hasSize(1);
    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.RESOLVED);
    assertThat(preview.items().get(0).productName()).isEqualTo("Trio Marino");
    assertThat(preview.tableStatus()).isEqualTo(VoiceOrderTableStatus.RESOLVED);
    assertThat(preview.tableId()).isEqualTo(100L);
    assertThat(preview.allResolved()).isTrue();
  }

  @Test
  void validate_resolvesItemWhenPriceMatchesBasePriceForProductWithNoVariants() {
    Product product = trioMarino(true, new BigDecimal("18.00"));

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    // No productVariantRepository stub: the base price short-circuits the match before the
    // variant lookup is ever reached — asserting that here would be an unnecessary stub.

    VoiceOrderItemExtraction item = itemWithPrice("un trio marino de 18", 1L, new BigDecimal("18"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.RESOLVED);
  }

  @Test
  void validate_resolvesItemWhenPriceMatchesBasePriceEvenWhenProductHasVariants() {
    // Regression test — this used to assert PRICE_MISMATCH here, which was wrong. The base price
    // is a real, orderable option regardless of how many variants a product has: confirmed
    // against ListProducts.tsx on the frontend ("Ninguno es un caso especial del otro") and
    // against real prod data ("Trio marisco" sells at both its base price of 25 and its
    // "Pescado" variant price of 30). No productVariantRepository stub needed: the base-price
    // match short-circuits before the variant lookup would run.
    Product product = trioMarino(true, new BigDecimal("25.00"));

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));

    VoiceOrderItemExtraction item = itemWithPrice("un trio marino de 25", 1L, new BigDecimal("25"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.RESOLVED);
  }

  // ── RESOLVED — spoken variant name (new) ────────────────────────────────

  @Test
  void validate_resolvesItemByVariantNameWhenNoPriceWasSpoken() {
    Product product = trioMarino(true, new BigDecimal("20.00"));
    ProductVariant mediano = variant(10L, product, "Mediano", new BigDecimal("25.00"), true);
    ProductVariant grande = variant(11L, product, "Grande", new BigDecimal("30.00"), true);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(mediano, grande));

    VoiceOrderItemExtraction item = itemWithVariantName("un ceviche mediano", 1L, "mediano", 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.RESOLVED);
    assertThat(preview.items().get(0).selectedPrice()).isEqualByComparingTo("25.00");
  }

  @Test
  void validate_variantNameMatchIsCaseInsensitive() {
    Product product = trioMarino(true, new BigDecimal("20.00"));
    ProductVariant pescado = variant(10L, product, "Pescado", new BigDecimal("30.00"), true);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(pescado));

    VoiceOrderItemExtraction item = itemWithVariantName("trio chaufa de pescado", 1L, "pescado", 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.RESOLVED);
    assertThat(preview.items().get(0).selectedPrice()).isEqualByComparingTo("30.00");
  }

  // ── RESOLVED — neither price nor variant, single-price product (new) ────

  @Test
  void validate_autoResolvesBasePriceWhenNeitherPriceNorVariantSpokenAndProductHasNoVariants() {
    // "una coca de un litro" — nobody states the price out loud for a drink. Safe to auto-resolve
    // because there's only one real price to disambiguate against.
    Product product = trioMarino(true, new BigDecimal("8.00"));

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(Collections.emptyList());

    VoiceOrderItemExtraction item = itemWithNeither("una coca de un litro", 1L, 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.RESOLVED);
    assertThat(preview.items().get(0).selectedPrice()).isEqualByComparingTo("8.00");
  }

  @Test
  void validate_ambiguousWhenNeitherPriceNorVariantSpokenAndProductHasVariants() {
    // The mesero must disambiguate somehow (price or variant label) when more than one real
    // price exists — guessing which tier they meant would violate "no default variant assumed".
    Product product = trioMarino(true, new BigDecimal("20.00"));
    ProductVariant mediano = variant(10L, product, "Mediano", new BigDecimal("25.00"), true);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(mediano));

    VoiceOrderItemExtraction item = itemWithNeither("un ceviche", 1L, 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.PRICE_MISMATCH);
  }

  // ── PRICE_MISMATCH ───────────────────────────────────────────────────────

  @Test
  void validate_marksPriceMismatchWhenPriceMatchesNoVariant() {
    Product product = trioMarino(true);
    ProductVariant variant20 = variant(10L, product, "variante", new BigDecimal("20.00"), true);
    ProductVariant variant25 = variant(11L, product, "variante", new BigDecimal("25.00"), true);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(variant20, variant25));

    VoiceOrderItemExtraction item = itemWithPrice("un trio marino de 28", 1L, new BigDecimal("28"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.PRICE_MISMATCH);
    assertThat(preview.allResolved()).isFalse();
  }

  @Test
  void validate_marksPriceMismatchWhenPriceMatchesNoVariantAndProductHasNoVariants() {
    Product product = trioMarino(true, new BigDecimal("18.00"));

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(Collections.emptyList());

    VoiceOrderItemExtraction item = itemWithPrice("un trio marino de 30", 1L, new BigDecimal("30"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.PRICE_MISMATCH);
  }

  @Test
  void validate_marksPriceMismatchWhenVariantNameDoesNotMatchAnyRealVariant() {
    Product product = trioMarino(true, new BigDecimal("20.00"));
    ProductVariant mediano = variant(10L, product, "Mediano", new BigDecimal("25.00"), true);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(mediano));

    VoiceOrderItemExtraction item = itemWithVariantName("un ceviche extra grande", 1L, "extra grande", 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.PRICE_MISMATCH);
  }

  // ── NOT_FOUND ────────────────────────────────────────────────────────────

  @Test
  void validate_marksNotFoundWhenProductIdIsEmpty() {
    VoiceOrderItemExtraction item = itemWithPrice("dos causas rellenas", null, new BigDecimal("15"), 2, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.NOT_FOUND);
    assertThat(preview.items().get(0).productName()).isNull();
    assertThat(preview.allResolved()).isFalse();
  }

  @Test
  void validate_marksNotFoundWhenProductIdDoesNotResolve() {
    when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());

    VoiceOrderItemExtraction item = itemWithPrice("un producto fantasma", 99L, new BigDecimal("10"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    // productId is echoed back even when invalid — so the mesero can see what the model guessed.
    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.NOT_FOUND);
    assertThat(preview.items().get(0).productId()).isEqualTo(99L);
    assertThat(preview.items().get(0).productName()).isNull();
  }

  // ── NOT_AVAILABLE ────────────────────────────────────────────────────────

  @Test
  void validate_marksNotAvailableWhenProductItselfIsDisabled() {
    Product product = trioMarino(false);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));

    VoiceOrderItemExtraction item = itemWithPrice("un trio marino de 25", 1L, new BigDecimal("25"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.NOT_AVAILABLE);
    assertThat(preview.items().get(0).productName()).isEqualTo("Trio Marino");
  }

  @Test
  void validate_marksNotAvailableWhenPriceMatchesDisabledVariant() {
    Product product = trioMarino(true);
    ProductVariant disabledVariant20 = variant(10L, product, "variante", new BigDecimal("20.00"), false);
    ProductVariant available25 = variant(11L, product, "variante", new BigDecimal("25.00"), true);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(disabledVariant20, available25));

    VoiceOrderItemExtraction item = itemWithPrice("un trio marino de 20", 1L, new BigDecimal("20"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.NOT_AVAILABLE);
  }

  @Test
  void validate_marksNotAvailableWhenVariantNameMatchesDisabledVariant() {
    Product product = trioMarino(true, new BigDecimal("20.00"));
    ProductVariant disabledMediano = variant(10L, product, "Mediano", new BigDecimal("25.00"), false);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(disabledMediano));

    VoiceOrderItemExtraction item = itemWithVariantName("un ceviche mediano", 1L, "mediano", 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.NOT_AVAILABLE);
  }

  // ── Multi-item / list-level behavior ────────────────────────────────────

  @Test
  void validate_mixedItemList_preservesOrderAndIsNotAllResolved() {
    // Mirrors the feature file's primary scenario: two items, one resolved, one not — the
    // mesero must see both, in order, and allResolved must be false so confirmation is blocked.
    Product trioMarino = trioMarino(true);
    ProductVariant marino25 = variant(10L, trioMarino, "variante", new BigDecimal("25.00"), true);

    Product trioChaufa = Product.builder()
      .id(2L).name("Trio Chaufa").price(BigDecimal.ZERO).isAvailable(true).build();

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(trioMarino));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(marino25));
    when(productRepository.findByIdWithCategory(2L)).thenReturn(Optional.of(trioChaufa));
    when(productVariantRepository.findByProductId(2L)).thenReturn(Collections.emptyList());
    when(tableRepository.findByNumber("8")).thenReturn(Optional.of(activeTable(100L, "8")));

    VoiceOrderItemExtraction resolvedItem = itemWithPrice("un trio marino de 25", 1L, new BigDecimal("25"), 1, "sin ají");
    VoiceOrderItemExtraction mismatchItem = itemWithPrice("un trio chaufa de 99", 2L, new BigDecimal("99"), 1, null);

    VoiceOrderPreview preview = validator.validate(
      dineInExtraction(Optional.of(8), List.of(resolvedItem, mismatchItem)));

    assertThat(preview.tableNumber()).isEqualTo(8);
    assertThat(preview.items()).hasSize(2);
    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.RESOLVED);
    assertThat(preview.items().get(0).notes()).isEqualTo("sin ají");
    assertThat(preview.items().get(1).status()).isEqualTo(VoiceOrderItemStatus.PRICE_MISMATCH);
    assertThat(preview.allResolved()).isFalse();
  }

  @Test
  void validate_allItemsResolved_allResolvedIsTrue() {
    Product trioMarino = trioMarino(true);
    ProductVariant marino25 = variant(10L, trioMarino, "variante", new BigDecimal("25.00"), true);

    Product trioChaufa = Product.builder()
      .id(2L).name("Trio Chaufa").price(BigDecimal.ZERO).isAvailable(true).build();
    ProductVariant chaufa30 = variant(20L, trioChaufa, "variante", new BigDecimal("30.00"), true);

    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(trioMarino));
    when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(marino25));
    when(productRepository.findByIdWithCategory(2L)).thenReturn(Optional.of(trioChaufa));
    when(productVariantRepository.findByProductId(2L)).thenReturn(List.of(chaufa30));
    when(tableRepository.findByNumber("8")).thenReturn(Optional.of(activeTable(100L, "8")));

    VoiceOrderItemExtraction item1 = itemWithPrice("un trio marino de 25", 1L, new BigDecimal("25"), 1, null);
    VoiceOrderItemExtraction item2 = itemWithPrice("un trio chaufa de 30", 2L, new BigDecimal("30"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.of(8), List.of(item1, item2)));

    assertThat(preview.allResolved()).isTrue();
  }

  @Test
  void validate_emptyItemList_stillRequiresTableToResolve() {
    // An empty item list has no unresolved ITEM, so items-only would vacuously say "resolved" —
    // but allResolved now also requires the table, so this must not be true just because there's
    // nothing to check on the item side. The "low-confidence audio" scenario (no items extracted
    // at all) is still fundamentally an extraction-layer concern, not this validator's.
    when(tableRepository.findByNumber("8")).thenReturn(Optional.of(activeTable(100L, "8")));

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.of(8), List.of()));

    assertThat(preview.items()).isEmpty();
    assertThat(preview.tableStatus()).isEqualTo(VoiceOrderTableStatus.RESOLVED);
    assertThat(preview.allResolved()).isTrue();
  }

  // ── Table resolution ─────────────────────────────────────────────────────

  @Test
  void validate_tableMissingWhenNoTableNumberDictated() {
    VoiceOrderItemExtraction item = itemWithNeither("una coca", null, 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.empty(), List.of(item)));

    assertThat(preview.tableStatus()).isEqualTo(VoiceOrderTableStatus.MISSING);
    assertThat(preview.tableId()).isNull();
    assertThat(preview.tableNumber()).isNull();
  }

  @Test
  void validate_tableNotFoundWhenNumberDoesNotMatchAnyTable() {
    when(tableRepository.findByNumber("99")).thenReturn(Optional.empty());

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.of(99), List.of()));

    assertThat(preview.tableStatus()).isEqualTo(VoiceOrderTableStatus.NOT_FOUND);
    assertThat(preview.tableId()).isNull();
    assertThat(preview.allResolved()).isFalse();
  }

  @Test
  void validate_tableNotFoundWhenTableIsInactive() {
    Table inactiveTable = Table.builder().id(100L).number("8").isActive(false).build();
    when(tableRepository.findByNumber("8")).thenReturn(Optional.of(inactiveTable));

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.of(8), List.of()));

    assertThat(preview.tableStatus()).isEqualTo(VoiceOrderTableStatus.NOT_FOUND);
    assertThat(preview.tableId()).isNull();
  }

  @Test
  void validate_allResolvedIsFalseWhenItemsResolveButTableDoesNot() {
    Product product = trioMarino(true, new BigDecimal("18.00"));
    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(tableRepository.findByNumber("99")).thenReturn(Optional.empty());

    VoiceOrderItemExtraction item = itemWithPrice("un trio marino de 18", 1L, new BigDecimal("18"), 1, null);

    VoiceOrderPreview preview = validator.validate(dineInExtraction(Optional.of(99), List.of(item)));

    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.RESOLVED);
    assertThat(preview.tableStatus()).isEqualTo(VoiceOrderTableStatus.NOT_FOUND);
    assertThat(preview.allResolved()).isFalse();
  }

  // ── Takeaway (whole order and per-item) ─────────────────────────────────

  @Test
  void validate_takeawayOrder_skipsTableResolutionAndMarksNotApplicable() {
    Product product = trioMarino(true, new BigDecimal("18.00"));
    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));

    VoiceOrderItemExtraction item = itemWithPrice("un trio marino de 18", 1L, new BigDecimal("18"), 1, null);

    VoiceOrderPreview preview = validator.validate(takeawayExtraction(List.of(item)));

    assertThat(preview.isTakeawayOrder()).isTrue();
    assertThat(preview.tableNumber()).isNull();
    assertThat(preview.tableId()).isNull();
    assertThat(preview.tableStatus()).isEqualTo(VoiceOrderTableStatus.NOT_APPLICABLE);
    assertThat(preview.allResolved()).isTrue();
    // Never stubbed tableRepository — proves table resolution is skipped entirely, not resolved
    // to some default.
  }

  @Test
  void validate_takeawayOrder_allResolvedIsFalseWhenAnItemDoesNotResolve() {
    VoiceOrderItemExtraction item = itemWithPrice("dos causas rellenas", null, new BigDecimal("15"), 2, null);

    VoiceOrderPreview preview = validator.validate(takeawayExtraction(List.of(item)));

    assertThat(preview.tableStatus()).isEqualTo(VoiceOrderTableStatus.NOT_APPLICABLE);
    assertThat(preview.items().get(0).status()).isEqualTo(VoiceOrderItemStatus.NOT_FOUND);
    assertThat(preview.allResolved()).isFalse();
  }

  @Test
  void validate_itemLevelTakeaway_passesThroughToPreviewItemWithoutAffectingTable() {
    Product product = trioMarino(true, new BigDecimal("18.00"));
    when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
    when(tableRepository.findByNumber("8")).thenReturn(Optional.of(activeTable(100L, "8")));

    VoiceOrderItemExtraction dineInItem = itemWithPrice("un trio marino de 18", 1L, new BigDecimal("18"), 1, null, false);
    VoiceOrderItemExtraction takeawayItem = itemWithPrice("una coca para llevar", 1L, new BigDecimal("18"), 1, null, true);

    VoiceOrderPreview preview = validator.validate(
      dineInExtraction(Optional.of(8), List.of(dineInItem, takeawayItem)));

    assertThat(preview.isTakeawayOrder()).isFalse();
    assertThat(preview.tableStatus()).isEqualTo(VoiceOrderTableStatus.RESOLVED);
    assertThat(preview.items().get(0).isTakeaway()).isFalse();
    assertThat(preview.items().get(1).isTakeaway()).isTrue();
  }
}
