package com.example.arInfra.endpoint.rest.controller.health;

import com.example.arInfra.InfraGenerated;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for basic connectivity health check.
 *
 * <p>Provides a simple ping-pong endpoint to verify that the application is running and responding
 * to HTTP requests.
 */
@InfraGenerated
@RestController
@AllArgsConstructor
public class PingPongController {

  /**
   * Basic health check endpoint that returns a simple "pong" response.
   *
   * <p>This endpoint can be used by load balancers, monitoring tools, or clients to verify that the
   * application is alive and responding.
   *
   * @return the string "pong"
   */
  @GetMapping("/ping")
  public String ping() {
    return "pong";
  }
}
