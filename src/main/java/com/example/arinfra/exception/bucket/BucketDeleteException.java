package com.example.arinfra.exception.bucket;

public class BucketDeleteException extends RuntimeException {
  public BucketDeleteException(String message) {
    super(message);
  }

  public BucketDeleteException(String message, Throwable cause) {
    super(message, cause);
  }
}
