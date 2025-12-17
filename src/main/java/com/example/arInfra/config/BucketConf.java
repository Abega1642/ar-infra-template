package com.example.arInfra.config;

import com.example.arInfra.InfraGenerated;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Spring configuration for S3-compatible cloud storage (Backblaze B2) integration.
 *
 * <p>This configuration class initializes and manages AWS S3 SDK clients for interacting with
 * Backblaze B2 or any S3-compatible storage service. It provides both a transfer manager for
 * efficient file uploads/downloads and a presigner for generating temporary signed URLs.
 *
 * <p><b>Required application properties:</b>
 *
 * <ul>
 *   <li>{@code b2.key.id} - Application key ID for authentication
 *   <li>{@code b2.application.key} - Application key secret for authentication
 *   <li>{@code b2.bucket.name} - Target bucket name
 *   <li>{@code b2.region} - Storage region (e.g., "us-west-001")
 *   <li>{@code b2.endpoint.prefix} - Endpoint URL prefix (e.g., "<a href="">https://s3</a>.")
 *   <li>{@code b2.endpoint.suffix} - Endpoint URL suffix (e.g., ".backblazeb2.com")
 * </ul>
 *
 * <p><b>Endpoint construction:</b>
 *
 * <ul>
 *   <li>Production: {@code prefix + region + suffix} (e.g.,
 *       "https://s3.us-west-001.backblazeb2.com")
 *   <li>Local development: Uses prefix only if it contains "localhost" or "127.0.0.1"
 * </ul>
 *
 * <p>The configuration automatically cleans up resources on application shutdown via the {@link
 * PreDestroy} lifecycle hook.
 */
@InfraGenerated
@Configuration
public class BucketConf {

  /** The name of the configured S3-compatible bucket. */
  @Getter private final String bucketName;

  /**
   * AWS S3 Transfer Manager for efficient multipart uploads and downloads. Handles large files
   * automatically with parallel transfers.
   */
  @Getter private final S3TransferManager s3TransferManager;

  /**
   * AWS S3 Presigner for generating temporary signed URLs. Allows clients to upload or download
   * files directly without proxy authentication.
   */
  @Getter private final S3Presigner s3Presigner;

  /**
   * Constructs and configures the S3-compatible storage clients.
   *
   * <p>Initializes the AWS S3 SDK with Backblaze B2 credentials and endpoint configuration. Creates
   * both an async transfer manager for file operations and a presigner for generating temporary
   * access URLs.
   *
   * <p><b>Endpoint resolution:</b> If the endpoint prefix contains "localhost" or "127.0.0.1" (for
   * local testing), uses the prefix as-is. Otherwise, constructs the full endpoint by concatenating
   * prefix + region + suffix.
   *
   * @param keyId the application key ID for B2 authentication
   * @param applicationKey the application key secret for B2 authentication
   * @param bucketName the target bucket name
   * @param regionString the storage region identifier
   * @param endpointPrefix the endpoint URL prefix (e.g., "<a href="">https://s3</a>.")
   * @param endpointSuffix the endpoint URL suffix (e.g., ".backblazeb2.com")
   */
  @SneakyThrows
  public BucketConf(
      @Value("${b2.key.id}") String keyId,
      @Value("${b2.application.key}") String applicationKey,
      @Value("${b2.bucket.name}") String bucketName,
      @Value("${b2.region}") String regionString,
      @Value("${b2.endpoint.prefix}") String endpointPrefix,
      @Value("${b2.endpoint.suffix}") String endpointSuffix) {
    this.bucketName = bucketName;
    String fullEndpoint =
        (endpointPrefix.contains("localhost") || endpointPrefix.contains("127.0.0.1"))
            ? endpointPrefix
            : endpointPrefix + regionString + endpointSuffix;
    URI endpoint = URI.create(fullEndpoint);

    Region region = Region.of(regionString);

    AwsCredentialsProvider credentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, applicationKey));

    S3AsyncClient s3AsyncClient =
        S3AsyncClient.builder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(credentialsProvider)
            .build();

    this.s3TransferManager = S3TransferManager.builder().s3Client(s3AsyncClient).build();

    this.s3Presigner =
        S3Presigner.builder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(credentialsProvider)
            .build();
  }

  /**
   * Cleans up S3 client resources on application shutdown.
   *
   * <p>This method is automatically invoked by Spring during application shutdown to properly close
   * the transfer manager and presigner, releasing any underlying network connections and thread
   * pools.
   *
   * <p>Ensures graceful shutdown and prevents resource leaks.
   */
  @PreDestroy
  public void cleanup() {
    if (s3TransferManager != null) s3TransferManager.close();

    if (s3Presigner != null) s3Presigner.close();
  }
}
