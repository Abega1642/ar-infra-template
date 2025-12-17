package com.example.arInfra.event.consumer;

import com.example.arInfra.InfraGenerated;
import com.example.arInfra.event.model.InfraEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ message consumer for processing infrastructure events.
 *
 * <p>This component listens to the configured RabbitMQ queue, deserializes incoming JSON messages
 * into {@link InfraEvent} instances, and delegates them to the {@link EventDispatcher} for
 * processing. Event processing is performed asynchronously using a thread pool sized to the
 * available processor count.
 *
 * <p><b>Features:</b>
 *
 * <ul>
 *   <li>Automatic JSON deserialization with polymorphic type handling
 *   <li>Asynchronous event processing using a fixed thread pool
 *   <li>Graceful shutdown hook for executor cleanup
 *   <li>Comprehensive error handling and logging
 * </ul>
 *
 * <p><b>Required application property:</b>
 *
 * <ul>
 *   <li>{@code spring.rabbitmq.queue} - Queue name to listen on
 * </ul>
 *
 * <p>The consumer uses Jackson's {@code DefaultTyping.NON_FINAL} to deserialize messages to their
 * concrete event types based on the {@code @class} property included in the JSON payload.
 */
@InfraGenerated
@Component
@Slf4j
public class EventConsumer implements Consumer<String> {

  private final EventDispatcher eventHandler;
  private final ObjectMapper objectMapper;
  private final ExecutorService executor;

  /**
   * Constructs an EventConsumer with required dependencies and configuration.
   *
   * <p>Initializes the Jackson ObjectMapper with polymorphic type handling enabled, creates a fixed
   * thread pool with one thread per available processor, and registers a JVM shutdown hook to
   * gracefully terminate the executor on application shutdown.
   *
   * @param eventHandler the dispatcher responsible for routing events to appropriate handlers
   */
  public EventConsumer(EventDispatcher eventHandler) {
    this.eventHandler = eventHandler;
    this.objectMapper =
        new ObjectMapper()
            .activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
    this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Shutting down event consumer executor");
                  executor.shutdown();
                }));
  }

  /**
   * RabbitMQ listener method that receives messages from the configured queue.
   *
   * <p>This method is invoked automatically by Spring AMQP when a message arrives on the queue
   * specified by {@code spring.rabbitmq.queue}. It delegates to {@link #accept(String)} for
   * processing.
   *
   * @param rawMessage the raw JSON message string from RabbitMQ
   */
  @RabbitListener(queues = "${spring.rabbitmq.queue}")
  public void onMessage(String rawMessage) {
    accept(rawMessage);
  }

  /**
   * Processes a raw message string by deserializing and dispatching it asynchronously.
   *
   * <p>Submits the message processing to the thread pool executor. The processing includes:
   *
   * <ol>
   *   <li>Deserializing the JSON message to an {@link InfraEvent} instance
   *   <li>Validating the deserialized event (warns if null)
   *   <li>Dispatching to the {@link EventDispatcher} for routing to the appropriate handler
   *   <li>Logging successful dispatch or any errors encountered
   * </ol>
   *
   * <p>Errors during processing are caught and logged but do not prevent other messages from being
   * processed.
   *
   * @param rawMessage the raw JSON message string to process
   */
  @Override
  public void accept(String rawMessage) {
    executor.submit(
        () -> {
          try {
            InfraEvent event = deserialize(rawMessage);
            if (event == null) {
              log.warn("Received unprocessable event: {}", rawMessage);
              return;
            }
            eventHandler.accept(event);
            log.info("Event dispatched: {}", event.getClass().getSimpleName());
          } catch (Exception e) {
            log.error("Error while consuming event: {}", rawMessage, e);
          }
        });
  }

  /**
   * Deserializes a JSON message string into an {@link InfraEvent} instance.
   *
   * <p>Uses the configured ObjectMapper with polymorphic type handling to reconstruct the concrete
   * event type based on the {@code @class} property in the JSON.
   *
   * <p>Deserialization errors are logged at ERROR level, and null is returned to indicate failure.
   * This allows the consumer to skip invalid messages rather than crashing.
   *
   * @param rawMessage the JSON message string to deserialize
   * @return the deserialized InfraEvent instance, or null if deserialization fails
   */
  private InfraEvent deserialize(String rawMessage) {
    try {
      return objectMapper.readValue(rawMessage, InfraEvent.class);
    } catch (JsonProcessingException e) {
      log.error("Deserialization failed: {}", rawMessage, e);
      return null;
    }
  }
}
