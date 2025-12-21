package com.example.arInfra.validator.file;

import static java.lang.String.format;
import static org.springframework.util.unit.DataSize.ofBytes;
import static org.springframework.util.unit.DataSize.ofMegabytes;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.validator.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Validator for multipart file upload configuration properties.
 *
 * <p>This validator ensures that file upload limits are explicitly configured and within acceptable
 * security bounds, preventing DoS attacks through unrestricted file uploads.
 *
 * <p>Security compliance:
 *
 * <ul>
 *   <li>OWASP - Top 10 2021 Category A5 - Security Misconfiguration
 *   <li>CWE-770 - Allocation of Resources Without Limits or Throttling
 *   <li>CWE-400 - Uncontrolled Resource Consumption
 * </ul>
 *
 * <p>Validation rules:
 *
 * <ul>
 *   <li>Max file size must be explicitly configured (not default/unlimited)
 *   <li>Max request size must be explicitly configured
 *   <li>Values must not exceed absolute maximum (100MB)
 *   <li>Warns if values exceed OWASP recommendation (8MB)
 * </ul>
 */
@Slf4j
@Component
@Validated
@InfraGenerated
public class MultipartPropertiesValidator implements Validator<MultipartProperties> {

  private static final DataSize RECOMMENDED_MAX_SIZE = ofMegabytes(8);
  private static final DataSize ABSOLUTE_MAX_SIZE = ofMegabytes(100);
  private static final DataSize ZERO = ofBytes(0);

  @Override
  public void validate(@NotNull MultipartProperties properties) {
    validateMaxFileSize(properties.getMaxFileSize());
    validateMaxRequestSize(properties.getMaxRequestSize());

    log.info(
        "Multipart configuration validated - maxFileSize: {}, maxRequestSize: {}",
        properties.getMaxFileSize(),
        properties.getMaxRequestSize());
  }

  @Override
  public Class<MultipartProperties> getValidatedType() {
    return MultipartProperties.class;
  }

  private void validateMaxFileSize(DataSize maxFileSize) {
    if (maxFileSize == null || maxFileSize.compareTo(ZERO) <= 0)
      throw new SecurityException(
          format(
              "%s %s",
              "Multipart max-file-size must be explicitly configured for security.",
              "Set spring.servlet.multipart.max-file-size in application.yml"));

    if (maxFileSize.compareTo(ABSOLUTE_MAX_SIZE) > 0)
      throw new SecurityException(
          format(
              "Multipart max-file-size (%s) exceeds absolute maximum (%s). "
                  + "This poses a serious DoS risk.",
              maxFileSize, ABSOLUTE_MAX_SIZE));

    if (maxFileSize.compareTo(RECOMMENDED_MAX_SIZE) > 0)
      log.warn(
          "Multipart max-file-size ({}) exceeds OWASP recommendation of {}. "
              + "Ensure this is required for your use case.",
          maxFileSize,
          RECOMMENDED_MAX_SIZE);
  }

  private void validateMaxRequestSize(DataSize maxRequestSize) {
    if (maxRequestSize == null || maxRequestSize.compareTo(ZERO) <= 0)
      throw new SecurityException(
          format(
              "%s %s",
              "Multipart max-request-size must be explicitly configured for security.",
              "Set spring.servlet.multipart.max-request-size in application.yml"));

    if (maxRequestSize.compareTo(ABSOLUTE_MAX_SIZE) > 0)
      throw new SecurityException(
          format(
              "Multipart max-request-size (%s) exceeds absolute maximum (%s). "
                  + "This poses a serious DoS risk.",
              maxRequestSize, ABSOLUTE_MAX_SIZE));

    if (maxRequestSize.compareTo(RECOMMENDED_MAX_SIZE) > 0)
      log.warn(
          "Multipart max-request-size ({}) exceeds OWASP recommendation of {}. "
              + "Ensure this is required for your use case.",
          maxRequestSize,
          RECOMMENDED_MAX_SIZE);
  }
}
