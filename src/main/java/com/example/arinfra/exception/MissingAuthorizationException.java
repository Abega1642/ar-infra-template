package com.example.arinfra.exception;

import com.example.arinfra.InfraGenerated;

@InfraGenerated
public class MissingAuthorizationException extends RuntimeException {
  public MissingAuthorizationException(String message) {
    super(message);
  }
}
