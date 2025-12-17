package com.example.arInfra.config;

import com.example.arInfra.InfraGenerated;
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
 *   <li>Debug mode: Enabled for troubleshooting
 * </ul>
 */
@InfraGenerated
@Getter
@Configuration
public class EmailConf {

  /** SMTP server hostname for sending emails. */
  private final String smtpHost;

  /** SMTP server port number (typically 587 for STARTTLS). */
  private final int smtpPort;

  /** Username for SMTP authentication. */
  private final String username;

  /** Password for SMTP authentication. */
  private final String password;

  /** Default "from" email address for outgoing emails. */
  private final String fromEmail;

  /**
   * Constructs the email configuration with SMTP server parameters.
   *
   * <p>All parameters are injected from application properties and stored for use in the {@link
   * #mailSender()} bean creation.
   *
   * @param smtpHost the SMTP server hostname
   * @param smtpPort the SMTP server port
   * @param username the SMTP authentication username
   * @param password the SMTP authentication password
   * @param fromEmail the default sender email address
   */
  public EmailConf(
      @Value("${spring.mail.host}") String smtpHost,
      @Value("${spring.mail.port}") int smtpPort,
      @Value("${spring.mail.username}") String username,
      @Value("${spring.mail.password}") String password,
      @Value("${spring.mail.from-email}") String fromEmail) {
    this.smtpHost = smtpHost;
    this.smtpPort = smtpPort;
    this.username = username;
    this.password = password;
    this.fromEmail = fromEmail;
  }

  /**
   * Creates and configures the JavaMail sender bean.
   *
   * <p>Configures the mail sender with:
   *
   * <ul>
   *   <li>SMTP server connection parameters (host, port, credentials)
   *   <li>SMTP protocol with authentication enabled
   *   <li>STARTTLS encryption for secure transmission
   *   <li>Debug mode enabled for detailed logging
   * </ul>
   *
   * <p>The mail sender can be injected into services for sending emails programmatically throughout
   * the application.
   *
   * @return configured JavaMailSender instance ready for sending emails
   */
  @Bean
  public JavaMailSender mailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(smtpHost);
    mailSender.setPort(smtpPort);
    mailSender.setUsername(username);
    mailSender.setPassword(password);

    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.debug", "true");

    return mailSender;
  }
}
