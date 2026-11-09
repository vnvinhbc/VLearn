# Backend Foundation Design

## Intent

Turn the existing Spring Boot scaffold and `.agent` guidance into one consistent starting point for VLearn backend development. Future agents must follow repository reality instead of assuming an already implemented identity service.

## Goals

- Give agents a discoverable root instruction file and valid, task-routed skills.
- Separate current repository state from target architecture.
- Add the libraries already required by the rules: validation, security/resource server, Lombok, MapStruct, Flyway, observability, and testing support.
- Establish the shared API response and exception model.
- Make local and test configuration explicit and keep production secrets environment-backed.
- Start schema history without inventing domain tables.
- Keep the application secure by default until an authentication flow is implemented.

## Non-goals

- Implement users, roles, login, JWT issuing, refresh tokens, or other domain behavior.
- Design the final authorization model.
- Create domain entities or repositories before their requirements exist.
- Publish, deploy, or change external infrastructure.

## Constraints

- Spring Boot 4.1.2, Java 25, Maven, PostgreSQL 16.
- Layer-based packages under `com.bvcgroup.vlearn`.
- Flyway owns production schema evolution; Hibernate validates it.
- API errors use `ApiResponse`, `ErrorCode`, `AppException`, and `GlobalExceptionHandler`.
- Tests remain runnable without a developer PostgreSQL instance; persistence-specific tests use PostgreSQL Testcontainers.

## Approaches considered

1. Document-only alignment: cheapest, but generated code would still fail because the stack is absent.
2. Build the full identity/auth module now: matches many old rules, but invents unrequested domain and security decisions.
3. Foundation-first alignment: add the cross-cutting stack and contracts, while keeping domain and token behavior out. This is the selected approach because it makes the documented starting point executable without premature business design.

## Design

`AGENTS.md` is the routing entrypoint. `../../../.agents` forces agents to inspect the repository before applying a pattern. Skills have standard YAML frontmatter, link to the exact rules they need, and end with verification/reporting requirements.

The application receives a generic response envelope and centralized exception handling. Security exposes a password encoder and a filter chain that permits health probes and denies all other traffic until a real authentication mechanism is deliberately configured. Flyway records an empty baseline migration; no speculative tables are introduced.

Default configuration requires environment-provided database credentials. The `local` profile contains Docker Compose development defaults. The `test` profile uses H2 only for context and web tests; any test asserting PostgreSQL/JPA behavior must use Testcontainers and migrations.
