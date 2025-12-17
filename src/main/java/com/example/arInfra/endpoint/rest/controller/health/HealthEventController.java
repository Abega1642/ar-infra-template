package com.example.arInfra.endpoint.rest.controller.health;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.service.health.HealthEventService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for event/messaging health check operations.
 *
 * <p>Provides endpoints to verify event publishing and message queue functionality by triggering
 * dummy events with configurable frequency and delays.
 */
@InfraGenerated
@RestController
@AllArgsConstructor
public class HealthEventController {
  private final HealthEventService healthEventService;

  /**
   * Triggers a series of dummy events to verify messaging functionality.
   *
   * <p>This endpoint publishes test events to verify that the event/messaging infrastructure is
   * working correctly. The number of events and delay between them can be configured via query
   * parameters.
   *
   * @param nbEvent the number of events to trigger (default: 1)
   * @param waitInSeconds the delay in seconds between each event (default: 2)
   * @return a list of status messages for each triggered event
   */
  @GetMapping("/health/message")
  public List<String> triggerDummyEvents(
      @RequestParam(defaultValue = "1") int nbEvent,
      @RequestParam(defaultValue = "2") int waitInSeconds) {
    return healthEventService.triggerDummyEvents(nbEvent, waitInSeconds);
  }
}
