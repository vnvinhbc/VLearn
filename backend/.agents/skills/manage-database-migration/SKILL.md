---
name: manage-database-migration
description: Use when creating or reviewing a Flyway migration, JPA schema change, database constraint, index, or data backfill.
---

# Manage Database Migration

Read `../../rules/database-migrations.md`, `../../rules/persistence.md`, and `../../rules/testing.md`.

## Workflow

1. Inspect all existing migrations and the deployed-version assumptions supplied by the task.
2. Choose the next version without editing previously applied migrations.
3. Define constraints and indexes from actual invariants and query paths.
4. For risky changes, design expand/migrate/contract steps and make destructive behavior explicit.
5. Write a PostgreSQL Testcontainers integration test that runs Flyway and proves the required schema or data behavior.
6. Run the focused integration test and `./mvnw verify`.

Report migration name, forward behavior, rollout/locking risk, reversibility, and verification evidence.
