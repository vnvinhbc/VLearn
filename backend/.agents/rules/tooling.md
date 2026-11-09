# Tooling Rules

## Stack

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security Resource Server
- Validation
- Actuator
- Lombok
- MapStruct
- Flyway
- PostgreSQL Testcontainers
- Maven

## Build and formatting

- Keep code compatible with the existing Maven build.
- Preserve Lombok and MapStruct usage patterns already configured in `pom.xml`.
- Format with Spotless/Palantir Java Format. `./mvnw verify` enforces the check.
- Use `./mvnw spotless:apply` to format and `./mvnw verify` as the completion gate.

## Configuration style

- Prefer small focused config classes.
- Keep application configuration in `application.yaml` with profile-specific overrides only where needed.
- Continue using environment-variable-backed datasource configuration if adding new deployable settings.

## Development posture

- Favor fast-moving, pragmatic defaults consistent with this project.
- For production-oriented services, stricter REST semantics and schema migration tooling are sensible defaults, not exotic extras.
- Flyway is the selected migration tool. Do not introduce Liquibase alongside it.
