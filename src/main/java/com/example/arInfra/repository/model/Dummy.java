package com.example.arInfra.repository.model;

import com.example.arInfra.InfraGenerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * JPA entity model representing dummy data for health check and testing purposes.
 *
 * <p>This entity is used by health check endpoints to verify database connectivity and basic CRUD
 * operations. It contains minimal fields to ensure fast query execution during health checks.
 *
 * <p>The entity is mapped to the "dummy" table in the database.
 */
@Entity
@Table(name = "dummy")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@InfraGenerated
public class Dummy {
  @Id private String id;
  private String description;
}
