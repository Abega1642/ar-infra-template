package com.example.arInfra.endpoint.rest.controller.health;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.exception.bucket.BucketHealthCheckException;
import com.example.arInfra.service.health.HealthBucketService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for cloud storage bucket health check operations.
 *
 * <p>Provides endpoints to verify cloud storage (S3, GCS, etc.) connectivity and access permissions
 * by performing basic bucket operations.
 */
@InfraGenerated
@RestController
@AllArgsConstructor
public class HealthBucketController {

  private final HealthBucketService healthBucketService;

  /**
   * Checks cloud storage bucket health and accessibility.
   *
   * <p>This endpoint performs operations on the configured cloud storage bucket to verify
   * connectivity, authentication, and basic read/write permissions.
   *
   * @return ResponseEntity containing the health check results as a string
   * @throws BucketHealthCheckException if the bucket health check fails
   */
  @GetMapping("/health/bucket")
  public ResponseEntity<String> checkBucketHealth() {
    return ResponseEntity.ok(healthBucketService.performHealthCheck().toString());
  }
}
