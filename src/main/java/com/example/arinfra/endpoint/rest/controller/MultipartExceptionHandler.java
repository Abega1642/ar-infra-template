package com.example.arinfra.endpoint.rest.controller;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * Global exception handler for multipart file upload errors.
 *
 * <p>Provides secure error handling without exposing sensitive system information.
 */
@Slf4j
@InfraGenerated
@RestControllerAdvice
public class MultipartExceptionHandler {

  @Value("${spring.servlet.multipart.max-file-size:10MB}")
  private String maxFileSize;

  @Value("${spring.servlet.multipart.max-request-size:10MB}")
  private String maxRequestSize;

  /**
   * Handles file size exceeded exceptions with user-friendly messages.
   *
   * @param ex the exception
   * @return problem detail response
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
    log.warn("File upload size limit exceeded", ex);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.PAYLOAD_TOO_LARGE,
            format(
                "Upload size exceeds the maximum allowed. Max file size: %s, Max request size: %s",
                forJava(maxFileSize), forJava(maxRequestSize)));

    problemDetail.setTitle("File Size Limit Exceeded");
    problemDetail.setProperty("maxFileSize", maxFileSize);
    problemDetail.setProperty("maxRequestSize", maxRequestSize);

    return problemDetail;
  }

  /**
   * Handles general multipart exceptions.
   *
   * @param ex the exception
   * @return problem detail response
   */
  @ExceptionHandler(MultipartException.class)
  public ProblemDetail handleMultipartException(MultipartException ex) {
    log.error("Multipart request processing failed", ex);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Failed to process multipart request. Please ensure your file meets the requirements.");

    problemDetail.setTitle("Multipart Request Error");

    return problemDetail;
  }
}
