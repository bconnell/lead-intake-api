# Validation

This project separates fast application feedback from checks that require a real PostgreSQL server.

## Local validation

Use the committed Maven Wrapper and the repository validation script:

```powershell
.\Invoke-Project.ps1 -Preset Targeted
.\Invoke-Project.ps1 -Preset Full
```

`Targeted` runs the H2-backed application tests and repository checks. It does not prove PostgreSQL compatibility. `Full` runs the available verification and checks the packaged JAR identity. It runs the PostgreSQL Testcontainers tests only when Docker Engine is reachable; otherwise it reports PostgreSQL proof as blocked and exits with code `2` after completing the checks that are available.

## GitHub Actions

The CI workflow uses Java 21 and the Maven Wrapper. `clean verify` runs the H2-backed tests, the `LeadPostgresIT` Failsafe suite against PostgreSQL 17 through Testcontainers, and Spring Boot packaging. A separate Compose smoke check starts only the `db` service, waits for its health check, calls `pg_isready`, and removes the service and its volume even when an earlier step fails. The API is not launched through Compose.

## Reading the results

H2 tests cover application behavior quickly but do not establish PostgreSQL behavior. The Testcontainers suite exercises the API against PostgreSQL; the Compose smoke check verifies that the documented database service starts and becomes ready. Review the GitHub Actions run for the exact commit under consideration. A skipped or blocked PostgreSQL check is not a pass.
