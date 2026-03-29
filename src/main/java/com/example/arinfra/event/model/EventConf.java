package com.example.arinfra.event.model;

import static java.lang.String.format;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.config.RabbitConf;
import com.example.arinfra.datastructure.ListGrouper;
import com.example.arinfra.event.consumer.EventConsumer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.StatefulRetryOperationsInterceptor;

/**
 * Configures the RabbitMQ connection, message template, retry behaviour, and listener container for
 * the arInfra event system.
 *
 * <h2>Overview</h2>
 *
 * <p>This class is the central configuration hub for the arInfra messaging infrastructure. It sets
 * up everything the application needs to <b>reliably send and consume</b> events over RabbitMQ: the
 * connection to the broker, the template used to publish messages, the retry interceptor that
 * automatically retries failed events with exponential backoff, and the listener container factory
 * that controls how {@link EventConsumer} receives messages.
 *
 * <p>You do not need to interact with this class directly. It is wired automatically by the arInfra
 * framework. Behaviour is controlled entirely through your {@code application.yml}.
 *
 * <h2>How retry works</h2>
 *
 * <p>When a consumer throws an exception while processing an event, the arInfra framework does
 * <b>not</b> silently drop the message. Instead, it follows this sequence:
 *
 * <ol>
 *   <li>The <b>retry interceptor</b> catches the exception and records the failure against the
 *       message's unique identity (stateful retry).
 *   <li>The message is <b>NACKed without requeue</b>, so it goes back to the main queue via
 *       RabbitMQ's internal redelivery mechanism.
 *   <li>The interceptor <b>waits</b> for the configured backoff duration before allowing redelivery
 *       (exponential: 1 s → 2 s → 4 s → 8 s, capped at 30 s by default).
 *   <li>Steps 1–3 repeat until either processing succeeds or {@code max-attempts} is reached.
 *   <li>On final failure, the <b>RepublishMessageRecoverer</b> forwards the message to the Dead
 *       Letter Exchange (DLX), from where it lands in the Dead Letter Queue (DLQ) for manual
 *       inspection. The message is enriched with failure headers at that point ({@code
 *       x-exception-message}, {@code x-exception-stacktrace}, etc.).
 * </ol>
 *
 * <p><b>Why stateful retry?</b> Stateful retry tracks failure state per message identity rather
 * than retrying in-memory within a single thread. This means if the application restarts mid-retry,
 * the retry count is not lost — the message is redelivered by RabbitMQ and the retry count resumes
 * correctly. This is critical for resilience in production environments.
 *
 * <h2>How acknowledgement works</h2>
 *
 * <p>The listener container is configured with <b>manual acknowledgement</b> ({@code
 * AcknowledgeMode.MANUAL}). This means:
 *
 * <ul>
 *   <li>A message is ACKed ({@code basicAck}) only after {@code accept()} completes successfully in
 *       {@link EventConsumer}.
 *   <li>If an exception propagates out of the listener method, the retry interceptor takes over —
 *       it handles NACKing internally. The consumer itself does not NACK on failure.
 *   <li>This guarantees <b>at-least-once delivery</b>: a message is never lost due to an
 *       application crash or processing failure.
 * </ul>
 *
 * <h2>Retry configuration (application.yml)</h2>
 *
 * <p>All retry parameters are externalized and can be tuned per environment without code changes:
 *
 * <pre>{@code
 * app:
 *   rabbitmq:
 *     ssl: false                       # enable SSL/TLS (default: false)
 *     retry:
 *       max-attempts:     5            # total attempts before DLQ (default: 5)
 *       initial-delay-ms: 1000         # wait before first retry in ms (default: 1000)
 *       multiplier:       2.0          # backoff multiplier (default: 2.0)
 *       max-delay-ms:     30000        # maximum backoff cap in ms (default: 30000)
 * }</pre>
 *
 * <p>With the defaults above, the retry schedule for a failing event is:
 *
 * <pre>
 *   Attempt 1: immediate
 *   Attempt 2: wait  1 000 ms
 *   Attempt 3: wait  2 000 ms
 *   Attempt 4: wait  4 000 ms
 *   Attempt 5: wait  8 000 ms → failure → DLQ
 * </pre>
 *
 * <h2>Connection configuration (application.yml)</h2>
 *
 * <pre>{@code
 * spring:
 *   rabbitmq:
 *     host:     localhost
 *     port:     5672
 *     username: guest
 *     password: guest
 *     vhost:    /                      # optional, defaults to "/"
 *     exchange: my-app.exchange
 *     routing-key: my-app.routing
 * }</pre>
 *
 * @see RabbitConf for queue, exchange, and DLQ broker declarations
 * @see EventConsumer for the message listener implementation
 * @see InfraEvent for the base event contract
 */
@InfraGenerated
@Configuration
public class EventConf {

  @Value("${spring.rabbitmq.username}")
  private String username;

  @Value("${spring.rabbitmq.password}")
  private String password;

  @Value("${spring.rabbitmq.host}")
  private String host;

  @Value("${spring.rabbitmq.port}")
  private int port;

  @Value("${spring.rabbitmq.vhost:/}")
  private String vhost;

  @Value("${app.rabbitmq.ssl:false}")
  private boolean sslEnabled;

  @Value("${app.rabbitmq.retry.max-attempts:5}")
  private int maxAttempts;

  @Value("${app.rabbitmq.retry.initial-delay-ms:1000}")
  private long initialDelayMs;

  @Value("${app.rabbitmq.retry.multiplier:2.0}")
  private double multiplier;

  @Value("${app.rabbitmq.retry.max-delay-ms:30000}")
  private long maxDelayMs;

  /**
   * Creates and configures the RabbitMQ connection factory.
   *
   * <p>This factory manages the physical TCP connections to the RabbitMQ broker. It uses Spring
   * AMQP's {@link CachingConnectionFactory}, which pools and reuses connections and channels for
   * efficiency — avoiding the overhead of creating a new connection per message.
   *
   * <p><b>Publisher confirms</b> are enabled in {@code CORRELATED} mode. This means every published
   * message receives a broker-level acknowledgement (or negative acknowledgement), allowing the
   * publisher to detect and react to delivery failures.
   *
   * <p><b>Publisher returns</b> are enabled. If a message is published to an exchange but cannot be
   * routed to any queue (e.g. a misconfigured routing key), the broker returns the message to the
   * publisher instead of silently dropping it.
   *
   * <p><b>SSL/TLS</b> is enabled when {@code app.rabbitmq.ssl=true}. This encrypts all
   * communication between the application and the RabbitMQ broker, which is required in production
   * environments.
   *
   * <p>The virtual host defaults to {@code "/"} if the property is absent or blank.
   *
   * @return a configured {@link CachingConnectionFactory}
   * @throws IllegalStateException if {@code app.rabbitmq.ssl=true} but SSL cannot be initialised
   */
  @Bean
  public CachingConnectionFactory connectionFactory() {
    CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
    factory.setUsername(username);
    factory.setPassword(password);
    factory.setVirtualHost((vhost == null || vhost.isBlank()) ? "/" : vhost);

    if (sslEnabled) {
      try {
        factory.getRabbitConnectionFactory().useSslProtocol();
      } catch (Exception e) {
        throw new IllegalStateException("Failed to enable SSL for RabbitMQ", e);
      }
    }

    factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    factory.setPublisherReturns(true);
    return factory;
  }

  /**
   * Creates and configures the RabbitMQ message template used by {@link EventProducer}.
   *
   * <p>The {@link RabbitTemplate} is the primary Spring AMQP abstraction for sending messages. It
   * is used internally by {@link EventProducer} to publish serialized events to the main exchange.
   *
   * <p>The {@code mandatory} flag is set to {@code true}. This activates the publisher returns
   * mechanism: if a published message cannot be routed to any queue (for example, because the
   * routing key is wrong), the broker returns it to the sender rather than silently discarding it.
   * This makes routing misconfigurations immediately visible.
   *
   * @param connectionFactory the RabbitMQ connection factory
   * @return a {@link RabbitTemplate} with mandatory routing enabled
   */
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMandatory(true);
    return template;
  }

  /**
   * Creates the recoverer that forwards exhausted messages to the Dead Letter Exchange (DLX).
   *
   * <p>The {@link RepublishMessageRecoverer} is invoked by the retry interceptor when a message has
   * failed processing {@code app.rabbitmq.retry.max-attempts} times and there are no more retries
   * left. It republishes the message to the DLX exchange declared in {@link
   * com.example.arinfra.config.RabbitConf}, from where it is routed to the DLQ.
   *
   * <p>The recoverer automatically enriches the dead message with diagnostic headers before
   * forwarding it, including:
   *
   * <ul>
   *   <li>{@code x-exception-message}: the exception message from the last failure
   *   <li>{@code x-exception-stacktrace}: the full stack trace of the last failure
   *   <li>{@code x-original-exchange}: the exchange the message was originally published to
   *   <li>{@code x-original-routing-key}: the original routing key
   * </ul>
   *
   * <p>These headers make it straightforward to diagnose failures by inspecting dead messages in
   * the RabbitMQ management UI without needing to look at application logs.
   *
   * @param rabbitTemplate the template used to republish to the DLX
   * @param exchangeName the main exchange name, used to derive the DLX name ({@code
   *     <exchange>.dlx})
   * @param routingKey the main routing key, used to derive the dead routing key ({@code
   *     <key>.dead})
   * @return a configured {@link RepublishMessageRecoverer}
   */
  @Bean
  public RepublishMessageRecoverer republishMessageRecoverer(
      RabbitTemplate rabbitTemplate,
      @Value("${spring.rabbitmq.exchange}") String exchangeName,
      @Value("${spring.rabbitmq.routing-key}") String routingKey) {
    return new RepublishMessageRecoverer(
        rabbitTemplate, format("%s.dlx", exchangeName), format("%s.dead", routingKey));
  }

  /**
   * Creates the stateful retry interceptor that automatically retries failed event processing.
   *
   * <p>This interceptor wraps every {@code @RabbitListener} method in the application (via the
   * listener container factory configured in {@link #rabbitListenerContainerFactory}). When a
   * listener throws an exception, the interceptor:
   *
   * <ol>
   *   <li>Records the failure against the message's unique identity (stateful tracking).
   *   <li>Allows the exception to propagate so the listener container can NACK the message.
   *   <li>On redelivery, waits for the computed backoff duration before allowing the next attempt.
   *   <li>After {@code max-attempts} failures, delegates to {@link RepublishMessageRecoverer} to
   *       forward the message to the DLQ.
   * </ol>
   *
   * <p><b>Stateful vs stateless retry:</b> this interceptor uses <i>stateful</i> retry, which means
   * retry state is tracked per message identity across multiple deliveries, not held in memory
   * within a single call stack. The practical benefit is that if the application restarts
   * mid-retry, the retry count is not reset — the message is redelivered by RabbitMQ and the
   * interceptor resumes from where it left off.
   *
   * <p><b>Backoff schedule</b> with default properties:
   *
   * <pre>
   *   Attempt 1: immediate
   *   Attempt 2: wait  1 000 ms
   *   Attempt 3: wait  2 000 ms
   *   Attempt 4: wait  4 000 ms
   *   Attempt 5: wait  8 000 ms → RepublishMessageRecoverer → DLQ
   * </pre>
   *
   * @param republishMessageRecoverer the recoverer to invoke after all retries are exhausted
   * @return a configured {@link StatefulRetryOperationsInterceptor}
   */
  @Bean
  public StatefulRetryOperationsInterceptor retryInterceptor(
      RepublishMessageRecoverer republishMessageRecoverer) {
    return RetryInterceptorBuilder.stateful()
        .maxAttempts(maxAttempts)
        .backOffOptions(initialDelayMs, multiplier, maxDelayMs)
        .recoverer(republishMessageRecoverer)
        .build();
  }

  /**
   * Creates the listener container factory that controls how RabbitMQ messages are received and
   * dispatched to {@link com.example.arinfra.event.consumer.EventConsumer}.
   *
   * <p>This factory is the bridge between the RabbitMQ broker and the application's listener
   * methods. Every {@code @RabbitListener} in the application uses this factory, which means the
   * retry interceptor and manual acknowledgement mode are applied automatically to all listeners
   * without any additional configuration on the developer's part.
   *
   * <p><b>Manual acknowledgement ({@code AcknowledgeMode.MANUAL}):</b> the application controls
   * exactly when a message is acknowledged. A message is ACKed only after {@link
   * com.example.arinfra.event.consumer.EventConsumer#accept(String)} completes successfully. If
   * processing throws an exception, the exception propagates to the retry interceptor, which
   * handles NACKing internally. This guarantees that no message is ever silently lost due to a
   * processing failure.
   *
   * <p><b>Retry interceptor:</b> the {@link StatefulRetryOperationsInterceptor} is set as an advice
   * on this factory, meaning it wraps every listener invocation. Developers writing event handler
   * services (e.g. {@code PaymentRequestedService}) do not need to implement any retry logic —
   * throwing an exception from the handler is sufficient to trigger the full retry-then-DLQ
   * pipeline.
   *
   * <p><b>Concurrency:</b> the factory creates one concurrent consumer per available processor by
   * default ({@code Runtime.getRuntime().availableProcessors()}), allowing parallel processing of
   * messages from the queue.
   *
   * @param connectionFactory the RabbitMQ connection factory
   * @param retryInterceptor the stateful retry interceptor
   * @return a configured {@link SimpleRabbitListenerContainerFactory}
   */
  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, StatefulRetryOperationsInterceptor retryInterceptor) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
    factory.setAdviceChain(retryInterceptor);
    factory.setConcurrentConsumers(Runtime.getRuntime().availableProcessors());
    return factory;
  }

  /**
   * Creates a list grouper utility for batching infrastructure events.
   *
   * <p>This utility is used internally by {@link EventProducer} to split large collections of
   * events into batches before publishing them to RabbitMQ, preventing memory pressure and network
   * saturation when publishing a high volume of events at once.
   *
   * @return a new {@link ListGrouper} instance for {@link InfraEvent} batching
   */
  @Bean
  public ListGrouper<InfraEvent> listGrouper() {
    return new ListGrouper<>();
  }
}
