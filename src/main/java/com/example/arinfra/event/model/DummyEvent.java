package com.example.arinfra.event.model;

import com.example.arinfra.InfraGenerated;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Test event for verifying messaging infrastructure functionality.
 *
 * <p>This event is used by health check endpoints to validate that the RabbitMQ messaging system is
 * working correctly, including event publishing, consumption, and handler dispatching.
 *
 * <p>The event includes a configurable wait duration that simulates processing time, allowing
 * health checks to verify that timeout and retry mechanisms are functioning as expected.
 *
 * <p><b>Associated handler:</b> This event should be handled by a service bean named {@code
 * dummyEventService} that implements {@code Consumer<InfraEvent>}.
 *
 * <p>The {@link JsonIgnoreProperties} annotation allows backward compatibility if additional fields
 * are added to serialized events in the queue.
 */
@InfraGenerated
@Getter
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DummyEvent extends InfraEvent {

  private static final Duration MAX_CONSUMER_BACKOFF_BET_RETRIES = Duration.ofSeconds(5);

  /**
   * Unique identifier for this event instance. Used for tracing and correlation across distributed
   * systems.
   */
  private String id;

  /**
   * Duration in seconds that the consumer should wait before processing this event. Used to
   * simulate processing time in health checks and load testing.
   */
  private int waitDurationBeforeConsumingInSeconds;

  /**
   * Returns the maximum duration allowed for processing this event.
   *
   * <p>For DummyEvent, this is set to the configured wait duration, allowing health checks to
   * verify that timeout handling works correctly with various processing durations.
   *
   * @return the maximum consumer duration equal to the configured wait time
   */
  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(waitDurationBeforeConsumingInSeconds);
  }

  /**
   * Returns the maximum backoff duration between retry attempts.
   *
   * <p>For DummyEvent, this is fixed at 5 seconds. If event processing fails, the system will wait
   * up to 5 seconds (randomized) before making the event visible again for retry.
   *
   * @return the maximum backoff duration of 5 seconds
   */
  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return MAX_CONSUMER_BACKOFF_BET_RETRIES;
  }
}
