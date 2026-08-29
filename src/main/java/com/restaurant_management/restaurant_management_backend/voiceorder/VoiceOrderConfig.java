package com.restaurant_management.restaurant_management_backend.voiceorder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

/**
 * Wiring for the experimental voice-order extraction module. Isolated from the rest of the
 * application — no other package depends on beans defined here.
 *
 * <p>Two providers, two jobs, never mixed: OpenAI's Whisper/gpt-4o-mini-transcribe only turns
 * audio into text ({@link VoiceTranscriptionService}) — it never reasons about the order.
 * Anthropic's Claude only extracts structure from that text ({@link VoiceOrderExtractionService})
 * — it never sees audio, never decides a price.
 */
@Configuration
public class VoiceOrderConfig {

  @Bean
  public AnthropicClient anthropicClient(@Value("${application.anthropic.api-key}") String anthropicApiKey) {
    return AnthropicOkHttpClient.builder()
      .apiKey(anthropicApiKey)
      .build();
  }

  @Bean
  public OpenAIClient openAiClient(@Value("${application.openai.api-key}") String openAiApiKey) {
    return OpenAIOkHttpClient.builder()
      .apiKey(openAiApiKey)
      .build();
  }
}
