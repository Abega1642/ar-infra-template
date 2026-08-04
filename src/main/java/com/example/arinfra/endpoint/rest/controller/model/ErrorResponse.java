package com.example.arinfra.endpoint.rest.controller.model;

import static java.time.LocalDateTime.now;

import com.example.arinfra.InfraGenerated;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

/**
 * Standard error response structure for API exceptions.
 *
 * <p>This record provides a consistent format for error responses across the application, including
 * timestamp, HTTP status information, error messages, request path, and optional error codes for
 * programmatic error handling.
 *
 * <p>String fields are not pre-encoded here. Jackson escapes quotes, backslashes, and control
 *
 * <p>characters per RFC 8259 when serializing this record to the response body, which is the
 *
 * <p>correct and sufficient control for valid, safe JSON output. An earlier version additionally
 *
 * <p>ran every field through {@code Encode.forJava()} to prevent XSS. That method is documented by
 *
 * <p>OWASP as a Java string-literal encoder for code generators and debug output, not an
 *
 * <p>HTML-context encoder: it does not escape {@code <}, {@code >}, or {@code &}, so it gave no
 *
 * <p>XSS protection, and because it also escapes {@code "} and {@code \}, running it ahead of
 *
 * <p>Jackson's own escaping double-encoded any message containing those characters, corrupting the
 *
 * <p>text delivered to the client. XSS defense for a value that ends up rendered as HTML belongs at
 *
 * <p>the point of DOM insertion on the consuming side, not in this JSON payload.
 *
 * @param timestamp The time when the error occurred
 * @param status The HTTP status code
 * @param error The HTTP status reason phrase
 * @param message A descriptive error message
 * @param path The request path where the error occurred
 * @param errorCode An optional application-specific error code for client-side error handling
 * @see <a href="https://cwe.mitre.org/data/definitions/116.html">CWE-116</a>
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
   * Creates an ErrorResponse without a custom error code.
   *
   * @param status The HTTP status
   * @param message The error message
   * @param path The request path
   * @return A new ErrorResponse instance
   */
  public static ErrorResponse of(HttpStatus status, String message, String path) {
    return new ErrorResponse(now(), status.value(), status.getReasonPhrase(), message, path, null);
  }

  /**
   * Creates an ErrorResponse with a custom error code.
   *
   * @param status The HTTP status
   * @param message The error message
   * @param path The request path
   * @param errorCode The application-specific error code
   * @return A new ErrorResponse instance
   */
  public static ErrorResponse of(HttpStatus status, String message, String path, String errorCode) {
    return new ErrorResponse(
        now(), status.value(), status.getReasonPhrase(), message, path, errorCode);
  }
}
