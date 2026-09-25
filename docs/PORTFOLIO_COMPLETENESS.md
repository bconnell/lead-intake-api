# Portfolio completeness

Statuses: DONE, IN PROGRESS, BLOCKED, DEFERRED, or NOT STARTED. Statuses describe current repository evidence. DONE is reserved for the evidence level required by the project; source code alone does not establish database, hosted CI, or publication proof.

## Horizontal requirements

| ID | Requirement | Status | Current evidence / next proof |
|---|---|---|---|
| H01 | Repository identity, Java 21, Maven Wrapper, reproducible build, ignore and line-ending policy, no machine details | IN PROGRESS | Wrapper Maven 3.9.16 compiled the application on OpenJDK 21.0.12.1; the Targeted PowerShell preset passed all four current tests. Packaging and API behavior remain unfinished. |
| H02 | Small, clear architecture | IN PROGRESS | The Boot entry point, lead entity/status, and Spring Data repository are separated into focused packages. Controller, service, and DTO boundaries follow with the API slices. |
| H03 | Complete, coherent REST contract and HTTP semantics | NOT STARTED | No routes yet. |
| H04 | Bean Validation and bounded, safe pagination input | NOT STARTED | No request or query validation yet. |
| H05 | Safe deterministic 400/404/409/500 error handling | NOT STARTED | No API exception mapping yet. |
| H06 | PostgreSQL/JPA persistence, UUIDs, uniqueness, timestamps, enums, transactions | IN PROGRESS | `LeadEntity`, string-backed `LeadStatus`, and `LeadRepository` exist. H2-backed Spring tests verified generated UUID, initial status, timestamps, persistence, and duplicate-email rejection; PostgreSQL and service transactions remain to prove. |
| H07 | Flyway-owned schema; Hibernate validates | IN PROGRESS | Flyway V1 created the initial leads table during the H2 test, and Hibernate validated the entity mapping against it. PostgreSQL execution remains unverified. |
| H08 | Database paging/filtering/counts and deterministic order | NOT STARTED | No persistence queries yet. |
| H09 | Environment-based database configuration without committed secrets | IN PROGRESS | Runtime DB values are required through environment variables. Review local/Compose examples when added. |
| H10 | Meaningful unit tests and appropriately used Mockito | IN PROGRESS | Repository integration tests cover migration application, UUID/status/timestamp persistence, and email uniqueness. Behavior-focused service and HTTP tests remain. |
| H11 | Spring wiring, Flyway, JPA, HTTP, and error-path integration tests | IN PROGRESS | Four Spring tests passed with H2; the new repository tests exercised Flyway V1, Hibernate validation, persistence, and duplicate-email handling. HTTP and error-path proof remains. |
| H12 | Real PostgreSQL integration proof through Testcontainers or equivalent | NOT STARTED | Test dependencies are declared. Add runnable PostgreSQL tests and execute them in Docker-capable CI; do not count H2 or skipped tests. |
| H13 | Fresh migration, JPA/schema agreement, enforced constraints | IN PROGRESS | A fresh H2 database applied Flyway V1, Hibernate validation passed, and the unique email constraint rejected a duplicate. PostgreSQL constraint proof remains. |
| H14 | Small PostgreSQL Compose development path | NOT STARTED | No Compose file yet. |
| H15 | Useful Docker-free ordinary development on the low-memory Windows host | IN PROGRESS | Wrapper build and H2 smoke test passed without Docker. A fresh docker info check failed because the Docker Desktop Linux engine pipe is absent; keep PostgreSQL proof available in Docker-capable CI. |
| H16 | Windows PowerShell 5.1 repository validation workflow with truthful native exit handling | IN PROGRESS | Invoke-Project.ps1 parsed under Windows PowerShell 5.1; Targeted passed; Full clean verify packaged a fresh JAR and returned exit code 2 with PostgreSQL test and Docker blockers stated. Extend checks with the API, Compose, and PostgreSQL tests. |
| H17 | Fresh Spring Boot packaging and artifact identity proof | IN PROGRESS | Full clean verify produced a fresh nonempty lead-intake-api JAR; its manifest main class and version matched the POM. Reverify on the final candidate. |
| H18 | Java 21 GitHub Actions CI with Docker-capable PostgreSQL proof | NOT STARTED | No workflow exists yet. |
| H19 | Secrets, private data, local paths, and unsupported security claims excluded | IN PROGRESS | Initial tracked files and new portable files reviewed for machine-specific content; repeat scans as code and workflows are added. |
| H20 | Accurate README for behavior, API, database, build, test, run, decisions, limits | IN PROGRESS | README now describes the initial lead model, Flyway migration, H2 test scope, and Targeted/Full validation distinction. Document the API and PostgreSQL Compose workflow as those capabilities land. |
| H21 | Exact staging, clean diffs, no generated/unrelated files, coherent local commits | IN PROGRESS | Foundation, validation workflow, and the verified persistence slice are committed locally on `main` with exact-file staging. Publication remains unauthorized. |
| H22 | Publication-ready exact candidate with full available validation and review | NOT STARTED | Depends on implementation, tests, packaging, CI configuration, and final audit. |
| H23 | Explicitly authorized publication and visibility boundary | DEFERRED | Push and visibility changes require separate explicit authorization. Current work remains local. |

## Vertical requirements

| ID | Capability | Status | Required proof still outstanding |
|---|---|---|---|
| V01 | Create lead | NOT STARTED | Validation, normalization policy, UUID/status/timestamps, persistence, 201/Location, duplicate conflict, constraints, tests, PostgreSQL proof, docs. |
| V02 | Retrieve lead | NOT STARTED | UUID lookup, DTO mapping, 200/404, malformed identifier behavior, tests, docs. |
| V03 | List leads | NOT STARTED | Database paging, validated page/size and maximum, stable order, empty result and metadata, tests, docs. |
| V04 | Filter leads | NOT STARTED | All valid statuses, safe invalid-status response, database filtering plus paging, empty result, persistence tests, docs. |
| V05 | Update status | NOT STARTED | Validated request, simple v1 transition behavior, persisted status and updatedAt, 200/404/400, tests, PostgreSQL proof, docs. |
| V06 | Delete lead | NOT STARTED | Existing deletion, 204, missing 404, persistence tests, PostgreSQL proof, docs. |
| V07 | Statistics | NOT STARTED | Total and per-status database counts, zero values, correct totals, tests proving no whole-table materialization, docs. |

## External proof boundary

The current Full run used Java 21 and the Maven Wrapper; its test and fresh package checks passed, then it exited 2 because no PostgreSQL Testcontainers test is configured and Docker Engine is unavailable. Local H2 proof is not PostgreSQL proof. Hosted CI has not run for the local candidate. Do not push or change repository visibility without explicit authorization.
