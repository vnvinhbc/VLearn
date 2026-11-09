# VLearn Backend

Spring Boot backend foundation for VLearn.

## Local development

Requirements: Java 25 and Docker.

```bash
docker compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The API context path is `/vlearn`. The public health probe is `GET /vlearn/actuator/health`. All other routes are denied until an authentication flow explicitly opens or secures them.

## Verification

```bash
./mvnw spotless:apply
./mvnw verify
```

Lightweight context and MVC tests use the `test` profile with H2. Tests that assert PostgreSQL, Flyway, constraint, transaction, or JPA behavior must use PostgreSQL Testcontainers.

## Runtime configuration

Without the `local` or `test` profile, provide:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional settings include `SERVER_PORT` and `JPA_SHOW_SQL` in the local profile. Do not commit real credentials or JWT key material.

The `staging` profile contains only safe operational overrides. Staging and production still require `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from the environment; future JWT and R2 credentials must follow the same rule.

## API path

The servlet context path remains `/vlearn`. Controllers use resource paths directly without an additional `/api` prefix, so an authentication controller mapped to `/auth/login` is served at `/vlearn/auth/login`.

Agent instructions start at [AGENTS.md](AGENTS.md).
