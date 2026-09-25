# Lead Intake API

A compact Java backend portfolio project for accepting and managing leads. The application is being built as a small Spring Boot REST API backed by PostgreSQL, Spring Data JPA, and Flyway.

## Current status

The repository contains the Java 21 / Spring Boot foundation, the initial JPA lead model and Flyway schema migration, and a PowerShell validation workflow. HTTP API slices are still being implemented; see [docs/PORTFOLIO_COMPLETENESS.md](docs/PORTFOLIO_COMPLETENESS.md) for current evidence and remaining work.

## Technology

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper 3.9.16
- Spring MVC, Bean Validation, Spring Data JPA
- PostgreSQL and Flyway

## Build the foundation

Use the Maven Wrapper; a globally installed Maven is not required.

Run `.\Invoke-Project.ps1 -Preset Targeted` from PowerShell for the supported fast local validation path, or run `.\mvnw.cmd test` for the test suite directly.

The current tests use an in-memory H2 database in PostgreSQL compatibility mode to apply the Flyway migration, validate the JPA mapping, and check repository persistence and uniqueness. This is a fast local signal only; it does not establish PostgreSQL compatibility. The Full preset reports PostgreSQL/Docker prerequisites separately and exits with code 2 when that proof is unavailable.

The production runtime requires DB_URL, DB_USERNAME, and DB_PASSWORD. PostgreSQL Compose configuration and API examples will be documented as those slices are implemented.
