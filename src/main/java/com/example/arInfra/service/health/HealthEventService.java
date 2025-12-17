package com.example.arInfra.service.health;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.event.model.DummyEvent;
import com.example.arInfra.event.model.EventProducer;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@InfraGenerated
public class HealthEventService {

  private static final int MIN_EVENT_COUNT = 1;
  private static final int MAX_EVENT_COUNT = 500;

  private final EventProducer<DummyEvent> eventProducer;

  /**
   * Triggers a specified number of dummy events for health check purposes.
   *
   * @param nbEvent the number of events to trigger (must be between 1 and 500)
   * @param waitInSeconds the wait time in seconds for each event
   * @return list of UUIDs representing the triggered events
   * @throws IllegalArgumentException if nbEvent is outside the valid range
   */
  public List<String> triggerDummyEvents(int nbEvent, int waitInSeconds) {
    log.info("Triggering {} dummy event(s) with {}s wait time", nbEvent, waitInSeconds);

    validateEventCount(nbEvent);
    validateWaitTime(waitInSeconds);

    List<String> eventIds = generateEventIds(nbEvent);
    List<DummyEvent> events = createDummyEvents(eventIds, waitInSeconds);

    publishEvents(events);

    log.info("Successfully triggered {} dummy event(s)", nbEvent);
    return eventIds;
  }

  /**
   * Validates that the event count is within acceptable bounds.
   *
   * @param nbEvent the number of events to validate
   * @throws IllegalArgumentException if the count is invalid
   */
  private void validateEventCount(int nbEvent) {
    if (nbEvent < MIN_EVENT_COUNT || nbEvent > MAX_EVENT_COUNT) {
      String errorMessage =
          String.format(
              "Event count must be between %d and %d, but was: %d",
              MIN_EVENT_COUNT, MAX_EVENT_COUNT, nbEvent);
      log.error(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Validates that the wait time is non-negative.
   *
   * @param waitInSeconds the wait time to validate
   * @throws IllegalArgumentException if the wait time is negative
   */
  private void validateWaitTime(int waitInSeconds) {
    if (waitInSeconds < 0) {
      String errorMessage =
          String.format("Wait time must be non-negative, but was: %d", waitInSeconds);
      log.error(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Generates a list of unique event identifiers.
   *
   * @param count the number of IDs to generate
   * @return list of UUID strings
   */
  private List<String> generateEventIds(int count) {
    List<String> eventIds =
        IntStream.range(0, count).mapToObj(i -> UUID.randomUUID().toString()).toList();

    log.debug("Generated {} event ID(s)", eventIds.size());
    return eventIds;
  }

  /**
   * Creates dummy events from the given event IDs and wait time.
   *
   * @param eventIds the list of event identifiers
   * @param waitInSeconds the wait time for each event
   * @return list of DummyEvent objects
   */
  private List<DummyEvent> createDummyEvents(List<String> eventIds, int waitInSeconds) {
    return eventIds.stream().map(id -> new DummyEvent(id, waitInSeconds)).toList();
  }

  /**
   * Publishes events through the event producer.
   *
   * @param events the list of events to publish
   */
  private void publishEvents(List<DummyEvent> events) {
    eventProducer.accept(events);
    log.debug("Published {} event(s) to event producer", events.size());
  }
}
