package com.example.arinfra.endpoint.rest.controller.health;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.repository.model.Dummy;
import com.example.arinfra.service.health.HealthRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for database health check operations.
 *
 * <p>Provides endpoints to verify database connectivity and query functionality by retrieving dummy
 * data with pagination support.
 */
@InfraGenerated
@RestController
@AllArgsConstructor
@RequestMapping("/health/db")
public class HealthRepositoryController {
  private final HealthRepositoryService healthRepositoryService;

  /**
   * Checks database health by retrieving paginated dummy data.
   *
   * <p>This endpoint performs a read operation on the database to verify connectivity and query
   * execution. Pagination parameters are optional and will use default values if not provided.
   *
   * @param page the page number to retrieve (optional)
   * @param size the number of items per page (optional)
   * @return a Page containing dummy data from the database
   */
  @GetMapping
  public Page<Dummy> checkDbHealth(
      @RequestParam(value = "page", required = false) Integer page,
      @RequestParam(value = "size", required = false) Integer size) {
    return healthRepositoryService.getAll(page, size);
  }
}
