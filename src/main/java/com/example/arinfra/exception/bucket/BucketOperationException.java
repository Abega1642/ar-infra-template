package com.example.arinfra.exception.bucket;

/**
 * Base exception for all bucket-related failures.
 *
 * <p>This exception is meant to be translated by a {@link
 * org.springframework.web.bind.annotation.ControllerAdvice} into a structured error response.
 */
public abstract class BucketOperationException extends RuntimeException {

  protected BucketOperationException(String message, Throwable cause) {
    super(message, cause);
  }

  protected BucketOperationException(String message) {
    super(message);
  }

  /**
   * @return application-specific error code
   */
  public abstract String getErrorCode();
}
