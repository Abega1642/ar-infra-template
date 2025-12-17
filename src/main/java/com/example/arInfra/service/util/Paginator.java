package com.example.arInfra.service.util;

import com.example.arInfra.InfraGenerated;
import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

/**
 * Utility component for validating and normalizing pagination parameters.
 *
 * <p>This component provides safe pagination parameter handling with default values and validation
 * to prevent abuse and resource exhaustion attacks. It ensures that pagination requests stay within
 * reasonable bounds to protect database and memory resources.
 *
 * <p><b>Default values:</b>
 *
 * <ul>
 *   <li>Default page: 0 (first page)
 *   <li>Default size: 10 items per page
 *   <li>Maximum size: 500 items per page
 *   <li>Maximum page: 10,000 (prevents excessive offset queries)
 * </ul>
 *
 * <p><b>Security considerations:</b>
 *
 * <ul>
 *   <li>Prevents negative page numbers
 *   <li>Enforces minimum size of 1
 *   <li>Enforces maximum size to prevent memory exhaustion
 *   <li>Enforces maximum page to prevent database abuse
 * </ul>
 *
 * <p><b>Example usage:</b>
 *
 * <pre>{@code
 * Paginator paginator = new Paginator();
 * Map<String, Integer> params = paginator.apply(null, null);
 * // Returns: {page=0, size=10}
 *
 * params = paginator.apply(2, 50);
 * // Returns: {page=2, size=50}
 *
 * params = paginator.apply(5, 1000);
 * // Throws: IllegalArgumentException (exceeds MAX_SIZE)
 * }</pre>
 *
 * <p>Implements {@link BiFunction} to allow functional-style composition in pagination pipelines.
 */
@Component
@InfraGenerated
public class Paginator implements BiFunction<Integer, Integer, Map<String, Integer>> {

  /** Default page number when none is specified. */
  private static final int DEFAULT_PAGE = 0;

  /** Default page size when none is specified. */
  private static final int DEFAULT_SIZE = 10;

  /**
   * Maximum allowed page size to prevent memory exhaustion and database performance issues.
   * Requests exceeding this limit will be rejected.
   */
  private static final int MAX_SIZE = 500;

  /**
   * Maximum allowed page number to prevent excessive database offset queries. Large offsets can
   * cause significant performance degradation.
   */
  private static final int MAX_PAGE = 10_000;

  /**
   * Validates and normalizes pagination parameters with security bounds.
   *
   * <p>This method applies default values for null parameters and validates that all parameters are
   * within safe, reasonable bounds to prevent resource abuse.
   *
   * <p><b>Validation rules:</b>
   *
   * <ul>
   *   <li>Page must be non-negative (0 ≤ page ≤ 10,000)
   *   <li>Size must be positive and not exceed maximum (1 ≤ size ≤ 500)
   * </ul>
   *
   * <p><b>Default behavior:</b>
   *
   * <ul>
   *   <li>Null page → 0 (first page)
   *   <li>Null size → 10 (default page size)
   * </ul>
   *
   * @param page the page number (0-based), or null to use default (0)
   * @param size the number of items per page, or null to use default (10)
   * @return a map containing validated "page" and "size" parameters
   * @throws IllegalArgumentException if page is negative
   * @throws IllegalArgumentException if size is less than 1
   * @throws IllegalArgumentException if size exceeds {@value #MAX_SIZE}
   * @throws IllegalArgumentException if page exceeds {@value #MAX_PAGE}
   */
  @Override
  public Map<String, Integer> apply(Integer page, Integer size) {
    var fPage = (null == page) ? DEFAULT_PAGE : page;
    var fSize = (null == size) ? DEFAULT_SIZE : size;

    if (0 > fPage) throw new IllegalArgumentException("Page cannot be negative");

    if (fPage > MAX_PAGE)
      throw new IllegalArgumentException(
          "Page cannot exceed " + MAX_PAGE + " (requested: " + fPage + ")");

    if (1 > fSize) throw new IllegalArgumentException("Size cannot be less than 1");

    if (fSize > MAX_SIZE)
      throw new IllegalArgumentException(
          "Size cannot exceed " + MAX_SIZE + " (requested: " + fSize + ")");

    return Map.of(
        "page", fPage,
        "size", fSize);
  }
}
