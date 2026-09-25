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
- Preserve all existing Git work. Inspect status and diffs before staging or committing; stage exact intended files.
- Never reset, clean, stash as disposal, amend, rewrite history, force-push, push, publish, or change repository visibility without explicit authorization.
- Do not commit secrets, real lead data, personal machine paths, private prompts, local logs, generated output, or the external Gold Standard document.
- Keep the completion map and README aligned with current behavior and current validation evidence.
- Do not claim hosted CI, PostgreSQL, packaging, or publication proof unless that exact proof has completed on the exact candidate.
