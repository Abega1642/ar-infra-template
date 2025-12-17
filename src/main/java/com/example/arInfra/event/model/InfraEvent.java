package com.example.arInfra.event.model;

import com.example.arInfra.InfraGenerated;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base class for all infrastructure events in the RabbitMQ messaging system.
 *
 * <p>All events published to RabbitMQ must extend this class and implement the required timing
 * configuration methods. This class provides visibility timeout calculation, retry handling, and
 * polymorphic deserialization support.
 *
 * <p>The {@link JsonTypeInfo} annotation enables polymorphic deserialization by including the
 * concrete class name in the JSON payload, allowing RabbitMQ consumers to correctly reconstruct the
 * original event type.
 *
 * <p><b>Implementation requirements:</b>
 *
 * <ul>
 *   <li>Implement {@link #maxConsumerDuration()} to specify maximum processing time
 *   <li>Implement {@link #maxConsumerBackoffBetweenRetries()} to define retry delay
 *   <li>Ensure the event class is {@link Serializable} for message queue persistence
 * </ul>
 */
@InfraGenerated
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class InfraEvent implements Serializable {

  private static final int MAX_HANDLER_INIT_DURATION_IN_SECOND = 90;
  private static final SecureRandom sRAND = new SecureRandom();

  /**
   * The number of times this event has been attempted for processing. Incremented after each failed
   * attempt before retry.
   */
  @Getter @Setter protected int attemptNb;

  /**
   * Defines the maximum duration allowed for a consumer to process this event.
   *
   * <p>This duration should account for all processing logic, database operations, external API
   * calls, and any other work required to handle the event. If processing exceeds this duration,
   * the event may be redelivered or considered failed.
   *
   * <p><b>Implementation guidance:</b> Set this conservatively high enough to handle normal
   * processing plus some buffer, but low enough to detect hung consumers.
   *
   * @return the maximum duration a consumer can take to process this event
   */
  public abstract Duration maxConsumerDuration();

  /**
   * Returns the maximum duration allowed for event handler initialization.
   *
   * <p>This provides time for the consumer application to start up, establish connections, and
   * prepare to process events before timing out.
   *
   * @return the maximum initialization duration (default: 90 seconds)
   */
  public Duration eventHandlerInitMaxDuration() {
    return Duration.ofSeconds(MAX_HANDLER_INIT_DURATION_IN_SECOND);
  }

  /**
   * Defines the maximum backoff duration between retry attempts for this event.
   *
   * <p>When event processing fails, the system will wait a random duration up to this maximum
   * before making the event visible again for retry. This helps prevent thundering herd problems
   * and gives transient issues time to resolve.
   *
   * <p><b>Implementation guidance:</b> Consider exponential backoff strategies for events that may
   * experience temporary failures (e.g., external service timeouts).
   *
   * @return the maximum backoff duration between retries
   */
  public abstract Duration maxConsumerBackoffBetweenRetries();

  /**
   * Calculates a randomized visibility timeout for this event in the message queue.
   *
   * <p>The visibility timeout determines how long a message remains invisible to other consumers
   * after being received. This calculation includes:
   *
   * <ul>
   *   <li>Handler initialization time
   *   <li>Maximum processing duration
   *   <li>Random backoff (0 to max backoff) for retry jitter
   * </ul>
   *
   * <p>The randomization prevents multiple failed events from all becoming visible simultaneously,
   * which could overwhelm the system.
   *
   * @return the calculated visibility timeout duration
   */
  public final Duration randomVisibilityTimeout() {
    return eventHandlerInitMaxDuration()
        .plus(maxConsumerDuration())
        .plus(Duration.ofSeconds(sRAND.nextLong(maxConsumerBackoffBetweenRetries().toSeconds())));
  }

  /**
   * Returns the source identifier for this event type.
   *
   * <p>By default, returns the simple class name of the concrete event class. This is useful for
   * logging, monitoring, and routing events to appropriate handlers.
   *
   * @return the event source identifier (defaults to class simple name)
   */
  public String getEventSource() {
    return getClass().getSimpleName();
  }
}
