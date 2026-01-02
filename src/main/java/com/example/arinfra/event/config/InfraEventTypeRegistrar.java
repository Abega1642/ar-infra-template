package com.example.arinfra.event.config;

import static com.example.arinfra.file.PackageUtils.getParentPackage;
import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.config.JacksonConfiguration.PolymorphicTypeRegistrar;
import com.example.arinfra.event.model.InfraEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

/**
 * Automatic registrar for InfraEvent polymorphic subtypes.
 *
 * <p>This component automatically discovers all classes extending {@link InfraEvent} at application
 * startup and registers them with Jackson for secure polymorphic deserialization.
 *
 * <p><b>How it works:</b>
 *
 * <ol>
 *   <li>Scans classpath for all InfraEvent subclasses
 *   <li>Registers each with its simple class name as the type identifier
 *   <li>Jackson can now safely deserialize using type names instead of class names
 * </ol>
 *
 * <p><b>Security:</b> By using simple class names (e.g., "UserCreatedEvent") instead of fully
 * qualified names (e.g., "com.example.UserCreatedEvent"), we prevent attackers from specifying
 * arbitrary classes for instantiation.
 *
 * <p><b>Developer Experience:</b> No manual registration needed. Just create a new event class
 * extending InfraEvent, and it's automatically discovered and registered.
 */
@Slf4j
@Component
@InfraGenerated
@RequiredArgsConstructor
public class InfraEventTypeRegistrar implements PolymorphicTypeRegistrar {

  private static final String EVENT_BASE_PACKAGE = getParentPackage(InfraEvent.class);

  private final ObjectMapper objectMapper;

  /** Automatically registers all InfraEvent subtypes at application startup. */
  @PostConstruct
  public void registerTypes() {
    registerTypes(objectMapper);
  }

  @Override
  public void registerTypes(ObjectMapper mapper) {
    Set<Class<? extends InfraEvent>> eventClasses = findInfraEventSubclasses();

    if (eventClasses.isEmpty()) {
      log.warn(
          format(
              "%s %s",
              "No InfraEvent subtypes found in package '{}'.",
              "Verify that event classes exist and package path is correct."),
          forJava(EVENT_BASE_PACKAGE));
      return;
    }

    int registeredCount = 0;
    for (Class<? extends InfraEvent> eventClass : eventClasses) {
      String typeName = eventClass.getSimpleName();
      mapper.registerSubtypes(new NamedType(eventClass, typeName));
      log.debug(
          "Registered polymorphic type: {} -> {}",
          forJava(typeName),
          forJava(eventClass.getName()));
      registeredCount++;
    }

    log.info(
        "Auto-registered {} InfraEvent subtypes for secure polymorphic deserialization",
        registeredCount);
  }

  /**
   * Scans the classpath to find all concrete classes that extend InfraEvent and registers them
   * directly without unsafe reflection.
   *
   * @return set of all discovered InfraEvent subclasses
   */
  private Set<Class<? extends InfraEvent>> findInfraEventSubclasses() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);

    scanner.addIncludeFilter(new AssignableTypeFilter(InfraEvent.class));

    Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(EVENT_BASE_PACKAGE);

    return candidateComponents.stream()
        .map(this::loadClassFromBeanDefinition)
        .filter(clazz -> clazz != null && !clazz.equals(InfraEvent.class))
        .collect(java.util.stream.Collectors.toSet());
  }

  /**
   * Safely loads a class from a BeanDefinition. Uses Spring's resource loading mechanism which is
   * considered safe.
   *
   * @param beanDefinition the Spring BeanDefinition
   * @return the Class object, or null if loading fails
   */
  @SuppressWarnings("unchecked")
  private Class<? extends InfraEvent> loadClassFromBeanDefinition(BeanDefinition beanDefinition) {
    String className = getClassNameFromBeanDefinition(beanDefinition);

    if (!isValidClassName(className)) return null;

    try {
      Class<?> loadedClass = loadAndValidateClass(className);
      return (Class<? extends InfraEvent>) loadedClass;
    } catch (IllegalArgumentException | LinkageError e) {
      log.error("Failed to load event class: {}", forJava(className), e);
      return null;
    }
  }

  /**
   * Extracts the class name from a BeanDefinition. For AnnotatedBeanDefinitions, uses metadata;
   * otherwise uses bean class name.
   *
   * @param beanDefinition the Spring BeanDefinition
   * @return the fully qualified class name
   */
  private String getClassNameFromBeanDefinition(BeanDefinition beanDefinition) {
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
  private boolean isValidClassName(String className) {
    if (className == null || !className.startsWith(EVENT_BASE_PACKAGE)) {
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
   * Loads a class using Spring's ClassUtils and validates it extends InfraEvent.
   *
   * @param className the fully qualified class name
   * @return the loaded and validated Class object
   * @throws IllegalArgumentException if class cannot be loaded or doesn't extend InfraEvent
   */
  private Class<?> loadAndValidateClass(String className) {
    ClassLoader classLoader = this.getClass().getClassLoader();
    Class<?> loadedClass =
        org.springframework.util.ClassUtils.resolveClassName(className, classLoader);

    if (!InfraEvent.class.isAssignableFrom(loadedClass)) {
      log.warn("Loaded class does not extend InfraEvent: {}", forJava(className));
      throw new IllegalArgumentException("Class does not extend InfraEvent: " + className);
    }

    return loadedClass;
  }
}
