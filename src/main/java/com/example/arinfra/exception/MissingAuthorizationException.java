package com.example.arinfra.exception;

public class MissingAuthorizationException extends RuntimeException {
  public MissingAuthorizationException(String message) {
    super(message);
  }
}
