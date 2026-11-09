# Persistence Rules

## Entity style

- Use simple JPA entities with annotations plus Lombok.
- Prefer data-centric entities over rich domain models.
- Keep entity behavior minimal.

## Repository style

- Use Spring Data JPA repositories extending `JpaRepository`.
- Prefer derived query methods such as `findByUsername` and `existsByUsername`.
- Keep orchestration and cross-repository logic inside services.

## Schema management

- Flyway is mandatory for production schema evolution.
- Never modify an already-applied migration in a shared environment; add a new migration.
- Test configuration must match the chosen schema strategy:
  - migration-driven tests run migrations
  - auto-generated test schema must not use `ddl-auto=none`

## Query complexity

- Do not introduce Specification, Querydsl, custom repository implementations, or `@Query` unless the use case truly requires it.
- Start with derived queries and simple CRUD.

## Transactions and performance

- Put transaction boundaries in services, not controllers.
- Use `@Transactional(readOnly = true)` for meaningful read use cases and regular transactions for multi-step writes.
- Back application-level uniqueness checks with database constraints; consider races rather than relying on `existsBy...` alone.
- Do not return unbounded collections.
- Review fetch plans for N+1 queries and never serialize JPA entities directly from controllers.
- Introduce optimistic or pessimistic locking only for a demonstrated concurrency invariant.

## Relationship modeling

- Many-to-many relationships are acceptable in this style when they match current project patterns.
- Let services manage relationship assignment after mapping.

## Mapping boundary

- Use MapStruct to map request DTOs into entities.
- Ignore relation fields in mappers when relation values depend on business logic.
- Assign roles, permissions, or similar associations in the service layer.
