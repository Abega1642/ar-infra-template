package com.example.arInfra.mail;

import static org.owasp.encoder.Encode.forJava;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.config.EmailConf;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Email delivery component responsible for sending emails through JavaMailSender.
 *
 * <p>This component implements {@link Consumer} to accept {@link Email} objects and send them
 * asynchronously. It handles various email features including:
 *
 * <ul>
 *   <li>HTML and plain text content
 *   <li>CC and BCC recipients
 *   <li>File attachments with error tolerance
 *   <li>Comprehensive error logging with injection protection
 * </ul>
 *
 * <p>Failed email deliveries are logged but do not throw exceptions, making this suitable for
 * fire-and-forget email operations.
 *
 * @see Email
 * @see JavaMailSender
 */
@Component
@Slf4j
@InfraGenerated
@RequiredArgsConstructor
public class Mailer implements Consumer<Email> {

  private static final String DEFAULT_EMAIL_BODY = "(no content — Unfaked health check)";
  private static final String CHARSET = StandardCharsets.UTF_8.name();

  private final JavaMailSender mailSender;
  private final EmailConf emailConf;

  /**
   * Accepts and sends an email. Validates the email before sending and logs any failures.
   *
   * @param email the email to send, must not be null and must have a recipient
   */
  @Override
  public void accept(Email email) {
    if (!isValidEmail(email)) {
      log.warn("Invalid email object. Skipping send.");
      return;
    }

    try {
      send(email);
    } catch (Exception e) {
      logError(email.to(), e);
    }
  }

  /**
   * Validates that the email object and its required fields are present.
   *
   * @param email the email to validate
   * @return true if the email is valid, false otherwise
   */
  private boolean isValidEmail(Email email) {
    return email != null && email.to() != null;
  }

  /**
   * Sends the email by creating and configuring a MIME message.
   *
   * @param email the email to send
   * @throws MessagingException if message creation or sending fails
   */
  private void send(Email email) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = createMessageHelper(message);

    configureBasicFields(helper, email);
    configureRecipients(helper, email);
    configureContent(helper, email);
    configureAttachments(helper, email);

    mailSender.send(message);
    logSuccess(email.to());
  }

  /**
   * Creates a MimeMessageHelper with multipart support and UTF-8 encoding.
   *
   * @param message the MIME message to wrap
   * @return configured MimeMessageHelper
   * @throws MessagingException if helper creation fails
   */
  private MimeMessageHelper createMessageHelper(MimeMessage message) throws MessagingException {
    return new MimeMessageHelper(message, true, CHARSET);
  }

  /** Configures basic email fields: from, to, and subject. */
  private void configureBasicFields(MimeMessageHelper helper, Email email)
      throws MessagingException {
    helper.setFrom(emailConf.getFromEmail());
    helper.setTo(email.to().getAddress());
    helper.setSubject(email.subject());
  }

  /** Configures CC and BCC recipients if present. */
  private void configureRecipients(MimeMessageHelper helper, Email email)
      throws MessagingException {
    if (hasRecipients(email.cc())) helper.setCc(toAddressArray(email.cc()));

    if (hasRecipients(email.bcc())) helper.setBcc(toAddressArray(email.bcc()));
  }

  /** Configures email content, preferring HTML over plain text. */
  private void configureContent(MimeMessageHelper helper, Email email) throws MessagingException {
    if (hasContent(email.htmlBody())) helper.setText(email.htmlBody(), true);
    else helper.setText(DEFAULT_EMAIL_BODY, false);
  }

  /** Attaches files to the email, logging warnings for individual attachment failures. */
  private void configureAttachments(MimeMessageHelper helper, Email email) {
    if (email.attachments() == null || email.attachments().isEmpty()) return;

    email.attachments().forEach(file -> attachFile(helper, file));
  }

  /** Attempts to attach a single file, logging any failure without throwing an exception. */
  private void attachFile(MimeMessageHelper helper, File file) {
    try {
      helper.addAttachment(file.getName(), file);
    } catch (Exception e) {
      log.warn(
          "Failed to attach file: filename={}, error={}",
          forJava(file.getName()),
          forJava(e.getMessage()));
    }
  }

  /** Checks if a list of recipients is present and non-empty. */
  private boolean hasRecipients(List<InternetAddress> recipients) {
    return recipients != null && !recipients.isEmpty();
  }

  /** Checks if content string is present and non-empty. */
  private boolean hasContent(String content) {
    return content != null && !content.isEmpty();
  }

  /** Converts a list of InternetAddress to an array of email address strings. */
  private String[] toAddressArray(List<InternetAddress> addresses) {
    return addresses.stream().map(InternetAddress::getAddress).toArray(String[]::new);
  }

  /** Logs successful email delivery with injection-safe logging. */
  private void logSuccess(InternetAddress recipient) {
    log.info("Email sent successfully to {}", forJava(recipient.getAddress()));
  }

  /** Logs email delivery errors with injection-safe logging. */
  private void logError(InternetAddress recipient, Exception e) {
    log.error(
        "{} to {}: {}",
        "Failed to send email",
        forJava(recipient.getAddress()),
        forJava(e.getMessage()),
        e);
  }
}
