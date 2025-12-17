package com.example.arInfra.conf;

import com.example.arInfra.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@InfraGenerated
@TestConfiguration
public class RabbitMQConf {

  private static final RabbitMQContainer RABBIT =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management")).withReuse(false);

  public void start() {
    if (!RABBIT.isRunning()) {
      RABBIT.start();
    }
  }

  public void stop() {
    if (RABBIT.isRunning()) {
      RABBIT.stop();
    }
  }

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", RABBIT::getHost);
    registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    registry.add("spring.rabbitmq.vhost", () -> "/");
    registry.add("app.rabbitmq.ssl", () -> "false");
  }
}
