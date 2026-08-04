package com.example.arinfra.mail;

import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.config.EmailConf;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
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

  private static final String DEFAULT_EMAIL_BODY = "(no content — Infra health check)";
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
    if (email == null || email.to() == null) {
      log.warn("Invalid email object. Skipping send.");
      return;
    }

    try {
      send(email);
    } catch (Exception e) {
      log.error(
          "{} to {}: {}",
          "Failed to send email",
          forJava(email.to().getAddress()),
          forJava(e.getMessage()),
          e);
    }
  }

  /**
   * Sends the email by creating and configuring a MIME message.
   *
   * @param email the email to send
   * @throws MessagingException if message creation or sending fails
   */
  private void send(Email email) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, CHARSET);

    helper.setFrom(emailConf.getFromEmail());
    helper.setTo(email.to().getAddress());
    helper.setSubject(email.subject());

    if (email.cc() != null && !email.cc().isEmpty())
      helper.setCc(email.cc().stream().map(InternetAddress::getAddress).toArray(String[]::new));

    if (email.bcc() != null && !email.bcc().isEmpty())
      helper.setBcc(email.bcc().stream().map(InternetAddress::getAddress).toArray(String[]::new));

    if (email.htmlBody() != null && !email.htmlBody().isEmpty())
      helper.setText(email.htmlBody(), true);
    else helper.setText(DEFAULT_EMAIL_BODY, false);

    if (email.attachments() == null || email.attachments().isEmpty()) return;

    email
        .attachments()
        .forEach(
            file -> {
              try {
                helper.addAttachment(file.getName(), file);
              } catch (Exception e) {
                log.warn(
                    "Failed to attach file: filename={}, error={}",
                    forJava(file.getName()),
                    forJava(e.getMessage()));
              }
            });

    mailSender.send(message);
    log.info("Email sent successfully to {}", forJava(email.to().getAddress()));
  }
}
