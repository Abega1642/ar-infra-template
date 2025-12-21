package com.example.arinfra.exception;

public class DirectoryUploadException extends RuntimeException {
  public DirectoryUploadException(String message) {
    super(message);
  }

  public DirectoryUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}
