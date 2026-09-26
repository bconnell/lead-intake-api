# Project guidance

## Purpose and scope
This repository is a compact Java 21 Spring Boot Lead Intake API. Keep changes within the agreed lead-intake scope and keep the architecture small: HTTP controllers, request/response DTOs, application services, Spring Data repositories, persistence entities, and focused exception/configuration code where useful.

Do not add authentication, a frontend, additional services, brokers, Kubernetes, cloud deployment, telemetry, analytics, AI, payments, CRM integrations, event sourcing, CQRS, or generic frameworks without an explicit project requirement.

## Backend contract
- PostgreSQL is the production database. Flyway owns schema creation and evolution; Hibernate validates the schema.
- Use UUID identifiers, Instant timestamps, and the five documented lead statuses.
- Keep JPA entities out of the HTTP contract. Validate request DTOs and return stable response DTOs.
- Preserve correct HTTP semantics, safe errors, database-backed pagination/filtering/counts, deterministic ordering, and transactions.
- Avoid loading full tables for pagination or statistics.

## Build and validation
- Java 21 and the committed Maven Wrapper are the supported build path; do not require globally installed Maven.
- The Maven Wrapper is the Windows entry point. Invoke-Project.ps1 with the Targeted preset is the fast local validation path; the Full preset runs all locally available proof and reports unavailable PostgreSQL infrastructure distinctly.
- Testcontainers PostgreSQL tests are real database proof only when they actually execute against PostgreSQL. H2 or skipped tests are not PostgreSQL proof.
- Keep PowerShell scripts compatible with Windows PowerShell 5.1. Capture native exit codes immediately; stderr text alone does not mean failure. Never use WMIC.
- Do not run concurrent Maven builds against the same target directory.

## Repository safety and documentation
- Preserve existing work. Inspect repository status and diffs before committing, and stage only the intended files.
- Keep credentials, personal machine paths, customer data, logs, generated output, and local-only configuration out of version control. Use clearly fictional examples.
- Keep the README and `docs/VALIDATION.md` aligned with current behavior and validation commands.
- Distinguish H2 tests from PostgreSQL proof, and do not describe CI or packaging checks as passing unless they completed for the commit being discussed.
- Use forward-only commits; do not rewrite project history or force-push.
