package com.example.arInfra.conf;

import com.example.arInfra.InfraGenerated;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for email service container lifecycle management.
 *
 * <p>This configuration provides a GreenMail SMTP server container for integration testing,
 * allowing email sending functionality to be tested without requiring actual email infrastructure.
 * GreenMail is an in-memory email server that captures sent emails for verification during tests.
 * It is designed to be used as part of the {@link FacadeIT} base test class infrastructure, which
 * orchestrates multiple test containers for comprehensive integration testing.
 *
 * <p><b>Container configuration:</b>
 *
 * <ul>
 *   <li>Image: greenmail/standalone:1.6.3
 *   <li>Service: SMTP only (port 3025)
 *   <li>Mode: Verbose logging enabled
 *   <li>Authentication: Test credentials (test@arsmedia.dev / testpass)
 * </ul>
 *
 * <p><b>Integration with FacadeIT:</b> <br>
 * This class is not used directly in test classes. Instead, extend {@link FacadeIT} which handles
 * the lifecycle of this and other containers:
 *
 * <pre>{@code
 * public class MyIntegrationTest extends FacadeIT {
 *   @Autowired
 *   private JavaMailSender mailSender;
 *
 *   @Test
 *   void testEmailSending() {
 *     // GreenMail SMTP is already running and configured
 *     // Test your email sending logic here
 *     // Emails are captured by GreenMail for verification
 *   }
 * }
 * }</pre>
 *
 * <p><b>Prerequisites:</b>
 *
 * <ul>
 *   <li>Docker must be installed and running on the test machine
 *   <li>Testcontainers dependency must be included in the project
 *   <li>Sufficient Docker resources to run GreenMail container
 * </ul>
 *
 * @see FacadeIT
 * @see org.testcontainers.containers.GenericContainer
 */
@InfraGenerated
@TestConfiguration
@Slf4j
@SuppressWarnings("resource")
public class EmailConf {

  /**
   * SMTP port exposed by the GreenMail container. GreenMail's default SMTP port is 3025 to avoid
   * conflicts with system mail servers.
   */
  private static final int SMTP_PORT = 3025;

  /**
   * GreenMail SMTP server container for capturing test emails. Configured with verbose logging to
   * aid in debugging email-related test failures.
   */
  private static final GenericContainer<?> GREENMAIL_CONTAINER =
      new GenericContainer<>(DockerImageName.parse("greenmail/standalone:1.6.3"))
          .withExposedPorts(SMTP_PORT)
          .withCommand("--smtp --verbose");

  /**
   * Starts the GreenMail SMTP container if it's not already running.
   *
   * <p>This method is called by {@link FacadeIT#beforeAll()} during test infrastructure setup. It
   * is idempotent - calling it multiple times will not start multiple containers.
   *
   * <p>The container exposes the SMTP service on a random available port, which is then configured
   * via {@link #configureProperties(DynamicPropertyRegistry)} to inject into the Spring test
   * context.
   *
   * <p>Startup is logged at INFO level with the actual host and port for debugging purposes.
   */
  public void start() {
    if (!GREENMAIL_CONTAINER.isRunning()) {
      GREENMAIL_CONTAINER.start();
      log.info(
          "GreenMail (SMTP) started at {}:{}",
          GREENMAIL_CONTAINER.getHost(),
          GREENMAIL_CONTAINER.getMappedPort(SMTP_PORT));
    }
  }

  /**
   * Stops the GreenMail SMTP container if it's running.
   *
   * <p>This method is called by the JVM shutdown hook registered in {@link FacadeIT#beforeAll()} to
   * ensure graceful cleanup of test resources. It is idempotent - calling it multiple times will
   * not cause errors.
   *
   * <p>The shutdown hook ensures containers are stopped even if tests fail or are interrupted.
   * Shutdown is logged at INFO level for debugging purposes.
   */
  public void stop() {
    if (GREENMAIL_CONTAINER.isRunning()) {
      GREENMAIL_CONTAINER.stop();
      log.info("GreenMail stopped");
    }
  }

  /**
   * Configures Spring Boot mail properties to connect to the test GreenMail SMTP container.
   *
   * <p>This method is called by {@link FacadeIT#configureProperties(DynamicPropertyRegistry)} to
   * dynamically register email connection properties based on the running container's actual host
   * and port.
   *
   * <p>If the container is not running when this method is called, it will be started automatically
   * to ensure properties are correctly configured.
   *
   * <p><b>Properties configured:</b>
   *
   * <ul>
   *   <li>{@code spring.mail.host} - Container host (typically localhost)
   *   <li>{@code spring.mail.port} - Dynamically assigned SMTP port
   *   <li>{@code spring.mail.username} - Test email address (test@arsmedia.dev)
   *   <li>{@code spring.mail.password} - Test password (testpass)
   *   <li>{@code spring.mail.from-email} - Default sender address (test@arsmedia.dev)
   * </ul>
   *
   * <p>These properties are automatically injected into the Spring test context, allowing the
   * application to send emails to GreenMail for capture and verification during testing.
   *
   * <p>Configuration is logged at INFO level with the actual host and port for debugging.
   *
   * @param registry the Spring DynamicPropertyRegistry to add properties to
   */
  public void configureProperties(DynamicPropertyRegistry registry) {
    if (!GREENMAIL_CONTAINER.isRunning()) start();

    String host = GREENMAIL_CONTAINER.getHost();
    int port = GREENMAIL_CONTAINER.getMappedPort(SMTP_PORT);

    registry.add("spring.mail.host", () -> host);
    registry.add("spring.mail.port", () -> port);
    registry.add("spring.mail.username", () -> "test@arsmedia.dev");
    registry.add("spring.mail.password", () -> "testpass");
    registry.add("spring.mail.from-email", () -> "test@arsmedia.dev");

    log.info("Test email properties configured for GreenMail: {}:{}", host, port);
  }

  /**
   * Creates and configures the JavaMail sender bean for test email operations.
   *
   * <p>This bean overrides the production email configuration with test-specific settings that
   * connect to the GreenMail container. The mail sender is configured with:
   *
   * <ul>
   *   <li>SMTP server connection to GreenMail container
   *   <li>Test credentials for authentication
   *   <li>STARTTLS encryption enabled (GreenMail supports it)
   *   <li>Debug mode enabled for detailed logging during tests
   * </ul>
   *
   * <p>The mail sender can be injected into test classes or services under test to send emails that
   * will be captured by GreenMail for verification.
   *
   * @param host the SMTP server host (injected from test properties)
   * @param port the SMTP server port (injected from test properties)
   * @param username the SMTP authentication username (injected from test properties)
   * @param password the SMTP authentication password (injected from test properties)
   * @return configured JavaMailSender instance connected to GreenMail test server
   */
  @Bean
  public JavaMailSender mailSender(
      @Value("${spring.mail.host}") String host,
      @Value("${spring.mail.port}") int port,
      @Value("${spring.mail.username}") String username,
      @Value("${spring.mail.password}") String password) {

    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(host);
    mailSender.setPort(port);
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
