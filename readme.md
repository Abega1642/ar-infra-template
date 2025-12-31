<p align="center">
  <img src="ar-infra-logo.png" alt="ar-infra-template logo" width="460"/>
</p>

<h1 align="center">ar-infra-template</h1>

<p align="center">
  <strong>Production-ready Spring Boot infrastructure template</strong><br/>
  Enterprise-grade backend foundation with CI/CD, security, and observability
</p>

<p align="center">
  <a href="https://github.com/Abega1642/ar-infra-template/actions/workflows/ci-test.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/Abega1642/ar-infra-template/ci-test.yml?label=tests&style=for-the-badge" />
  </a>
  <a href="https://github.com/Abega1642/ar-infra-template/actions/workflows/ci-build.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/Abega1642/ar-infra-template/ci-build.yml?label=build&style=for-the-badge" />
  </a>
  <a href="https://github.com/Abega1642/ar-infra-template/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/Abega1642/ar-infra-template?style=for-the-badge" />
  </a>
  <img src="https://img.shields.io/badge/java-21-007396?style=for-the-badge" />
  <img src="https://img.shields.io/badge/spring%20boot-3.6.9-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
</p>

<p align="center">
  <img src="https://skillicons.dev/icons?i=spring,docker,gradle,java,postgres,rabbitmq,gmail,bitbucket&theme=light" />
</p>

<p align="center">
  <sub>Designed & maintained by <a href="https://github.com/Abega1642">Abegà Razafindratelo</a></sub>
</p>

---

# Table of Contents

1. [Introduction](#1-introduction)
2. [General Operation](#2-general-operation)
3. [Advantages](#3-advantages)
4. [Why Use This Template](#4-why-use-this-template)
5. [Detailed Architecture](#5-detailed-architecture)
6. [Testing Strategy](#6-testing-strategy)
7. [Environment Management](#7-environment-management)
8. [API Documentation](#8-api-documentation)
9. [Development Workflow with Makefile](#9-development-workflow-with-makefile)
10. [Deployment](#10-deployment)
11. [CI/CD Workflows](#11-cicd-workflows)
12. [Conclusion](#12-conclusion)

---

# 1. Introduction

The **ar-infra-template** is a production-ready Spring Boot infrastructure template engineered for enterprise backend
applications. It provides a comprehensive foundation including messaging, persistent storage, database migrations, email
delivery, security, health endpoints, integration testing, CI/CD automation, and containerization.

This template is consumed through the companion CLI [ar-infra-cli](https://github.com/Abega1642/ar-infra-cli.git), which
generates new Spring Boot projects from this base. The design philosophy ensures that development teams can immediately
focus on domain logic rather than infrastructure configuration, as all foundational components are pre-configured and
production-ready.

---

# 2. General Operation

When a project is generated via **ar-infra-cli**, the resulting codebase is immediately runnable and deployable. The
template provides:

- **Infrastructure integrations**: RabbitMQ for messaging, S3-compatible storage for object persistence, PostgreSQL with
  Flyway for database schema management, Spring Mail for email delivery, and multipart file upload validation with
  security constraints.
- **Architectural patterns**: A layered architecture separating endpoint DTOs, domain models, and persistence entities
  through explicit mapping strategies, ensuring clear boundaries between transport, business logic, and data persistence
  concerns.
- **Operational readiness**: Health endpoints that validate real infrastructure components through actual operations
  including bucket read/write/presign, email transmission, event dispatch/consumption, and database queries with
  pagination.
- **Testing infrastructure**: Integration tests using Testcontainers through a [
  `FacadeIT`](src/test/java/com/example/arinfra/conf/FacadeIT.java) base class that provisions PostgreSQL, RabbitMQ,
  S3-compatible storage, and email simulators once per JVM with dynamic property injection and controlled lifecycle
  management.
- **Quality automation**: CI/CD workflows implementing build verification, comprehensive test execution, code formatting
  validation, and multi-layered security scanning through CodeQL, Qodana, and Semgrep.
- **Container packaging**: Docker image assembly using multi-stage builds with non-root runtime execution and integrated
  health check mechanisms.

Infrastructure-generated components are annotated with `@InfraGenerated` to establish clear audit trails and distinguish
foundational code from domain-specific implementations.

---

# 3. Advantages

- **Standardization**: Every generated project adheres to identical structural conventions, quality gates, and
  operational patterns, significantly reducing team onboarding time and eliminating architectural variance across
  microservices.
- **Reliability**: Health endpoints and integration tests validate actual infrastructure behavior through real I/O
  operations rather than relying on mock objects, ensuring that validation reflects production conditions.
- **Maintainability**: Clear separation of concerns through the Diamond Model architecture (endpoint model, core model,
  repository model) with explicit mapper boundaries prevents accidental coupling and maintains long-term code quality.
- **Security posture**: Comprehensive security measures including startup validation for sensitive configurations,
  sanitized filename handling, safe exception management for storage and email operations, secure JSON configuration
  through Jackson, and ZIP bomb protection with CWE-409 mitigation.
- **Developer velocity**: Teams immediately focus on feature delivery as the template resolves all foundational
  infrastructure setup, dependency management, and integration complexity.
- **Deployment readiness**: Multi-stage Docker builds, non-root runtime execution, actuator health checks, and optimized
  image layering make artifacts immediately suitable for production deployment without additional hardening.
- **Continuous quality**: Automated code formatting through Google Java Format, static analysis via Qodana,
  vulnerability detection through CodeQL and Semgrep, and reproducible builds through GitHub Actions are integrated by
  default.

---

# 4. Why Use This Template

Use **ar-infra-template** when building backend services that must meet production standards from the initial commit.
This template is particularly valuable for organizations developing multiple services that require shared architecture
principles, consistent security baselines, and unified operational tooling.

The template ensures:

- **Accelerated time-to-market**: Elimination of infrastructure setup work allows teams to deliver business features
  immediately.
- **Validated infrastructure**: Auditable, testable infrastructure components with realistic integration verification
  ensure confidence in deployment readiness.
- **Strong operational defaults**: Integrated documentation, observability endpoints, and CI/CD automation reduce
  operational overhead and incident response time.
- **Adaptive architecture**: The layered design remains flexible and scalable as domain complexity grows, supporting
  both greenfield projects and incremental refactoring.
- **Predictable developer experience**: Consistent patterns across teams and services reduce cognitive load and enable
  engineers to move between projects efficiently.

---

# 5. Detailed Architecture

## 5.1 Configuration Layer

**Location**: [`config/`](src/main/java/com/example/arinfra/config)

The configuration layer establishes infrastructure integrations and cross-cutting concerns:

- **[`BucketConf`](src/main/java/com/example/arinfra/config/BucketConf.java)**: Configures S3-compatible storage client
  with credential management, region settings, and endpoint configuration. Provides bean definitions for S3 client
  instances and transfer managers used throughout the application.

- **[`EmailConf`](src/main/java/com/example/arinfra/config/EmailConf.java)**: Establishes Spring Mail sender
  configuration with SMTP settings, authentication, and TLS configuration. Integrates email health indicators for
  operational monitoring.

- **[`RabbitConfig`](src/main/java/com/example/arinfra/config/RabbitConfig.java)**: Configures RabbitMQ messaging
  infrastructure including connection factories, message templates, exchange declarations, queue definitions, and
  binding configurations. Establishes message converter strategies and retry policies.

- **[`SecurityConf`](src/main/java/com/example/arinfra/config/SecurityConf.java)**: Provides baseline Spring Security
  configuration serving as a foundation for project-specific authentication and authorization strategies. Teams extend
  this configuration to implement JWT validation, OAuth2 flows, or custom role-based access control policies.

- **[`SwaggerDocConf`](src/main/java/com/example/arinfra/config/SwaggerDocConf.java)**: Integrates SpringDoc OpenAPI 3.1
  specification rendering. Configures automatic API documentation generation and ensures developer documentation is
  accessible at `/doc` through redirect mechanisms.

- **[`JacksonConfiguration`](src/main/java/com/example/arinfra/config/JacksonConfiguration.java)**: Centralizes JSON
  serialization and deserialization policies. Configures secure Jackson settings including fail-on-unknown-properties
  behavior, timestamp formatting, and module registration for modern Java types.

- **[
  `MultipartConfigurationInitializer`](src/main/java/com/example/arinfra/config/MultipartConfigurationInitializer.java)
  **: Validates multipart upload limits at application startup using [
  `MultipartPropertiesValidator`](src/main/java/com/example/arinfra/validator/file/MultipartPropertiesValidator.java).
  Enforces fail-fast behavior if upload configurations are missing or exceed security thresholds, preventing runtime
  vulnerabilities.

## 5.2 Data Structures

**Location**: [`datastructure/`](src/main/java/com/example/arinfra/datastructure)

Algorithmic utilities supporting infrastructure operations without coupling to domain or persistence layers. These
components provide reusable data manipulation patterns that maintain independence from business logic.

**Example**: `ListGrouper` partitions large collections into bounded batches, enabling efficient message publishing or
bulk processing operations while respecting broker message size limits and transaction boundaries.

## 5.3 Event Layer

**Location**: [`event/`](src/main/java/com/example/arinfra/event)

The event layer implements asynchronous messaging patterns:

- **[`consumer/`](src/main/java/com/example/arinfra/event/consumer)**: [
  `EventConsumer`](src/main/java/com/example/arinfra/event/consumer/EventConsumer.java) receives messages from
  configured queues while [`EventDispatcher`](src/main/java/com/example/arinfra/event/consumer/EventDispatcher.java)
  routes messages to appropriate handlers. This architecture provides a structured consumer pipeline with error
  handling, retry logic, and dead-letter queue integration.

- **[`model/`](src/main/java/com/example/arinfra/event/model)**: Defines event abstractions including [
  `InfraEvent`](src/main/java/com/example/arinfra/event/model/InfraEvent.java) base interface, [
  `EventProducer`](src/main/java/com/example/arinfra/event/model/EventProducer.java) for message publication, [
  `EventConf`](src/main/java/com/example/arinfra/event/model/EventConf.java) for routing configuration, and [
  `DummyEvent`](src/main/java/com/example/arinfra/event/model/DummyEvent.java) demonstrating domain event implementation
  patterns.

## 5.4 Exception Layer

**Location**: [`exception/`](src/main/java/com/example/arinfra/exception)

Centralized exception definitions for infrastructure operations:

- Bucket operation failures including upload, download, and presign errors
- Email transmission and health check failures
- File conversion and validation errors
- Directory upload processing exceptions
- Missing authorization and authentication errors
- Multipart handling and validation failures

REST-specific exception handling is implemented in the endpoint layer through [
`ApiExceptionHandler`](src/main/java/com/example/arinfra/endpoint/rest/controller/ApiExceptionHandler.java), which
translates infrastructure exceptions into consistent HTTP responses with standardized error payloads.

## 5.5 Endpoint Layer

**Location**: [`endpoint/`](src/main/java/com/example/arinfra/endpoint)

The endpoint layer exposes HTTP interfaces and manages request/response transformation:

- **[`rest/controller/health/`](src/main/java/com/example/arinfra/endpoint/rest/controller/health)**: Infrastructure
  health
  verification endpoints:
    - `/ping`: Liveness probe for container orchestration
    - `/health/bucket`: Storage health validation through upload, download, and presigned URL generation
    - `/health/email`: Email system health verification through test message transmission
    - `/health/message`: Message broker health through event production and optional consumption verification
    - `/health/db`: Database connectivity and query performance through paginated entity retrieval

- **[
  `rest/controller/ApiExceptionHandler`](src/main/java/com/example/arinfra/endpoint/rest/controller/ApiExceptionHandler.java)
  **: Global exception handler implementing `@ControllerAdvice` pattern. Transforms infrastructure exceptions into
  standardized `ErrorResponse` payloads with consistent status codes, error messages, and traceability information.

- **[
  `rest/controller/model/ErrorResponse`](src/main/java/com/example/arinfra/endpoint/rest/controller/model/ErrorResponse.java)
  **: Canonical error response structure containing timestamp, HTTP status, error classification, detailed message,
  request path, and application-specific error codes for client-side error handling.

## 5.6 File Utilities

**Location**: [`file/`](src/main/java/com/example/arinfra/file)

Secure file handling and storage abstractions:

- **[`BucketComponent`](src/main/java/com/example/arinfra/file/BucketComponent.java)**: High-level abstraction over
  bucket operations providing upload (single files and directory trees), download, and presigned URL generation with
  automatic retry logic and error translation.

- **[`FilenameSanitizer`](src/main/java/com/example/arinfra/file/FilenameSanitizer.java)**: Sanitizes user-provided
  filenames to prevent path traversal attacks, remove dangerous characters, and enforce length limits.

- **[`MultipartFileConverter`](src/main/java/com/example/arinfra/file/MultipartFileConverter.java)**: Safe conversion of
  multipart uploads to filesystem representations with validation and temporary storage management.

- **[`SecureTempFileManager`](src/main/java/com/example/arinfra/file/SecureTempFileManager.java)**: Manages temporary
  file lifecycle with automatic cleanup, secure permissions, and isolation from shared temporary directories.

- **[`TempFileCleaner`](src/main/java/com/example/arinfra/file/TempFileCleaner.java)**: Scheduled cleanup of orphaned
  temporary files with configurable retention policies.

- **[`FileHash`](src/main/java/com/example/arinfra/file/FileHash.java)**: Cryptographic hash computation for file
  integrity verification supporting multiple algorithms (SHA-256, SHA-512).

## 5.7 Repository Layer

**Location**: [`repository/`](src/main/java/com/example/arinfra/repository)

Data persistence abstractions:

- **[`repository/model/`](src/main/java/com/example/arinfra/repository/model)**: JPA entity definitions mapped to
  database tables with appropriate constraints, indexes, and relationship configurations.

- **[`repository/`](src/main/java/com/example/arinfra/repository)**: Spring Data JPA repository interfaces extending
  `JpaRepository` and `JpaSpecificationExecutor` for declarative query methods and type-safe criteria queries.

## 5.8 Mail Layer

**Location**: [`mail/`](src/main/java/com/example/arinfra/mail)

Email delivery abstractions:

- **[`Email`](src/main/java/com/example/arinfra/mail/Email.java)**: Immutable value object capturing sender address,
  recipient list, subject line, and message body (plain text or HTML).

- **[`Mailer`](src/main/java/com/example/arinfra/mail/Mailer.java)**: Email transmission service integrating with Spring
  Mail infrastructure. Provides synchronous and asynchronous sending with comprehensive error handling. Integration
  tests use GreenMail for SMTP simulation without external dependencies.

## 5.9 Service Layer

**Location**: [`service/`](src/main/java/com/example/arinfra/service)

Business logic orchestration implementing domain operations across repository, event, file, and mail subsystems.
Services coordinate transactional boundaries, manage domain invariants, and implement business rules. Health services
aggregate infrastructure checks for operational monitoring.

## 5.10 Manager Layer

**Location**: [`manager/`](src/main/java/com/example/arinfra/manager)

Higher-level orchestration components coordinating multiple services or implementing complex workflows spanning multiple
bounded contexts. Managers are optional and used when service composition requires additional coordination logic or when
implementing saga patterns for distributed transactions.

## 5.11 Core Models

**Location**: [`model/`](src/main/java/com/example/arinfra/model)

Domain models representing business concepts independent of transport protocols and persistence mechanisms. These models
form the core domain language and remain isolated from framework-specific annotations or infrastructure concerns.

## 5.12 Mapper Layer

**Location**: [`mapper/`](src/main/java/com/example/arinfra/mapper)

Bidirectional mapping infrastructure using MapStruct for compile-time code generation. Mappers bridge endpoint DTOs,
core domain models, and repository entities, enforcing the Diamond Model architecture to preserve boundaries between
REST, domain, and persistence layers. Custom mapping methods handle complex transformations while maintaining type
safety.

## 5.13 Validator Layer

**Location**: [`validator/`](src/main/java/com/example/arinfra/validator)

Centralized validation and security verification:

**Base Interface**: `Validator<T>` defines `validate(T)` and `getValidatedType()` with JSR-380 constraint annotations.
Validation failures produce `IllegalArgumentException` for invalid input or `SecurityException` for security violations,
providing explicit failure semantics.

**File Validators** ([`validator/file/`](src/main/java/com/example/arinfra/validator/file)):

- **[`MultipartPropertiesValidator`](src/main/java/com/example/arinfra/validator/file/MultipartPropertiesValidator.java)
  **: Validates Spring `MultipartProperties` at application startup. Requires explicit max file and request sizes,
  rejects values exceeding absolute maximums, and warns when configurations exceed OWASP recommendations. Integrated
  through `MultipartConfigurationInitializer` for fail-fast behavior.

- **[`FileValidator`](src/main/java/com/example/arinfra/validator/file/FileValidator.java)**: Validates secure file and
  directory operations using NIO.2 APIs (`toRealPath`, symbolic link detection, readable file verification). Prevents
  TOCTOU vulnerabilities and path traversal attacks through canonical path resolution.

- **[`ZipEntryValidator`](src/main/java/com/example/arinfra/validator/file/ZipEntryValidator.java)**: Comprehensive ZIP
  archive validation protecting against CWE-409 zip bombs and malicious archives. Validates entry names for path
  traversal, verifies compression ratios, enforces size limits, detects symbolic links, prevents duplicate entries, and
  monitors total decompressed size. Provides validation methods for both `java.util.zip.ZipEntry` and Apache Commons
  `ZipArchiveEntry`, plus runtime extracted size verification.

**Integration Strategy**: Configuration initializers invoke validators at application startup to enforce fail-fast
behavior. Services and file utilities invoke validators during runtime operations to maintain ongoing security and
correctness guarantees.

## 5.14 Database Migrations

**Location**: [`resources/db/migration/`](src/main/resources/db/migration)

Versioned Flyway SQL scripts implementing incremental schema evolution:

- **[`V0_0_1__Create_dummy_tables.sql`](src/main/resources/db/migration/V0_0_1__Create_dummy_tables.sql)**: Initial
  schema definition creating tables, indexes, constraints, and sequences for demonstration entities.

- **[
  `V0_0_2__Insert_dummy_values_in_dummy_tables.sql`](src/main/resources/db/migration/V0_0_2__Insert_dummy_values_in_dummy_tables.sql)
  **: Reference data seeding for development and testing environments.

Migrations execute automatically at application startup through Flyway's versioned migration strategy, ensuring
reproducible schema evolution across all environments (development, testing, staging, production). Each migration is
transactional and idempotent.

---

# 6. Testing Strategy

The testing strategy emphasizes validation of actual infrastructure behavior over mock-based verification:

**Unit Tests**: Verify isolated business logic components using mocks where external dependencies are required. Unit
tests focus on algorithm correctness, edge case handling, and domain rule enforcement without infrastructure setup
overhead.

**Integration Tests**: Extend [`FacadeIT`](src/test/java/com/example/arinfra/conf/FacadeIT.java) base class, which
provisions real infrastructure components through Testcontainers including PostgreSQL, RabbitMQ, S3-compatible storage (
LocalStack), and SMTP servers (GreenMail). [`FacadeIT`](src/test/java/com/example/arinfra/conf/FacadeIT.java) configures
dynamic properties through Spring's `DynamicPropertyRegistry`, ensures container reuse per JVM to minimize startup
overhead, and provides controlled lifecycle management with automatic cleanup.

This approach validates actual I/O paths, message flows, storage operations, and database queries rather than testing
interface contracts alone. Integration tests reflect production behavior and catch configuration errors, network issues,
and serialization problems that mock-based tests cannot detect.

---

# 7. Environment Management

Environment-specific configuration is managed through externalized properties:

**[`.env.template`](.env.template)**: Canonical environment variable specification documenting all required and optional
configuration parameters including RabbitMQ connection details, PostgreSQL datasource configuration, Backblaze/B2 S3
credentials and endpoints, SMTP mail service settings, and application port bindings. Teams copy this template to `.env`
and populate values appropriate for each environment (local development, continuous integration, staging, production).

**[`EnvConf`](src/test/java/com/example/arinfra/conf/EnvConf.java)**: Test-specific configuration class injecting
environment-like properties dynamically during integration test execution. Supports API keys, feature flags, JWT signing
keys, external service endpoints, and application-specific thresholds. [
`FacadeIT`](src/test/java/com/example/arinfra/conf/FacadeIT.java) discovers and applies `EnvConf` implementations
through reflection, allowing test-specific overrides without modifying application configuration files.

---

# 8. API Documentation

OpenAPI 3.1 specification located at [`doc/api.yaml`](doc/api.yaml) provides comprehensive API documentation. The
application automatically redirects root path (`/`) and `/doc` to Swagger UI, rendering the specification immediately
accessible without additional configuration.

Health endpoints are documented as reference examples demonstrating request/response patterns, status codes, and error
scenarios. Development teams extend [`doc/api.yaml`](doc/api.yaml) to describe new API endpoints, request schemas,
response models, and authentication requirements.

This contract-first approach drives consumer clarity, supports automated client generation, enables API versioning
strategies, and maintains documentation synchronization with implementation through validation in CI/CD pipelines.

---

# 9. Development Workflow with Makefile

The template includes a comprehensive Makefile that standardizes development workflows and enables local execution of
CI/CD pipelines. The Makefile detects the operating system (Linux, macOS, Windows) and adapts commands accordingly,
ensuring consistent behavior across development environments.

## 9.1 Makefile Structure

The Makefile is organized into logical sections:

- **Development**: Build, compilation, and local execution targets
- **Testing**: Unit, integration, and coverage test execution
- **Code Quality**: Formatting, linting, and static analysis
- **Docker**: Container image management and runtime operations
- **CI/CD**: Local replication of continuous integration pipelines
- **Cleanup**: Artifact and resource cleanup operations
- **Setup**: Development tool installation and verification

## 9.2 Common Development Tasks

**Viewing Available Targets**:

```bash
make help
```

This command displays all available Makefile targets with descriptions, organized by category.

**Code Formatting Before Commit**:

```bash
make format
git add --all
make ci-format
```

The `make format` command applies Google Java Format to all Java source files and formats YAML files using yamlfmt.
After staging changes with `git add --all`, `make ci-format` verifies that formatting is correct and matches CI
requirements. This workflow prevents formatting-related CI failures.

**Running Tests Locally**:

```bash
make test              # All tests (unit + integration)
make test-unit         # Unit tests only (*Test.java)
make test-integration  # Integration tests only (*IT.java)
```

Tests execute with the same Testcontainers configuration used in CI, ensuring local test results match continuous
integration behavior.

**Building the Application**:

```bash
make build
```

This target performs format verification, compiles the application, and generates the JAR artifact. The build process
mirrors CI build steps.

**Running the Application**:

```bash
make run    # Standard execution
make dev    # Development mode with active profile
```

## 9.3 Docker Operations

**Building and Running Containers**:

```bash
make docker-build                    # Build image
make docker-run                      # Start container
make docker-logs                     # View container logs
make health-check                    # Verify application health
make docker-stop                     # Stop and remove container
```

**Custom Configuration**:

```bash
make docker-build IMAGE_TAG=v1.2.3
make docker-run PORT=9090
```

## 9.4 Security Scanning

**Semgrep Analysis**:

```bash
make semgrep
```

The `semgrep` target automatically installs pipx (if not present) and Semgrep, then executes security analysis with
SARIF output generation. The Makefile handles cross-platform pipx installation (apt-get for Debian/Ubuntu, dnf for
Fedora, brew for macOS, pip for Windows) and verifies installation success before running analysis.

**Qodana Analysis**:

```bash
make qodana
```

Executes JetBrains Qodana static analysis in a Docker container, generating comprehensive code quality reports.

## 9.5 Local CI/CD Validation

Before pushing code, developers can replicate complete CI/CD pipelines locally:

```bash
make ci-format    # Format check (ci-format.yml)
make ci-test      # Test execution (ci-test.yml)
make ci-build     # Docker build (ci-build.yml)
make ci-qodana    # Qodana analysis (ci-qodana.yml)
make ci-semgrep   # Semgrep analysis (ci-semgrep.yml)
```

These targets execute identical commands to GitHub Actions workflows, enabling early detection of CI failures and
reducing feedback cycle time.

## 9.6 Cleanup Operations

```bash
make clean         # Remove build artifacts
make clean-docker  # Remove Docker images and containers
make clean-all     # Complete cleanup
```

## 9.7 Development Environment Setup

**Tool Installation**:

```bash
make install    # Install Google Java Format and configure permissions
make verify     # Verify Java, Gradle, and Docker availability
```

The `make install` target downloads required tooling (Google Java Format JAR) and configures executable permissions for
scripts. The `make verify` target validates that all required tools are installed and accessible.

## 9.8 Best Practices

1. **Pre-commit validation**: Always execute `make format` before committing code changes.
2. **Pre-push validation**: Run `make ci-format ci-test ci-build` before pushing to remote branches to catch CI failures
   early.
3. **Incremental testing**: Use `make test-unit` during development for faster feedback, reserving `make test` for
   pre-push validation.
4. **Container cleanup**: Periodically execute `make clean-docker` to reclaim disk space from unused images.
5. **Tool verification**: Run `make verify` after system updates to ensure development environment remains consistent.

---

# 10. Deployment

The template includes a multi-stage Dockerfile optimized for production deployment:

**Build Stage**: Uses Gradle 8.10 on JDK 21 Alpine to resolve dependencies and build the Spring Boot JAR. Dependencies
are cached separately from source code to optimize layer caching and reduce rebuild time.

**Runtime Stage**: Uses Eclipse Temurin JRE 21 Alpine as the base image. Creates a non-root `spring` user and group,
copies the application JAR and documentation, and configures execution permissions. The container runs as the `spring`
user to minimize attack surface.

**Health Check Integration**: Dockerfile includes a `HEALTHCHECK` instruction that queries the actuator health
endpoint (`/actuator/health`) at 30-second intervals. Container orchestration platforms (Kubernetes, Docker Swarm, ECS)
use this health check for readiness probes and automatic container restart.

**Entry Point**: [`docker-start.sh`](docker-start.sh) serves as the container entry point, executing `java -jar app.jar`
with support for command-line arguments passed from container runtime.

This configuration produces a lean, secure container image suitable for production deployment with standard health
probing and non-root execution.

---

# 11. CI/CD Workflows

Continuous integration and deployment workflows are defined in `.github/workflows/`:

**[`ci-build.yml`](.github/workflows/ci-build.yml)**: Builds the Docker image using BuildKit for layer caching, verifies
image creation, validates JAR file presence, and confirms Java version. This workflow ensures that the containerized
application is properly assembled and executable. The workflow invokes `make ci-build` to execute the build pipeline
locally within the CI environment.

**[`ci-test.yml`](.github/workflows/ci-test.yml)**: Executes unit and integration test suites with Gradle test caching
and Testcontainers integration. Tests run with the same configuration as local development, ensuring consistency. The
workflow sets required environment variables for Testcontainers and invokes `make ci-test` to execute the test pipeline.

**[`ci-format.yml`](.github/workflows/ci-format.yml)**: Enforces Google Java Format standards and YAML formatting
requirements. The pipeline fails if any file does not match formatting conventions, ensuring code consistency across all
contributions. Developers run `./format.sh` or `make format` locally to apply formatting before pushing. The workflow
invokes `make ci-format` to verify formatting compliance.

**[`ci-codeql.yml`](.github/workflows/ci-codeql.yml)**: Performs security analysis using GitHub CodeQL, identifying
potential security vulnerabilities, code quality issues, and common programming errors through semantic code analysis.

**[`ci-qodana.yml`](.github/workflows/ci-qodana.yml)**: Executes JetBrains Qodana static analysis with PR comment
integration, annotations, and detailed issue reporting. Results are printed to workflow logs for immediate visibility.

**[`ci-semgrep.yml`](.github/workflows/ci-semgrep.yml)**: Runs Semgrep security linting with automatic rule updates and
SARIF output for GitHub Security tab integration. The workflow installs Semgrep via pipx and invokes `make ci-semgrep`
to execute the security scan.

Together, these workflows enforce formatting standards, verify functional correctness, and establish security baselines
on every push and pull request across all branches. The Makefile integration ensures that developers can replicate these
exact checks locally before committing code.

---

# 12. Conclusion

**ar-infra-template** provides a robust, production-ready Spring Boot foundation that codifies enterprise architecture
patterns, comprehensive security validations, operational health observability, realistic integration testing,
documentation standards, CI/CD automation, and deployment hardening.

Projects generated from this template via [ar-infra-cli](https://github.com/Abega1642/ar-infra-cli.git) begin with
production-grade infrastructure from the initial commit. The Makefile integration enables developers to execute the
complete CI/CD pipeline locally, ensuring that code quality, formatting, and security standards are met before
committing changes.

For technical leadership, the template delivers predictable quality and development velocity across multiple services
through consistent architecture, unified security guardrails, and standardized operational practices. For engineering
teams, the template eliminates infrastructure setup overhead and provides proven patterns for common backend
requirements, allowing immediate focus on domain problem-solving.

This template is designed for organizations building distributed systems that require maintainable, secure, and reliable
services at scale. The comprehensive testing strategy, security-first design, and operational readiness features make it
suitable for enterprise applications requiring high availability and regulatory compliance.