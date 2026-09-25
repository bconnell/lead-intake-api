# Portfolio completeness

Statuses: DONE, IN PROGRESS, BLOCKED, DEFERRED, or NOT STARTED. Statuses describe current repository evidence. DONE is reserved for the evidence level required by the project; source code alone does not establish database, hosted CI, or publication proof.

## Horizontal requirements

| ID | Requirement | Status | Current evidence / next proof |
|---|---|---|---|
| H01 | Repository identity, Java 21, Maven Wrapper, reproducible build, ignore and line-ending policy, no machine details | IN PROGRESS | Wrapper Maven 3.9.16 compiled the application on OpenJDK 21.0.12.1; the Targeted PowerShell preset passed all seventeen tests under Windows PowerShell 5.1. Reverify packaging and the completed API on the final candidate. |
| H02 | Small, clear architecture | IN PROGRESS | The Boot entry point, lead entity/status, repository, transactional service, controller, request/response DTOs, and shared exception advice are separated into focused packages. Finish review as remaining routes land. |
| H03 | Complete, coherent REST contract and HTTP semantics | IN PROGRESS | POST, GET by UUID, and paged/filterable GET routes return DTOs with tested 201/200/400/404/409 behavior. Status update, delete, statistics, and final contract review remain. |
| H04 | Bean Validation and bounded, safe pagination input | IN PROGRESS | Create requests use Jakarta Validation; page is zero-based and size is limited to 1–100 with safe 400 responses. Additional route and malformed-body cases remain. |
| H05 | Safe deterministic 400/404/409/500 error handling | IN PROGRESS | Problem Details cover request validation, malformed identifiers/query parameters, missing leads, and duplicate-email conflicts. A generic safe 500 path and full error-path review remain. |
| H06 | PostgreSQL/JPA persistence, UUIDs, uniqueness, timestamps, enums, transactions | IN PROGRESS | `LeadEntity`, string-backed `LeadStatus`, and `LeadRepository` exist. H2-backed Spring tests verified generated UUID, initial status, timestamps, persistence, duplicate-email rejection, and service transactions; PostgreSQL proof remains. |
| H07 | Flyway-owned schema; Hibernate validates | IN PROGRESS | Flyway V1 created the leads table and V2 added the status/order index; Hibernate validated both against the JPA mapping in H2. PostgreSQL execution remains unverified. |
| H08 | Database paging/filtering/counts and deterministic order | IN PROGRESS | Spring Data returns status-filtered database pages ordered by `createdAt DESC, id DESC`; H2 API tests verify filter metadata, page boundaries, and UUID tie-breaking. Statistics remain. |
| H09 | Environment-based database configuration without committed secrets | IN PROGRESS | Runtime DB values are required through environment variables. Review local/Compose examples when added. |
| H10 | Meaningful unit tests and appropriately used Mockito | IN PROGRESS | Seventeen Targeted tests pass, covering repository persistence and create/retrieve/list HTTP behavior, validation, ordering, pagination, empty results, and expected errors. Statistics and state-change tests remain. |
| H11 | Spring wiring, Flyway, JPA, HTTP, and error-path integration tests | IN PROGRESS | Seventeen Spring tests passed with H2; they exercised both Flyway migrations, Hibernate validation, persistence, and current HTTP routes/error paths. PostgreSQL and remaining-route proof remain. |
| H12 | Real PostgreSQL integration proof through Testcontainers or equivalent | NOT STARTED | Test dependencies are declared. Add runnable PostgreSQL tests and execute them in Docker-capable CI; do not count H2 or skipped tests. |
| H13 | Fresh migration, JPA/schema agreement, enforced constraints | IN PROGRESS | A fresh H2 database applied Flyway V1 and V2, Hibernate validation passed, and the unique email constraint rejected a duplicate. PostgreSQL constraint proof remains. |
| H14 | Small PostgreSQL Compose development path | NOT STARTED | No Compose file yet. |
| H15 | Useful Docker-free ordinary development on the low-memory Windows host | IN PROGRESS | Wrapper build and H2 smoke test passed without Docker. A fresh docker info check failed because the Docker Desktop Linux engine pipe is absent; keep PostgreSQL proof available in Docker-capable CI. |
| H16 | Windows PowerShell 5.1 repository validation workflow with truthful native exit handling | IN PROGRESS | Invoke-Project.ps1 parsed and Targeted passed all seventeen tests under Windows PowerShell 5.1. Its Maven failure path was corrected for PowerShell 7 and reports native exit codes; rerun Full on the final API/Compose/CI candidate. |
| H17 | Fresh Spring Boot packaging and artifact identity proof | IN PROGRESS | Full clean verify produced a fresh nonempty lead-intake-api JAR; its manifest main class and version matched the POM. Reverify on the final candidate. |
| H18 | Java 21 GitHub Actions CI with Docker-capable PostgreSQL proof | NOT STARTED | No workflow exists yet. |
| H19 | Secrets, private data, local paths, and unsupported security claims excluded | IN PROGRESS | Initial tracked files and new portable files reviewed for machine-specific content; repeat scans as code and workflows are added. |
| H20 | Accurate README for behavior, API, database, build, test, run, decisions, limits | IN PROGRESS | README describes the current create/retrieve/list routes, Flyway-backed persistence, H2 test scope, and Targeted/Full validation distinction. Add the remaining routes and PostgreSQL Compose workflow as they land. |
| H21 | Exact staging, clean diffs, no generated/unrelated files, coherent local commits | IN PROGRESS | Foundation, validation workflow, and the verified persistence slice are committed locally on `main` with exact-file staging. Publication remains unauthorized. |
| H22 | Publication-ready exact candidate with full available validation and review | NOT STARTED | Depends on implementation, tests, packaging, CI configuration, and final audit. |
| H23 | Explicitly authorized publication and visibility boundary | DEFERRED | Push and visibility changes require separate explicit authorization. Current work remains local. |

## Vertical requirements

| ID | Capability | Status | Required proof still outstanding |
|---|---|---|---|
| V01 | Create lead | IN PROGRESS | DTO validation and trimming/lowercase normalization, UUID/status/timestamps, persistence, 201/Location, duplicate 409, and H2 tests are in place; PostgreSQL proof and final docs remain. |
| V02 | Retrieve lead | IN PROGRESS | UUID lookup, DTO mapping, 200/404, malformed identifier 400, and H2 HTTP tests are in place; PostgreSQL proof and final docs remain. |
| V03 | List leads | IN PROGRESS | Database paging, bounded page/size, createdAt/UUID ordering, metadata, an empty page beyond the last result, and H2 API tests are in place; PostgreSQL proof and final docs remain. |
| V04 | Filter leads | IN PROGRESS | Valid status filtering uses a database query alongside paging; invalid status returns safe 400 and H2 API tests cover page metadata. PostgreSQL proof and final docs remain. |
| V05 | Update status | NOT STARTED | Validated request, simple v1 transition behavior, persisted status and updatedAt, 200/404/400, tests, PostgreSQL proof, docs. |
| V06 | Delete lead | NOT STARTED | Existing deletion, 204, missing 404, persistence tests, PostgreSQL proof, docs. |
| V07 | Statistics | NOT STARTED | Total and per-status database counts, zero values, correct totals, tests proving no whole-table materialization, docs. |

## External proof boundary

The latest Full run was on the persistence-only candidate, before the create/retrieve/list API slice. Its H2 test and fresh package checks passed, then it exited 2 because no PostgreSQL integration test was configured and Docker Engine was unavailable. Local H2 proof is not PostgreSQL proof. Hosted CI has not run for the local candidate. Do not push or change repository visibility without explicit authorization.
