package com.example.arinfra.event.model;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.datastructure.ListGrouper;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for RabbitMQ messaging infrastructure.
 *
 * <p>This configuration class sets up the RabbitMQ connection factory, message template, and
 * supporting beans required for event-driven messaging. It configures connection parameters from
 * application properties and enables publisher confirms and returns for reliable message delivery.
 *
 * <p><b>Required application properties:</b>
 *
 * <ul>
 *   <li>{@code spring.rabbitmq.username} - RabbitMQ username
 *   <li>{@code spring.rabbitmq.password} - RabbitMQ password
 *   <li>{@code spring.rabbitmq.host} - RabbitMQ server host
 *   <li>{@code spring.rabbitmq.port} - RabbitMQ server port
 *   <li>{@code spring.rabbitmq.vhost} - Virtual host (defaults to "/")
 *   <li>{@code app.rabbitmq.ssl} - Enable SSL connection (defaults to false)
 * </ul>
 *
 * <p>The configuration enables:
 *
 * <ul>
 *   <li>Publisher confirms for message acknowledgment
 *   <li>Publisher returns for unroutable message handling
 *   <li>Optional SSL/TLS encryption
 *   <li>Connection caching for improved performance
 * </ul>
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

  /**
   * Creates and configures the RabbitMQ connection factory.
   *
   * <p>This factory manages connections to the RabbitMQ server with the following features:
   *
   * <ul>
   *   <li>Connection pooling and caching for improved performance
   *   <li>Publisher confirms (CORRELATED mode) for reliable message delivery
   *   <li>Publisher returns enabled to handle unroutable messages
   *   <li>Optional SSL/TLS encryption when {@code app.rabbitmq.ssl=true}
   * </ul>
   *
   * <p>The virtual host defaults to "/" if not specified or blank.
   *
   * @return configured CachingConnectionFactory for RabbitMQ
   * @throws IllegalStateException if SSL is enabled but cannot be configured
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
   * Creates and configures the RabbitMQ message template.
   *
   * <p>This template is used for sending messages to RabbitMQ exchanges and queues. The mandatory
   * flag is enabled, which means if a message cannot be routed to any queue, it will be returned to
   * the publisher via the return callback instead of being silently dropped.
   *
   * <p>This ensures message delivery failures are detectable and can be handled appropriately by
   * the application.
   *
   * @param connectionFactory the RabbitMQ connection factory
   * @return configured RabbitTemplate with mandatory routing enabled
   */
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMandatory(true);
    return template;
  }

  /**
   * Creates a list grouper utility for batching infrastructure events.
   *
   * <p>This utility bean provides functionality to group multiple {@link InfraEvent} instances into
   * batches for efficient processing or bulk operations.
   *
   * @return a new ListGrouper instance for InfraEvent batching
   */
  @Bean
  public ListGrouper<InfraEvent> listGrouper() {
    return new ListGrouper<>();
  }
}
