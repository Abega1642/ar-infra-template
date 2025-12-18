package com.example.arInfra.conf;

import com.example.arInfra.InfraGenerated;
import lombok.SneakyThrows;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * Test configuration for S3-compatible storage container lifecycle management.
 *
 * <p>This configuration provides a LocalStack S3 container for integration testing, simulating
 * S3-compatible cloud storage (like Backblaze B2) without requiring actual cloud resources. It is
 * designed to be used as part of the {@link FacadeIT} base test class infrastructure, which
 * orchestrates multiple test containers for comprehensive integration testing.
 *
 * <p><b>Container configuration:</b>
 *
 * <ul>
 *   <li>Image: localstack/localstack:2.3.0
 *   <li>Service: S3 only (lightweight configuration)
 *   <li>Test bucket: "test-bucket" (auto-created on startup)
 *   <li>Path-style access: Enabled for compatibility
 * </ul>
 *
 * <p><b>Integration with FacadeIT:</b> <br>
 * This class is not used directly in test classes. Instead, extend {@link FacadeIT} which handles
 * the lifecycle of this and other containers:
 *
 * <pre>{@code
 * public class MyIntegrationTest extends FacadeIT {
 *   @Test
 *   void testFileUpload() {
 *     // LocalStack S3 is already running with test-bucket created
 *     // Test your file storage logic here
 *   }
 * }
 * }</pre>
 *
 * <p><b>Prerequisites:</b>
 *
 * <ul>
 *   <li>Docker must be installed and running on the test machine
 *   <li>Testcontainers dependency must be included in the project
 *   <li>LocalStack Testcontainers module must be included
 *   <li>Sufficient Docker resources to run LocalStack container
 * </ul>
 *
 * @see FacadeIT
 * @see org.testcontainers.containers.localstack.LocalStackContainer
 */
@InfraGenerated
@TestConfiguration
public class BucketConf {

  /**
   * LocalStack container configured with S3 service for simulating cloud storage. Uses LocalStack
   * 2.3.0 which provides S3-compatible API endpoints.
   */
  private static final LocalStackContainer LOCALSTACK =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.3.0"))
          .withServices(LocalStackContainer.Service.S3);

  /**
   * Name of the test bucket automatically created during container startup. This bucket is used for
   * all file storage operations during integration tests.
   */
  private static final String TEST_BUCKET = "test-bucket";

  /**
   * Starts the LocalStack S3 container and creates the test bucket if not already running.
   *
   * <p>This method is called by {@link FacadeIT#beforeAll()} during test infrastructure setup. It
   * is idempotent - calling it multiple times will not start multiple containers or create
   * duplicate buckets.
   *
   * <p>On first start, this method:
   *
   * <ol>
   *   <li>Starts the LocalStack container
   *   <li>Waits for the S3 service to be ready
   *   <li>Creates the "test-bucket" if it doesn't exist
   * </ol>
   *
   * <p>The container exposes S3 API on a random available port, which is then configured via {@link
   * #configureProperties(DynamicPropertyRegistry)} to inject into the Spring test context.
   */
  public void start() {
    if (!LOCALSTACK.isRunning()) {
      LOCALSTACK.start();
      createBucket();
    }
  }

  /**
   * Stops the LocalStack S3 container if it's running.
   *
   * <p>This method is called by the JVM shutdown hook registered in {@link FacadeIT#beforeAll()} to
   * ensure graceful cleanup of test resources. It is idempotent - calling it multiple times will
   * not cause errors.
   *
   * <p>The shutdown hook ensures containers are stopped even if tests fail or are interrupted.
   */
  public void stop() {
    if (LOCALSTACK.isRunning()) LOCALSTACK.stop();
  }

  /**
   * Creates the test bucket in LocalStack S3 if it doesn't already exist.
   *
   * <p>This method is called automatically during {@link #start()} to prepare the storage
   * infrastructure. It configures an S3 client with LocalStack credentials and endpoint, then
   * creates the test bucket with path-style access enabled for compatibility.
   *
   * <p>The method checks if the bucket already exists before attempting creation to ensure
   * idempotency.
   */
  @SneakyThrows
  private void createBucket() {
    S3Client s3Client =
        S3Client.builder()
            .endpointOverride(LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3))
            .region(Region.of(LOCALSTACK.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();

    if (s3Client.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(TEST_BUCKET)))
      s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build());
  }

  /**
   * Configures Spring Boot application properties to connect to the test LocalStack S3 container.
   *
   * <p>This method is called by {@link FacadeIT#configureProperties(DynamicPropertyRegistry)} to
   * dynamically register S3-compatible storage connection properties based on the running
   * container's actual endpoint, credentials, and configuration.
   *
   * <p><b>Properties configured:</b>
   *
   * <ul>
   *   <li>{@code b2.key.id} - LocalStack access key
   *   <li>{@code b2.application.key} - LocalStack secret key
   *   <li>{@code b2.bucket.name} - Test bucket name ("test-bucket")
   *   <li>{@code b2.region} - LocalStack region (typically "us-east-1")
   *   <li>{@code b2.endpoint.prefix} - Full LocalStack S3 endpoint URL
   *   <li>{@code b2.endpoint.suffix} - Empty (prefix contains full URL for LocalStack)
   *   <li>{@code b2.upload.part-size-mb} - Multipart upload part size (5 MB)
   *   <li>{@code b2.upload.target-throughput-gbps} - Target upload throughput (10 Gbps)
   * </ul>
   *
   * <p>These properties are automatically injected into the Spring test context, allowing the
   * application's {@link com.example.arInfra.conf.BucketConf} to connect to LocalStack instead of
   * real cloud storage during testing.
   *
   * @param registry the Spring DynamicPropertyRegistry to add properties to
   */
  @SneakyThrows
  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("b2.key.id", LOCALSTACK::getAccessKey);
    registry.add("b2.application.key", LOCALSTACK::getSecretKey);
    registry.add("b2.bucket.name", () -> TEST_BUCKET);
    registry.add("b2.region", LOCALSTACK::getRegion);

    registry.add(
        "b2.endpoint.prefix",
        () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    registry.add("b2.endpoint.suffix", () -> "");
    registry.add("b2.upload.part-size-mb", () -> 5);
    registry.add("b2.upload.target-throughput-gbps", () -> 10.0);
  }
}
