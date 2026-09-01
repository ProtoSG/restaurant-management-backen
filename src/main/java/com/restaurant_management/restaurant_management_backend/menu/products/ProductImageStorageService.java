package com.restaurant_management.restaurant_management_backend.menu.products;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.restaurant_management.restaurant_management_backend.shared.config.MinioProperties;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.BadRequestException;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;

/**
 * Uploads/removes product photos in MinIO (S3-compatible). The only place in the app that talks
 * to MinIO directly — {@link ProductServiceImpl} calls this, never the client itself.
 *
 * <p>Every public method checks {@link MinioProperties#isConfigured()} first and rejects with a
 * clear {@link BadRequestException} instead of letting {@link MinioClient} fail against the
 * inert placeholder endpoint it falls back to when unconfigured (see {@code MinioConfig}) —
 * that failure would otherwise surface as a confusing connection-refused error, not "this
 * feature isn't set up yet".
 */
@Service
@RequiredArgsConstructor
public class ProductImageStorageService {

  // Menu photos, not user uploads at large — no reason to accept anything heavier, and it
  // keeps the tablet's grid view fast to load over a restaurant's own wifi.
  private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

  // Standard S3-style anonymous-read policy, scoped to this one bucket — photos are meant to be
  // publicly loadable by <img src>, nothing else in the bucket is sensitive.
  private static final String PUBLIC_READ_POLICY_TEMPLATE = """
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Effect": "Allow",
          "Principal": {"AWS": ["*"]},
          "Action": ["s3:GetObject"],
          "Resource": ["arn:aws:s3:::%s/*"]
        }
      ]
    }
    """;

  private final MinioClient minioClient;
  private final MinioProperties minioProperties;

  public String upload(Long productId, MultipartFile file) {
    requireConfigured();
    validate(file);
    ensureBucketExists();

    String objectKey = "products/%d/%s%s".formatted(productId, UUID.randomUUID(), extensionFor(file.getContentType()));

    try (InputStream in = file.getInputStream()) {
      minioClient.putObject(PutObjectArgs.builder()
        .bucket(minioProperties.getBucket())
        .object(objectKey)
        .stream(in, file.getSize(), -1)
        .contentType(file.getContentType())
        .build());
    } catch (Exception e) {
      throw new RuntimeException("No se pudo subir la imagen — intentá de nuevo", e);
    }

    return publicUrlFor(objectKey);
  }

  /** No-op (not an error) when the URL isn't one this service manages, or MinIO isn't configured
   * — deleting a product should never fail just because its photo couldn't be cleaned up. */
  public void deleteIfManaged(String imageUrl) {
    if (imageUrl == null || !minioProperties.isConfigured()) return;

    String prefix = minioProperties.getPublicBaseUrl() + "/" + minioProperties.getBucket() + "/";
    if (!imageUrl.startsWith(prefix)) return;

    String objectKey = imageUrl.substring(prefix.length());
    try {
      minioClient.removeObject(RemoveObjectArgs.builder()
        .bucket(minioProperties.getBucket())
        .object(objectKey)
        .build());
    } catch (Exception e) {
      // Best-effort — an orphaned object in the bucket is a minor cleanup issue, not worth
      // failing the caller's request (product update/delete) over.
    }
  }

  private void requireConfigured() {
    if (!minioProperties.isConfigured()) {
      throw new BadRequestException("El almacenamiento de imágenes no está configurado todavía");
    }
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("La imagen es obligatoria");
    }
    if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
      throw new BadRequestException("La imagen no puede pesar más de 5MB");
    }
    if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
      throw new BadRequestException("Formato de imagen no soportado — usá JPG, PNG o WEBP");
    }
  }

  private void ensureBucketExists() {
    try {
      String bucket = minioProperties.getBucket();
      boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (!exists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
          .bucket(bucket)
          .config(PUBLIC_READ_POLICY_TEMPLATE.formatted(bucket))
          .build());
      }
    } catch (Exception e) {
      throw new RuntimeException("No se pudo preparar el almacenamiento de imágenes", e);
    }
  }

  private String publicUrlFor(String objectKey) {
    return minioProperties.getPublicBaseUrl() + "/" + minioProperties.getBucket() + "/" + objectKey;
  }

  private String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> ".jpg";
    };
  }
}
