package com.example.arinfra.config;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.validator.file.MultipartPropertiesValidator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes and validates multipart file upload configuration at application startup.
 *
 * <p>This configuration class ensures that multipart upload limits are validated before the
 * application accepts any requests, providing fail-fast behavior for security misconfigurations.
 */
@Configuration
@InfraGenerated
@RequiredArgsConstructor
public class MultipartConfigurationInitializer {

  private final MultipartProperties multipartProperties;
  private final MultipartPropertiesValidator multipartPropertiesValidator;

  /**
   * Validates multipart configuration at application startup.
   *
   * <p>If validation fails, the application will not start, ensuring that insecure configurations
   * are caught immediately rather than at runtime.
   *
   * @throws SecurityException if multipart configuration is insecure or missing
   */
  @PostConstruct
  public void validateMultipartConfiguration() {
    multipartPropertiesValidator.validate(multipartProperties);
  }
}
