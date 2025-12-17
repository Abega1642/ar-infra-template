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

@InfraGenerated
@TestConfiguration
@Slf4j
@SuppressWarnings("resource")
public class EmailConf {

  private static final int SMTP_PORT = 3025;

  private static final GenericContainer<?> GREENMAIL_CONTAINER =
      new GenericContainer<>(DockerImageName.parse("greenmail/standalone:1.6.3"))
          .withExposedPorts(SMTP_PORT)
          .withCommand("--smtp --verbose");

  public void start() {
    if (!GREENMAIL_CONTAINER.isRunning()) {
      GREENMAIL_CONTAINER.start();
      log.info(
          "GreenMail (SMTP) started at {}:{}",
          GREENMAIL_CONTAINER.getHost(),
          GREENMAIL_CONTAINER.getMappedPort(SMTP_PORT));
    }
  }

  public void stop() {
    if (GREENMAIL_CONTAINER.isRunning()) {
      GREENMAIL_CONTAINER.stop();
      log.info("GreenMail stopped");
    }
  }

  public void configureProperties(DynamicPropertyRegistry registry) {
    if (!GREENMAIL_CONTAINER.isRunning()) {
      start();
    }

    String host = GREENMAIL_CONTAINER.getHost();
    int port = GREENMAIL_CONTAINER.getMappedPort(SMTP_PORT);

    registry.add("spring.mail.host", () -> host);
    registry.add("spring.mail.port", () -> port);
    registry.add("spring.mail.username", () -> "test@arsmedia.dev");
    registry.add("spring.mail.password", () -> "testpass");
    registry.add("spring.mail.from-email", () -> "test@arsmedia.dev");

    log.info("Test email properties configured for GreenMail: {}:{}", host, port);
  }

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
