package com.example.arInfra.conf;

import com.example.arInfra.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

@InfraGenerated
@TestConfiguration
public class EnvConf {

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.exchange", () -> "infra-event-exchange");
    registry.add("spring.rabbitmq.queue", () -> "infra-health-queue");
    registry.add("spring.rabbitmq.routing-key", () -> "spring.event.key");
  }
}
