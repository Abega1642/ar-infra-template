package com.example.arinfra.endpoint.rest.controller;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.endpoint.rest.controller.model.ErrorResponse;
import com.example.arinfra.exception.MissingAuthorizationException;
import com.example.arinfra.exception.bucket.BucketHealthCheckException;
import com.example.arinfra.exception.bucket.BucketOperationException;
import com.example.arinfra.exception.bucket.DirectoryUploadException;
import com.example.arinfra.exception.health.EmailHealthCheckException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Global exception handler for REST API endpoints.
 *
 * <p>This controller advice intercepts exceptions thrown by REST controllers and converts them into
 * consistent, user-friendly error responses. All user-controlled input is sanitized through the
 * ErrorResponse class to prevent XSS vulnerabilities.
 *
 * <p>Handles various exception types including validation errors, authentication/authorization
 * failures, data integrity violations, and application-specific business exceptions.
 *
 * <p>All exceptions are logged with appropriate severity levels for monitoring and debugging.
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
@InfraGenerated
public class ApiExceptionHandler {

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex, WebRequest request) {

    log.warn(
        "Missing required parameter '{}' at path: {}",
        forJava(ex.getParameterName()),
        forJava(getRequestPath(request)));

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            format("Required parameter '%s' is missing", ex.getParameterName()),
            getRequestPath(request),
            "MISSING_REQUIRED_PARAMETER");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BucketHealthCheckException.class)
  public ResponseEntity<ErrorResponse> handleBucketHealthCheckException(
      BucketHealthCheckException ex, WebRequest request) {

    log.error(
        "Bucket health check failed at path: {}, error code: {}",
        forJava(getRequestPath(request)),
        forJava(ex.getErrorCode()),
        ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.SERVICE_UNAVAILABLE,
            ex.getMessage(),
            getRequestPath(request),
            ex.getErrorCode());

    return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(EmailHealthCheckException.class)
  public ResponseEntity<ErrorResponse> handleEmailHealthCheckException(
      EmailHealthCheckException ex, WebRequest request) {

    log.error(
        "Email health check failed at path: {}, test case: {}",
        forJava(getRequestPath(request)),
        forJava(ex.getTestCaseName()),
        ex);

    String errorMessage =
        format(
            "Email health check failed at test case '%s': %s",
            ex.getTestCaseName(),
            ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            errorMessage,
            getRequestPath(request),
            "EMAIL_HEALTH_CHECK_FAILED");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(BucketOperationException.class)
  public ResponseEntity<ErrorResponse> handleBucketOperationException(
      BucketOperationException ex, WebRequest request) {

    log.error(
        "Bucket operation failed at path: {}, error code: {}",
        forJava(getRequestPath(request)),
        forJava(ex.getErrorCode()),
        ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            getRequestPath(request),
            ex.getErrorCode());

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(
      MissingServletRequestPartException ex, WebRequest request) {

    log.warn(
        "Missing required request part '{}' at path: {}",
        forJava(ex.getRequestPartName()),
        forJava(getRequestPath(request)));

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            "Required part '" + ex.getRequestPartName() + "' is not present",
            getRequestPath(request),
            "MISSING_REQUIRED_PART");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {

    String message =
        ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining(", "));

    log.warn(
        "Constraint violation at path: {}, violations: {}",
        forJava(getRequestPath(request)),
        forJava(message));

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST, message, getRequestPath(request), "INVALID_PARAMETER");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, WebRequest request) {

    log.warn("Malformed JSON request at path: {}", forJava(getRequestPath(request)), ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            "Malformed JSON request",
            getRequestPath(request),
            "MALFORMED_JSON");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, WebRequest request) {

    log.warn(
        "Unsupported HTTP method '{}' at path: {}",
        forJava(ex.getMethod()),
        forJava(getRequestPath(request)));

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.METHOD_NOT_ALLOWED,
            "HTTP method not supported for this endpoint",
            getRequestPath(request),
            "METHOD_NOT_ALLOWED");

    return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(
      AuthorizationDeniedException ex, WebRequest request) {

    log.warn(
        "Authorization denied at path: {}, reason: {}",
        forJava(getRequestPath(request)),
        forJava(ex.getMessage()));

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.FORBIDDEN, ex.getMessage(), getRequestPath(request), "AUTHORIZATION_DENIED");
    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, WebRequest request) {

    String userMessage = "A conflict occurred with existing data";
    String errorCode = "DATA_CONFLICT";

    Throwable rootCause = ex.getRootCause();

    if (rootCause != null) {
      String errorMessage = rootCause.getMessage().toLowerCase();

      if (errorMessage.contains("email")
          && (errorMessage.contains("unique") || errorMessage.contains("duplicate"))) {
        userMessage = "Email already exists";
        errorCode = "DUPLICATE_EMAIL";
      } else if (errorMessage.contains("clerk_id")
          || errorMessage.contains("clerk")
              && (errorMessage.contains("unique") || errorMessage.contains("duplicate"))) {
        userMessage = "Clerk ID already exists";
        errorCode = "DUPLICATE_CLERK_ID";
      }
    }

    log.warn(
        "Data integrity violation at path: {}, error code: {}, root cause: {}",
        forJava(getRequestPath(request)),
        forJava(errorCode),
        forJava(rootCause != null ? rootCause.getMessage() : "unknown"));

    var errorResponse =
        ErrorResponse.of(HttpStatus.CONFLICT, userMessage, getRequestPath(request), errorCode);

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(DirectoryUploadException.class)
  public ResponseEntity<ErrorResponse> handleDirectoryUploadException(
      DirectoryUploadException ex, WebRequest request) {

    log.error(
        "Directory upload failed at path: {}, reason: {}",
        forJava(getRequestPath(request)),
        forJava(ex.getMessage()),
        ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            getRequestPath(request),
            "DIRECTORY_UPLOAD_FAILED");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
      EntityNotFoundException ex, WebRequest request) {

    log.warn(
        "Entity not found at path: {}, reason: {}",
        forJava(getRequestPath(request)),
        forJava(ex.getMessage()));

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.NOT_FOUND, ex.getMessage(), getRequestPath(request), "ENTITY_NOT_FOUND");
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, WebRequest request) {

    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("Validation failed");

    log.warn(
        "Validation failed at path: {}, message: {}",
        forJava(getRequestPath(request)),
        forJava(message));

    var errorResponse =
        ErrorResponse.of(HttpStatus.BAD_REQUEST, message, getRequestPath(request), "BAD_FORM");

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(MissingAuthorizationException.class)
  public ResponseEntity<ErrorResponse> handleMissingAuthorization(
      MissingAuthorizationException ex, WebRequest request) {

    log.warn(
        "Missing authorization at path: {}, reason: {}",
        forJava(getRequestPath(request)),
        forJava(ex.getMessage()));

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage(),
            getRequestPath(request),
            "MISSING_AUTHORIZATION");
    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
    log.error(
        "Unexpected error at path: {}, exception type: {}, message: {}",
        forJava(getRequestPath(request)),
        forJava(ex.getClass().getName()),
        forJava(ex.getMessage()),
        ex);

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An internal server error occurred",
            getRequestPath(request),
            "INTERNAL_SERVER_ERROR");
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex, WebRequest request) {

    log.warn(
        "Handler method validation failed at path: {}, message: {}",
        forJava(getRequestPath(request)),
        forJava(ex.getMessage()));

    var errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST, ex.getMessage(), getRequestPath(request), "VALIDATION_ERROR");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  private String getRequestPath(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest)
      return servletWebRequest.getRequest().getRequestURI();

    return "N/A";
  }
}
