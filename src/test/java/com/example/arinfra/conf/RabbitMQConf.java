package com.example.arinfra.conf;

import com.example.arinfra.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for RabbitMQ container lifecycle management.
 *
 * <p>This configuration provides a containerized RabbitMQ instance for integration testing using
 * Testcontainers. It is designed to be used as part of the {@link FacadeIT} base test class
 * infrastructure, which orchestrates multiple test containers (PostgreSQL, RabbitMQ, S3, Email) for
 * comprehensive integration testing.
 *
 * <p><b>Container configuration:</b>
 *
 * <ul>
 *   <li>Image: rabbitmq:3.13-management
 *   <li>Reuse: Disabled (fresh container per test JVM)
 *   <li>Management UI: Available on mapped port
 *   <li>Virtual host: "/" (default)
 *   <li>SSL: Disabled (not needed for testing)
 * </ul>
 *
 * <p><b>Integration with FacadeIT:</b> <br>
 * This class is not used directly in test classes. Instead, extend {@link FacadeIT} which handles
 * the lifecycle of this and other containers:
 *
 * <pre>{@code
 * public class MyIntegrationTest extends FacadeIT {
 *   @Test
 *   void testWithRabbitMQ() {
 *     // RabbitMQ is already running and configured
 *     // Test your messaging logic here
 *   }
 * }
 * }</pre>
 *
 * <p><b>Prerequisites:</b>
 *
 * <ul>
 *   <li>Docker must be installed and running on the test machine
 *   <li>Testcontainers dependency must be included in the project
 *   <li>Sufficient Docker resources to run RabbitMQ container
 * </ul>
 *
 * @see FacadeIT
 * @see org.testcontainers.containers.RabbitMQContainer
 */
@InfraGenerated
@TestConfiguration
public class RabbitMQConf {

  /**
   * Testcontainers RabbitMQ instance with management plugin enabled. Configured with reuse disabled
   * to ensure test isolation across test runs.
   */
  private static final RabbitMQContainer RABBIT =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management")).withReuse(false);

  /**
   * Starts the RabbitMQ Docker container if it's not already running.
   *
   * <p>This method is called by {@link FacadeIT#beforeAll()} during test infrastructure setup. It
   * is idempotent - calling it multiple times will not start multiple containers.
   *
   * <p>The container will expose RabbitMQ on a random available port, which is then configured via
   * {@link #configureProperties(DynamicPropertyRegistry)} to inject into the Spring test context.
   */
  public void start() {
    if (!RABBIT.isRunning()) RABBIT.start();
  }

  /**
   * Stops the RabbitMQ Docker container if it's running.
   *
   * <p>This method is called by the JVM shutdown hook registered in {@link FacadeIT#beforeAll()} to
   * ensure graceful cleanup of test resources. It is idempotent - calling it multiple times will
   * not cause errors.
   *
   * <p>The shutdown hook ensures containers are stopped even if tests fail or are interrupted.
   */
  public void stop() {
    if (RABBIT.isRunning()) RABBIT.stop();
  }

  /**
   * Configures Spring Boot application properties to connect to the test RabbitMQ container.
   *
   * <p>This method is called by {@link FacadeIT#configureProperties(DynamicPropertyRegistry)} to
   * dynamically register RabbitMQ connection properties based on the running container's actual
   * host, port, and credentials.
   *
   * <p><b>Properties configured:</b>
   *
   * <ul>
   *   <li>{@code spring.rabbitmq.host} - Container host (typically localhost)
   *   <li>{@code spring.rabbitmq.port} - Dynamically assigned AMQP port
   *   <li>{@code spring.rabbitmq.username} - Admin username from container
   *   <li>{@code spring.rabbitmq.password} - Admin password from container
   *   <li>{@code spring.rabbitmq.vhost} - Virtual host (fixed to "/")
   *   <li>{@code spring.rabbitmq.exchange} - Test exchange name ("infra-event-exchange")
   *   <li>{@code spring.rabbitmq.queue} - Test queue name ("infra-health-queue")
   *   <li>{@code spring.rabbitmq.routing-key} - Test routing key ("spring.event.key")
   *   <li>{@code app.rabbitmq.ssl} - SSL disabled for testing
   * </ul>
   *
   * <p>These properties are automatically injected into the Spring test context, allowing the
   * application to connect to the test RabbitMQ instance without any hardcoded configuration.
   *
   * @param registry the Spring DynamicPropertyRegistry to add properties to
   */
  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", RABBIT::getHost);
    registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    registry.add("spring.rabbitmq.vhost", () -> "/");
    registry.add("app.rabbitmq.ssl", () -> "false");
    registry.add("spring.rabbitmq.exchange", () -> "infra-event-exchange");
    registry.add("spring.rabbitmq.queue", () -> "infra-health-queue");
    registry.add("spring.rabbitmq.routing-key", () -> "spring.event.key");
  }
}
