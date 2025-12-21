package com.example.arinfra.endpoint.rest.controller.health;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.exception.health.EmailHealthCheckException;
import com.example.arinfra.service.health.HealthEmailService;
import jakarta.mail.internet.AddressException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for email health check operations.
 *
 * <p>Provides endpoints to verify email functionality by sending test emails.
 */
@InfraGenerated
@Slf4j
@RestController
@AllArgsConstructor
public class HealthEmailController {

  private final HealthEmailService healthEmailService;

  /**
   * Sends a series of test emails to verify email functionality.
   *
   * <p>Sends 5 different types of test emails:
   *
   * <ol>
   *   <li>Subject only
   *   <li>With CC
   *   <li>With BCC
   *   <li>With HTML body
   *   <li>With attachment
   * </ol>
   *
   * @param to the recipient email address for the health check
   * @return ResponseEntity with status message
   */
  @GetMapping("/health/email")
  public ResponseEntity<String> sendHealthCheckEmails(@RequestParam String to) {
    try {
      healthEmailService.sendHealthCheckEmails(to);
      String message = format("All 5 test emails sent successfully to %s", forJava(to));
      return ResponseEntity.ok(message);

    } catch (AddressException e) {
      log.error("Invalid email address provided: {}", forJava(to), e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid email address: " + forJava(to));

    } catch (EmailHealthCheckException e) {
      log.error(
          "Email health check failed for {}: test case '{}' failed",
          forJava(to),
          forJava(e.getTestCaseName()),
          e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              format(
                  "Email health check failed at test case '%s': %s",
                  forJava(e.getTestCaseName()),
                  forJava(e.getCause() != null ? e.getCause().getMessage() : e.getMessage())));

    } catch (Exception e) {
      log.error("Unexpected error during email health check for: {}", forJava(to), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to send emails: " + forJava(e.getMessage()));
    }
  }
}
