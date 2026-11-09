---
name: scaffold-backend-feature
description: Use when adding a new resource-oriented or workflow-oriented backend feature to this Spring Boot repository.
---

# Scaffold Backend Feature

Build only the layers required by a defined use case, using the repository's layer-based architecture.

## Read first

- `../../rules/project-state.md`
- `../../rules/architecture.md`
- `../../rules/api.md`
- `../../rules/error-handling.md`
- `../../rules/mapping-and-dto.md`
- `../../rules/persistence.md`
- `../../rules/testing.md`

Read security, migration, configuration, and observability rules when the feature touches those concerns.

## Workflow

1. Inspect related source, dependencies, configuration, migrations, and tests.
2. Confirm the resource/workflow boundary, API contract, ownership, validation, and persistence needs. State material assumptions; do not invent an absent domain.
3. Write a failing focused test for the first behavior.
4. Add the smallest required vertical path: request/response DTOs, mapper, repository, service, controller, error code, security, and migration only when each is needed.
5. Repeat test-first for failure and authorization behavior.
6. Run focused tests, format, then `./mvnw verify`.

## Completion contract

- Controllers contain transport logic only; services own business orchestration and transactions.
- Controllers never serialize entities.
- Persistence changes have Flyway migrations and database constraints.
- Growing collections are paginated.
- Error and response contracts use the shared foundation.
- The final report lists API, schema, configuration, tests, commands, and unresolved assumptions.
