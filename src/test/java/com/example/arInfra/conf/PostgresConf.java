package com.example.arInfra.conf;

import com.example.arInfra.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

@InfraGenerated
@TestConfiguration
@SuppressWarnings("resource")
public class PostgresConf {

  private static final String TEST_VALUE = "test";
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withReuse(false)
          .withDatabaseName("unfaked")
          .withUsername(TEST_VALUE)
          .withPassword(TEST_VALUE);

  public void start() {
    if (!POSTGRES.isRunning()) {
      POSTGRES.start();
    }
  }

  public void stop() {
    if (POSTGRES.isRunning()) {
      POSTGRES.stop();
    }
  }

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
  }
}
