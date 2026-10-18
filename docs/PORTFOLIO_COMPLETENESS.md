# Portfolio completeness

Reviewed on 2026-10-17 against the validated code revision `e26ccc2812e712ebff17b947314aed8a8f25be5a` on the integrity-classification pull request.

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

The hosted validation of the inspected code revision is [CI run 39081372196](https://github.com/bconnell/lead-intake-api/actions/runs/39081372196).

- `clean verify` completed with 40 application tests and 4 PostgreSQL Testcontainers integration tests. There were no failures, errors, or skipped tests.
- Spring Boot packaging completed successfully.
- The Compose PostgreSQL service became healthy, passed the configured `pg_isready` check, and was removed with its container, network, and volume.
- The run checked out the exact commit recorded above. Its results apply to that revision.

## Perfection Matrix

| Dimension | Current state | Evidence or limit |
|---|---|---|
| API behavior | Available (Unhardened) | HTTP tests cover validation, creation, retrieval, pagination, filtering, status updates, deletion, statistics, and safe error responses |
| Persistence and recovery | Available (Unhardened) | Flyway owns two migrations; Hibernate validates the schema; PostgreSQL integration tests exercise persistence and constraints |
| Failure handling | Available (Unhardened) | HTTP tests cover safe not-found, duplicate email, and unrelated database integrity failure responses against H2 and PostgreSQL; full operational monitoring and recovery remain outside scope |
| Performance and resource use | Available (Unhardened) | Database-backed paging, a maximum page size of 100, and count queries avoid returning whole tables; no high-volume performance budget has been measured |
| Packaging and reproducibility | Available (Unhardened) | The Maven Wrapper and fresh Spring Boot packaging are exercised in hosted CI |
| Accessibility and visual quality | Not applicable | This API has no visual interface |
| Production operations | Not in scope | No deployment, authentication, monitoring, backup, or operational readiness claim is made |

## Database integrity error classification

The API distinguishes the named `uk_leads_email` uniqueness violation from other database integrity failures. Only that recognized constraint maps to `409 Conflict` with the existing duplicate-email response. An unrelated or unrecognized violation maps to the same generic `500 Internal Server Error` response used for unexpected application failures, with no constraint or driver details in the HTTP body.

Regression coverage includes end-to-end duplicate-email checks and HTTP-boundary checks using actual status-check failures from H2 and PostgreSQL. The hosted run above exercised the PostgreSQL cases through Testcontainers.

**Validation status:** Hosted `clean verify` passed for the reviewed code revision, with no skipped tests, and the PostgreSQL Compose readiness and cleanup stages succeeded. Evidence is scoped to that commit; later code changes require fresh validation. This does not establish production deployment or security readiness.

## Explicit scope limits

The repository guidance keeps this project focused on the lead-intake backend. Authentication, a frontend, additional services, brokers, Kubernetes, cloud deployment, telemetry, analytics, AI, payments, CRM integrations, event sourcing, CQRS, and generic frameworks are outside the declared scope unless separately requested.
