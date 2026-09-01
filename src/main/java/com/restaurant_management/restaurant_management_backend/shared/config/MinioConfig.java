package com.restaurant_management.restaurant_management_backend.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;

/**
 * Always builds a {@code MinioClient} successfully, even when MinIO isn't configured — unlike
 * OpenAI's key (a plain string the SDK never validates at build time), {@code MinioClient.builder()}
 * parses the endpoint as a real URL and throws on a blank one. Falling back to an inert
 * placeholder here keeps app startup unaffected by MinIO configuration, matching this app's
 * established "optional third-party integration" posture (see {@code VoiceOrderConfig}'s
 * OpenAI client for the same idea applied differently).
 *
 * <p>The placeholder client is never actually called: {@link MinioProperties#isConfigured()}
 * gates every real usage in {@code ProductImageStorageService}.
 */
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

  private static final String PLACEHOLDER_ENDPOINT = "http://localhost:9000";
  private static final String PLACEHOLDER_CREDENTIAL = "unconfigured";

  private final MinioProperties minioProperties;

  @Bean
  public MinioClient minioClient() {
    boolean configured = minioProperties.isConfigured();

    return MinioClient.builder()
      .endpoint(configured ? minioProperties.getEndpoint() : PLACEHOLDER_ENDPOINT)
      .credentials(
        configured ? minioProperties.getAccessKey() : PLACEHOLDER_CREDENTIAL,
        configured ? minioProperties.getSecretKey() : PLACEHOLDER_CREDENTIAL
      )
      .build();
  }
}
