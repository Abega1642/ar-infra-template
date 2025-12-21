package com.example.arinfra.config;

import com.example.arinfra.InfraGenerated;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for RabbitMQ queue, exchange, and binding setup.
 *
 * <p>This configuration class declares the RabbitMQ infrastructure components required for message
 * routing: a durable queue for storing messages, a direct exchange for routing, and a binding that
 * connects them with a specific routing key.
 *
 * <p><b>Required application properties:</b>
 *
 * <ul>
 *   <li>{@code spring.rabbitmq.queue} - Queue name for message storage
 *   <li>{@code spring.rabbitmq.exchange} - Exchange name for message routing
 *   <li>{@code spring.rabbitmq.routing-key} - Routing key for queue binding
 * </ul>
 *
 * <p><b>Infrastructure components:</b>
 *
 * <ul>
 *   <li><b>Queue:</b> Durable queue that persists messages across broker restarts
 *   <li><b>Exchange:</b> Direct exchange routes messages based on exact routing key matches
 *   <li><b>Binding:</b> Connects the queue to the exchange with the specified routing key
 * </ul>
 *
 * <p>These components are automatically declared on the RabbitMQ broker when the application
 * starts, ensuring the messaging infrastructure is ready for use.
 */
@InfraGenerated
@Configuration
public class RabbitConfig {

  @Value("${spring.rabbitmq.queue}")
  private String queueName;

  @Value("${spring.rabbitmq.exchange}")
  private String exchangeName;

  @Value("${spring.rabbitmq.routing-key}")
  private String routingKey;

  /**
   * Creates a durable RabbitMQ queue bean.
   *
   * <p>The queue is configured as durable ({@code true}), meaning it will survive RabbitMQ broker
   * restarts. Messages published to this queue will be stored until consumed by a listener.
   *
   * <p>This queue is automatically declared on the RabbitMQ broker when the application context
   * starts.
   *
   * @return a durable Queue instance with the configured name
   */
  @Bean
  public Queue myQueue() {
    return new Queue(queueName, true);
  }

  /**
   * Creates a direct exchange bean for message routing.
   *
   * <p>A direct exchange routes messages to queues based on exact routing key matches. When a
   * message is published with a routing key, it will be delivered only to queues bound to the
   * exchange with that exact routing key.
   *
   * <p>This exchange is automatically declared on the RabbitMQ broker when the application context
   * starts.
   *
   * @return a DirectExchange instance with the configured name
   */
  @Bean
  public DirectExchange myExchange() {
    return new DirectExchange(exchangeName);
  }

  /**
   * Creates a binding between the queue and exchange with the specified routing key.
   *
   * <p>This binding configures RabbitMQ to route messages published to the exchange with the
   * specified routing key to the configured queue. Messages published with different routing keys
   * will not be routed to this queue.
   *
   * <p>The binding is automatically declared on the RabbitMQ broker when the application context
   * starts.
   *
   * @param myQueue the queue to bind (injected by Spring)
   * @param myExchange the exchange to bind to (injected by Spring)
   * @return a Binding connecting the queue to the exchange with the routing key
   */
  @Bean
  public Binding binding(Queue myQueue, DirectExchange myExchange) {
    return BindingBuilder.bind(myQueue).to(myExchange).with(routingKey);
  }
}
