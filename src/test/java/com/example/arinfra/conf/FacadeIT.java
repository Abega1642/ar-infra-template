package com.example.arinfra.conf;

import static java.lang.Runtime.getRuntime;

import com.example.arinfra.InfraGenerated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base integration test facade responsible for bootstrapping all infrastructure dependencies
 * required for end-to-end tests.
 *
 * <p>This class starts and manages the lifecycle of the following Testcontainers:
 *
 * <ul>
 *   <li><b>PostgreSQL</b> – persistence layer
 *   <li><b>RabbitMQ</b> – asynchronous messaging
 *   <li><b>S3-compatible bucket</b> – file storage
 *   <li><b>Email service</b> – outbound email testing
 * </ul>
 *
 * <p>Additionally, it dynamically injects environment variables and container connection properties
 * into the Spring context using {@link DynamicPropertySource}.
 *
 * <p><b>Lifecycle guarantees:</b>
 *
 * <ul>
 *   <li>Containers are started once per test JVM
 *   <li>Containers are stopped gracefully via JVM shutdown hook
 * </ul>
 *
 * <p>This class is marked as {@link InfraGenerated} and must be extended by all integration tests
 * that require real infrastructure.
 */
@Slf4j
@InfraGenerated
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
public abstract class FacadeIT {

  private static final PostgresConf POSTGRES_CONF = new PostgresConf();
  private static final RabbitMQConf RABBITMQ_CONF = new RabbitMQConf();
  private static final BucketConf BUCKET_CONF = new BucketConf();
  private static final EmailConf EMAIL_CONF = new EmailConf();

  @BeforeAll
  static void beforeAll() {
    POSTGRES_CONF.start();
    RABBITMQ_CONF.start();
    BUCKET_CONF.start();
    EMAIL_CONF.start();

    getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  POSTGRES_CONF.stop();
                  RABBITMQ_CONF.stop();
                  BUCKET_CONF.stop();
                  EMAIL_CONF.stop();
                }));
  }

  /**
   * Registers dynamic container and environment properties into the Spring context.
   *
   * <p>If {@code EnvConf} is present in the project, it will be loaded reflectively to allow
   * project-specific environment variables without hard dependency.
   */
  @SneakyThrows
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    POSTGRES_CONF.configureProperties(registry);
    RABBITMQ_CONF.configureProperties(registry);
    BUCKET_CONF.configureProperties(registry);
    EMAIL_CONF.configureProperties(registry);

    try {
      var envConfClazz = Class.forName("com.example.arinfra.conf.EnvConf");
      var configureMethod =
          envConfClazz.getDeclaredMethod("configureProperties", DynamicPropertyRegistry.class);
      var envConfInstance = envConfClazz.getConstructor().newInstance();
      configureMethod.invoke(envConfInstance, registry);
    } catch (ClassNotFoundException e) {
      log.warn("EnvConf not found: skipping project-specific test env variables");
    }
  }
}
