# Lead Intake API

A compact Java backend portfolio project for accepting and managing leads. The application is being built as a small Spring Boot REST API backed by PostgreSQL, Spring Data JPA, and Flyway.

## Current status

The repository currently contains the Java 21 / Spring Boot foundation and a local test profile. The API slices and full validation workflow are being implemented; see [docs/PORTFOLIO_COMPLETENESS.md](docs/PORTFOLIO_COMPLETENESS.md) for current evidence and remaining work.

## Technology

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper 3.9.16
- Spring MVC, Bean Validation, Spring Data JPA
- PostgreSQL and Flyway

## Build the foundation

Use the Maven Wrapper; a globally installed Maven is not required.

Run `.\mvnw.cmd test` from PowerShell.

The current smoke test starts the Spring application against an in-memory H2 test database in PostgreSQL compatibility mode. This is a fast local signal only; it does not establish PostgreSQL compatibility.

The production runtime requires DB_URL, DB_USERNAME, and DB_PASSWORD. PostgreSQL Compose configuration, API examples, and the final validation commands will be documented as those slices are implemented.
