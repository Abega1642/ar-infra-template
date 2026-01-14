package com.example.arinfra.conf.db;

import static com.example.arinfra.file.PackageUtils.getParentPackage;
import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.conf.FacadeIT;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * Factory for obtaining database test container configurations in integration tests.
 *
 * <p>This factory provides the database configuration used by {@link FacadeIT} without requiring
 * manual instantiation or configuration. It automatically detects which database implementation is
 * available in your project.
 *
 * <p><b>Default Behavior:</b><br>
 * The factory automatically discovers and uses the available database configuration. No
 * configuration files or properties are needed:
 *
 * <pre>{@code
 * // In your test base class (typically FacadeIT)
 * private static final PersistenceConf DB_CONF = PersistenceConfFactory.create();
 *
 * @BeforeAll
 * static void setup() {
 *   DB_CONF.start(); // Starts the database container
 * }
 *
 * @DynamicPropertySource
 * static void configureProperties(DynamicPropertyRegistry registry) {
 *   DB_CONF.configureProperties(registry); // Injects connection properties
 * }
 * }</pre>
 *
 * <p><b>Selecting a Specific Database:</b><br>
 * If multiple database implementations are present, you can specify which one to use:
 *
 * <pre>{@code
 * // Using system property
 * mvn test -Dtest.database.type=mysql
 *
 * // Using environment variable
 * export TEST_DATABASE_TYPE=postgresql
 * mvn test
 * }</pre>
 *
 * <p><b>Supported Database Types:</b><br>
 * The database type corresponds to the value returned by {@link PersistenceConf#getDatabaseType()}.
 * Common values include:
 *
 * <ul>
 *   <li>{@code postgresql} - PostgreSQL database
 *   <li>{@code mysql} - MySQL database
 *       <p><b>Error Handling:</b><br>
 *       The factory throws {@link IllegalStateException} if no database implementation is found:
 *       <pre>
 * IllegalStateException: No PersistenceConf implementation found in package: com.example.arinfra.conf
 * Ensure at least one database configuration class (PostgresConf, MysqlConf, etc.) exists.
 * </pre>
 *       It throws {@link IllegalArgumentException} if an explicitly specified database type is not
 *       found:
 *       <pre>
 * IllegalArgumentException: No PersistenceConf implementation found for database type: oracle
 * Available implementations: postgresql, mysql
 * </pre>
 *
 * @see PersistenceConf
 * @see FacadeIT
 */
@Slf4j
@InfraGenerated
public class PersistenceConfFactory {

  private static final String CONF_BASE_PACKAGE = getParentPackage(PersistenceConf.class);
  private static final String DB_TYPE_PROPERTY = "test.database.type";
  private static final String DB_TYPE_ENV = "TEST_DATABASE_TYPE";

  /**
   * Creates the appropriate database configuration instance using auto-discovery.
   *
   * <p>Discovery process:
   *
   * <ol>
   *   <li>Check for explicit override (system property or environment variable)
   *   <li>If override specified, find matching implementation by database type
   *   <li>Otherwise, return first discovered implementation
   *   <li>Throw exception if no implementation found
   * </ol>
   *
   * @return a configured PersistenceConf instance
   * @throws IllegalStateException if no database implementation is found
   */
  public static PersistenceConf create() {
    String explicitDbType = getExplicitDbType();
    Set<Class<? extends PersistenceConf>> implementations = findPersistenceConfImplementations();

    if (implementations.isEmpty()) {
      throw new IllegalStateException(
          "No PersistenceConf implementation found in package: "
              + CONF_BASE_PACKAGE
              + ". Ensure at least one database configuration class (PostgresConf, MysqlConf, etc.)"
              + " exists.");
    }

    if (explicitDbType != null) {
      return createExplicit(explicitDbType, implementations);
    }

    return autoDiscover(implementations);
  }

  /**
   * Auto-discovers and instantiates the first available PersistenceConf implementation.
   *
   * @param implementations set of discovered PersistenceConf implementations
   * @return the first discovered and instantiated PersistenceConf
   * @throws IllegalStateException if instantiation fails
   */
  private static PersistenceConf autoDiscover(
      Set<Class<? extends PersistenceConf>> implementations) {
    Class<? extends PersistenceConf> implClass = implementations.iterator().next();

    try {
      PersistenceConf instance = implClass.getDeclaredConstructor().newInstance();
      log.info(
          "Auto-discovered database: {} ({})",
          instance.getDatabaseType(),
          forJava(implClass.getSimpleName()));
      return instance;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to instantiate database configuration: " + implClass.getName(), e);
    }
  }

  /**
   * Creates a database instance based on explicit configuration by matching database type.
   *
   * @param dbType the database type specified by user
   * @param implementations set of available implementations
   * @return the configured PersistenceConf instance matching the type
   * @throws IllegalArgumentException if the specified type is not found
   */
  private static PersistenceConf createExplicit(
      String dbType, Set<Class<? extends PersistenceConf>> implementations) {

    log.info("Looking for explicitly specified database type: {}", forJava(dbType));

    for (Class<? extends PersistenceConf> implClass : implementations) {
      try {
        PersistenceConf instance = implClass.getDeclaredConstructor().newInstance();
        if (dbType.equalsIgnoreCase(instance.getDatabaseType())) {
          log.info(
              "Found matching database: {} ({})",
              instance.getDatabaseType(),
              forJava(implClass.getSimpleName()));
          return instance;
        }
      } catch (Exception e) {
        log.warn(
            "Failed to instantiate {} for type checking: {}",
            forJava(implClass.getSimpleName()),
            e.getMessage());
      }
    }

    throw new IllegalArgumentException(
        "No PersistenceConf implementation found for database type: "
            + dbType
            + ". "
            + "Available implementations: "
            + getAvailableDatabaseTypes(implementations));
  }

  /**
   * Scans the classpath to find all concrete classes implementing PersistenceConf.
   *
   * @return set of all discovered PersistenceConf implementations
   */
  private static Set<Class<? extends PersistenceConf>> findPersistenceConfImplementations() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);

    scanner.addIncludeFilter(new AssignableTypeFilter(PersistenceConf.class));

    Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(CONF_BASE_PACKAGE);

    return candidateComponents.stream()
        .map(PersistenceConfFactory::loadClassFromBeanDefinition)
        .filter(clazz -> clazz != null && !clazz.equals(PersistenceConf.class))
        .collect(java.util.stream.Collectors.toSet());
  }

  /**
   * Safely loads a class from a BeanDefinition.
   *
   * @param beanDefinition the Spring BeanDefinition
   * @return the Class object, or null if loading fails
   */
  @SuppressWarnings("unchecked")
  private static Class<? extends PersistenceConf> loadClassFromBeanDefinition(
      BeanDefinition beanDefinition) {
    String className = getClassNameFromBeanDefinition(beanDefinition);

    if (!isValidClassName(className)) return null;

    try {
      Class<?> loadedClass = loadAndValidateClass(className);
      return (Class<? extends PersistenceConf>) loadedClass;
    } catch (IllegalArgumentException | LinkageError e) {
      log.error("Failed to load persistence configuration class: {}", forJava(className), e);
      return null;
    }
  }

  /**
   * Extracts the class name from a BeanDefinition.
   *
   * @param beanDefinition the Spring BeanDefinition
   * @return the fully qualified class name
   */
  private static String getClassNameFromBeanDefinition(BeanDefinition beanDefinition) {
    if (beanDefinition
        instanceof org.springframework.beans.factory.annotation.AnnotatedBeanDefinition annotated)
      return annotated.getMetadata().getClassName();

    return beanDefinition.getBeanClassName();
  }

  /**
   * Validates that a class name is safe to load.
   *
   * @param className the class name to validate
   * @return true if valid, false otherwise
   */
  private static boolean isValidClassName(String className) {
    if (className == null || !className.startsWith(CONF_BASE_PACKAGE)) {
      log.warn(
          "Rejecting class loading attempt for class outside expected package: {}",
          forJava(className));
      return false;
    }

    if (className.contains("..") || className.contains("/") || className.contains("\\")) {
      log.warn(
          "Rejecting class loading attempt with suspicious characters: {}", forJava(className));
      return false;
    }

    return true;
  }

  /**
   * Loads a class using Spring's ClassUtils and validates it implements PersistenceConf.
   *
   * @param className the fully qualified class name
   * @return the loaded and validated Class object
   * @throws IllegalArgumentException if class cannot be loaded or doesn't implement PersistenceConf
   */
  private static Class<?> loadAndValidateClass(String className) {
    ClassLoader classLoader = PersistenceConfFactory.class.getClassLoader();
    Class<?> loadedClass = ClassUtils.resolveClassName(className, classLoader);

    if (!PersistenceConf.class.isAssignableFrom(loadedClass)) {
      log.warn("Loaded class does not implement PersistenceConf: {}", forJava(className));
      throw new IllegalArgumentException("Class does not implement PersistenceConf: " + className);
    }

    return loadedClass;
  }

  /**
   * Gets the explicitly specified database type from system properties or environment.
   *
   * @return the database type if specified, null otherwise
   */
  private static String getExplicitDbType() {
    String dbType = System.getProperty(DB_TYPE_PROPERTY);
    if (dbType != null && !dbType.isBlank()) {
      return dbType.trim();
    }

    dbType = System.getenv(DB_TYPE_ENV);
    if (dbType != null && !dbType.isBlank()) {
      return dbType.trim();
    }

    return null;
  }

  /**
   * Gets a human-readable list of available database types from implementations.
   *
   * @param implementations set of PersistenceConf implementations
   * @return comma-separated list of database types
   */
  private static String getAvailableDatabaseTypes(
      Set<Class<? extends PersistenceConf>> implementations) {
    return implementations.stream()
        .map(
            implClass -> {
              try {
                return implClass.getDeclaredConstructor().newInstance().getDatabaseType();
              } catch (Exception e) {
                return implClass.getSimpleName();
              }
            })
        .collect(java.util.stream.Collectors.joining(", "));
  }

  private PersistenceConfFactory() {
    throw new UnsupportedOperationException("This class is not supposed to be instantiated.");
  }
}
