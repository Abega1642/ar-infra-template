package com.example.arinfra.exception.bucket;

import com.example.arinfra.InfraGenerated;

@InfraGenerated
public class BucketHealthCheckException extends RuntimeException {

  public static final String ERROR_CODE = "BUCKET_HEALTH_CHECK_FAILED";

  public BucketHealthCheckException(String message, Throwable cause) {
    super(message, cause);
  }

  public BucketHealthCheckException(String message) {
    super(message);
  }

  public String getErrorCode() {
    return ERROR_CODE;
  }
}
