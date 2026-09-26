# Portfolio completeness

Statuses: DONE, IN PROGRESS, BLOCKED, DEFERRED, or NOT STARTED. Statuses describe current repository evidence. DONE is reserved for the evidence level required by the project; source code alone does not establish database, hosted CI, or publication proof.

## Horizontal requirements

| ID | Requirement | Status | Current evidence / next proof |
|---|---|---|---|
| H01 | Repository identity, Java 21, Maven Wrapper, reproducible build, ignore and line-ending policy, no machine details | DONE | Windows PowerShell 5.1 Targeted passed all 39 tests on OpenJDK 21.0.12.1. A no-hardlink local clone of `120e84c` passed Full's wrapper/toolchain/hygiene preflight, all 39 tests, and artifact checks. Git tracks `mvnw` as executable with LF endings. |
| H02 | Small, clear architecture | DONE | The Boot entry point, lead entity/status, repository, transactional service, controller, request/response DTOs, and shared exception advice remain separated into focused packages. No speculative layers were added. |
| H03 | Complete, coherent REST contract and HTTP semantics | DONE | POST, GET by UUID, paged/filterable GET, PATCH status, DELETE, and GET statistics return DTOs with tested 201/200/204/400/404/409 behavior. H2 HTTP tests cover all routes; the PostgreSQL suite exercises the same API boundary. |
| H04 | Bean Validation and bounded, safe pagination input | DONE | Create and status requests use Jakarta Validation; page is zero-based and size is limited to 1–100. Invalid status, malformed identifiers/body, invalid page/size, and null status return safe 400 responses. |
| H05 | Safe deterministic 400/404/409/500 error handling | DONE | Problem Details cover validation, malformed requests/identifiers/query parameters, missing leads, duplicate-email conflicts, unmapped paths, and a generic 500 that hides exception detail. |
| H06 | PostgreSQL/JPA persistence, UUIDs, uniqueness, timestamps, enums, transactions | DONE | GitHub Actions run [36240646499](https://github.com/bconnell/lead-intake-api/actions/runs/36240646499) passed all 3 `LeadPostgresIT` tests on PostgreSQL 17 with zero failures, errors, or skips, covering persisted lead operations and uniqueness. |
| H07 | Flyway-owned schema; Hibernate validates | DONE | The same hosted PostgreSQL run applied Flyway V1/V2 and started the application with Hibernate schema validation; all 3 PostgreSQL integration tests passed. |
| H08 | Database paging/filtering/counts and deterministic order | DONE | `LeadPostgresIT` passed in hosted CI, including database paging, status filtering, statistics, and delete behavior; the 39 H2/unit tests also passed. Ordering remains deterministic by `createdAt DESC, id DESC`. |
| H09 | Environment-based database configuration without committed secrets | DONE | Runtime settings require `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Compose reads a local ignored `.env`; the committed `.env.example` contains only a replace-before-use placeholder and the configurable host port. |
| H10 | Meaningful unit tests and appropriately used Mockito | DONE | The 39-test Targeted suite includes a focused Mockito unit test for service count aggregation, plus validation, normalization, status changes, repository persistence/uniqueness, paging/filtering, statistics, deletes, and safe errors. A repository spy verifies statistics do not call either `findAll` overload. |
| H11 | Spring wiring, Flyway, JPA, HTTP, and error-path integration tests | DONE | All 38 Spring/H2 tests pass and exercise both migrations, Hibernate validation, JPA persistence, HTTP routes, and important error paths. A distinct real-PostgreSQL suite is configured under Failsafe. |
| H12 | Real PostgreSQL integration proof through Testcontainers or equivalent | DONE | Hosted CI run [36240646499](https://github.com/bconnell/lead-intake-api/actions/runs/36240646499) ran `LeadPostgresIT` against PostgreSQL 17: 3 tests, zero failures, errors, or skips. |
| H13 | Fresh migration, JPA/schema agreement, enforced constraints | DONE | Hosted PostgreSQL integration passed Flyway migration, Hibernate validation, and unique-email constraint coverage; all 3 `LeadPostgresIT` tests passed. |
| H14 | Small PostgreSQL Compose development path | IN PROGRESS | `compose.yaml` defines PostgreSQL 17 Alpine, loopback-only binding, a named volume, readiness check, and configurable port. `docker compose config --quiet` passes, but the Compose service itself has not been started because the local Docker Engine is unavailable. Hosted PostgreSQL proof used Testcontainers, not Compose. |
| H15 | Useful Docker-free ordinary development on the low-memory Windows host | DONE | Targeted and Full run without Docker in both the repository and a clean clone; all 39 H2/unit tests and packaging pass. `docker info` confirms the Docker Desktop Linux Engine pipe is unavailable, so PostgreSQL proof stays a separate gate. |
| H16 | Windows PowerShell 5.1 repository validation workflow with truthful native exit handling | DONE | Windows PowerShell 5.1 parsed the workflow and Targeted passed all 39 tests with exit 0. Full reports its Docker proof blocker with exit 2 after successful available validation. |
| H17 | Fresh Spring Boot packaging and artifact identity proof | DONE | Full on the Compose-complete candidate produced a fresh nonempty `lead-intake-api` JAR whose manifest main class and version match the POM. |
| H18 | Java 21 GitHub Actions CI with Docker-capable PostgreSQL proof | DONE | GitHub Actions run [36240646499](https://github.com/bconnell/lead-intake-api/actions/runs/36240646499) passed on commit `372b085536375923094b986a43803b8021f8de21`: 39 H2/unit tests, 3 PostgreSQL Testcontainers tests, and Maven packaging all succeeded. |
| H19 | Secrets, private data, local paths, and unsupported security claims excluded | DONE | Full and final Targeted preflights scanned tracked and untracked text for common credential patterns and personal paths after adding Compose and `.env.example`; README and workflow were manually reviewed. No real lead data or Gold Standard material is included. |
| H20 | Accurate README for behavior, API, database, build, test, run, decisions, limits | DONE | README documents all routes and examples, safe errors, paging/filtering, Flyway/JPA ownership, Compose startup and host-run API, H2 versus PostgreSQL evidence, Testcontainers/Failsafe, CI, Docker limitations, and current proof boundaries. |
| H21 | Exact staging, clean diffs, no generated/unrelated files, coherent local commits | DONE | The Compose development slice was committed on `main` as `387c004b76b11be435835f1a75711420ed42c526` after exact five-file staging and staged-diff checks. The post-commit worktree was clean; generated files remain ignored. |
| H22 | Publication-ready exact candidate with full available validation and review | DONE | Local Targeted and Full available checks passed; the published candidate passed hosted H2/unit, PostgreSQL integration, and packaging checks. The only outstanding runtime proof is starting the Compose service itself. |
| H23 | Explicitly authorized publication and visibility boundary | DONE | The candidate was pushed to private `origin/main` as a fast-forward, and the remote ref matched local `main` after publication. Repository visibility remains private. |

## Vertical requirements

| ID | Capability | Status | Required proof still outstanding |
|---|---|---|---|
| V01 | Create lead | DONE | H2 HTTP tests and hosted `LeadPostgresIT` cover validation, normalization, UUID/status/timestamps, persistence, 201/Location, and duplicate-email 409. |
| V02 | Retrieve lead | DONE | H2 and hosted PostgreSQL tests cover UUID lookup, DTO mapping, 200/404, and malformed identifier 400; README documents the route. |
| V03 | List leads | DONE | H2 and hosted PostgreSQL tests cover bounded paging, deterministic order, metadata, and empty pages; README documents the route. |
| V04 | Filter leads | DONE | H2 and hosted PostgreSQL tests cover status filters and invalid-status 400 behavior; README documents the query parameter. |
| V05 | Update status | DONE | H2 and hosted PostgreSQL tests cover validated updates across statuses, persistence, refreshed `updatedAt`, and 200/404/400 behavior. |
| V06 | Delete lead | DONE | H2 and hosted PostgreSQL tests cover deletion with 204 and missing IDs with 404. |
| V07 | Statistics | DONE | H2 and hosted PostgreSQL tests cover total and per-status database counts, including zero values; the service does not materialize all leads. |

## External proof boundary

Windows PowerShell 5.1 Targeted passed all 39 tests on the final local candidate, including Compose syntax and privacy/path scanning. Local Full passed all 39 H2/unit tests and packaged a fresh artifact with matching main-class/version identity; it compiled but skipped PostgreSQL tests because the local Docker Engine was unavailable. Hosted CI run [36240646499](https://github.com/bconnell/lead-intake-api/actions/runs/36240646499) passed on published commit `372b085536375923094b986a43803b8021f8de21`: 39 H2/unit tests, 3 `LeadPostgresIT` tests against PostgreSQL 17, and packaging all completed with zero failures, errors, or skips. The Compose file passed `config --quiet`, but its service has not been started. The repository remains private and the verified remote branch is `main`.
