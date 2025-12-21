package com.example.arinfra.repository;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.repository.model.Dummy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link Dummy} entity operations.
 *
 * <p>This repository provides standard CRUD operations for dummy data used in health checks. It
 * extends {@link JpaRepository} to inherit basic database operations including pagination and
 * sorting capabilities.
 *
 * <p>Primary use case is for database health check endpoints to verify connectivity and query
 * execution.
 */
@Repository
@InfraGenerated
public interface DummyRepository extends JpaRepository<Dummy, String> {}
