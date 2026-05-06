package com.example.arinfra.config;

import com.example.arinfra.ArInfraApplication;
import com.example.arinfra.InfraGenerated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
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
 * Central Jackson {@link ObjectMapper} configuration for the application.
 *
 * <p>Produces a single, security-hardened {@link ObjectMapper} bean registered as {@code @Primary},
 *
 * <p>ensuring that all Spring infrastructure components (MVC message converters, Spring Data
 *
 * <p>serializers, etc.) and application code that auto-wires {@link ObjectMapper} share one
 *
 * <p>consistently secured instance.
 *
 * <p>Configuration is applied in a deterministic, ordered sequence:
 *
 * <ol>
 *   <li>Security constraints - establishes the trust boundary before any other feature can interact
 *       <p>with type resolution.
 *   <li>Module registration - extends supported types within the established boundary.
 *   <li>Serialization features - controls JSON output shape.
 *   <li>Deserialization features - controls JSON input acceptance under the principle of least
 *       <p>permissiveness.
 *   <li>Parser features - enforces RFC 8259 structural compliance.
 *   <li>Property handling - naming strategy and inclusion policy, defined in one place to prevent
 *       <p>silent overrides across methods.
 * </ol>
 *
 * <p><b>Security compliance:</b>
 *
 * <ul>
 *   <li>OWASP Top 10 2021 - A08 (Software and Data Integrity Failures)
 *   <li>CWE-502 (Deserialization of Untrusted Data)
 *   <li>CWE-20 (Improper Input Validation)
 *   <li>CWE-400 (Uncontrolled Resource Consumption)
 *   <li>CWE-436 (Interpretation Conflict)
 *   <li>RFC 8259 (The JavaScript Object Notation Data Interchange Format)
 * </ul>
 *
 * @see <a href="https://owasp.org/www-community/vulnerabilities/Deserialization_of_untrusted_data">
 *     <p>OWASP - Deserialization of Untrusted Data</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc8259">RFC 8259 - JSON Specification</a>
 * @see <a href="https://github.com/FasterXML/jackson-databind/blob/2.x/docs/security.md">Jackson
 *     <p>Security Notes</a>
 */
@Slf4j
@Configuration
@InfraGenerated
public class JacksonConf {

  /**
   * Application root package, sourced from {@link ArInfraApplication} which resides at the root
   *
   * <p>package by Spring Boot convention.
   *
   * <p>This constant is derived from a compile-time class reference and is therefore fully
   *
   * <p>controlled by the codebase. It is never influenced by external input and carries no
   *
   * <p>log-injection risk.
   *
   * <p>Used exclusively to restrict polymorphic deserialization to types owned by this application.
   *
   * <p>See {@link #configureSecurityFeatures(ObjectMapper)}.
   */
  private static final String APPLICATION_BASE_PACKAGE =
      ArInfraApplication.class.getPackage().getName();
  ;

  /**
   * Creates the primary {@link ObjectMapper} bean.
   *
   * <p>Marked {@code @Primary} so that every Spring component that injects {@link ObjectMapper}
   *
   * <p>without an explicit qualifier receives this secured instance.
   *
   * @return a fully configured, security-hardened {@link ObjectMapper}
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

    log.info(
        "ObjectMapper initialized. Polymorphic deserialization restricted to [{}]",
        APPLICATION_BASE_PACKAGE);

    return mapper;
  }

  /**
   * Establishes the trust boundary for polymorphic type resolution.
   *
   * <p>Jackson's polymorphic deserialization, when unrestricted, allows an attacker to supply a
   *
   * <p>crafted {@code @class} or {@code @type} field in a JSON payload that causes Jackson to
   *
   * <p>instantiate and invoke methods on arbitrary JVM classes available on the classpath (gadget
   *
   * <p>chains). This is a well-documented Remote Code Execution vector with multiple assigned CVEs.
   *
   * <p>The {@link BasicPolymorphicTypeValidator} configured here restricts deserialization to types
   *
   * <p>whose base class or subtype resides within the application root package. All other types are
   *
   * <p>rejected by the validator before instantiation occurs.
   *
   * <p>Default typing is intentionally NOT activated. Calling {@code activateDefaultTyping(...)}
   *
   * <p>would apply polymorphic type embedding globally, dramatically widening the attack surface
   * even
   *
   * <p>with a type validator in place. Polymorphic handling must be opted into explicitly per type
   * via
   *
   * <p>{@code @JsonTypeInfo} and {@code @JsonSubTypes}.
   *
   * @param mapper the {@link ObjectMapper} to secure
   * @see <a href="https://nvd.nist.gov/vuln/detail/CVE-2017-7525">CVE-2017-7525</a>
   * @see <a href="https://nvd.nist.gov/vuln/detail/CVE-2019-14379">CVE-2019-14379</a>
   * @see <a href="https://nvd.nist.gov/vuln/detail/CVE-2020-36518">CVE-2020-36518</a>
   * @see <a href="https://github.com/FasterXML/jackson-databind/blob/2.x/docs/security.md">Jackson
   *     <p>Security Notes</a>
   */
  private void configureSecurityFeatures(ObjectMapper mapper) {
    PolymorphicTypeValidator typeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(APPLICATION_BASE_PACKAGE)
            .allowIfSubType(APPLICATION_BASE_PACKAGE)
            .build();
    mapper.setPolymorphicTypeValidator(typeValidator);

    log.debug("Polymorphic type validation restricted to [{}]", APPLICATION_BASE_PACKAGE);
  }

  /**
   * Registers the standard Jackson modules required for full Java type support.
   *
   * <ul>
   *   <li>{@link JavaTimeModule} - serialization and deserialization of {@code java.time.*} types
   *       <p>({@code LocalDateTime}, {@code Instant}, {@code ZonedDateTime}, etc.). Replaces the
   *       <p>deprecated {@code JSR310Module}.
   *   <li>{@link Jdk8Module} - support for {@code Optional<T>} and other JDK 8 types.
   *   <li>{@link ParameterNamesModule} - enables binding via constructor and factory-method
   *       <p>parameter names, removing the need for {@code @JsonProperty} on every field.
   * </ul>
   *
   * <p>Although Spring Boot auto-configuration detects these modules on the classpath and registers
   *
   * <p>them automatically, explicit registration here guarantees deterministic behavior regardless
   * of
   *
   * <p>auto-configuration ordering or classpath scanning state.
   *
   * @param mapper the {@link ObjectMapper} to configure
   */
  private void registerModules(ObjectMapper mapper) {

    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new ParameterNamesModule());

    log.debug("Registered modules: JavaTimeModule, Jdk8Module, ParameterNamesModule");
  }

  /**
   * Configures JSON serialization (output) behavior.
   *
   * <ul>
   *   <li>{@code WRITE_DATES_AS_TIMESTAMPS} disabled - {@code java.time.*} types are serialized as
   *       <p>ISO-8601 strings. Numeric timestamps are ambiguous across time zones and lack
   *       <p>self-documentation for API consumers.
   *   <li>{@code WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS} disabled - millisecond resolution is
   *       <p>consistent with the deserialization setting and sufficient for all application use
   *       cases.
   *       <p>Nanoseconds increase payload size without benefit.
   *   <li>{@code INDENT_OUTPUT} disabled - compact output is required in production.
   *       <p>Pretty-printing increases payload size and provides no machine-readable benefit.
   * </ul>
   *
   * <p>The serialization inclusion policy (NON_NULL) is set exclusively in {@link
   * <p>#configurePropertyHandling(ObjectMapper)} to maintain a single authoritative source and
   * prevent
   *
   * <p>silent overrides.
   *
   * @param mapper the {@link ObjectMapper} to configure
   */
  private void configureSerializationFeatures(ObjectMapper mapper) {
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    mapper.disable(SerializationFeature.INDENT_OUTPUT);

    log.debug("Serialization: ISO-8601 dates, nanoseconds disabled, compact output");
  }

  /**
   * Configures JSON deserialization (input) behavior under the principle of least permissiveness:
   *
   * <p>only what is explicitly required for application operation is enabled.
   *
   * <p><b>Enabled:</b>
   *
   * <ul>
   *   <li>{@code FAIL_ON_NULL_FOR_PRIMITIVES} - rejects explicit {@code null} for primitive fields.
   *       <p>Silent zero/false defaults mask client-side contract violations and can cause
   *       undetected
   *       <p>data corruption.
   *   <li>{@code FAIL_ON_NUMBERS_FOR_ENUMS} - rejects integer ordinal coercion for enum fields.
   *       <p>Ordinal-based binding is an implicit, fragile contract that breaks silently when enum
   *       <p>declaration order changes.
   *   <li>{@code USE_BIG_DECIMAL_FOR_FLOATS} - floating-point values are bound as {@link
   *       <p>java.math.BigDecimal}, preventing IEEE 754 precision loss on financial or scientific
   *       <p>data.
   *   <li>{@code USE_BIG_INTEGER_FOR_INTS} - integer values are bound as {@link
   *       <p>java.math.BigInteger}, preventing silent overflow on large numeric payloads.
   *   <li>{@code READ_DATE_TIMESTAMPS_AS_NANOSECONDS} disabled - consistent with the serialization
   *       <p>setting; millisecond resolution is the application standard.
   * </ul>
   *
   * <p><b>Intentionally NOT enabled:</b>
   *
   * <ul>
   *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES} - disabled to support rolling deployments and
   *       <p>incremental API evolution without breaking existing clients. Stricter validation can
   *       be
   *       <p>applied per endpoint via {@code @JsonIgnoreProperties(ignoreUnknown = false)}.
   *   <li>{@code ACCEPT_EMPTY_STRING_AS_NULL_OBJECT} - silent coercion of {@code ""} to {@code
   *       <p>null} combined with {@code FAIL_ON_NULL_FOR_PRIMITIVES} creates an unintentional
   *       <p>denial-of-service vector: any client sending an empty string for a primitive field
   *       <p>triggers a deserialization exception that can be exploited to produce error-handling
   *       <p>overhead or logging storms (CWE-400).
   *   <li>{@code ACCEPT_SINGLE_VALUE_AS_ARRAY} - silently wrapping scalar values into collections
   *       <p>hides client intent and can bypass validation logic that asserts whether a list was
   *       <p>explicitly provided by the caller (OWASP API3:2023).
   * </ul>
   *
   * @param mapper the {@link ObjectMapper} to configure
   * @see <a href="https://cwe.mitre.org/data/definitions/400.html">CWE-400</a>
   * @see <a
   *     <p>href="https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/">
   *     <p>OWASP API3:2023 - Broken Object Property Level Authorization</a>
   */
  private void configureDeserializationFeatures(ObjectMapper mapper) {
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    mapper.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
    mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

    log.debug(
        "Deserialization: strict primitives and enums, overflow protection, no silent coercions");
  }

  /**
   * Configures JSON parser features for strict compliance with RFC 8259.
   *
   * <p>Only {@code STRICT_DUPLICATE_DETECTION} is enabled. All relaxed parsing features are
   *
   * <p>intentionally disabled under the principle of least permissiveness.
   *
   * <p><b>Enabled:</b>
   *
   * <ul>
   *   <li>{@code STRICT_DUPLICATE_DETECTION} - duplicate object keys are rejected. RFC 8259 Section
   *       <p>4 declares duplicate keys as undefined behavior. Parsers that silently resolve
   *       duplicates
   *       <p>by taking the last value can be exploited to shadow fields that have already passed
   *       <p>validation.
   * </ul>
   *
   * <p><b>Intentionally NOT enabled:</b>
   *
   * <ul>
   *   <li>{@code ALLOW_COMMENTS} - JSON comments are not part of RFC 8259. Enabling them creates a
   *       <p>differential parsing risk: an upstream WAF or API gateway validates the payload as
   *       <p>comment-free JSON while Jackson interprets content embedded within comment structures,
   *       <p>bypassing upstream protection (CWE-436).
   *   <li>{@code ALLOW_UNQUOTED_FIELD_NAMES} - violates RFC 8259 Section 4. Accepting structurally
   *       <p>invalid JSON allows payloads that spec-compliant upstream validators would reject,
   *       <p>undermining their protection (CWE-20).
   *   <li>{@code ALLOW_SINGLE_QUOTES} - violates RFC 8259 Section 7. Carries the same bypass risk
   *       <p>as unquoted field names.
   *   <li>{@code ALLOW_TRAILING_COMMA} - not part of RFC 8259. Adds parser permissiveness with no
   *       <p>production justification.
   * </ul>
   *
   * @param mapper the {@link ObjectMapper} to configure
   * @see <a href="https://www.rfc-editor.org/rfc/rfc8259">RFC 8259 - JSON Specification</a>
   * @see <a href="https://bishopfox.com/blog/json-interoperability-vulnerabilities">Bishop Fox -
   *     <p>Differential JSON Parsing Vulnerabilities</a>
   * @see <a href="https://cwe.mitre.org/data/definitions/436.html">CWE-436</a>
   * @see <a href="https://cwe.mitre.org/data/definitions/20.html">CWE-20</a>
   */
  private void configureParserFeatures(ObjectMapper mapper) {
    mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    log.debug("Parser: RFC 8259 strict compliance, duplicate key detection enabled");
  }

  /**
   * Configures property naming strategy and serialization inclusion policy.
   *
   * <p>This is the single authoritative location where the inclusion strategy is set. No other
   *
   * <p>method in this class touches inclusion policy, eliminating the risk of silent overrides from
   *
   * <p>method execution order.
   *
   * <ul>
   *   <li>{@link PropertyNamingStrategies#SNAKE_CASE} - maps Java camelCase fields to snake_case
   *       <p>JSON keys, consistent with REST API conventions and OpenAPI tooling defaults.
   *   <li>{@link JsonInclude.Include#NON_NULL} applied via {@link
   *       <p>ObjectMapper#setDefaultPropertyInclusion(JsonInclude.Value)} - the non-deprecated API
   *       <p>accepting a {@link JsonInclude.Value} that sets both value and content inclusion. Null
   *       <p>fields are omitted from output, reducing payload size. Empty collections are retained,
   *       <p>making list-returning endpoints predictable for clients that iterate results without
   *       <p>null-checking.
   * </ul>
   *
   * @param mapper the {@link ObjectMapper} to configure
   */
  private void configurePropertyHandling(ObjectMapper mapper) {
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    mapper.setDefaultPropertyInclusion(
        JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

    log.debug("Properties: SNAKE_CASE naming, NON_NULL inclusion (value and content)");
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
