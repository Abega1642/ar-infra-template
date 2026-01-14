package com.example.arinfra.conf.db;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.conf.FacadeIT;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;

/**
 * Test configuration for MySQL database container lifecycle management.
 *
 * <p>This configuration provides a containerized MySQL 8 instance for integration testing,
 * eliminating the need for a separate database server during test execution. It is designed to be
 * used as part of the {@link FacadeIT} base test class infrastructure, which orchestrates multiple
 * test containers for comprehensive integration testing.
 *
 * <p><b>Container configuration:</b>
 *
 * <ul>
 *   <li>Image: mysql:8 (MySQL 8.x version)
 *   <li>Database name: "ar_infra"
 *   <li>Username: "test"
 *   <li>Password: "test"
 *   <li>Reuse: Disabled (fresh database per test JVM)
 * </ul>
 *
 * <p><b>Integration with FacadeIT:</b> <br>
 * This class is not used directly in test classes. Instead, extend {@link FacadeIT} which handles
 * the lifecycle of this and other containers:
 *
 * <pre>{@code
 * public class MyIntegrationTest extends FacadeIT {
 *   @Autowired
 *   private UserRepository userRepository;
 *
 *   @Test
 *   void testDatabaseOperations() {
 *     // MySQL is already running and configured
 *     // Test your database operations here
 *   }
 * }
 * }</pre>
 *
 * <p><b>Prerequisites:</b>
 *
 * <ul>
 *   <li>Docker must be installed and running on the test machine
 *   <li>Testcontainers dependency must be included in the project
 *   <li>MySQL Testcontainers module must be included
 *   <li>Sufficient Docker resources to run MySQL container
 * </ul>
 *
 * @see PersistenceConf
 * @see PersistenceConfFactory
 * @see FacadeIT
 * @see org.testcontainers.containers.MySQLContainer
 */
@InfraGenerated
@TestConfiguration
@SuppressWarnings("resource")
public class MysqlConf implements PersistenceConf {

  /**
   * Test credential value used for both username and password. Simplifies test configuration with
   * consistent, easily identifiable credentials.
   */
  private static final String TEST_VALUE = "test";

  /**
   * MySQL 8 container configured for integration testing. Reuse is disabled to ensure test
   * isolation with a fresh database for each test run.
   */
  private static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.0")
          .withReuse(false)
          .withDatabaseName("ar_infra")
          .withUsername(TEST_VALUE)
          .withPassword(TEST_VALUE);

  /**
   * Starts the MySQL Docker container if it's not already running.
   *
   * <p>This method is called by {@link FacadeIT#beforeAll()} during test infrastructure setup. It
   * is idempotent - calling it multiple times will not start multiple containers.
   *
   * <p>The container will expose MySQL on a random available port, which is then configured via
   * {@link #configureProperties(DynamicPropertyRegistry)} to inject into the Spring test context.
   * Database schema migrations (Flyway/Liquibase) will run automatically after the container starts
   * if configured in the application.
   */
  @Override
  public void start() {
    if (!MYSQL.isRunning()) MYSQL.start();
  }

  /**
   * Stops the MySQL Docker container if it's running.
   *
   * <p>This method is called by the JVM shutdown hook registered in {@link FacadeIT#beforeAll()} to
   * ensure graceful cleanup of test resources. It is idempotent - calling it multiple times will
   * not cause errors.
   *
   * <p>The shutdown hook ensures containers are stopped even if tests fail or are interrupted. All
   * test data is lost when the container stops, ensuring test isolation.
   */
  @Override
  public void stop() {
    if (MYSQL.isRunning()) MYSQL.stop();
  }

  /**
   * Configures Spring Boot datasource properties to connect to the test MySQL container.
   *
   * <p>This method is called by {@link FacadeIT#configureProperties(DynamicPropertyRegistry)} to
   * dynamically register database connection properties based on the running container's actual
   * JDBC URL, credentials, and driver class.
   *
   * <p><b>Properties configured:</b>
   *
   * <ul>
   *   <li>{@code spring.datasource.url} - JDBC URL with dynamically assigned port
   *   <li>{@code spring.datasource.username} - Database username ("test")
   *   <li>{@code spring.datasource.password} - Database password ("test")
   *   <li>{@code spring.datasource.driver-class-name} - MySQL JDBC driver class
   * </ul>
   *
   * <p>These properties are automatically injected into the Spring test context, allowing JPA
   * repositories, JDBC templates, and other database components to connect to the test MySQL
   * instance without any hardcoded configuration.
   *
   * <p>The database starts empty; schema initialization happens through Spring Boot's normal
   * mechanisms (Flyway, Liquibase, or JPA DDL auto-generation).
   *
   * @param registry the Spring DynamicPropertyRegistry to add properties to
   */
  @Override
  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @Override
  public String getDatabaseType() {
    return "mysql";
  }
}
