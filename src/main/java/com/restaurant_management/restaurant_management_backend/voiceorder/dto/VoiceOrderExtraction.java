package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * The full structured extraction the LLM produces from one dictated order. Extraction only —
 * never authoritative, never written to the database. Must pass through
 * {@link com.restaurant_management.restaurant_management_backend.voiceorder.VoiceOrderValidator}
 * and human confirmation before it can become a real order.
 */
@JsonClassDescription("Structured extraction of a dictated restaurant order")
public record VoiceOrderExtraction(

  @JsonPropertyDescription("The table number mentioned in the dictated text. Empty if no table was mentioned, OR if isTakeawayOrder is true (a takeaway order never has a table).")
  Optional<Integer> tableNumber,

  @JsonPropertyDescription("True ONLY when the WHOLE order is for takeaway with no table at all (e.g. 'para llevar, dos trio marisco', nothing about a mesa). False when a table is mentioned — if a mesero says 'para llevar' for one item inside a table order, that is VoiceOrderItemExtraction.isTakeaway on that item, not this field.")
  Boolean isTakeawayOrder,

  @JsonPropertyDescription("The list of items dictated, one entry per distinct item (with its resolved quantity, not repeated lines)")
  List<VoiceOrderItemExtraction> items

) {}
