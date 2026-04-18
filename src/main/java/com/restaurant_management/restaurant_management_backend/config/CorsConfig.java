package com.restaurant_management.restaurant_management_backend.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Value("${frontend.url}")
  private String frontendUrl;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    if (frontendUrl == null || frontendUrl.isBlank()) {
      throw new IllegalStateException(
        "La variable de entorno FRONTEND_URL no está definida. " +
        "CORS no puede configurarse correctamente sin ella."
      );
    }

    List<String> allowedOrigins = Arrays.stream(frontendUrl.split(","))
      .map(String::trim)
      .filter(s -> !s.isBlank())
      .collect(Collectors.toList());

    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOriginPatterns(allowedOrigins);

    configuration.setAllowedMethods(Arrays.asList(
      "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    ));

    configuration.setAllowedHeaders(Arrays.asList(
      "Authorization",
      "Content-Type",
      "Accept",
      "X-Requested-With",
      "remember-me"
    ));

    configuration.setExposedHeaders(Arrays.asList(
      "Authorization",
      "Set-Cookie"
    ));

    configuration.setAllowCredentials(true);

    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

}
