package com.restaurant_management.restaurant_management_backend.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reads the real (blank-default) MinIO env vars directly, independent of {@link MinioConfig}'s
 * {@code MinioClient} bean — that bean always builds successfully (it falls back to an inert
 * placeholder endpoint when unconfigured, since {@code MinioClient.builder()} validates the
 * endpoint as a real URL and would throw on an empty string). This class is the single source
 * of truth for "is MinIO actually configured", checked by
 * {@link com.restaurant_management.restaurant_management_backend.menu.products.ProductImageStorageService}
 * before ever touching the client.
 */
@Component
public class MinioProperties {

  @Value("${application.minio.endpoint:}")
  private String endpoint;

  @Value("${application.minio.access-key:}")
  private String accessKey;

  @Value("${application.minio.secret-key:}")
  private String secretKey;

  @Value("${application.minio.bucket:product-images}")
  private String bucket;

  // Empty means "use endpoint itself" — set this separately only when the internally-reachable
  // MinIO endpoint (Docker network, localhost) differs from the publicly reachable one used to
  // build image URLs (a reverse-proxied domain).
  @Value("${application.minio.public-base-url:}")
  private String publicBaseUrl;

  public boolean isConfigured() {
    return !endpoint.isBlank() && !accessKey.isBlank() && !secretKey.isBlank();
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public String getBucket() {
    return bucket;
  }

  // MinioClient.builder().endpoint(String) tolerates a bare host (no scheme) and assumes one
  // internally — confirmed in prod: MINIO_ENDPOINT without "https://" still let uploads work.
  // But a public image URL built from that same bare value is a real bug, not a harmless
  // internal detail: a browser reads a scheme-less "host/path" as relative to the CURRENT page's
  // own origin, not as an external domain — every <img> silently 404s against the frontend's own
  // domain instead of MinIO. Never trust the operator to remember the scheme; guarantee one here.
  public String getPublicBaseUrl() {
    String base = publicBaseUrl.isBlank() ? endpoint : publicBaseUrl;
    return withScheme(base);
  }

  private String withScheme(String url) {
    if (url.isBlank() || url.startsWith("http://") || url.startsWith("https://")) {
      return url;
    }
    return "https://" + url;
  }
}
