package com.example.arinfra.conf;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.conf.db.PersistenceConf;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Test configuration for application-specific environment variables and properties.
 *
 * <p>This configuration provides a centralized location for injecting custom environment variables
 * and application properties that don't belong to specific infrastructure components (PostgreSQL,
 * RabbitMQ, S3, Email). Use this class for test-specific configuration such as API keys, feature
 * flags, external service endpoints, and other application-level settings.
 *
 * <p><b>Purpose:</b> <br>
 * While infrastructure-specific configurations ({@link PersistenceConf}, {@link RabbitMQConf},
 * {@link BucketConf}, {@link EmailConf}) handle container connection properties, {@code EnvConf}
 * manages general application environment variables that would typically reside in a {@code .env}
 * file or {@code application-test.properties}.
 *
 * <p><b>Common use cases:</b>
 *
 * <ul>
 *   <li>Third-party API keys (e.g., payment gateways, SMS services)
 *   <li>JWT secret keys and token configurations
 *   <li>Feature flags and environment-specific toggles
 *   <li>External service URLs (e.g., OAuth providers, webhooks)
 *   <li>Application-specific timeouts and thresholds
 *   <li>Encryption keys and security-related properties
 * </ul>
 *
 * <p><b>Integration with FacadeIT:</b> <br>
 * This class is automatically discovered and loaded by {@link
 * FacadeIT#configureProperties(DynamicPropertyRegistry)} through reflection. It is invoked after
 * all infrastructure containers are configured. If this class is not present in the project, {@code
 * FacadeIT} will log a warning and continue without error.
 *
 * <pre>{@code
 * public class MyIntegrationTest extends FacadeIT {
 *   @Value("${app.api.stripe.key}")
 *   private String stripeApiKey;
 *
 *   @Test
 *   void testPaymentIntegration() {
 *     // Uses the API key configured in EnvConf
 *     // Test your payment logic here
 *   }
 * }
 * }</pre>
 *
 * <p><b>Example configuration:</b>
 *
 * <pre>{@code
 * @Override
 * public void configureProperties(DynamicPropertyRegistry registry) {
 *   // API Keys
 *   registry.add("app.api.stripe.key", () -> "sk_test_123");
 *   registry.add("app.api.sendgrid.key", () -> "SG.test.456");
 *
 *   // JWT Configuration
 *   registry.add("app.jwt.secret", () -> "test-secret-key");
 *   registry.add("app.jwt.expiration", () -> "3600000");
 *
 *   // Feature Flags
 *   registry.add("app.feature.new-ui.enabled", () -> "true");
 *
 *   // External Services
 *   registry.add("app.oauth.google.client-id", () -> "test-client-id");
 *   registry.add("app.webhook.callback-url", () -> "http://localhost:8080/webhook");
 * }
 * }</pre>
 *
 * <p><b>Important notes:</b>
 *
 * <ul>
 *   <li>This class is optional; {@code FacadeIT} will work without it
 *   <li>Properties are registered dynamically and override static test properties
 *   <li>Use test-safe values; avoid real production credentials
 *   <li>All properties are scoped to the test JVM lifecycle
 *   <li>Properties are available to all Spring components during test execution
 * </ul>
 *
 * <p><b>Security considerations:</b> <br>
 * Never commit real API keys or production credentials. Use placeholder values suitable for
 * testing. For sensitive integration tests requiring real credentials, consider using environment
 * variables or a secrets management solution.
 *
 * @see FacadeIT
 * @see DynamicPropertyRegistry
 * @see PersistenceConf
 * @see RabbitMQConf
 * @see BucketConf
 * @see EmailConf
 */
@InfraGenerated
@TestConfiguration
public class EnvConf {

  /**
   * Configures application-specific environment variables and properties for integration testing.
   *
   * <p>This method is called by {@link FacadeIT#configureProperties(DynamicPropertyRegistry)}
   * through reflection after all infrastructure containers have been configured. Use this method to
   * register any application-level properties that don't belong to specific infrastructure
   * components.
   *
   * <p><b>Implementation guidelines:</b>
   *
   * <ul>
   *   <li>Use descriptive property keys following Spring Boot conventions (e.g., {@code
   *       app.feature.name})
   *   <li>Provide lambda suppliers for dynamic values: {@code registry.add("key", () -> "value")}
   *   <li>Group related properties together with comments for clarity
   *   <li>Use test-safe values that won't affect external systems
   *   <li>Document any non-obvious property purposes in comments
   * </ul>
   *
   * <p><b>Example implementation:</b>
   *
   * <pre>{@code
   * @Override
   * public void configureProperties(DynamicPropertyRegistry registry) {
   *   // Payment Gateway Configuration
   *   registry.add("app.payment.stripe.api-key", () -> "sk_test_mock_key");
   *   registry.add("app.payment.stripe.webhook-secret", () -> "whsec_test_secret");
   *
   *   // Authentication & Security
   *   registry.add("app.security.jwt.secret", () -> "test-jwt-secret-key-min-256-bits");
   *   registry.add("app.security.jwt.expiration-ms", () -> "3600000");
   *   registry.add("app.security.cors.allowed-origins", () -> "http://localhost:3000");
   *
   *   // External Service Integration
   *   registry.add("app.external.weather-api.key", () -> "test-api-key");
   *   registry.add("app.external.weather-api.url", () -> "http://localhost:8081/mock-weather");
   *
   *   // Application Behavior
   *   registry.add("app.cache.ttl-seconds", () -> "300");
   *   registry.add("app.retry.max-attempts", () -> "3");
   *   registry.add("app.async.pool-size", () -> "5");
   * }
   * }</pre>
   *
   * <p>Properties registered here are available throughout the Spring test context and can be
   * injected using {@code @Value}, {@code @ConfigurationProperties}, or accessed via the {@code
   * Environment}.
   *
   * @param registry the Spring DynamicPropertyRegistry to add properties to
   */
  public void configureProperties(DynamicPropertyRegistry registry) {
    // Add your application-specific test properties here
  }
}
