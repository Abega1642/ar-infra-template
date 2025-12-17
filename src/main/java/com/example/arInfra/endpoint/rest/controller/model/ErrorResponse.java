package com.example.arInfra.endpoint.rest.controller.model;

import static java.time.LocalDateTime.now;
import static org.owasp.encoder.Encode.forJava;

import com.example.arInfra.InfraGenerated;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

/**
 * Standard error response structure for API exceptions.
 *
 * <p>This record provides a consistent format for error responses across the application, including
 * timestamp, HTTP status information, error messages, request path, and optional error codes for
 * programmatic error handling.
 *
 * <p>All user-controlled input is automatically sanitized to prevent XSS attacks using OWASP
 * Encoder library, which provides context-appropriate encoding before being included in the
 * response.
 *
 * @param timestamp The time when the error occurred
 * @param status The HTTP status code
 * @param error The HTTP status reason phrase
 * @param message A descriptive error message (sanitized)
 * @param path The request path where the error occurred (sanitized)
 * @param errorCode An optional application-specific error code for client-side error handling
 */
@InfraGenerated
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    String errorCode) {

  /**
   * Creates an ErrorResponse without a custom error code. All string parameters are sanitized to
   * prevent XSS attacks.
   *
   * @param status The HTTP status
   * @param message The error message (will be sanitized)
   * @param path The request path (will be sanitized)
   * @return A new ErrorResponse instance with sanitized inputs
   */
  public static ErrorResponse of(HttpStatus status, String message, String path) {
    return new ErrorResponse(
        now(), status.value(), status.getReasonPhrase(), sanitize(message), sanitize(path), null);
  }

  /**
   * Creates an ErrorResponse with a custom error code. All string parameters are sanitized to
   * prevent XSS attacks.
   *
   * @param status The HTTP status
   * @param message The error message (will be sanitized)
   * @param path The request path (will be sanitized)
   * @param errorCode The application-specific error code (will be sanitized)
   * @return A new ErrorResponse instance with sanitized inputs
   */
  public static ErrorResponse of(HttpStatus status, String message, String path, String errorCode) {
    return new ErrorResponse(
        now(),
        status.value(),
        status.getReasonPhrase(),
        sanitize(message),
        sanitize(path),
        sanitize(errorCode));
  }

  /**
   * Sanitizes input strings using OWASP Encoder to prevent XSS attacks. Uses forJava() encoding
   * which is appropriate for JSON string values in REST APIs.
   *
   * @param input The string to sanitize
   * @return The sanitized string, or null if input is null
   */
  private static String sanitize(String input) {
    return input != null ? forJava(input) : null;
  }
}
