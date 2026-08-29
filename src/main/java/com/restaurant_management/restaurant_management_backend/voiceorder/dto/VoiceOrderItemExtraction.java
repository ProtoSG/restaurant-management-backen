package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import java.math.BigDecimal;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * One item as extracted by the LLM from the dictated order — its best-effort guess only,
 * never authoritative. Every field here is subject to deterministic validation
 * ({@link com.restaurant_management.restaurant_management_backend.voiceorder.VoiceOrderValidator})
 * before it can be trusted.
 *
 * <p><b>productId / selectedPrice / variantName are {@code Optional<T>}, not the bare type.</b>
 * Verified empirically (2026-08-29) against the installed anthropic-java 2.34.0 jar: a bare
 * {@code Long}/{@code BigDecimal} field is derived into a JSON schema with
 * {@code "type": "integer"}/{@code "number"} and listed in {@code required} — the model is then
 * contractually unable to emit {@code null}, so when it genuinely has nothing to put (no product
 * match, or no price/variant was spoken) it fabricates a placeholder number instead (observed:
 * a hallucinated id outside the real range, and once literally {@code -1E-999}). Wrapping in
 * {@code Optional<T>} changes the derived type to {@code ["integer", "null"]} (still required as
 * a key, but the value itself may genuinely be {@code null}) — confirmed by dumping the real
 * {@code JsonOutputFormat} the SDK builds, not assumed.
 */
@JsonClassDescription("One order item as heard in the dictated text — a best-effort guess, not a validated fact")
public record VoiceOrderItemExtraction(

  @JsonPropertyDescription("The exact phrase this item came from, e.g. 'un trio marino de 25' — for audit/debug")
  String rawText,

  @JsonPropertyDescription("The catalog product id this item most likely refers to. Empty if you are not confident which catalog product was meant — never guess.")
  Optional<Long> productId,

  @JsonPropertyDescription("The price exactly as the mesero said it, in soles, ONLY if a number was actually spoken. Empty if no price number was said — do not invent one, and do not fill in what you think the price should be.")
  Optional<BigDecimal> selectedPrice,

  @JsonPropertyDescription("The variant name exactly as spoken, ONLY if the mesero named a variant by its label instead of a price (e.g. 'mediano', 'grande', 'pescado'). Empty if no variant label was said. Never put a price here, and never put this label in selectedPrice.")
  Optional<String> variantName,

  @JsonPropertyDescription("How many units of this item were ordered, resolved from the phrase (e.g. 'dos trio chaufa' = 2)")
  Integer quantity,

  @JsonPropertyDescription("Any note attached to this item, including notes referring to it by position (e.g. 'el primero sin ají')")
  String notes,

  @JsonPropertyDescription("True ONLY if the mesero said THIS SPECIFIC item is for takeaway (e.g. 'el primero para llevar', 'una coca para llevar'), while the rest of the order stays dine-in. False by default — do NOT set this true just because the whole order is a takeaway order (see VoiceOrderExtraction.isTakeawayOrder for that case instead).")
  Boolean isTakeaway

) {}
