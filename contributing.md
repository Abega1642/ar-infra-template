# Contributing Guide

Thank you for your interest in contributing to this project.

This project prioritizes **security, maintainability, reliability, and consistency**.
All contributions must strictly follow the established rules and structure.

---

## Contribution Workflow

1. Fork the repository
2. Create a dedicated branch from `main`
3. Implement your changes
4. Write or update tests
5. Ensure all CI checks pass
6. Open a Pull Request

---

## Project Structure Rules (Mandatory)

The existing project structure is **intentional** and **must be respected**.

- Do **NOT** introduce new directories or files that do not align with the current architecture
- Do **NOT** reorganize packages or folders without prior discussion
- Do **NOT** bypass existing abstractions or conventions

If you believe a structural change is necessary:
- Open a discussion **before** implementing it
- Provide clear technical justification

Unauthorized structural changes will be rejected.

---

## Code Quality & Security Requirements

All contributions **must**:

- Include appropriate tests
- Pass all CI checks:
  - Build
  - Tests
  - Formatting
  - CodeQL
  - Semgrep
  - Qodana
  - SonarQube (where applicable)
- Maintain or improve:
  - Security
  - Maintainability
  - Reliability

Any change that degrades these qualities will not be accepted.

---

## Commit Messages

Follow **Conventional Commits**:

feat: add content verification pipeline
fix: handle null metadata safely
refactor: simplify validation logic
refactor(readme): refactor last line
ci: feat: update ci-test.yml

---

## Pull Request Rules

- One logical change per Pull Request
- Clear description of the change and motivation
- CI **must** pass before review
- Approval is required before merge

---

## Security Issues

Security vulnerabilities must **not** be reported via public issues.

Please refer to [security.md](security.md).
