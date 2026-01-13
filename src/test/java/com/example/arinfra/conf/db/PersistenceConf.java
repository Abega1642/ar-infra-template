package com.example.arinfra.conf.db;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.conf.FacadeIT;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Contract for database test container lifecycle management in integration tests.
 *
 * <p>Implementations of this interface provide containerized database instances for testing,
 * eliminating the need for external database servers. Each implementation manages a specific
 * database type (PostgreSQL, MySQL, Oracle, etc.) using Testcontainers.
 *
 * <p><b>Usage:</b><br>
 * This interface is not used directly. Extend {@link FacadeIT} in your integration tests, which
 * automatically discovers and configures the database container:
 *
 * <pre>{@code
 * public class UserRepositoryIT extends FacadeIT {
 *   @Autowired
 *   private UserRepository userRepository;
 *
 *   @Test
 *   void shouldSaveUser() {
 *     User user = new User("john@example.com");
 *     userRepository.save(user);
 *
 *     assertThat(userRepository.findByEmail("john@example.com"))
 *         .isPresent()
 *         .get()
 *         .extracting(User::getEmail)
 *         .isEqualTo("john@example.com");
 *   }
 * }
 * }</pre>
 *
 * <p><b>Adding Custom Database Support:</b><br>
 * To add support for a new database type, create a class implementing this interface:
 *
 * <pre>{@code
 * @TestConfiguration
 * public class OracleConf implements PersistenceConf {
 *   private static final OracleContainer ORACLE =
 *       new OracleContainer("gvenzl/oracle-xe:21-slim")
 *           .withDatabaseName("testdb")
 *           .withUsername("test")
 *           .withPassword("test");
 *
 *   @Override
 *   public void start() {
 *     if (!ORACLE.isRunning()) ORACLE.start();
 *   }
 *
 *   @Override
 *   public void stop() {
 *     if (ORACLE.isRunning()) ORACLE.stop();
 *   }
 *
 *   @Override
 *   public void configureProperties(DynamicPropertyRegistry registry) {
 *     registry.add("spring.datasource.url", ORACLE::getJdbcUrl);
 *     registry.add("spring.datasource.username", ORACLE::getUsername);
 *     registry.add("spring.datasource.password", ORACLE::getPassword);
 *     registry.add("spring.datasource.driver-class-name", ORACLE::getDriverClassName);
 *   }
 *
 *   @Override
 *   public String getDatabaseType() {
 *     return "oracle";
 *   }
 * }
 * }</pre>
 *
 * <p><b>Prerequisites:</b>
 *
 * <ul>
 *   <li>Docker must be installed and running
 *   <li>Testcontainers dependency must be included
 *   <li>Database-specific Testcontainers module must be included
 * </ul>
 *
 * @see FacadeIT
 * @see PersistenceConfFactory
 */
@InfraGenerated
public interface PersistenceConf {

  /**
   * Starts the database container if not already running.
   *
   * <p>This method is idempotent and safe to call multiple times.
   */
  void start();

  /**
   * Stops the database container if running.
   *
   * <p>This method is idempotent and safe to call multiple times.
   */
  void stop();

  /**
   * Configures Spring Boot datasource properties for the running container.
   *
   * @param registry the Spring DynamicPropertyRegistry to add properties to
   */
  void configureProperties(DynamicPropertyRegistry registry);

  /**
   * Returns the database type identifier.
   *
   * @return database type (e.g., "postgresql", "mysql", "oracle")
   */
  String getDatabaseType();
}
