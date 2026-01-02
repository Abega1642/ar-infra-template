package com.example.arinfra.exception;

import com.example.arinfra.InfraGenerated;

@InfraGenerated
public class EmailSendException extends RuntimeException {
  public EmailSendException(String message) {
    super(message);
  }

  public EmailSendException(String message, Throwable cause) {
    super(message, cause);
  }
}
