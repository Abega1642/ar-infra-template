package com.example.arinfra.event.consumer;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.config.RabbitConf;
import com.example.arinfra.event.config.InfraEventTypeRegistrar;
import com.example.arinfra.event.model.EventConf;
import com.example.arinfra.event.model.EventProducer;
import com.example.arinfra.event.model.InfraEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Listens to the main RabbitMQ queue, deserializes incoming messages into {@link InfraEvent}
 * instances, and dispatches them to their corresponding handler services.
 *
 * <h2>Overview</h2>
 *
 * <p>This class is the entry point for all event consumption in the arInfra framework. It sits
 * between the RabbitMQ broker and your application logic, handling the low-level concerns of
 * message reception, deserialization, acknowledgement, and error propagation — so that your event
 * handler services can focus purely on business logic.
 *
 * <p>You do not need to interact with this class directly. To handle an event, create a Spring
 * service that implements {@code Consumer<InfraEvent>} and name it after the event class (see
 * <b>How to handle an event</b> below). The framework will route the event to your service
 * automatically.
 *
 * <h2>How a message flows through this class</h2>
 *
 * <ol>
 *   <li>RabbitMQ delivers a raw JSON string to {@link #onMessage(String, Channel, long)}.
 *   <li>The message is deserialized from JSON into the concrete {@link InfraEvent} subtype using
 *       the shared {@link ObjectMapper}, which has been pre-configured by {@link
 *       InfraEventTypeRegistrar} with an explicit allowlist of all known event types.
 *   <li>The deserialized event's {@code attemptNb} counter is incremented to reflect the current
 *       attempt number, which is useful for logging and conditional logic in handlers.
 *   <li>The event is passed to {@link EventDispatcher}, which resolves the correct handler service
 *       by convention (see {@link EventDispatcher} for the naming rules) and invokes it.
 *   <li>If processing completes without exception, the message is <b>ACKed</b> ({@code basicAck}) —
 *       RabbitMQ removes it from the queue permanently.
 *   <li>If processing throws an exception, it propagates out of {@link #onMessage} to the <b>retry
 *       interceptor</b> configured in {@link EventConf}, which handles redelivery and eventual
 *       routing to the Dead Letter Queue (DLQ).
 * </ol>
 *
 * <h2>Acknowledgement and retry interaction</h2>
 *
 * <p>The listener container is configured with <b>manual acknowledgement</b>. This means:
 *
 * <ul>
 *   <li>On <b>success</b>: this class calls {@code channel.basicAck()} explicitly after {@link
 *       #accept(String)} returns normally.
 *   <li>On <b>failure</b>: this class does <b>not</b> call {@code basicNack()}. The exception
 *       propagates to the retry interceptor (an AOP advice wrapping this listener), which owns the
 *       NACK and redelivery scheduling. This avoids double-signalling the same delivery tag, which
 *       would corrupt the AMQP channel.
 * </ul>
 *
 * <p>The full retry-to-DLQ path is managed by the infrastructure configured in {@link EventConf}.
 * Handler services do not need to implement any retry logic — throwing an unchecked exception is
 * sufficient to trigger the full pipeline.
 *
 * <h2>Deserialization and security</h2>
 *
 * <p>This class uses the application-wide {@link ObjectMapper} bean, which is configured by {@link
 * InfraEventTypeRegistrar} to use an explicit allowlist of named subtypes (via {@code
 * JsonTypeInfo.Id.NAME}). This prevents arbitrary class instantiation from crafted JSON payloads
 * (CWE-502 / OWASP Deserialization of Untrusted Data).
 *
 * <p>If deserialization fails (malformed JSON, unknown type, etc.), the message is logged at ERROR
 * level and {@link #accept(String)} returns normally — triggering an ACK. This is intentional: a
 * malformed message will never succeed no matter how many times it is retried, so retrying it would
 * only fill the DLQ with noise. The message is effectively discarded, and the error log is the
 * signal to investigate.
 *
 * <h2>How to handle an event</h2>
 *
 * <p>Create a Spring {@code @Service} that implements {@code Consumer<InfraEvent>} and name it
 * after the event class in camelCase with the suffix {@code Service}. The framework will route the
 * event to your service automatically — no registration or wiring is needed.
 *
 * <p>Example: to handle {@code PaymentRequested} events:
 *
 * <pre>{@code
 * @Service("paymentRequestedService")
 * public class PaymentRequestedService implements Consumer<PaymentRequested> {
 *
 *   @Override
 *   public void accept(PaymentRequested paymentRequested) {
 *     // your business logic here
 *     // throw any RuntimeException to trigger retry → DLQ
 *   }
 * }
 * }</pre>
 *
 * @see EventDispatcher for the convention-based event-to-handler routing mechanism
 * @see EventConf for retry, backoff, and acknowledgement configuration
 * @see RabbitConf for the queue and DLQ broker declarations
 * @see InfraEvent for the base event contract
 */
@InfraGenerated
@Component
@Slf4j
@RequiredArgsConstructor
public class EventConsumer implements Consumer<String> {

  private final EventDispatcher eventHandler;
  private final ObjectMapper objectMapper;

  /**
   * Receives a raw JSON message from the RabbitMQ queue, processes it, and acknowledges it.
   *
   * <p>This method is invoked automatically by Spring AMQP whenever a message is available on the
   * queue configured by {@code spring.rabbitmq.queue}. The {@link Channel} and delivery tag are
   * injected by the framework to support manual acknowledgement.
   *
   * <p><b>On success:</b> delegates to {@link #accept(String)}, then sends {@code basicAck} to
   * permanently remove the message from the queue.
   *
   * <p><b>On failure:</b> any exception thrown by {@link #accept(String)} propagates out of this
   * method without being caught. The retry interceptor (configured in {@link EventConf}) intercepts
   * the exception, applies the configured backoff, and schedules redelivery. After all retry
   * attempts are exhausted, the {@code RepublishMessageRecoverer} forwards the message to the Dead
   * Letter Queue.
   *
   * <p>This class intentionally does <b>not</b> call {@code basicNack} on failure. The retry
   * interceptor owns that responsibility. Calling both would result in double-signalling the same
   * delivery tag, which corrupts the AMQP channel.
   *
   * @param rawMessage the raw JSON string delivered by RabbitMQ
   * @param channel the AMQP channel, used to send the manual acknowledgement
   * @param deliveryTag the unique identifier of this delivery, required for acknowledgement
   * @throws IOException if the {@code basicAck} call fails due to a channel or connection error
   */
  @RabbitListener(queues = "${spring.rabbitmq.queue}")
  public void onMessage(
      String rawMessage, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
      throws IOException {

    accept(rawMessage);
    channel.basicAck(deliveryTag, false);
  }

  /**
   * Deserializes a raw JSON message and dispatches the resulting event to its handler service.
   *
   * <p>This method is the core processing pipeline for a single message:
   *
   * <ol>
   *   <li>Deserializes the JSON string into the concrete {@link InfraEvent} subtype.
   *   <li>If deserialization fails, logs the error and returns normally (no retry — see below).
   *   <li>Increments the event's {@code attemptNb} counter for observability in handler logs.
   *   <li>Passes the event to {@link EventDispatcher}, which routes it to the matching handler.
   * </ol>
   *
   * <p><b>Deserialization failures are not retried.</b> A message that cannot be deserialized
   * (malformed JSON, unrecognised type, schema mismatch) will never succeed on retry. To avoid
   * flooding the DLQ with unprocessable messages, deserialization failures cause this method to
   * return normally — which triggers an ACK and discards the message. The ERROR log is the
   * actionable signal.
   *
   * <p><b>Handler failures are retried.</b> Any exception thrown by {@link EventDispatcher} or the
   * underlying handler service propagates out of this method. {@link #onMessage} does not catch it,
   * so it reaches the retry interceptor, which applies the configured backoff and eventually routes
   * the message to the DLQ if all attempts fail.
   *
   * @param rawMessage the raw JSON string to deserialize and dispatch
   */
  @Override
  public void accept(String rawMessage) {
    InfraEvent event = deserialize(rawMessage);

    if (event == null) {
      log.error("Unprocessable event — deserialization returned null: {}", rawMessage);
      return;
    }

    log.info(
        "Dispatching event: {} (attempt {})",
        event.getClass().getSimpleName(),
        event.getAttemptNb() + 1);
    event.setAttemptNb(event.getAttemptNb() + 1);

    eventHandler.accept(event);
    log.info("Event processed: {}", event.getClass().getSimpleName());
  }

  /**
   * Deserializes a raw JSON string into a concrete {@link InfraEvent} subtype.
   *
   * <p>Deserialization relies on the {@code @type} property embedded in the JSON payload by {@link
   * EventProducer} at publish time. Jackson uses this property to look up the correct subtype from
   * the allowlist registered by {@link InfraEventTypeRegistrar}.
   *
   * <p>If deserialization fails for any reason (malformed JSON, unknown type name, missing required
   * fields), the exception is caught, logged at ERROR level, and {@code null} is returned. The
   * caller ({@link #accept(String)}) treats a {@code null} result as an unprocessable message and
   * discards it without retrying.
   *
   * @param rawMessage the JSON string to deserialize
   * @return the deserialized {@link InfraEvent}, or {@code null} if deserialization fails
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
