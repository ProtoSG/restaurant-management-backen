package com.restaurant_management.restaurant_management_backend.voiceorder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

/**
 * Wiring for the experimental voice-order extraction module. Isolated from the rest of the
 * application — no other package depends on beans defined here.
 */
@Configuration
public class VoiceOrderConfig {

  @Bean
  public AnthropicClient anthropicClient(@Value("${application.anthropic.api-key}") String anthropicApiKey) {
    return AnthropicOkHttpClient.builder()
      .apiKey(anthropicApiKey)
      .build();
  }
}
