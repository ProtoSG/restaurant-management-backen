package com.restaurant_management.restaurant_management_backend.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regression test for a real prod bug: an operator set MINIO_ENDPOINT without a scheme
 * ("minio.example.com" instead of "https://minio.example.com"). MinioClient.builder() tolerates
 * that internally (uploads worked), but a public image URL built from the same bare value made
 * every &lt;img&gt; in the browser resolve as relative to the frontend's own origin instead of
 * MinIO — silent 404s, no error anywhere. getPublicBaseUrl() must guarantee a scheme regardless
 * of what the operator actually set.
 */
class MinioPropertiesTest {

  private MinioProperties propertiesWith(String endpoint, String publicBaseUrl) {
    MinioProperties props = new MinioProperties();
    ReflectionTestUtils.setField(props, "endpoint", endpoint);
    ReflectionTestUtils.setField(props, "accessKey", "any");
    ReflectionTestUtils.setField(props, "secretKey", "any");
    ReflectionTestUtils.setField(props, "publicBaseUrl", publicBaseUrl);
    return props;
  }

  @Test
  void getPublicBaseUrl_addsHttpsScheme_whenEndpointHasNone() {
    MinioProperties props = propertiesWith("minio.example.com", "");

    assertThat(props.getPublicBaseUrl()).isEqualTo("https://minio.example.com");
  }

  @Test
  void getPublicBaseUrl_keepsHttpsScheme_whenAlreadyPresent() {
    MinioProperties props = propertiesWith("https://minio.example.com", "");

    assertThat(props.getPublicBaseUrl()).isEqualTo("https://minio.example.com");
  }

  @Test
  void getPublicBaseUrl_keepsHttpScheme_whenAlreadyPresent() {
    // Local dev docker-compose.dev.yml's MinIO is plain http — must not be forced to https.
    MinioProperties props = propertiesWith("http://localhost:9010", "");

    assertThat(props.getPublicBaseUrl()).isEqualTo("http://localhost:9010");
  }

  @Test
  void getPublicBaseUrl_addsScheme_toExplicitPublicBaseUrlToo_whenBare() {
    MinioProperties props = propertiesWith("http://minio-internal:9000", "cdn.example.com");

    assertThat(props.getPublicBaseUrl()).isEqualTo("https://cdn.example.com");
  }

  @Test
  void getPublicBaseUrl_fallsBackToEndpoint_whenPublicBaseUrlBlank() {
    MinioProperties props = propertiesWith("https://minio.example.com", "");

    assertThat(props.getPublicBaseUrl()).isEqualTo("https://minio.example.com");
  }
}
