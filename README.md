# Lead Intake API

[![CI](https://github.com/bconnell/lead-intake-api/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/bconnell/lead-intake-api/actions/workflows/ci.yml?query=branch%3Amain)

A compact Java 21 and Spring Boot 4 REST API for lead intake, backed by PostgreSQL, Spring Data JPA, and Flyway.

The API supports lead creation, retrieval, pagination and filtering, status updates, deletion, and statistics. CI runs the H2-backed test suite and real PostgreSQL Testcontainers integration tests, then starts the Compose database service and verifies its health and readiness. See [validation guidance](docs/VALIDATION.md) for what each check proves.

## Technology and structure

- Java 21, Spring Boot 4, and Maven Wrapper
- Spring MVC and Jakarta Bean Validation
- Controllers and request/response DTOs define the HTTP boundary
- Services own transactions and lead operations
- Spring Data repositories provide persistence, paging, filtering, and count queries
- JPA entities map to the Flyway-owned schema; Hibernate validates the schema at startup

## API

| Method and path | Behavior |
|---|---|
| `POST /api/leads` | Create a lead; returns `201 Created` and `Location` |
| `GET /api/leads/{id}` | Retrieve a lead by UUID |
| `GET /api/leads` | List leads with database paging and optional status filtering |
| `PATCH /api/leads/{id}/status` | Set one of the five supported statuses |
| `DELETE /api/leads/{id}` | Delete a lead; returns `204 No Content` |
| `GET /api/leads/stats` | Return total and per-status counts |

Create request:

```json
{
  "name": "Ada Lovelace",
  "email": "ada@example.test",
  "phone": "+1-555-0100"
}
```

Names, emails, and phone numbers are trimmed; emails are lowercased before storage. Name and email are required, email must be valid, and phone is optional. Email uniqueness is enforced by the database.

Successful create, retrieve, and status-update responses use the same DTO shape:

```json
{
  "id": "8e03978e-40d5-43e8-bc93-6894a57f9324",
  "name": "Ada Lovelace",
  "email": "ada@example.test",
  "phone": "+1-555-0100",
  "status": "NEW",
  "createdAt": "2026-01-15T12:00:00Z",
  "updatedAt": "2026-01-15T12:00:00Z"
}
```

The supported statuses are `NEW`, `CONTACTED`, `QUALIFIED`, `CLOSED`, and `REJECTED`. For example:

```http
PATCH /api/leads/8e03978e-40d5-43e8-bc93-6894a57f9324/status
Content-Type: application/json

{"status":"QUALIFIED"}
```

List requests accept `page` (default `0`), `size` (default `20`, range `1–100`), and optional `status`:

```http
GET /api/leads?status=QUALIFIED&page=0&size=20
```

The response contains `content`, `page`, `size`, `totalElements`, and `totalPages`. Results are ordered by creation time descending, then UUID descending.

Statistics response:

```json
{
  "total": 12,
  "new": 4,
  "contacted": 3,
  "qualified": 2,
  "closed": 2,
  "rejected": 1
}
```

Validation and malformed requests return safe `400` Problem Details. A missing lead returns `404`, a duplicate email returns `409`, deletion returns an empty `204`, and unexpected failures return a generic `500` response without exception details.

## Database and configuration

PostgreSQL is the production database. Flyway owns schema creation and evolution; `spring.jpa.hibernate.ddl-auto` is set to `validate`. Migrations are in `src/main/resources/db/migration`.

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for the runtime database connection. Do not commit real credentials. The optional Compose setup starts only PostgreSQL; run the API from the host:

```powershell
Copy-Item .env.example .env
# Edit .env and set POSTGRES_PASSWORD to a local development password.
docker compose up --detach --wait db
```

Compose binds PostgreSQL to `127.0.0.1`, creates the `lead_intake` database/user, persists data in the `postgres_data` volume, and waits for its health check. Set `POSTGRES_PORT` in `.env` to change the host port.

To run against a PostgreSQL database already available on `localhost:5432`, set local environment values and start Spring Boot:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/lead_intake'
$env:DB_USERNAME = 'lead_intake'
$env:DB_PASSWORD = '<same value as POSTGRES_PASSWORD in .env>'
.\mvnw.cmd spring-boot:run
```

When finished, stop the database with `docker compose stop db`; its named volume remains in place for the next start.

## Build and validation

Use the Maven Wrapper; a globally installed Maven is not required.

```powershell
.\Invoke-Project.ps1 -Preset Targeted
.\Invoke-Project.ps1 -Preset Full
.\mvnw.cmd test
```

The test profile uses an in-memory H2 database to exercise Spring wiring, Flyway, Hibernate validation, persistence constraints, and HTTP behavior. H2 is not PostgreSQL compatibility proof. The Full preset also checks packaging and the real PostgreSQL integration gate; if Docker or that proof path is unavailable, it reports the blocker and exits `2` rather than treating skipped tests as a pass.

`mvn verify` runs the `LeadPostgresIT` suite through Maven Failsafe. It starts PostgreSQL 17 with Testcontainers, then exercises Flyway, Hibernate validation, HTTP persistence, uniqueness, filtering, paging, status updates, statistics, and deletion. GitHub Actions runs the Maven Wrapper `clean verify` path on Java 21 and separately starts only the Compose database service, waits for it to become healthy, checks `pg_isready`, and removes the service and its volume. The API itself is run from the host; it is not started through Compose. Ordinary Java development and the Targeted preset do not require Docker Desktop.
