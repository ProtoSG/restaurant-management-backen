package com.restaurant_management.restaurant_management_backend.menu.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.restaurant_management.restaurant_management_backend.shared.config.MinioProperties;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

@ExtendWith(MockitoExtension.class)
class ProductImageStorageServiceTest {

  @Mock MinioClient minioClient;
  @Mock MinioProperties minioProperties;

  @InjectMocks
  ProductImageStorageService storageService;

  private static MultipartFile jpegFile(long sizeBytes) {
    byte[] content = new byte[(int) sizeBytes];
    return new MockMultipartFile("image", "plato.jpg", "image/jpeg", content);
  }

  // ── upload ───────────────────────────────────────────────────────────────────

  @Test
  void upload_throwsBadRequest_whenMinioNotConfigured() {
    when(minioProperties.isConfigured()).thenReturn(false);

    assertThatThrownBy(() -> storageService.upload(1L, jpegFile(100)))
      .isInstanceOf(BadRequestException.class);

    verifyNoMinioClientInteractions();
  }

  @Test
  void upload_throwsBadRequest_whenFileIsEmpty() {
    when(minioProperties.isConfigured()).thenReturn(true);
    MultipartFile empty = new MockMultipartFile("image", "plato.jpg", "image/jpeg", new byte[0]);

    assertThatThrownBy(() -> storageService.upload(1L, empty))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  void upload_throwsBadRequest_whenFileTooLarge() {
    when(minioProperties.isConfigured()).thenReturn(true);

    assertThatThrownBy(() -> storageService.upload(1L, jpegFile(6L * 1024 * 1024)))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("5MB");
  }

  @Test
  void upload_throwsBadRequest_whenContentTypeNotSupported() {
    when(minioProperties.isConfigured()).thenReturn(true);
    MultipartFile pdf = new MockMultipartFile("image", "menu.pdf", "application/pdf", new byte[]{1, 2, 3});

    assertThatThrownBy(() -> storageService.upload(1L, pdf))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  void upload_returnsPublicUrl_whenValidAndBucketAlreadyExists() throws Exception {
    when(minioProperties.isConfigured()).thenReturn(true);
    when(minioProperties.getBucket()).thenReturn("product-images");
    when(minioProperties.getPublicBaseUrl()).thenReturn("http://minio.local:9000");
    when(minioClient.bucketExists(any())).thenReturn(true);

    String url = storageService.upload(7L, jpegFile(100));

    assertThat(url).startsWith("http://minio.local:9000/product-images/products/7/");
    assertThat(url).endsWith(".jpg");
    verify(minioClient).putObject(any(PutObjectArgs.class));
    // Bucket already existed — never re-created, never re-policied.
    verify(minioClient, never()).makeBucket(any());
  }

  // ── deleteIfManaged ──────────────────────────────────────────────────────────

  @Test
  void deleteIfManaged_doesNothing_whenUrlIsNull() throws Exception {
    storageService.deleteIfManaged(null);

    verifyNoMinioClientInteractions();
  }

  @Test
  void deleteIfManaged_doesNothing_whenMinioNotConfigured() throws Exception {
    when(minioProperties.isConfigured()).thenReturn(false);

    storageService.deleteIfManaged("http://minio.local:9000/product-images/products/1/x.jpg");

    verifyNoMinioClientInteractions();
  }

  @Test
  void deleteIfManaged_doesNothing_whenUrlIsNotFromThisBucket() throws Exception {
    when(minioProperties.isConfigured()).thenReturn(true);
    when(minioProperties.getPublicBaseUrl()).thenReturn("http://minio.local:9000");
    when(minioProperties.getBucket()).thenReturn("product-images");

    // URL de una foto externa pegada a mano, no gestionada por este servicio — no debe intentar
    // borrar nada fuera de su propio prefijo.
    storageService.deleteIfManaged("https://otra-cosa.com/foto.jpg");

    verify(minioClient, never()).removeObject(any());
  }

  @Test
  void deleteIfManaged_removesObject_whenUrlMatchesManagedPrefix() throws Exception {
    when(minioProperties.isConfigured()).thenReturn(true);
    when(minioProperties.getPublicBaseUrl()).thenReturn("http://minio.local:9000");
    when(minioProperties.getBucket()).thenReturn("product-images");

    storageService.deleteIfManaged("http://minio.local:9000/product-images/products/7/old.jpg");

    verify(minioClient).removeObject(any(RemoveObjectArgs.class));
  }

  private void verifyNoMinioClientInteractions() {
    org.mockito.Mockito.verifyNoInteractions(minioClient);
  }
}
