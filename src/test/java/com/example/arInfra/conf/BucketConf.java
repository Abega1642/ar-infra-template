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

@InfraGenerated
@TestConfiguration
public class BucketConf {
  private static final LocalStackContainer LOCALSTACK =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.3.0"))
          .withServices(LocalStackContainer.Service.S3);
  private static final String TEST_BUCKET = "test-bucket";

  public void start() {
    if (!LOCALSTACK.isRunning()) {
      LOCALSTACK.start();
      createBucket();
    }
  }

  public void stop() {
    if (LOCALSTACK.isRunning()) {
      LOCALSTACK.stop();
    }
  }

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

    if (s3Client.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(TEST_BUCKET))) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build());
    }
  }

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
