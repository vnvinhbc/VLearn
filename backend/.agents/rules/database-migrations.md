# Database Migration Rules

- Every production schema change requires a Flyway migration in `src/main/resources/db/migration`.
- Use versioned names such as `V2__create_users.sql`; never edit a migration already applied outside a disposable local database.
- Hibernate uses `ddl-auto: validate` outside lightweight tests. It does not own production schema creation.
- Define database constraints for invariants: primary keys, foreign keys, uniqueness, nullability, and checks where appropriate.
- Consider indexes for foreign keys and measured query paths; do not add speculative indexes.
- Separate risky backfills from destructive cleanup. Prefer expand/migrate/contract changes when deployments may overlap.
- Document irreversible or data-destructive operations in the change summary.
- Integration tests that verify SQL, constraints, locking, or JPA behavior run the real Flyway migrations against PostgreSQL Testcontainers.
