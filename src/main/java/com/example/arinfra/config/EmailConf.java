package com.example.arinfra.config;

import com.example.arinfra.InfraGenerated;
import jakarta.validation.constraints.Email;
import java.util.Properties;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Spring configuration for email sending functionality via SMTP.
 *
 * <p>This configuration class sets up the JavaMail sender with SMTP server connection parameters
 * and authentication credentials. The mail sender is configured with STARTTLS encryption and
 * authentication enabled for secure email transmission.
 *
 * <p><b>Required application properties:</b>
 *
 * <ul>
 *   <li>{@code spring.mail.host} - SMTP server hostname (e.g., "smtp.gmail.com")
 *   <li>{@code spring.mail.port} - SMTP server port (typically 587 for TLS)
 *   <li>{@code spring.mail.username} - SMTP authentication username
 *   <li>{@code spring.mail.password} - SMTP authentication password
 *   <li>{@code spring.mail.from-email} - Default sender email address
 * </ul>
 *
 * <p><b>SMTP configuration:</b>
 *
 * <ul>
 *   <li>Protocol: SMTP
 *   <li>Authentication: Enabled
 *   <li>STARTTLS: Enabled for encrypted connections
 *   <li>Connection, read, and write timeouts: bounded, see {@link #mailSender()}
 * </ul>
 *
 * <p><b>Security compliance:</b>
 *
 * <ul>
 *   <li>CWE-532 (Insertion of Sensitive Information into Log File)
 *   <li>CWE-668 (Exposure of Resource to Wrong Sphere)
 *   <li>CWE-400 (Uncontrolled Resource Consumption)
 * </ul>
 *
 * @see <a href="https://cwe.mitre.org/data/definitions/532.html">CWE-532</a>
 * @see <a href="https://cwe.mitre.org/data/definitions/668.html">CWE-668</a>
 */
@InfraGenerated
@Configuration
public class EmailConf {

  /** Connection timeout, bounding how long a socket connect attempt to the SMTP host may block. */
  private static final int CONNECTION_TIMEOUT_MILLIS = 5_000;

  /** Read timeout, bounding how long a read on an established SMTP connection may block. */
  private static final int READ_TIMEOUT_MILLIS = 3_000;

  /** Write timeout, bounding how long a write to the SMTP connection may block. */
  private static final int WRITE_TIMEOUT_MILLIS = 5_000;

  /**
   * SMTP server hostname for sending emails. No public getter: used only to build {@link
   * #mailSender()}.
   */
  @Value("${spring.mail.host}")
  private String smtpHost;

  /**
   * SMTP server port number (typically 587 for STARTTLS). No public getter: used only to build
   * {@link #mailSender()}.
   */
  @Value("${spring.mail.port}")
  private int smtpPort;

  /**
   * Username for SMTP authentication. No public getter: used only to build {@link #mailSender()}.
   */
  @Value("${spring.mail.username}")
  private String username;

  /**
   * Password for SMTP authentication. No public getter: used only to build {@link #mailSender()}.
   */
  @Value("${spring.mail.password}")
  private String password;

  /**
   * Default "from" email address for outgoing emails. Exposed: services need it to build messages.
   */
  @Getter
  @Value("${spring.mail.from-email}")
  @Email
  private String fromEmail;

  /**
   * Creates and configures the JavaMail sender bean.
   *
   * <p>Configures the mail sender with:
   *
   * <ul>
   *   <li>SMTP server connection parameters (host, port, credentials)
   *   <li>SMTP protocol with authentication enabled
   *   <li>STARTTLS encryption for secure transmission
   *   <li>Bounded connect, read, and write timeouts
   * </ul>
   *
   * <p>Debug mode is not enabled here. JavaMail's SMTP debug output prints the full protocol
   *
   * <p>transcript, including the {@code AUTH LOGIN} exchange with the base64-encoded username and
   *
   * <p>password, to the application log (CWE-532). Base64 is encoding, not encryption, so this is
   *
   * <p>equivalent to logging the credential in the clear. If SMTP-level troubleshooting is needed,
   *
   * <p>enable it explicitly and temporarily on a non-production environment, never as a hardcoded
   *
   * <p>default.
   *
   * <p>Timeouts are set explicitly because neither JavaMail nor {@link JavaMailSenderImpl} apply a
   *
   * <p>default. Without them, a hung or slow-responding SMTP server blocks the calling thread
   *
   * <p>indefinitely; in a bounded thread pool, enough concurrent hangs exhaust it and stop the
   *
   * <p>application from serving unrelated requests (CWE-400).
   *
   * @return configured JavaMailSender instance ready for sending emails
   * @see <a href="https://cwe.mitre.org/data/definitions/532.html">CWE-532</a>
   * @see <a href="https://cwe.mitre.org/data/definitions/400.html">CWE-400</a>
   */
  @Bean
  public JavaMailSender mailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(smtpHost);
    mailSender.setPort(smtpPort);
    mailSender.setUsername(username);
    mailSender.setPassword(password);

    Properties props = mailSender.getJavaMailProperties();

    props.setProperty("mail.transport.protocol", "smtp");
    props.setProperty("mail.smtp.auth", "true");
    props.setProperty("mail.smtp.starttls.enable", "true");
    props.setProperty("mail.smtp.connectiontimeout", String.valueOf(CONNECTION_TIMEOUT_MILLIS));
    props.setProperty("mail.smtp.timeout", String.valueOf(READ_TIMEOUT_MILLIS));
    props.setProperty("mail.smtp.writetimeout", String.valueOf(WRITE_TIMEOUT_MILLIS));

    return mailSender;
  }
}
