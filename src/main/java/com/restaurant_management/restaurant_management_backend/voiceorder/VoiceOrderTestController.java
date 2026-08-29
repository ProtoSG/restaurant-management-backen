package com.restaurant_management.restaurant_management_backend.voiceorder;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderExtraction;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderPreview;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderTestRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Experimental, ADMIN-only endpoint for the voice-order extraction module. Extracts + validates
 * only — never writes to the database, never touches the real orders flow. Not wired into the
 * frontend; exists to exercise {@link VoiceOrderExtractionService} and {@link VoiceOrderValidator}
 * end to end.
 */
@RestController
@RequestMapping("/voice-order-test")
@RequiredArgsConstructor
public class VoiceOrderTestController {

  private final VoiceOrderExtractionService voiceOrderExtractionService;
  private final VoiceOrderValidator voiceOrderValidator;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<VoiceOrderPreview> extract(@RequestBody @Valid VoiceOrderTestRequest request) {
    VoiceOrderExtraction extraction = voiceOrderExtractionService.extract(request.text());
    VoiceOrderPreview preview = voiceOrderValidator.validate(extraction);

    return ResponseEntity.ok(preview);
  }
}
