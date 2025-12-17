package com.example.arInfra.config;

import com.example.arInfra.InfraGenerated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for Jackson ObjectMapper JSON serialization/deserialization.
 *
 * <p>This configuration provides a centralized, application-wide ObjectMapper bean with
 * standardized settings for handling JSON in REST APIs. The mapper is configured with sensible
 * defaults for date handling, property naming, and null value handling.
 *
 * <p><b>Key configurations:</b>
 *
 * <ul>
 *   <li><b>Java Time support:</b> Handles java.time.* types (LocalDateTime, Instant, etc.)
 *   <li><b>Flexible deserialization:</b> Ignores unknown JSON properties for backward compatibility
 *   <li><b>Null handling:</b> Empty strings treated as null, null values excluded from output
 *   <li><b>Date format:</b> Dates serialized as ISO-8601 strings, not timestamps
 *   <li><b>Naming strategy:</b> snake_case property naming (e.g., firstName → first_name)
 * </ul>
 *
 * <p>This bean is marked as {@code @Primary}, making it the default ObjectMapper throughout the
 * application, including for Spring MVC's JSON message conversion.
 */
@Configuration
@InfraGenerated
public class ObjectMapperConfig {

  /**
   * Creates and configures the primary ObjectMapper bean for JSON processing.
   *
   * <p><b>Configuration details:</b>
   *
   * <ul>
   *   <li>{@link JavaTimeModule} - Enables support for java.time.* date/time types
   *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false} - Allows adding new fields without breaking
   *       old clients
   *   <li>{@code ACCEPT_EMPTY_STRING_AS_NULL_OBJECT = true} - Treats "" as null during
   *       deserialization
   *   <li>{@code WRITE_DATES_AS_TIMESTAMPS = false} - Serializes dates as ISO-8601 strings
   *   <li>{@code SNAKE_CASE} naming - Converts camelCase to snake_case (e.g., userId → user_id)
   *   <li>{@code NON_NULL} inclusion - Omits null fields from JSON output
   * </ul>
   *
   * <p>These settings provide a good balance between flexibility (backward compatibility) and
   * consistency (standardized naming and date formats).
   *
   * @return configured ObjectMapper instance as the primary JSON processor
   */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
  }
}
