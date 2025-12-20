package com.example.arInfra.validator;

import com.example.arInfra.InfraGenerated;
import jakarta.validation.constraints.NotNull;

/**
 * Base interface for all infrastructure validators.
 *
 * <p>All validator components must implement this interface to ensure consistent validation
 * behavior across the system. Implementations should be stateless Spring components that provide
 * validation logic without side effects.
 *
 * <p>Implementations must:
 *
 * <ul>
 *   <li>Be annotated with {@code @Component} for Spring dependency injection
 *   <li>Be annotated with {@code @Validated} to enable method-level validation
 *   <li>Use {@code @NotNull} and other JSR-380 annotations on method parameters
 *   <li>Throw {@link IllegalArgumentException} for validation failures
 *   <li>Throw {@link SecurityException} for security-related validation failures
 *   <li>Never return null from validation methods
 * </ul>
 *
 * <p>Example implementation:
 *
 * <pre>
 * {@code @Component
 * @Validated
 * public class MyValidator implements Validator <MyType> {
 *     public void validate(@NotNull MyType input) {
 *         if (isInvalid(input)) {
 *             throw new IllegalArgumentException("Validation failed");
 *         }
 *     }
 *
 *     public <MyType> getValidatedType() {
 *         return MyType.class;
 *     }
 * }}
 *
 * </pre>
 *
 * @param <T> the type of object this validator validates
 */
@InfraGenerated
public interface Validator<T> {

  /**
   * Validates the given input according to implementation-specific rules.
   *
   * @param input the object to validate, must not be null
   * @throws IllegalArgumentException if validation fails due to invalid input
   * @throws SecurityException if validation fails due to security constraints
   */
  void validate(@NotNull T input);

  /**
   * Returns the type of object this validator can validate.
   *
   * @return the class object representing the validated type
   */
  Class<T> getValidatedType();
}
