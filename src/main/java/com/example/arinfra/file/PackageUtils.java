package com.example.arinfra.file;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import java.util.Objects;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

/**
 * Utility class for package hierarchy operations with comprehensive validation.
 *
 * <p>Provides methods to safely traverse and analyze Java package structures with protection
 * against security vulnerabilities including path traversal and injection attacks.
 *
 * <p><b>Security Features:</b>
 *
 * <ul>
 *   <li>Validates package names against Java naming conventions
 *   <li>Prevents directory traversal attacks
 *   <li>Blocks path separator injection
 *   <li>Sanitizes inputs for audit logging
 * </ul>
 *
 * <p><b>Thread Safety:</b> All methods are stateless and thread-safe.
 *
 * @since 1.0
 */
@InfraGenerated
public final class PackageUtils {

  private PackageUtils() {
    throw new IllegalStateException("Utility class cannot be instantiated");
  }

  /**
   * Retrieves the package name of the specified class with security validation.
   *
   * @param clazz the class to examine
   * @return the fully qualified package name, or empty string if in default package
   * @throws IllegalArgumentException if {@code clazz} is {@code null}
   * @throws SecurityException if the package name fails security validation
   */
  @NonNull
  public static String getPackage(@NonNull Class<?> clazz) {
    validateClassNotNull(clazz);
    String packageName = ClassUtils.getPackageName(clazz);
    validatePackageName(packageName);
    return StringUtils.defaultString(packageName);
  }

  /**
   * Retrieves the parent package of the specified class.
   *
   * @param clazz the class whose parent package is requested
   * @return the parent package name, or empty string if none exists
   * @throws IllegalArgumentException if {@code clazz} is {@code null}
   * @throws SecurityException if the package name fails security validation
   */
  @NonNull
  public static String getParentPackage(@NonNull Class<?> clazz) {
    validateClassNotNull(clazz);
    String packageName = ClassUtils.getPackageName(clazz);
    validatePackageName(packageName);
    return getParentPackage(packageName);
  }

  /**
   * Retrieves the parent package of the specified package name.
   *
   * @param packageName the fully qualified package name
   * @return the parent package name, or empty string if none exists
   * @throws SecurityException if the package name fails security validation
   */
  @NonNull
  public static String getParentPackage(@NonNull String packageName) {
    if (StringUtils.isBlank(packageName)) return StringUtils.EMPTY;

    validatePackageName(packageName);
    return extractParentPackage(packageName);
  }

  /**
   * Retrieves the grandparent package of the specified class.
   *
   * @param clazz the class whose grandparent package is requested
   * @return the grandparent package name, or empty string if insufficient depth
   * @throws IllegalArgumentException if {@code clazz} is {@code null}
   * @throws SecurityException if the package name fails security validation
   */
  @NonNull
  public static String getGrandparentPackage(@NonNull Class<?> clazz) {
    return getAncestorPackage(clazz, 2);
  }

  /**
   * Retrieves the grandparent package of the specified package name.
   *
   * @param packageName the fully qualified package name
   * @return the grandparent package name, or empty string if insufficient depth
   * @throws SecurityException if the package name fails security validation
   */
  @NonNull
  public static String getGrandparentPackage(@NonNull String packageName) {
    return getAncestorPackage(packageName, 2);
  }

  /**
   * Retrieves an ancestor package at the specified level above the given class.
   *
   * @param clazz the class whose ancestor package is requested
   * @param levels the number of levels to traverse upward (1 = parent, 2 = grandparent, etc.)
   * @return the ancestor package name, or empty string if insufficient depth
   * @throws IllegalArgumentException if {@code clazz} is {@code null} or {@code levels} is negative
   * @throws SecurityException if the package name fails security validation
   */
  @NonNull
  public static String getAncestorPackage(@NonNull Class<?> clazz, int levels) {
    validateClassNotNull(clazz);
    validateLevels(levels);

    String packageName = ClassUtils.getPackageName(clazz);
    validatePackageName(packageName);

    return getAncestorPackage(packageName, levels);
  }

  /**
   * Retrieves an ancestor package at the specified level above the given package.
   *
   * @param packageName the fully qualified package name
   * @param levels the number of levels to traverse upward (1 = parent, 2 = grandparent, etc.)
   * @return the ancestor package name, or empty string if insufficient depth
   * @throws IllegalArgumentException if {@code levels} is negative
   * @throws SecurityException if the package name fails security validation
   */
  @NonNull
  public static String getAncestorPackage(@NonNull String packageName, int levels) {
    if (StringUtils.isBlank(packageName)) {
      return StringUtils.EMPTY;
    }

    validateLevels(levels);
    validatePackageName(packageName);

    String result = packageName;
    for (int i = 0; i < levels; i++) {
      result = extractParentPackage(result);
      if (StringUtils.isEmpty(result)) {
        break;
      }
    }

    return result;
  }

  /**
   * Determines the depth of a package hierarchy.
   *
   * @param packageName the fully qualified package name
   * @return the number of package levels (e.g., "com.example.app" returns 3)
   * @throws SecurityException if the package name fails security validation
   */
  public static int getPackageDepth(@NonNull String packageName) {
    if (StringUtils.isBlank(packageName)) {
      return 0;
    }

    validatePackageName(packageName);
    return StringUtils.countMatches(packageName, ClassUtils.PACKAGE_SEPARATOR_CHAR) + 1;
  }

  /**
   * Validates that a class resides within the specified package hierarchy.
   *
   * @param clazz the class to validate
   * @param basePackage the base package that must contain the class
   * @return {@code true} if the class is within the package hierarchy
   * @throws IllegalArgumentException if any parameter is {@code null}
   * @throws SecurityException if package names fail security validation
   */
  public static boolean isWithinPackageHierarchy(
      @NonNull Class<?> clazz, @NonNull String basePackage) {

    validateClassNotNull(clazz);
    validatePackageName(basePackage);

    String classPackage = ClassUtils.getPackageName(clazz);
    validatePackageName(classPackage);

    return classPackage.equals(basePackage)
        || classPackage.startsWith(format("%s%c", basePackage, ClassUtils.PACKAGE_SEPARATOR_CHAR));
  }

  /**
   * Checks if one package is a direct child of another.
   *
   * @param childPackage the potential child package
   * @param parentPackage the potential parent package
   * @return {@code true} if childPackage is a direct child of parentPackage
   * @throws SecurityException if package names fail security validation
   */
  public static boolean isDirectChild(@NonNull String childPackage, @NonNull String parentPackage) {

    if (StringUtils.isAnyBlank(childPackage, parentPackage)) return false;

    validatePackageName(childPackage);
    validatePackageName(parentPackage);

    String expectedPrefix = format("%s%c", parentPackage, ClassUtils.PACKAGE_SEPARATOR_CHAR);

    return childPackage.startsWith(expectedPrefix)
        && childPackage.indexOf(ClassUtils.PACKAGE_SEPARATOR_CHAR, expectedPrefix.length())
            == StringUtils.INDEX_NOT_FOUND;
  }

  /**
   * Extracts the parent package from a validated package name.
   *
   * @param packageName the validated package name
   * @return the parent package, or empty string if none exists
   */
  @NonNull
  private static String extractParentPackage(@NonNull String packageName) {
    int lastDotIndex = packageName.lastIndexOf(ClassUtils.PACKAGE_SEPARATOR_CHAR);

    if (lastDotIndex <= 0) return StringUtils.EMPTY;

    return packageName.substring(0, lastDotIndex);
  }

  /**
   * Validates package name against security threats and Java naming conventions.
   *
   * @param packageName the package name to validate
   * @throws SecurityException if validation fails
   */
  private static void validatePackageName(@NonNull String packageName) {
    if (packageName.contains(".."))
      throw new SecurityException(
          format("Package name contains directory traversal pattern: %s", forJava(packageName)));

    if (StringUtils.containsAny(packageName, '/', '\\'))
      throw new SecurityException(
          format("Package name contains path separator: %s", forJava(packageName)));

    if (StringUtils.containsAny(packageName, ';', '<', '>', '|', '&', '$', '`'))
      throw new SecurityException(
          format("Package name contains unsafe characters: %s", forJava(packageName)));

    if (!isValidJavaPackageName(packageName))
      throw new SecurityException(
          format("Invalid Java package name format: %s", forJava(packageName)));
  }

  /**
   * Validates package name structure using Java naming rules.
   *
   * @param packageName the package name to validate
   * @return {@code true} if valid Java package name
   */
  private static boolean isValidJavaPackageName(@NonNull String packageName) {
    if (StringUtils.isBlank(packageName)) return false;

    String[] segments = StringUtils.split(packageName, ClassUtils.PACKAGE_SEPARATOR_CHAR);

    for (String segment : segments) {
      if (!isValidJavaIdentifier(segment)) return false;
    }

    return true;
  }

  /**
   * Validates a single package segment as a Java identifier.
   *
   * @param identifier the identifier to validate
   * @return {@code true} if valid Java identifier
   */
  private static boolean isValidJavaIdentifier(@NonNull String identifier) {
    if (StringUtils.isEmpty(identifier)) return false;

    if (!Character.isJavaIdentifierStart(identifier.charAt(0))) return false;

    for (int i = 1; i < identifier.length(); i++) {
      if (!Character.isJavaIdentifierPart(identifier.charAt(i))) return false;
    }

    return true;
  }

  /**
   * Validates that a class reference is not null.
   *
   * @param clazz the class to validate
   * @throws IllegalArgumentException if {@code clazz} is {@code null}
   */
  private static void validateClassNotNull(Class<?> clazz) {
    Objects.requireNonNull(clazz, "Class parameter cannot be null");
  }

  /**
   * Validates that the levels parameter is non-negative.
   *
   * @param levels the levels to validate
   * @throws IllegalArgumentException if {@code levels} is negative
   */
  private static void validateLevels(int levels) {
    if (levels < 0)
      throw new IllegalArgumentException(format("Levels parameter cannot be negative: %d", levels));
  }
}
