package com.example.arinfra.event.consumer;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.event.model.InfraEvent;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Event dispatcher that routes infrastructure events to their corresponding service handlers.
 *
 * <p>This component implements a convention-based routing mechanism where events are automatically
 * dispatched to Spring beans named according to the pattern: {@code <eventName>Service} (e.g.,
 * {@code UserCreateRequested} → {@code UserCreateRequestedService}).
 *
 * <p><b>Naming convention:</b>
 *
 * <ul>
 *   <li>Event class: {@code MyCustomEvent}
 *   <li>Expected service bean name: {@code myCustomEventService}
 *   <li>Service must implement: {@code Consumer<InfraEvent>}
 * </ul>
 *
 * <p>If no matching service bean is found, a warning is logged and the event is silently dropped
 * (no exception is thrown). This allows the system to gracefully handle events for which no handler
 * has been registered.
 *
 * <p><b>Example handler implementation:</b>
 *
 * <pre>{@code
 * @Service("UserCreateRequestedService")
 * public class UserCreateRequestedService implements Consumer<InfraEvent> {
 *   public void accept(InfraEvent event) {
 *     UserCreateRequested userEvent = (UserCreateRequested) event;
 *     // Handle the event...
 *   }
 * }
 * }</pre>
 */
@InfraGenerated
@Component
@Slf4j
public class EventDispatcher implements Consumer<InfraEvent>, ApplicationContextAware {
  private static final String EXPECTED_CONSUMER_NAME_SUFFIX = "Service";
  private ApplicationContext applicationContext;

  /**
   * Sets the Spring application context for bean lookup.
   *
   * <p>This method is called automatically by Spring during component initialization to provide
   * access to the application context for dynamic bean resolution.
   *
   * @param applicationContext the Spring application context
   * @throws BeansException if context initialization fails
   */
  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext)
      throws BeansException {
    this.applicationContext = applicationContext;
  }

  /**
   * Dispatches an event to its corresponding service handler.
   *
   * <p>Determines the target service bean name by converting the event's simple class name to
   * camelCase and appending "Service" (e.g., {@code MyEvent} → {@code myEventService}). The service
   * is looked up in the Spring application context and invoked with the event.
   *
   * <p><b>Behavior:</b>
   *
   * <ul>
   *   <li>If a matching service bean is found, the event is dispatched to it
   *   <li>If no service bean exists, a warning is logged and the event is dropped
   *   <li>The service must implement {@code Consumer<InfraEvent>}
   * </ul>
   *
   * <p>This convention-based approach eliminates the need for explicit event-to-handler mappings,
   * relying instead on consistent naming conventions.
   *
   * @param event the infrastructure event to dispatch (non-null)
   */
  @Override
  public void accept(InfraEvent event) {
    String eventSimpleName = event.getClass().getSimpleName();
    String serviceBeanName =
        Character.toLowerCase(eventSimpleName.charAt(0))
            + eventSimpleName.substring(1)
            + EXPECTED_CONSUMER_NAME_SUFFIX;

    try {
      @SuppressWarnings("unchecked")
      Consumer<InfraEvent> consumer =
          (Consumer<InfraEvent>) applicationContext.getBean(serviceBeanName);

      log.info("Dispatching {} to {}", eventSimpleName, serviceBeanName);
      consumer.accept(event);
    } catch (NoSuchBeanDefinitionException e) {
      log.warn(
          "No service found for event {} (looking for bean: {})", eventSimpleName, serviceBeanName);
    }
  }
}
