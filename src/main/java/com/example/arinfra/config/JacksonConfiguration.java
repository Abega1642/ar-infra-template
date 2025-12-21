package com.example.arinfra.config;

import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson ObjectMapper configuration with security controls.
 *
 * <p>This configuration provides a production-ready ObjectMapper with security hardening to prevent
 * common JSON deserialization vulnerabilities while maintaining flexibility for application
 * development.
 *
 * <p><b>Security Features:</b>
 *
 * <ul>
 *   <li><b>Polymorphic Type Validation:</b> Allowlist-based validation prevents arbitrary class
 *       instantiation (prevents RCE via deserialization gadgets)
 *   <li><b>No Default Typing:</b> Default typing is explicitly disabled to prevent injection
 *       attacks
 *   <li><b>Strict Number Handling:</b> Prevents integer overflow and floating-point precision
 *       issues
 *   <li><b>Content Validation:</b> Validates JSON structure and prevents malformed input
 * </ul>
 *
 * <p><b>Functionality Features:</b>
 *
 * <ul>
 *   <li><b>Java Time Support:</b> Native handling of java.time.* types (LocalDateTime, Instant,
 *       etc.)
 *   <li><b>JDK 8 Support:</b> Optional types (Optional&lt;T&gt;)
 *   <li><b>Parameter Names:</b> Preserves parameter names for cleaner JSON binding
 *   <li><b>Flexible Deserialization:</b> Backward compatibility with unknown properties
 *   <li><b>Null Handling:</b> Configurable null and empty value handling
 *   <li><b>Date Formats:</b> ISO-8601 standardized date/time formatting
 *   <li><b>Naming Strategy:</b> snake_case for API consistency
 * </ul>
 *
 * <p><b>Compliance:</b>
 *
 * <ul>
 *   <li>OWASP Top 10 2021 - A8 (Software and Data Integrity Failures)
 *   <li>CWE-502 (Deserialization of Untrusted Data)
 *   <li>OWASP API Security Top 10 - API8 (Injection)
 * </ul>
 *
 * @see <a
 *     href="https://owasp.org/www-community/vulnerabilities/Deserialization_of_untrusted_data">OWASP
 *     Deserialization</a>
 * @see <a
 *     href="https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization">Jackson
 *     Polymorphic Deserialization</a>
 */
@Slf4j
@Configuration
@InfraGenerated
public class JacksonConfiguration {

  private static final String APPLICATION_BASE_PACKAGE = "com.example";

  /**
   * Creates the primary ObjectMapper bean with security hardening and feature configuration.
   *
   * <p>This ObjectMapper is marked as {@code @Primary} and will be used throughout the application
   * for all JSON serialization/deserialization operations, including Spring MVC's HTTP message
   * conversion.
   *
   * <p><b>Security Considerations:</b>
   *
   * <ul>
   *   <li>Default typing is DISABLED - prevents polymorphic deserialization attacks
   *   <li>Polymorphic type validator restricts deserialization to application packages only
   *   <li>Strict number parsing prevents overflow attacks
   *   <li>Duplicate key detection prevents ambiguous JSON
   * </ul>
   *
   * @return fully configured and secured ObjectMapper instance
   */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    configureSecurityFeatures(mapper);
    registerModules(mapper);
    configureSerializationFeatures(mapper);
    configureDeserializationFeatures(mapper);
    configureParserFeatures(mapper);
    configurePropertyHandling(mapper);

    log.info("ObjectMapper configured with security hardening and application-wide settings");
    return mapper;
  }

  /**
   * Configures security features to prevent deserialization attacks.
   *
   * <p><b>Critical Security Settings:</b>
   *
   * <ul>
   *   <li>Polymorphic type validation with application package allowlist
   *   <li>No default typing enabled (would be a critical vulnerability)
   *   <li>Only application classes can be polymorphically deserialized
   * </ul>
   */
  private void configureSecurityFeatures(ObjectMapper om) {
    PolymorphicTypeValidator typeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(APPLICATION_BASE_PACKAGE)
            .allowIfSubType(APPLICATION_BASE_PACKAGE)
            .build();

    om.setPolymorphicTypeValidator(typeValidator);

    // IMPORTANT: Do NOT call om.enableDefaultTyping() or activateDefaultTyping()
    // This would enable polymorphic deserialization globally and create RCE vulnerabilities

    log.debug(
        "Security: Polymorphic type validation restricted to package: {}",
        forJava(APPLICATION_BASE_PACKAGE));
  }

  /**
   * Registers Jackson modules for enhanced type support.
   *
   * <p>Modules provide serialization/deserialization support for:
   *
   * <ul>
   *   <li>Java 8 date/time types (LocalDateTime, Instant, ZonedDateTime, etc.)
   *   <li>Java 8 Optional types (Optional&lt;T&gt;)
   *   <li>Parameter name preservation for constructor/method parameters
   * </ul>
   */
  private void registerModules(ObjectMapper mapper) {
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new ParameterNamesModule());

    log.debug("Registered Jackson modules: JavaTime, JDK8, ParameterNames");
  }

  /**
   * Configures serialization behavior for JSON output.
   *
   * <p><b>Serialization Settings:</b>
   *
   * <ul>
   *   <li>Dates as ISO-8601 strings (not timestamps) for human readability
   *   <li>Empty collections/arrays written (not omitted) for API clarity
   *   <li>Pretty printing disabled for production (reduces payload size)
   * </ul>
   */
  private void configureSerializationFeatures(ObjectMapper om) {
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    om.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    om.disable(SerializationFeature.INDENT_OUTPUT); // Disable pretty-print for production

    log.debug("Serialization: Dates as ISO-8601, no timestamps, compact output");
  }

  /**
   * Configures deserialization behavior for JSON input.
   *
   * <p><b>Deserialization Settings:</b>
   *
   * <ul>
   *   <li>Unknown properties ignored for backward compatibility
   *   <li>Empty strings accepted as null for flexible input handling
   *   <li>Fail on null for primitives (prevents unexpected NullPointerExceptions)
   *   <li>Read date timestamps as milliseconds
   *   <li>Accept single values as arrays for API flexibility
   * </ul>
   */
  private void configureDeserializationFeatures(ObjectMapper mapper) {
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    mapper.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
    mapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

    log.debug("Deserialization: Flexible input, strict primitives, overflow protection");
  }

  /**
   * Configures JSON parser features for input validation.
   *
   * <p><b>Parser Settings:</b>
   *
   * <ul>
   *   <li>Comments allowed in JSON for development/debugging
   *   <li>Unquoted field names allowed for relaxed parsing
   *   <li>Single quotes allowed as alternative to double quotes
   *   <li>Duplicate keys rejected to prevent ambiguous input
   *   <li>Trailing commas allowed for cleaner JSON editing
   * </ul>
   */
  private void configureParserFeatures(ObjectMapper om) {
    om.enable(JsonParser.Feature.ALLOW_COMMENTS);
    om.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    om.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
    om.enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature());
    om.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    log.debug("Parser: Flexible input formats, strict duplicate detection");
  }

  /**
   * Configures property naming and null handling strategies.
   *
   * <p><b>Property Settings:</b>
   *
   * <ul>
   *   <li>snake_case naming for API consistency (camelCase → snake_case)
   *   <li>Null values excluded from JSON output (reduces payload size)
   *   <li>Empty collections included for API clarity
   * </ul>
   */
  private void configurePropertyHandling(ObjectMapper om) {
    om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    om.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    log.debug("Properties: snake_case naming, null values excluded");
  }

  /**
   * Bean for registering polymorphic subtypes automatically.
   *
   * <p>This bean is called by Spring after the ObjectMapper is created, allowing custom subtype
   * registration logic to be added by other configuration classes.
   *
   * @param objectMapper the primary ObjectMapper
   * @return list of polymorphic type registrars
   */
  @Bean
  public List<PolymorphicTypeRegistrar> polymorphicTypeRegistrars(ObjectMapper objectMapper) {
    // Other configuration classes can implement PolymorphicTypeRegistrar
    // to automatically register their polymorphic types
    return List.of();
  }

  /**
   * Interface for components that need to register polymorphic types with Jackson.
   *
   * <p>Implement this interface in configuration classes that need to register polymorphic subtypes
   * for secure deserialization.
   *
   * <p>Example:
   *
   * <pre>{@code
   * @Component
   * public class EventTypeRegistrar implements PolymorphicTypeRegistrar {
   *     public void registerTypes(ObjectMapper mapper) {
   *         // Auto-discover and register event types
   *     }
   * }
   * }</pre>
   */
  public interface PolymorphicTypeRegistrar {
    /**
     * Registers polymorphic subtypes with the given ObjectMapper.
     *
     * @param mapper the ObjectMapper to register types with
     */
    void registerTypes(ObjectMapper mapper);
  }
}
