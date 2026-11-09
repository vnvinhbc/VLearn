# Project State Rules

## Source of truth

Before planning or editing, inspect `pom.xml`, `src/main`, `src/test`, configuration, and migrations. Repository code wins when this file becomes stale. Update this file in the same change when the foundation changes materially.

Never claim a pattern is current merely because a rule names it. Reuse a class, endpoint, JWT flow, or invalidation strategy only after locating it in the repository.

## Current foundation

- Root package: `com.bvcgroup.vlearn`
- Java 25 and Spring Boot 4.1.2
- Layer-based package target
- Spring MVC, JPA, Validation, Security Resource Server, Actuator
- Lombok and MapStruct annotation processing
- PostgreSQL runtime database and Flyway migrations
- H2 only for lightweight context/web tests
- Testcontainers PostgreSQL for persistence behavior
- Shared API/error foundation under `dto.common` and `exception`
- Shared collection contract uses `PageResponse<T>` with page size defaults in `ApiConstants`
- Flyway auto-configuration uses `spring-boot-starter-flyway`; migration integration tests reuse `PostgresIntegrationTest`

## Not implemented yet

No user, role, permission, authentication, token issuing, refresh-token, or logout domain exists. Do not invent these requirements. When requested, establish explicit decisions for identifiers, ownership, token lifecycle, authority naming, and revocation before implementation.

## Change gate

If a requested pattern requires a dependency or infrastructure that is absent, either add it as an explicit part of the task or report the mismatch. Do not generate uncompilable imports or placeholder production behavior.
