package com.example.arInfra.service.health;

import static org.owasp.encoder.Encode.forJava;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.exception.health.EmailHealthCheckException;
import com.example.arInfra.file.SecureTempFileManager;
import com.example.arInfra.mail.Email;
import com.example.arInfra.mail.Mailer;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@InfraGenerated
public class HealthEmailService {

  private static final String HEALTH_CHECK_PREFIX = "[unfaked health check";
  private static final String TEST_ATTACHMENT_PREFIX = "test-attachment-";
  private static final String TEST_ATTACHMENT_SUFFIX = ".txt";

  private final Mailer mailer;
  private final SecureTempFileManager secureTempFileManager;

  /**
   * Sends a comprehensive set of test emails to verify email functionality.
   *
   * @param recipientEmail the email address to send test emails to
   * @throws AddressException if the email address is invalid
   * @throws EmailHealthCheckException if any test case fails
   */
  public void sendHealthCheckEmails(String recipientEmail)
      throws AddressException, EmailHealthCheckException {
    log.info("Starting email health check for: {}", forJava(recipientEmail));

    InternetAddress toAddress = validateAndParseEmail(recipientEmail);
    EmailComponents emailComponents = parseEmailComponents(recipientEmail);

    List<EmailTestCase> testCases = buildTestCases(toAddress, emailComponents);
    executeTestCases(testCases);

    log.info("Email health check completed successfully for: {}", forJava(recipientEmail));
  }

  private List<EmailTestCase> buildTestCases(
      InternetAddress toAddress, EmailComponents components) {
    return List.of(
        new EmailTestCase("subject-only", () -> sendSubjectOnlyEmail(toAddress)),
        new EmailTestCase("with-cc", () -> sendEmailWithCc(toAddress, components)),
        new EmailTestCase("with-bcc", () -> sendEmailWithBcc(toAddress, components)),
        new EmailTestCase("with-body", () -> sendEmailWithBody(toAddress)),
        new EmailTestCase("with-attachment", () -> sendEmailWithAttachment(toAddress)));
  }

  private void executeTestCases(List<EmailTestCase> testCases) throws EmailHealthCheckException {
    for (EmailTestCase testCase : testCases) {
      try {
        testCase.execute();
      } catch (EmailHealthCheckException e) {
        log.error("Email health check test failed: {}", testCase.name(), e);
        throw e;
      }
    }
  }

  private InternetAddress validateAndParseEmail(String email) throws AddressException {
    InternetAddress address = new InternetAddress(email);
    address.validate();
    return address;
  }

  private EmailComponents parseEmailComponents(String email) {
    int lastAtIndex = email.lastIndexOf('@');
    if (lastAtIndex <= 0 || lastAtIndex == email.length() - 1) {
      throw new IllegalArgumentException("Invalid email format: " + email);
    }

    String localPart = email.substring(0, lastAtIndex);
    String domain = "@" + email.substring(lastAtIndex + 1);

    return new EmailComponents(localPart, domain);
  }

  private void sendSubjectOnlyEmail(InternetAddress toAddress) {
    mailer.accept(createEmail(toAddress, null, null, "1/5] Subject only", null, List.of()));
    log.debug("Sent subject-only test email");
  }

  private void sendEmailWithCc(InternetAddress toAddress, EmailComponents components)
      throws EmailHealthCheckException {
    try {
      InternetAddress ccAddress =
          new InternetAddress(components.localPart() + "+cc" + components.domain());
      mailer.accept(
          createEmail(toAddress, List.of(ccAddress), null, "2/5] With cc", null, List.of()));
      log.debug("Sent test email with CC");
    } catch (AddressException e) {
      throw new EmailHealthCheckException("with-cc", "Failed to create CC address", e);
    }
  }

  private void sendEmailWithBcc(InternetAddress toAddress, EmailComponents components)
      throws EmailHealthCheckException {
    try {
      InternetAddress bccAddress =
          new InternetAddress(components.localPart() + "+bcc" + components.domain());
      mailer.accept(
          createEmail(toAddress, null, List.of(bccAddress), "3/5] With bcc", null, List.of()));
      log.debug("Sent test email with BCC");
    } catch (AddressException e) {
      throw new EmailHealthCheckException("with-bcc", "Failed to create BCC address", e);
    }
  }

  private void sendEmailWithBody(InternetAddress toAddress) {
    String htmlBody =
        """
        <div>
            <h1>Hello from Unfaked!</h1>
            <p>This is a <b>test email</b> with HTML content.</p>
        </div>
        """;
    mailer.accept(createEmail(toAddress, null, null, "4/5] With body", htmlBody, List.of()));
    log.debug("Sent test email with HTML body");
  }

  private void sendEmailWithAttachment(InternetAddress toAddress) throws EmailHealthCheckException {
    String attachmentContent =
        String.format(
            "This is a test attachment from Unfaked.%nTimestamp: %d", System.currentTimeMillis());

    File attachment = null;
    try {
      attachment =
          secureTempFileManager.createSecureTempFileWithContent(
              TEST_ATTACHMENT_PREFIX, TEST_ATTACHMENT_SUFFIX, attachmentContent);

      mailer.accept(
          createEmail(
              toAddress,
              null,
              null,
              "5/5] With attachment",
              "<p>This email has an attachment</p>",
              List.of(attachment)));
      log.debug("Sent test email with attachment");
    } catch (IOException e) {
      throw new EmailHealthCheckException(
          "with-attachment", "Failed to create or send attachment", e);
    } finally {
      if (attachment != null) {
        secureTempFileManager.deleteTempFile(attachment);
      }
    }
  }

  private Email createEmail(
      InternetAddress to,
      List<InternetAddress> cc,
      List<InternetAddress> bcc,
      String subjectSuffix,
      String body,
      List<File> attachments) {
    return new Email(
        to,
        cc != null ? cc : List.of(),
        bcc != null ? bcc : List.of(),
        HEALTH_CHECK_PREFIX + " " + subjectSuffix,
        body,
        attachments);
  }

  /**
   * Functional interface for executing email test cases.
   *
   * @throws EmailHealthCheckException if the test case execution fails
   */
  @FunctionalInterface
  private interface TestCaseExecutor {
    void execute() throws EmailHealthCheckException;
  }

  /** Record to hold parsed email components. */
  private record EmailComponents(String localPart, String domain) {}

  /** Record to encapsulate a test case with its name and execution logic. */
  private record EmailTestCase(String name, TestCaseExecutor executor) {
    void execute() throws EmailHealthCheckException {
      executor.execute();
    }
  }
}
