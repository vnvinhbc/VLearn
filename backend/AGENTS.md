# VLearn Backend Agent Guide

This file is the entrypoint for every agent working in this repository.

## Required reading

Before changing code, read:

1. `.agents`
2. `.agents`
3. `.agents`
4. The rules that match the task

The repository is the source of truth. Inspect `pom.xml`, existing source, configuration, and tests before relying on a documented pattern. A class or flow named in a rule is reusable only when it actually exists.

## Task routing

- New resource or business feature: `.agents`
- Authentication, JWT, refresh, logout, or authorization: `.agents`
- Tests: `.agents`
- Database changes: `.agents`
- Review: `.agents`

Read every rule linked by the selected skill. Skills do not grant permission to invent missing domain requirements.

## Required workflow

1. Inspect the current implementation and dependency set.
2. State assumptions that affect API, persistence, or security behavior.
3. Write or update a failing test before production behavior changes.
4. Implement the smallest complete change.
5. Run focused tests, then `./mvnw verify`.
6. Report migrations, configuration changes, verification commands, and unresolved risks.

Never commit secrets, bypass authorization, silently change an API contract, or modify a production schema without a Flyway migration.
