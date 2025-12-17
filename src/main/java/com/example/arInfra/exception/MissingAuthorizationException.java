package com.example.arInfra.exception;

public class MissingAuthorizationException extends RuntimeException {
  public MissingAuthorizationException(String message) {
    super(message);
  }
}
