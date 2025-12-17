package com.example.arInfra.service.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.event.model.DummyEvent;
import com.example.arInfra.event.model.EventProducer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@InfraGenerated
class HealthEventServiceIT {

  private static final int DEFAULT_EVENT_COUNT = 1;
  private static final int DEFAULT_WAIT_TIME = 2;
  private static final int MAX_EVENT_COUNT = 500;
  private static final int MIN_EVENT_COUNT = 1;

  @Mock private EventProducer<DummyEvent> eventProducer;

  @InjectMocks private HealthEventService healthEventService;

  @Captor private ArgumentCaptor<List<DummyEvent>> eventsCaptor;

  @Test
  void should_trigger_single_event_with_default_parameters() {
    List<String> result =
        healthEventService.triggerDummyEvents(DEFAULT_EVENT_COUNT, DEFAULT_WAIT_TIME);

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(eventProducer).accept(anyList());
  }

  @Test
  void should_trigger_multiple_events_successfully() {
    int eventCount = 5;

    List<String> result = healthEventService.triggerDummyEvents(eventCount, DEFAULT_WAIT_TIME);

    assertEquals(eventCount, result.size());
    verify(eventProducer).accept(eventsCaptor.capture());

    List<DummyEvent> capturedEvents = eventsCaptor.getValue();
    assertEquals(eventCount, capturedEvents.size());
  }

  @Test
  void should_generate_unique_event_ids() {
    int eventCount = 10;

    List<String> result = healthEventService.triggerDummyEvents(eventCount, DEFAULT_WAIT_TIME);

    long uniqueCount = result.stream().distinct().count();
    assertEquals(eventCount, uniqueCount);
  }

  @Test
  void should_create_events_with_correct_wait_time() {
    int waitTime = 5;

    healthEventService.triggerDummyEvents(3, waitTime);

    verify(eventProducer).accept(eventsCaptor.capture());
    List<DummyEvent> capturedEvents = eventsCaptor.getValue();

    capturedEvents.forEach(
        event -> assertEquals(waitTime, event.getWaitDurationBeforeConsumingInSeconds()));
  }

  @Test
  void should_trigger_maximum_allowed_events() {
    List<String> result = healthEventService.triggerDummyEvents(MAX_EVENT_COUNT, DEFAULT_WAIT_TIME);

    assertEquals(MAX_EVENT_COUNT, result.size());
    verify(eventProducer).accept(eventsCaptor.capture());
    assertEquals(MAX_EVENT_COUNT, eventsCaptor.getValue().size());
  }

  @Test
  void should_trigger_minimum_allowed_events() {
    List<String> result = healthEventService.triggerDummyEvents(MIN_EVENT_COUNT, DEFAULT_WAIT_TIME);

    assertEquals(MIN_EVENT_COUNT, result.size());
    verify(eventProducer).accept(anyList());
  }

  @Test
  void should_throw_exception_when_event_count_is_zero() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> healthEventService.triggerDummyEvents(0, DEFAULT_WAIT_TIME));

    assertTrue(exception.getMessage().contains("Event count must be between"));
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void should_throw_exception_when_event_count_is_negative() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> healthEventService.triggerDummyEvents(-1, DEFAULT_WAIT_TIME));

    assertTrue(exception.getMessage().contains("Event count must be between"));
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void should_throw_exception_when_event_count_exceeds_maximum() {
    int exceedingCount = MAX_EVENT_COUNT + 1;

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> healthEventService.triggerDummyEvents(exceedingCount, DEFAULT_WAIT_TIME));

    assertTrue(exception.getMessage().contains("Event count must be between"));
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void should_throw_exception_when_wait_time_is_negative() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> healthEventService.triggerDummyEvents(DEFAULT_EVENT_COUNT, -1));

    assertTrue(exception.getMessage().contains("Wait time must be non-negative"));
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void should_accept_zero_wait_time() {
    List<String> result = healthEventService.triggerDummyEvents(DEFAULT_EVENT_COUNT, 0);

    assertNotNull(result);
    verify(eventProducer).accept(eventsCaptor.capture());

    DummyEvent capturedEvent = eventsCaptor.getValue().getFirst();
    assertEquals(0, capturedEvent.getWaitDurationBeforeConsumingInSeconds());
  }

  @Test
  void should_map_event_ids_to_dummy_events_correctly() {
    int eventCount = 3;

    List<String> eventIds = healthEventService.triggerDummyEvents(eventCount, DEFAULT_WAIT_TIME);

    verify(eventProducer).accept(eventsCaptor.capture());
    List<DummyEvent> capturedEvents = eventsCaptor.getValue();

    for (int i = 0; i < eventCount; i++) {
      assertEquals(eventIds.get(i), capturedEvents.get(i).getUuid());
    }
  }

  @Test
  void should_return_event_ids_in_same_order_as_created() {
    int eventCount = 5;

    List<String> eventIds = healthEventService.triggerDummyEvents(eventCount, DEFAULT_WAIT_TIME);

    verify(eventProducer).accept(eventsCaptor.capture());
    List<DummyEvent> capturedEvents = eventsCaptor.getValue();

    for (int i = 0; i < eventCount; i++) {
      assertEquals(eventIds.get(i), capturedEvents.get(i).getUuid());
    }
  }

  @Test
  void should_handle_large_wait_time_values() {
    int largeWaitTime = Integer.MAX_VALUE;

    List<String> result = healthEventService.triggerDummyEvents(DEFAULT_EVENT_COUNT, largeWaitTime);

    assertNotNull(result);
    verify(eventProducer).accept(eventsCaptor.capture());

    DummyEvent capturedEvent = eventsCaptor.getValue().getFirst();
    assertEquals(largeWaitTime, capturedEvent.getWaitDurationBeforeConsumingInSeconds());
  }
}
