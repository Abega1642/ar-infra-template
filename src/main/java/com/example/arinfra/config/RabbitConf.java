package com.example.arinfra.config;

import static java.lang.String.format;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.event.consumer.EventConsumer;
import com.example.arinfra.event.model.EventConf;
import com.example.arinfra.event.model.EventProducer;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the complete RabbitMQ broker infrastructure required by the arInfra event system.
 *
 * <h2>Overview</h2>
 *
 * <p>This class is responsible for one thing: telling RabbitMQ what queues, exchanges, and bindings
 * must exist before the application starts processing events. All declarations are
 * <b>idempotent</b> — if the infrastructure already exists on the broker with the same
 * configuration, RabbitMQ will leave it untouched. If it does not exist, RabbitMQ will create it
 * automatically when the application context starts.
 *
 * <p>You do not need to interact with this class directly. It is wired automatically by the arInfra
 * framework. Its only dependency is your {@code application.yml} configuration.
 *
 * <h2>Infrastructure declared</h2>
 *
 * <p>This class declares two parallel sets of infrastructure: the <b>main</b> pipeline that
 * processes live events, and the <b>Dead Letter</b> pipeline that receives events that have
 * exhausted all retry attempts.
 *
 * <h3>Main pipeline</h3>
 *
 * <ul>
 *   <li><b>Main queue</b> ({@code spring.rabbitmq.queue}): durable queue where published events
 *       land and wait for consumption. Durable means messages survive a RabbitMQ broker restart.
 *   <li><b>Main exchange</b> ({@code spring.rabbitmq.exchange}): a direct exchange that routes
 *       incoming messages to the main queue using an exact routing key match.
 *   <li><b>Main binding</b>: connects the main queue to the main exchange via {@code
 *       spring.rabbitmq.routing-key}.
 * </ul>
 *
 * <h3>Dead Letter (DLQ) pipeline</h3>
 *
 * <ul>
 *   <li><b>DLX exchange</b> ({@code <exchange>.dlx}): receives messages that the retry interceptor
 *       has given up on, after exhausting all configured retry attempts.
 *   <li><b>DLQ queue</b> ({@code <queue>.dlq}): durable queue where dead messages accumulate for
 *       manual inspection or offline reprocessing.
 *   <li><b>DLQ binding</b>: connects the DLQ queue to the DLX exchange via {@code
 *       <routing-key>.dead}.
 * </ul>
 *
 * <p>The DLQ names are always derived from the main names — no additional properties are needed:
 *
 * <pre>
 *   Main queue:    my-app.events     →  DLQ:  my-app.events.dlq
 *   Main exchange: my-app.exchange   →  DLX:  my-app.exchange.dlx
 *   Routing key:   my-app.routing    →  Dead: my-app.routing.dead
 * </pre>
 *
 * <h2>How the Dead Letter flow works</h2>
 *
 * <p>The main queue is declared with two RabbitMQ arguments ({@code x-dead-letter-exchange} and
 * {@code x-dead-letter-routing-key}) that instruct the broker where to forward a message when it is
 * rejected without requeue. This rejection happens after all retry attempts configured in {@link
 * EventConf} are exhausted.
 *
 * <p>The full failure path is:
 *
 * <pre>
 *   Event published
 *     → Main exchange
 *       → Main queue
 *         → Consumer fails
 *           → Retry interceptor retries N times with exponential backoff
 *             → All retries exhausted
 *               → RepublishMessageRecoverer forwards to DLX
 *                 → DLQ queue (message stored for inspection)
 * </pre>
 *
 * <p>Dead messages in the DLQ are <b>never automatically reprocessed</b>. They can be inspected via
 * the RabbitMQ management UI, re-queued manually after fixing the underlying issue, or consumed by
 * a separate monitoring or alerting service.
 *
 * <h2>Required application properties</h2>
 *
 * <pre>{@code
 * spring:
 *   rabbitmq:
 *     queue:       my-app.events
 *     exchange:    my-app.exchange
 *     routing-key: my-app.routing
 * }</pre>
 *
 * @see EventConf for retry interceptor, backoff configuration, and listener container setup
 * @see EventConsumer for the message listener
 * @see EventProducer for the message publisher
 */
@InfraGenerated
@Configuration
public class RabbitConf {

  @Value("${spring.rabbitmq.queue}")
  private String queueName;

  @Value("${spring.rabbitmq.exchange}")
  private String exchangeName;

  @Value("${spring.rabbitmq.routing-key}")
  private String routingKey;

  private String dlqName() {
    return format("%s.dlq", queueName);
  }

  private String dlxName() {
    return format("%s.dlx", exchangeName);
  }

  private String dlqRoutingKey() {
    return format("%s.dead", routingKey);
  }

  /**
   * Declares the main durable queue where published events are stored for consumption.
   *
   * <p>This queue is the entry point for all events in the arInfra system. When {@link
   * EventProducer} publishes an event, RabbitMQ routes it here via the main exchange and binding.
   *
   * <p><b>Durability:</b> the queue is declared durable, meaning it and its messages survive a
   * RabbitMQ broker restart.
   *
   * <p><b>Dead letter configuration:</b> the queue carries two broker-level arguments that tell
   * RabbitMQ where to forward a message when it is rejected without requeue — which is what happens
   * after the retry interceptor (configured in {@link EventConf}) exhausts all retry attempts:
   *
   * <ul>
   *   <li>{@code x-dead-letter-exchange} → the DLX exchange declared by {@link
   *       #deadLetterExchange()}
   *   <li>{@code x-dead-letter-routing-key} → the routing key used when forwarding to the DLX
   * </ul>
   *
   * @return a durable {@link Queue} configured to forward failed messages to the dead letter
   *     pipeline
   */
  @Bean
  public Queue myQueue() {
    return QueueBuilder.durable(queueName)
        .withArgument("x-dead-letter-exchange", dlxName())
        .withArgument("x-dead-letter-routing-key", dlqRoutingKey())
        .build();
  }

  /**
   * Declares the main direct exchange used to route published events to the main queue.
   *
   * <p>A direct exchange routes messages based on an exact match between the message routing key
   * and the binding key. Only messages published with the exact value of {@code
   * spring.rabbitmq.routing-key} will be delivered to the main queue.
   *
   * @return the main {@link DirectExchange}
   */
  @Bean
  public DirectExchange myExchange() {
    return new DirectExchange(exchangeName);
  }

  /**
   * Binds the main queue to the main exchange using the configured routing key.
   *
   * <p>This binding is what connects a published message to its destination queue. When {@link
   * EventProducer} sends a message to the main exchange with the configured routing key, RabbitMQ
   * uses this binding to deliver it to the main queue, where {@link EventConsumer} will pick it up.
   *
   * @param myQueue the main queue bean
   * @param myExchange the main exchange bean
   * @return the {@link Binding} connecting the main queue to the main exchange
   */
  @Bean
  public Binding binding(Queue myQueue, DirectExchange myExchange) {
    return BindingBuilder.bind(myQueue).to(myExchange).with(routingKey);
  }

  /**
   * Declares the Dead Letter Queue (DLQ) where events that exhausted all retry attempts are stored.
   *
   * <p>The DLQ is a durable queue named {@code <queue>.dlq}. A message arrives here only after the
   * retry interceptor (configured in {@link EventConf}) has attempted processing {@code
   * app.rabbitmq.retry.max-attempts} times and all attempts have failed.
   *
   * <p>Messages in the DLQ are not automatically reprocessed. Typical actions are:
   *
   * <ul>
   *   <li>Inspect the message payload and headers in the RabbitMQ management UI — the {@code
   *       RepublishMessageRecoverer} enriches dead messages with failure headers ({@code
   *       x-exception-message}, {@code x-exception-stacktrace}, etc.)
   *   <li>Fix the underlying issue and manually re-queue the message to the main queue
   *   <li>Set up a separate consumer on this queue for automated alerting or archiving
   * </ul>
   *
   * @return a durable {@link Queue} for dead letter messages
   */
  @Bean
  public Queue deadLetterQueue() {
    return QueueBuilder.durable(dlqName()).build();
  }

  /**
   * Declares the Dead Letter Exchange (DLX) that receives events after all retries are exhausted.
   *
   * <p>The DLX is a direct exchange named {@code <exchange>.dlx}. It is the target of two
   * independent routing mechanisms that work together to ensure no failed message is lost:
   *
   * <ul>
   *   <li>The {@code x-dead-letter-exchange} argument on the main queue — used by RabbitMQ itself
   *       if a message is NACKed without requeue at the broker level.
   *   <li>The {@code RepublishMessageRecoverer} in {@link EventConf} — used by the Spring retry
   *       interceptor to explicitly publish to this exchange after retries are exhausted at the
   *       application level.
   * </ul>
   *
   * @return the dead letter {@link DirectExchange}
   */
  @Bean
  public DirectExchange deadLetterExchange() {
    return new DirectExchange(dlxName());
  }

  /**
   * Binds the DLQ queue to the DLX exchange using the dead letter routing key.
   *
   * <p>This binding ensures that messages arriving at the DLX exchange with the routing key {@code
   * <routing-key>.dead} are delivered to the DLQ queue. Without this binding, dead letter messages
   * published to the DLX would be silently dropped by RabbitMQ.
   *
   * @return the {@link Binding} connecting the DLQ to the DLX
   */
  @Bean
  public Binding deadLetterBinding() {
    return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(dlqRoutingKey());
  }
}
