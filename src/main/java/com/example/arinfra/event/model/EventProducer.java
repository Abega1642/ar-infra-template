package com.example.arinfra.event.model;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.datastructure.ListGrouper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generic event producer for publishing infrastructure events to RabbitMQ.
 *
 * <p>This component handles the serialization and publishing of {@link InfraEvent} instances to a
 * configured RabbitMQ exchange. Events are processed in batches for efficiency, with automatic
 * grouping when collections exceed the maximum batch size.
 *
 * <p><b>Features:</b>
 *
 * <ul>
 *   <li>Automatic batching (max 10 events per batch)
 *   <li>JSON serialization with polymorphic type information
 *   <li>Comprehensive error handling and logging
 *   <li>Thread-safe event publishing
 * </ul>
 *
 * <p><b>Required application properties:</b>
 *
 * <ul>
 *   <li>{@code spring.rabbitmq.exchange} - Target exchange name
 *   <li>{@code spring.rabbitmq.routing-key} - Routing key for message routing
 * </ul>
 *
 * <p>This class implements {@link Consumer} to allow functional-style event publishing and
 * integration with reactive streams or batch processing pipelines.
 *
 * @param <T> the type of infrastructure event, must extend {@link InfraEvent}
 */
@InfraGenerated
@Component
@Slf4j
public class EventProducer<T extends InfraEvent> implements Consumer<Collection<T>> {

  private static final int MAX_EVENTS_PER_BATCH = 10;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;
  private final String exchangeName;
  private final String routingKey;
  private final ListGrouper<T> listGrouper;

  /**
   * Constructs an EventProducer with required dependencies and configuration.
   *
   * @param rabbitTemplate the Spring AMQP template for RabbitMQ operations
   * @param objectMapper the Jackson ObjectMapper for JSON serialization
   * @param exchangeName the target RabbitMQ exchange name (from properties)
   * @param routingKey the routing key for message routing (from properties)
   * @param listGrouper utility for grouping events into batches
   */
  public EventProducer(
      RabbitTemplate rabbitTemplate,
      ObjectMapper objectMapper,
      @Value("${spring.rabbitmq.exchange}") String exchangeName,
      @Value("${spring.rabbitmq.routing-key}") String routingKey,
      ListGrouper<T> listGrouper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
    this.exchangeName = exchangeName;
    this.routingKey = routingKey;
    this.listGrouper = listGrouper;
  }

  /**
   * Accepts and publishes a collection of events to RabbitMQ.
   *
   * <p>Events are automatically grouped into batches of up to 10 events each for efficient
   * processing. Each event is serialized to JSON (preserving polymorphic type information) and
   * published to the configured exchange and routing key.
   *
   * <p>If the collection is null or empty, a warning is logged and no action is taken.
   *
   * <p><b>Error handling:</b> Serialization and publishing errors are logged but do not prevent
   * other events in the batch from being processed.
   *
   * @param events the collection of events to publish (may be null or empty)
   */
  @Override
  public void accept(Collection<T> events) {
    if (events == null || events.isEmpty()) {
      log.warn("No events to publish.");
      return;
    }

    listGrouper.apply(List.copyOf(events), MAX_EVENTS_PER_BATCH).forEach(this::publishBatch);
  }

  /**
   * Publishes a batch of events to RabbitMQ.
   *
   * <p>Logs the batch size and routing information, then publishes each event individually. This
   * method is called internally by {@link #accept(Collection)}.
   *
   * @param batch the batch of events to publish (non-null, non-empty)
   */
  private void publishBatch(List<T> batch) {
    log.info(
        "Publishing batch of {} events to exchange '{}' with routing '{}'",
        batch.size(),
        exchangeName,
        routingKey);

    batch.forEach(this::publishEvent);
  }

  /**
   * Publishes a single event to RabbitMQ.
   *
   * <p>Serializes the event to JSON and sends it to the configured exchange. Errors during
   * serialization or publishing are caught and logged without propagating exceptions.
   *
   * @param event the event to publish (non-null)
   */
  private void publishEvent(T event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
      log.debug("Published event: {}", event.getClass().getSimpleName());
    } catch (JsonProcessingException e) {
      log.error("Serialization failed for event: {}", event, e);
    } catch (Exception e) {
      log.error("Publishing failed for event: {}", event, e);
    }
  }
}
