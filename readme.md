<h1 align="center">🚀 ar-infra-template</h1>

<p align="center">
  <b>Production-ready Spring Boot Infrastructure Template</b><br>
  Author: <a href="https://github.com/Abega1642">Abegà Razafindratelo</a><br>
  Repository: <a href="https://github.com/Abega1642/ar-infra-template.git">ar-infra-template</a>
</p>

<p align="center">
  <img src="https://img.shields.io/github/license/Abega1642/ar-infra-template?style=for-the-badge&color=blue" alt="License"/>
  <img src="https://img.shields.io/github/actions/workflow/status/Abega1642/ar-infra-template/ci-build.yml?style=for-the-badge&logo=github" alt="Build Status"/>
  <img src="https://img.shields.io/github/actions/workflow/status/Abega1642/ar-infra-template/ci-test.yml?style=for-the-badge&logo=github&label=tests" alt="Tests"/>
  <img src="https://img.shields.io/github/actions/workflow/status/Abega1642/ar-infra-template/ci-codeql.yml?style=for-the-badge&logo=github&label=CodeQL" alt="CodeQL"/>
  <img src="https://img.shields.io/github/actions/workflow/status/Abega1642/ar-infra-template/ci-qodana.yml?style=for-the-badge&logo=jetbrains&label=Qodana" alt="Qodana"/>
  <img src="https://img.shields.io/github/actions/workflow/status/Abega1642/ar-infra-template/ci-semgrep.yml?style=for-the-badge&logo=semgrep&label=Semgrep" alt="Semgrep"/>
</p>

---

<h2>📖 Overview</h2>

<p>
<code>ar-infra-template</code> is a <b>production-ready Spring Boot template</b> designed to be generated via the companion CLI tool 
<a href="https://github.com/Abega1642/ar-infra-cli.git">ar-infra-cli</a>.
</p>

<p>
Instead of starting from scratch, developers can generate a fully configured Spring Boot project with messaging, storage, database, email, security, health checks, CI/CD, and Docker support already wired in. This ensures teams can focus on <b>business logic</b> rather than infrastructure setup.
</p>

<p><b>⚠️ Note:</b> This repository is <i>not intended to be cloned and customized manually</i>. It serves as the <b>base template</b> for <code>ar-infra-cli</code>.</p>

---

<h2>🛠️ Features</h2>

<p>
This template comes with everything you need to run a backend service in production:
</p>

<ul>
  <li><b>Messaging</b>: RabbitMQ configured for async tasks and queues.</li>
  <li><b>Storage</b>: Backblaze S3-compatible bucket integration.</li>
  <li><b>Database</b>: PostgreSQL with Flyway migrations.</li>
  <li><b>Email</b>: Ready-to-use mailer service with health checks.</li>
  <li><b>Security</b>: Default <code>SecurityConf</code> to be customized.</li>
  <li><b>Health checks</b>: Endpoints for bucket, email, events, repository, and ping.</li>
  <li><b>Testing</b>: Unit + integration tests with Testcontainers.</li>
  <li><b>Documentation</b>: OpenAPI spec (<code>doc/api.yaml</code>) rendered via Swagger UI.</li>
  <li><b>CI/CD</b>: GitHub Actions for build, test, format, CodeQL, Qodana, Semgrep.</li>
  <li><b>Dockerized</b>: Multi-stage Dockerfile with health checks and non-root user.</li>
</ul>

---

<h2>📂 Project Structure</h2>

<p>
The project follows a clean architecture with clear separation of concerns. Each package has a specific responsibility:
</p>

<table>
<tr><td><b>config/</b></td><td>Spring bean configurations (BucketConf, EmailConf, RabbitConfig, SecurityConf, SwaggerDocConf, ObjectMapperConfig).</td></tr>
<tr><td><b>datastructure/</b></td><td>Algorithmic utilities (e.g., ListGrouper for batch processing).</td></tr>
<tr><td><b>event/</b></td><td>Messaging foundations (consumers, producers, event models). Developers add new events here.</td></tr>
<tr><td><b>exception/</b></td><td>Custom exceptions for bucket, email, health, file. REST handler lives in <code>endpoint/ApiExceptionHandler</code>.</td></tr>
<tr><td><b>endpoint/</b></td><td>REST API layer: controllers, health endpoints, error models, global exception handler.</td></tr>
<tr><td><b>file/</b></td><td>File utilities: BucketComponent, sanitizers, converters, temp file managers.</td></tr>
<tr><td><b>repository/</b></td><td>Persistence layer: JPA entities + repositories.</td></tr>
<tr><td><b>mail/</b></td><td>Email infrastructure: Email + Mailer service.</td></tr>
<tr><td><b>service/</b></td><td>Business logic: core services, health services, utilities.</td></tr>
<tr><td><b>manager/</b></td><td>Orchestration layer: placeholder for managers coordinating multiple services.</td></tr>
<tr><td><b>model/</b></td><td>Core domain models (business entities).</td></tr>
<tr><td><b>mapper/</b></td><td>Mapping layer bridging DTOs, domain models, and persistence entities (Diamond Model).</td></tr>
<tr><td><b>resources/db/migration/</b></td><td>Flyway migration scripts for schema evolution.</td></tr>
</table>

---

<h2>🧪 Testing Philosophy</h2>

<p>
Testing is split into two layers:
</p>

<ul>
  <li><b>Unit tests</b> (<code>MyClassTest</code>): Validate isolated class logic with mocks.</li>
  <li><b>Integration tests</b> (<code>MyClassIT</code>): Extend <code>FacadeIT</code> to bootstrap real infrastructure via Testcontainers.</li>
</ul>

<p>
<code>FacadeIT</code> ensures containers (Postgres, RabbitMQ, S3, Email) are started once per JVM, stopped gracefully, and dynamically injected into the Spring context. This guarantees <b>end-to-end reliability</b>.
</p>

---

<h2>🌍 Environment Management</h2>

<p>
Environment variables are managed through:
</p>

<ul>
  <li><b>.env.template</b>: Placeholder for production/preproduction variables (RabbitMQ, DB, S3, Mail).</li>
  <li><b>.env</b>: Developers fill in real values per environment.</li>
  <li><b>EnvConf</b>: Injects test-safe environment variables dynamically during integration tests.</li>
  <li><b>SecurityConf</b>: Default security setup, intended to be customized.</li>
</ul>

---

<h2>📑 API Documentation</h2>

<p>
API documentation is built-in:
</p>

<ul>
  <li><b>OpenAPI spec</b>: <code>doc/api.yaml</code></li>
  <li><b>Swagger UI</b>: Accessible at <code>/doc</code></li>
  <li><b>Default endpoints</b>: <code>/ping</code>, <code>/health/bucket</code>, <code>/health/email</code>, <code>/health/message</code>, <code>/health/db</code></li>
</ul>

<p>
Developers simply edit <code>doc/api.yaml</code> to describe their APIs, and Swagger UI renders them automatically.
</p>

---

<h2>🐳 Dockerization</h2>

<p>
The template includes a multi-stage Dockerfile:
</p>

<ul>
  <li>Stage 1: Gradle build (<code>bootJar</code>).</li>
  <li>Stage 2: Eclipse Temurin JRE runtime.</li>
  <li>Non-root user: <code>spring</code>.</li>
  <li>Healthcheck: <code>/actuator/health</code>.</li>
  <li>Entrypoint: <code>docker-start.sh</code>.</li>
</ul>

---

<h2>🔄 CI/CD Workflows</h2>

<p>
GitHub Actions workflows are located in <code>.github/workflows</code>:
</p>

<ul>
  <li><code>ci-build.yml</code> → Docker build</li>
  <li><code>ci-test.yml</code> → Unit + integration tests</li>
  <li><code>ci-format.yml</code> → Code formatting check with Google Java Format</li>
  <li><code>ci-codeql.yml</code> → Security analysis with GitHub CodeQL</li>
  <li><code>ci-qodana.yml</code> → Static analysis with JetBrains Qodana</li>
  <li><code>ci-semgrep.yml</code> → Security linting with Semgrep</li>
</ul>

<p>
These workflows ensure that every commit and pull request is automatically validated for build integrity, test coverage, code formatting, and security compliance.
</p>

---

<h2>📌 Related Projects</h2>

<ul>
  <li><b>ar-infra-cli</b> → <a href="https://github.com/Abega1642/ar-infra-cli.git">GitHub Repository</a><br>
  CLI tool that generates Spring Boot projects using this template.</li>
</ul>

<p>
The CLI is the intended way to use <code>ar-infra-template</code>. It automates project generation with commands like:
</p>

```bash
ar-infra generate --group=com.example --artifact=myapp
```

<p>
This produces a fully configured Spring Boot project with infrastructure, tests, CI/CD, and Docker support ready to run.
</p>

---

<h2>📝 License</h2>

<p>
This project is licensed under the <b>MIT License</b>.<br>
© <b>Abegà Razafindratelo</b>
</p>

---

<h2>🎯 Purpose</h2>

<p>
The purpose of <code>ar-infra-template</code> is to serve as a <b>foundation for generated projects</b> via <code>ar-infra-cli</code>. 
While developers <i>can</i> clone and customize it manually, the intended workflow is to use the CLI for effortless generation of production-ready Spring Boot applications.
</p>

<p>
By combining infrastructure, testing, documentation, CI/CD, and Dockerization, this template ensures that every generated project starts with <b>enterprise-grade readiness</b>.
</p>