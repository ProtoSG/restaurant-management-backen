package com.restaurant_management.restaurant_management_backend.voiceorder;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.openai.client.OpenAIClient;
import com.openai.models.audio.AudioModel;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse;

import lombok.RequiredArgsConstructor;

/**
 * Turns a dictated audio clip into text. Nothing else — no order-domain reasoning happens here,
 * that's {@link VoiceOrderExtractionService}'s job, using Claude on the transcribed text this
 * service returns. Two providers, two single-purpose jobs, never merged into one call.
 *
 * <p>Model: {@code gpt-4o-mini-transcribe} — OpenAI's current recommendation over
 * {@code gpt-4o-transcribe} for accuracy, and well ahead of the legacy {@code whisper-1} on cost.
 * Chosen for robustness to background noise (kitchen environment) over the browser's Web Speech
 * API, and for the simple single-request file-upload shape — this flow doesn't need real-time
 * streaming transcription (the mesero records a full clip, then sends it, like a voice note).
 *
 * <p>Verified against the real installed {@code openai-java-core}/{@code openai-java-client-okhttp}
 * 4.54.0 jars via {@code javap} (2026-08-29), not assumed from docs: {@code TranscriptionCreateParams
 * .Builder} accepts {@code file(InputStream)} directly (no temp file needed for a multipart
 * upload), and {@code create(...)} returns a {@code TranscriptionCreateResponse} union type —
 * {@code .asTranscription().text()} unwraps it for the default (non-verbose, non-diarized)
 * response format this service requests.
 */
@Service
@RequiredArgsConstructor
public class VoiceTranscriptionService {

  private final OpenAIClient openAiClient;

  public String transcribe(MultipartFile audio) {
    try (InputStream audioStream = audio.getInputStream()) {
      TranscriptionCreateParams params = TranscriptionCreateParams.builder()
        .file(audioStream)
        .model(AudioModel.GPT_4O_MINI_TRANSCRIBE)
        .build();

      TranscriptionCreateResponse response = openAiClient.audio().transcriptions().create(params);

      return response.asTranscription().text();
    } catch (IOException e) {
      throw new IllegalStateException("Could not read the uploaded audio file", e);
    }
  }
}
