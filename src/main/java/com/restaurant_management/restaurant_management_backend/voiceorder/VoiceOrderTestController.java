package com.restaurant_management.restaurant_management_backend.voiceorder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderResponse;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderCapabilities;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderConfirmRequest;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderExtraction;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderPreview;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderTestRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Experimental, ADMIN-only endpoints for the voice-order extraction module. Extracts + validates
 * only — never writes to the database, never touches the real orders flow. Not wired into the
 * frontend; exists to exercise {@link VoiceTranscriptionService}, {@link VoiceOrderExtractionService},
 * and {@link VoiceOrderValidator} end to end.
 */
@RestController
@RequestMapping("/voice-order-test")
@RequiredArgsConstructor
public class VoiceOrderTestController {

  // Empty when OPENAI_API_KEY isn't set (Whisper is a pending decision, not yet committed) —
  // see application-dev.yml. /capabilities lets a client know whether to offer the audio path
  // or fall back to native device dictation + the text-only path, without guessing or probing.
  @Value("${application.openai.api-key:}")
  private String openAiApiKey;

  private final VoiceTranscriptionService voiceTranscriptionService;
  private final VoiceOrderExtractionService voiceOrderExtractionService;
  private final VoiceOrderValidator voiceOrderValidator;
  private final VoiceOrderConfirmService voiceOrderConfirmService;

  /**
   * Tells the client which input path to offer. The backend can't trigger native device
   * dictation itself — that's entirely client-side — this just reports whether Whisper is
   * actually configured so the client doesn't have to guess or fail an upload to find out.
   */
  @GetMapping("/capabilities")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<VoiceOrderCapabilities> capabilities() {
    return ResponseEntity.ok(new VoiceOrderCapabilities(!openAiApiKey.isBlank()));
  }

  /**
   * Text-only path — bypasses transcription. Used directly for testing extraction/validation in
   * isolation, and it's also where the client sends text from native device dictation when
   * Whisper isn't configured (see {@link #capabilities()}).
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<VoiceOrderPreview> extract(@RequestBody @Valid VoiceOrderTestRequest request) {
    return ResponseEntity.ok(extractAndValidate(request.text()));
  }

  /** Full path — audio in, validated preview out. Max upload size: 10MB (application.yml). */
  @PostMapping("/audio")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<VoiceOrderPreview> extractFromAudio(@RequestParam("audio") MultipartFile audio) {
    String dictatedText = voiceTranscriptionService.transcribe(audio);
    return ResponseEntity.ok(extractAndValidate(dictatedText));
  }

  /**
   * Turns a mesero-confirmed preview into a real order — the only write in this whole module.
   * Everything is re-validated server-side from scratch (see {@link VoiceOrderConfirmService}),
   * never trusting the submitted payload just because it looks like a preview this backend
   * itself produced moments earlier.
   */
  @PostMapping("/confirm")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<OrderResponse> confirm(@RequestBody @Valid VoiceOrderConfirmRequest request) {
    return ResponseEntity.ok(voiceOrderConfirmService.confirm(request));
  }

  private VoiceOrderPreview extractAndValidate(String dictatedText) {
    VoiceOrderExtraction extraction = voiceOrderExtractionService.extract(dictatedText);
    return voiceOrderValidator.validate(extraction);
  }
}
