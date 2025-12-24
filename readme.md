# ar‑infra‑template

 **Author**: [*Abegà Razafindratelo*](https://github.com/Abega1642)
 
 **Repository**: https://github.com/Abega1642/ar-infra-template.git

------

# 1. Introduction

The **ar‑infra‑template** is a production‑ready Spring Boot infrastructure template engineered for enterprise backend applications. It is a comprehensive foundation including *messaging*, *persistent storage*, *database migrations*, *email delivery*, *security*, *health endpoints*, *integration testing*, *CI/CD*, and *containerization*.

This template is consumed through the companion CLI [ar‑infra‑cli](https://github.com/Abega1642/ar-infra-cli.git), which generates new Spring Boot projects from this base. The intention is to start teams on a fully configured, standards‑compliant backend stack so they can invest their effort in domain logic rather than infrastructure plumbing.

------

# 2. General operation

When a project is generated via **ar‑infra‑cli**, the resulting codebase is immediately runnable and deployable. It provides:

- Configurations for *RabbitMQ* (messaging), *S3‑compatible bucket* (storage), *PostgreSQL* with *Flyway* (database and schema management), *Spring Mail* (email), multipart file upload validation, *security configuration*, and *Swagger/OpenAPI* documentation.
- A layered architecture separating endpoint DTOs, domain models, and persistence entities, with explicit mapping.
- Health endpoints that exercise the real infrastructure (bucket read/write/presign, email send, event dispatch/consume, database pagination, ping).
- *Integration tests using Testcontainers* and a [`FacadeIT`](src/test/java/com/example/arinfra/conf/FacadeIT.java) base that brings up *Postgres, RabbitMQ, S3, and email simulators* once per JVM and injects dynamic properties.
- CI/CD workflows that build, test, format, and scan (*CodeQL*, *Qodana*, *Semgrep*).
- *Docker image packaging* with a non‑root runtime user and health checks.

Infrastructure‑generated components are annotated with @InfraGenerated to make the infrastructure footprint auditable and distinct from business code.

------

# 3. Advantages

- ***Standardization***: Every generated project follows the same structure, conventions, and quality gates, reducing onboarding time and variance between services.
- ***Reliability***: Health endpoints and integration tests validate actual infrastructure behavior rather than relying on mocks.
- ***Maintainability***: Clear separation of concerns through the “Diamond Model” (endpoint model, core model, repository model) with explicit mappers avoids accidental coupling.
- ***Security posture***: Startup validation for sensitive configurations (multipart upload limits), sanitized filename handling, storage and email exception safety, and secure JSON configuration (Jackson).
- ***Developer velocity***: Teams focus on delivering features; the template solves the foundational setup.
- ***Deployment readiness***: Multi‑stage Docker builds, non‑root runtime, and actuator health checks make the artifact suitable for production.
- ***Continuous quality***: Formatting (*Google Java Forma*t), static analysis (*Qodana*), vulnerability analysis (*CodeQL*, *Semgrep*), and repeatable builds through GitHub Actions are provided by default.

------

# 4. Why use this template

Use *ar‑infra‑template* if you want a backend that is **immediately** fit for **production environments** with mature engineering practices. It is ideal for organizations building multiple services that must share architecture principles, security baselines, and operational tooling.

It ensures:

- Reduced time‑to‑first‑feature by eliminating setup work.
- Auditable, testable infrastructure with realistic integration checks.
- Strong defaults for documentation, observability, and CI/CD.
- Architecture that remains adaptable and scalable as the domain grows.
- A predictable developer experience across teams and services.

------

# 5. Detailed architecture

## 5.1 Configuration layer ([`config/`](src/main/java/com/example/arinfra/config))

- [`BucketConf`](src/main/java/com/example/arinfra/config/BucketConf.java): Configures S3‑compatible storage client and integration.
- [`EmailConf`](src/main/java/com/example/arinfra/config/EmailConf.java): Configures Spring Mail sender and email health.
- [`RabbitConfig`](src/main/java/com/example/arinfra/config/RabbitConfig.java): Configures messaging templates, exchanges, queues, and bindings.
- [`SecurityConf`](src/main/java/com/example/arinfra/config/SecurityConf.java): Provides default Spring Security configuration, intended to be adapted per project (e.g., JWT/OAuth2, role policies).
- [`SwaggerDocConf`](src/main/java/com/example/arinfra/config/SwaggerDocConf.java): Integrates SpringDoc and ensures developer documentation appears at /doc.
- [`JacksonConfiguration`](src/main/java/com/example/arinfra/config/JacksonConfiguration.java): Centralizes JSON serialization/deserialization policies and modern, secure Jackson settings.
- [`MultipartConfigurationInitializer`](src/main/java/com/example/arinfra/config/MultipartConfigurationInitializer.java): Validates multipart upload limits at startup using [`MultipartPropertiesValidator`](src/main/java/com/example/arinfra/validator/file/MultipartPropertiesValidator.java) to enforce fail‑fast behavior if misconfigured.

## 5.2 Data structures ([`datastructure/`](src/main/java/com/example/arinfra/datastructure))

- Algorithmic utilities that support infra operations without coupling to domain or persistence.
- ***Example***: ListGrouper partitions large collections into bounded batches for messaging or bulk processing.

## 5.3 Event layer ([`event/`](src/main/java/com/example/arinfra/event))

- [`consumer/`](src/main/java/com/example/arinfra/event/consumer): [`EventConsumer`](src/main/java/com/example/arinfra/event/consumer/EventConsumer.java) and [`EventDispatcher`](src/main/java/com/example/arinfra/event/consumer/EventDispatcher.java) receive and dispatch messages from queues, providing a structured consumer pipeline.
- [`model/`](src/main/java/com/example/arinfra/event/model): [`InfraEvent`](src/main/java/com/example/arinfra/model/InfraEvent.java), [`EventProducer`](src/main/java/com/example/arinfra/model/EventProducer.java), [`EventConf`](src/main/java/com/example/arinfra/model/EventConf.java), and example [`DummyEvent`](src/main/java/com/example/arinfra/model/DummyEvent.java) demonstrate how to define and publish domain events.

## 5.4 Exception layer ([`exception/`](src/main/java/com/example/arinfra/exception))

- Centralized custom exceptions for bucket operations, email sending and health checks, file conversions, directory uploads, missing authorization, and multipart handling.
- REST exception handling is not here; it is in the endpoint layer ([`ApiExceptionHandler`](src/main/java/com/example/arinfra/endpoint/rest/controller/ApiExceptionHandler.java)) to translate exceptions into consistent HTTP responses.

## 5.5 Endpoint layer ([`endpoint/`](src/main/java/com/example/arinfra/endpoint))

  - [`rest/controller/health`](src/main/java/com/example/arinfra/rest/controller/health): Health controllers exposing:
  - `/ping` for liveness.
  - `/health/bucket` to upload, download, and presign a file (storage health).
  - `/health/email` to send a test email.
  - `/health/message` to produce and optionally process dummy events through the broker.
  - `/health/db` to paginate dummy entities (database health).
- [`rest/controller/ApiExceptionHandler`](src/main/java/com/example/arinfra/rest/controller/ApiExceptionHandler.java): Global `@ControllerAdvice` that transforms exceptions into standardized ErrorResponse.
- [`rest/controller/model/ErrorResponse`](src/main/java/com/example/arinfra/rest/controller/model/ErrorResponse.java): Canonical error payload with timestamp, status, error, message, path, and application error code.

## 5.6 File utilities ([`file/`](src/main/java/com/example/arinfra/file))

- [`BucketComponent`](src/main/java/com/example/arinfra/file/BucketComponent.java): Abstraction over bucket operations (upload files/directories, download, presign URLs).
- [`FilenameSanitizer`](src/main/java/com/example/arinfra/file/FilenameSanitizer.java), [`MultipartFileConverter`](src/main/java/com/example/arinfra/file/MultipartFileConverter): Safe handling of user‑provided file names and uploads.
- [`SecureTempFileManager`](src/main/java/com/example/arinfra/file/SecureTempFileManager.java), [`TempFileCleaner`](src/main/java/com/example/arinfra/file/TempFileCleaner.java), [`FileHash`](src/main/java/com/example/arinfra/file/FileHash.java): Secure temporary storage lifecycle and utility functions for integrity.

## 5.7 Repository layer ([`repository/`](src/main/java/com/example/arinfra/repository))

- [`repository/model/`](src/main/java/com/example/arinfra/repository/model): JPA entities mapped to database tables.
- [`repository/`](src/main/java/com/example/arinfra/repository): **Spring Data repositories** (`JpaRepository` and related interfaces).

## 5.8 Mail layer ([`mail/`](src/main/java/com/example/arinfra/mail))

- [`Email`](src/main/java/com/example/arinfra/mail/Email.java): Simple object capturing sender, recipient, subject, and body.
- [`Mailer`](src/main/java/com/example/arinfra/Mailer.java): Service that sends emails, integrates with Spring Mail, and is testable using GreenMail.

## 5.9 Service layer ([`service/`](src/main/java/com/example/arinfra/service))

- Business services implementing domain logic and orchestration across repository, event, file, and mail subsystems.
- Health services aggregating checks for bucket, email, event, and database.

## 5.10 Manager layer ([`manager/`](src/main/java/com/example/arinfra/manager))

- Higher‑level orchestration classes (optionally used by teams) coordinating multiple services or workflows.

## 5.11 Core models ([`model/`](src/main/java/com/example/arinfra/model))

- Domain models independent of transport and persistence concerns.

## 5.12 Mapper layer ([`mapper/`](src/main/java/com/example/arinfra/mapper))

- Bridges endpoint DTOs, core domain models, and repository entities using MapStruct.
- Enforces the Diamond Model to preserve boundaries between REST, domain, and persistence.

## 5.13 Validator layer ([`validator/`](src/main/java/com/example/arinfra/validator))

- Purpose: Centralizes infrastructure validation and security checks to enforce safe configurations and file handling.
- Base interface: Validator<T> defines validate(T) and getValidatedType() with JSR‑380 annotations and explicit failure semantics (`IllegalArgumentException` for invalid input; `SecurityException` for security violations).
- File validators ([`validator/file/`](src/main/java/com/example/arinfra/validator/file)):
  - [`MultipartPropertiesValidator`](src/main/java/com/example/arinfra/validator/file): Validates Spring `MultipartProperties` at startup; requires explicit max file/request sizes, rejects values over absolute maximum, warns above OWASP recommendations. Integrated via MultipartConfigurationInitializer.
  - [`FileValidator`](src/main/java/com/example/arinfra/validator/file/FileValidator.java): Validates secure file and directory operations using NIO.2 (toRealPath, link checks, readable file verification) to prevent TOCTOU and path traversal.
  - [`ZipEntryValidator`](src/main/java/com/example/arinfra/validator/file/ZipEntryValidator.java): Validates ZIP entry names, sizes, compression ratios, path traversal, symbolic links, duplicate entries, and total decompressed limits to guard against CWE‑409 zip bombs and malicious archives. Provides methods for both `java.util.zip.ZipEntry` and **Apache** `ZipArchiveEntry`, plus runtime extracted size checks.
- Integration:
  - Configuration initializers invoke validators at application startup for fail‑fast behavior.
  - Services and file utilities use validators during runtime operations to ensure ongoing safety.

## 5.14 Database migrations ([`resources/db/migration/`](src/main/java/com/example/arinfra/resources/db/migration))

- Versioned Flyway SQL scripts:
  - [`V0_0_1__Create_dummy_tables.sql`](src/main/java/com/example/arinfra/resources/db/migration/V0_0_1__Create_dummy_tables.sql) initializes schema for dummy entities.
  - [`V0_0_2__Insert_dummy_values_in_dummy_tables.sql`](src/main/java/com/example/arinfra/resources/db/migration/V0_0_2__Insert_dummy_values_in_dummy_tables.sql) seeds test data.
- Migrations run automatically at startup, ensuring reproducible schema evolution across environments.

------

# 6. Testing strategy

Unit tests verify isolated logic with mocks where appropriate. Integration tests extend a [`FacadeIT`](src/test/java/com/example/arinfra/conf/FacadeIT.java) base that brings up real infrastructure components via **Testcontainers** ***(PostgreSQL, RabbitMQ, S3‑compatible storage, and email servers)***. [`FacadeIT`](src/test/java/com/example/arinfra/conf/FacadeIT.java) configures dynamic properties through Spring’s `DynamicPropertyRegistry`, ensures containers are reused per JVM, and provides controlled teardown. This approach validates actual I/O paths, message flows, and storage operations, not just interfaces, and is aligned with production behavior.

------

# 7. Environment management

- [`.env.template`](.env.template) is the canonical list of environment variables for **RabbitMQ**, **datasource**, **Backblaze/B2 S3 settings**, **mail service**, and **application port**. Teams copy this to `.env` and fill values per environment (production, preproduction, etc.).
- [`EnvConf`](src/test/java/com/example/arinfra/conf/EnvConf.java) (in tests) injects environment‑like properties dynamically during integration tests for API keys, feature flags, JWT settings, external endpoints, and application thresholds. It is optional and discovered via reflection by [`FacadeIT`](src/test/java/com/example/arinfra/conf/FacadeIT.java).

------

# 8. API documentation

An *OpenAPI 3.1* specification resides at [`doc/api.yaml`](doc/api.yaml). The application redirects `/` and `/doc` to *Swagger UI*, rendering the spec immediately. Health endpoints are documented as examples, and teams extend doc/api.yaml to describe new APIs. This drives consumer clarity and supports contract‑first evolution without ad‑hoc documentation.

------

# 9. Deployment

The template includes a multi‑stage Dockerfile:

- Build stage uses ***Gradle*** on **JDK 21 Alpine** to resolve dependencies and build the bootJar.
- Runtime stage uses **Eclipse Temurin JRE 21 Alpine**, creates and runs as a non‑root spring user, and exposes actuator health via a HEALTHCHECK. Entry is [`docker‑start.sh`](docker-start.sh) executing java -jar app.jar.

This yields a lean, secure image suitable for production environments with standard health probing.

------

# 10. CI/CD workflows

Workflows in .github/workflows include:

- [`ci-build.yml`](.github/workflows/ci-build.yml): Builds the ***Docker image***.
- [`ci-test.yml`](.github/workflows/ci-test.yml): Runs unit and integration tests.
- [`ci-format.yml`](.github/workflows/ci-format.yml): Enforces ***Google Java Format***; the pipeline fails on unformatted code. Locally run ``` ./format.sh``` to allow the [`google-java-format-1.28.0-all-deps.jar`](google-java-format-1.28.0-all-deps.jar) format your code so it can pass this CI.
- [`ci-codeql.yml`](.github/workflows/ci-codeql.yml): Performs security analysis using ***CodeQL***.
- [`ci-qodana.yml`](.github/workflows/ci-qodana.yml): Runs static analysis via JetBrains ***Qodana***.
- [`ci-semgrep.yml`](.github/workflows/ci-semgrep.yml): Executes ***Semgrep*** rules for security linting.

Together, these pipelines enforce formatting, correctness, and security baselines on every push and pull request.

------

# 11. Conclusion

***ar‑infra‑template*** is a **robust**, **production‑ready** Spring Boot foundation. It codifies architecture boundaries, security validations, health observability, realistic integration testing, documentation practices, CI/CD automation, and deployment hardening. Projects generated from this template via ar‑infra‑cli start on day one with an enterprise‑grade posture.

For tech leads, the benefit is predictable quality and velocity across services: the same structure, the same guardrails, and the same operational readiness. For engineers, it means focusing on domain problems while relying on proven infrastructure. This template is suitable for large projects that need to scale while preserving maintainability, security, and reliability.
