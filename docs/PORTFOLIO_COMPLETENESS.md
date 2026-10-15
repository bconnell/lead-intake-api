# Portfolio completeness

Reviewed on 2026-10-15 against `main` at `6679d359adf5958d98e55f5f319a649d3ccdf235`.

**Maturity:** Available (Unhardened) for the API and database scope listed here. This describes the implemented portfolio contract. It is not a production deployment or security certification.

## Declared scope

A compact Java 21 and Spring Boot REST API backed by PostgreSQL, Spring Data JPA, and Flyway. It accepts and manages leads through these routes:

| Route | Supported behavior |
|---|---|
| `POST /api/leads` | Validates and normalizes a lead, creates it, and returns `201 Created` with a `Location` header |
| `GET /api/leads/{id}` | Retrieves one lead or returns `404` |
| `GET /api/leads` | Lists leads with database pagination and optional status filtering |
| `PATCH /api/leads/{id}/status` | Sets one of the supported statuses |
| `DELETE /api/leads/{id}` | Deletes a lead or returns `404`; successful deletion returns `204` |
| `GET /api/leads/stats` | Returns total and per-status counts |

The supported statuses are `NEW`, `CONTACTED`, `QUALIFIED`, `CLOSED`, and `REJECTED`. List page size is limited to 1 through 100. Ordering is deterministic by creation time descending and UUID descending. Request validation and malformed requests use safe Problem Details responses. Database uniqueness protects normalized email addresses.

## Evidence for the inspected revision

The latest recorded hosted run for the inspected `main` revision is [CI run 36242743644](https://github.com/bconnell/lead-intake-api/actions/runs/36242743644).

- `clean verify` completed with 39 application tests and 3 PostgreSQL Testcontainers integration tests. There were no failures, errors, or skipped tests.
- Spring Boot packaging completed successfully.
- The Compose PostgreSQL service became healthy, passed the configured `pg_isready` check, and was removed with its container, network, and volume.
- The run checked out the exact commit recorded above. Its results apply to that revision.

## Perfection Matrix

| Dimension | Current state | Evidence or limit |
|---|---|---|
| API behavior | Available (Unhardened) | HTTP tests cover validation, creation, retrieval, pagination, filtering, status updates, deletion, statistics, and safe error responses |
| Persistence and recovery | Available (Unhardened) | Flyway owns two migrations; Hibernate validates the schema; PostgreSQL integration tests exercise persistence and constraints |
| Failure handling | In Progress | Expected not-found and duplicate-email responses are covered. One constraint-classification defect is listed below |
| Performance and resource use | Available (Unhardened) | Database-backed paging, a maximum page size of 100, and count queries avoid returning whole tables; no high-volume performance budget has been measured |
| Packaging and reproducibility | Available (Unhardened) | The Maven Wrapper and fresh Spring Boot packaging are exercised in hosted CI |
| Accessibility and visual quality | Not applicable | This API has no visual interface |
| Production operations | Not in scope | No deployment, authentication, monitoring, backup, or operational readiness claim is made |

## Remaining engineering finding

`ApiExceptionHandler.handleConflict` currently converts every `DataIntegrityViolationException` into a `409 Conflict` saying the email already exists. The schema also has required-field and status-check constraints. If one of those constraints fails because of a future code defect or an unexpected persistence condition, the response will incorrectly blame the user's email.

The next code loop should distinguish the named email uniqueness constraint from other integrity failures, retain the current duplicate-email `409`, return a generic safe server error for unexpected constraint failures, and add regression coverage against both the H2 application path and PostgreSQL Testcontainers path. Do not expose database constraint names or driver messages in HTTP responses.

## Explicit scope limits

The repository guidance keeps this project focused on the lead-intake backend. Authentication, a frontend, additional services, brokers, Kubernetes, cloud deployment, telemetry, analytics, AI, payments, CRM integrations, event sourcing, CQRS, and generic frameworks are outside the declared scope unless separately requested.
