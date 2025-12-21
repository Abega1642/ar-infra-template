package com.example.arinfra.exception.health;

import lombok.Getter;

/**
 * Exception thrown when an email health check test case fails.
 *
 * <p>This exception wraps underlying failures during email testing operations, providing context
 * about which specific test case failed.
 */
@Getter
public class EmailHealthCheckException extends Exception {

  private final String testCaseName;

  public EmailHealthCheckException(String testCaseName, String message) {
    super(message);
    this.testCaseName = testCaseName;
  }

  public EmailHealthCheckException(String testCaseName, String message, Throwable cause) {
    super(message, cause);
    this.testCaseName = testCaseName;
  }

  public EmailHealthCheckException(String testCaseName, Throwable cause) {
    super("Email health check test case '" + testCaseName + "' failed", cause);
    this.testCaseName = testCaseName;
  }

  @Override
  public String getMessage() {
    return String.format("Test case '%s' failed: %s", testCaseName, super.getMessage());
  }
}
