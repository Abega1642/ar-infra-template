package com.example.arinfra.config;

import static java.time.Duration.ofSeconds;

import com.example.arinfra.InfraGenerated;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
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
 *   <li>{@code cloud.storage.key.id} - Application key ID for authentication
 *   <li>{@code cloud.storage.application.key} - Application key secret for authentication
 *   <li>{@code cloud.storage.bucket.name} - Target bucket name
 *   <li>{@code cloud.storage.region} - Storage region (e.g., "us-west-006")
 *   <li>{@code cloud.storage.full-endpoint} - Endpoint URL prefix (e.g., " href=""><a
 *       href="https://s3.us-west-006.backblaze.com">...</a></a>.")
 * </ul>
 *
 * <p><b>Checksum configuration:</b> {@code RequestChecksumCalculation.WHEN_REQUIRED} and {@code
 * ResponseChecksumValidation.WHEN_REQUIRED} are set on both {@link S3Client} and {@link
 * <p>S3AsyncClient}, replacing the deprecated {@code S3Configuration#checksumValidationEnabled}.
 *
 * <p>Since AWS SDK 2.30.0 the default request checksum behavior is {@code WHEN_SUPPORTED}, which
 *
 * <p>attaches a CRC32 trailer to requests. Third-party S3-compatible backends, including some B2
 *
 * <p>deployments, do not all support that trailer and can reject the request. {@code
 * <p>WHEN_REQUIRED} restricts checksum calculation to operations that mandate it, matching AWS's
 *
 * <p>own guidance for non-AWS S3-compatible endpoints.
 *
 * <p>The configuration automatically cleans up resources on application shutdown via the {@link
 * PreDestroy} lifecycle hook, including the underlying {@link S3AsyncClient} used by the transfer
 * manager, which the transfer manager does not close on its own.
 *
 * @see <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/s3-checksums.html">
 *     <p>AWS SDK for Java 2.x - Data integrity protection with checksums</a>
 * @see <a href="https://cwe.mitre.org/data/definitions/772.html">CWE-772</a>
 */
@InfraGenerated
@Configuration
public class BucketConf {

  private static final Duration API_CALL_TIMEOUT = ofSeconds(5L);
  private static final Duration API_CALL_ATTEMPT_TIMEOUT = ofSeconds(5L);

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

  @Getter private final S3Client s3Client;

  /**
   * The async client backing {@link #s3TransferManager}. Not exposed via {@code @Getter}: it
   *
   * <p>exists only to be closed alongside the transfer manager, which does not close it on its
   *
   * <p>own. See {@link #cleanup()}.
   */
  private final S3AsyncClient s3AsyncClient;

  /**
   * Constructs and configures the S3-compatible storage clients.
   *
   * <p>Initializes the AWS S3 SDK with Backblaze B2 credentials and endpoint configuration. Creates
   * both an async transfer manager for file operations and a presigner for generating temporary
   * access URLs.
   *
   * @param keyId the application key ID for B2 authentication
   * @param applicationKey the application key secret for B2 authentication
   * @param bucketName the target bucket name
   * @param regionString the storage region identifier
   * @param fullEndpoint the endpoint URL (e.g., "" href=""><a
   *     href="https://s3.us-west-006.backblaze.com">...</a></a>.")
   */
  public BucketConf(
      @Value("${cloud.storage.key.id}") String keyId,
      @Value("${cloud.storage.application.key}") String applicationKey,
      @Value("${cloud.storage.bucket.name}") String bucketName,
      @Value("${cloud.storage.region}") String regionString,
      @Value("${cloud.storage.full-endpoint}") String fullEndpoint) {
    this.bucketName = bucketName;
    URI endpoint = URI.create(fullEndpoint);

    Region region = Region.of(regionString);

    final AwsCredentialsProvider credentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, applicationKey));

    final S3Configuration s3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(true).build();

    final ClientOverrideConfiguration overrideConfiguration =
        ClientOverrideConfiguration.builder()
            .apiCallTimeout(API_CALL_TIMEOUT)
            .apiCallAttemptTimeout(API_CALL_ATTEMPT_TIMEOUT)
            .build();

    this.s3Client =
        S3Client.builder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(s3Configuration)
            .overrideConfiguration(overrideConfiguration)
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .build();

    this.s3AsyncClient =
        S3AsyncClient.builder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(s3Configuration)
            .overrideConfiguration(overrideConfiguration)
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .build();

    this.s3TransferManager = S3TransferManager.builder().s3Client(s3AsyncClient).build();

    this.s3Presigner =
        S3Presigner.builder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(s3Configuration)
            .build();
  }

  /**
   * Cleans up S3 client resources on application shutdown.
   *
   * <p>This method is automatically invoked by Spring during application shutdown to properly close
   * the transfer manager, the async client backing it, the presigner, and the synchronous client,
   * releasing any underlying network connections and thread pools.
   *
   * <p>{@link S3TransferManager#close()} does not close the {@link S3AsyncClient} it was built
   *
   * <p>with, per the AWS SDK javadoc, so {@link #s3AsyncClient} is closed explicitly here.
   *
   * <p>Ensures graceful shutdown and prevents resource leaks.
   */
  @PreDestroy
  public void cleanup() {
    s3TransferManager.close();
    s3AsyncClient.close();
    s3Presigner.close();
    s3Client.close();
  }
}
