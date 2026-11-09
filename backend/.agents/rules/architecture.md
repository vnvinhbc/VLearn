# Architecture Rules

Build new code from the verified repository state and these rules. Do not infer an existing feature pattern from documentation alone.

## Package organization

- Use layer-based packaging, not feature-first packaging.
- Place new code under the repository's actual root package.
- Infer the root package from the application class and existing source tree instead of hardcoding an old template package.
- Prefer these packages:
  - `configuration`
  - `controller`
  - `service`
  - `repository`
  - `entity`
  - `dto.request`
  - `dto.response`
  - `dto.common` or another shared package for cross-cutting DTOs such as `ApiResponse`
  - `mapper`
  - `exception`
  - `validator`
  - `constant`

## Layer responsibilities

- Keep controllers thin.
- Put orchestration and business rules in services.
- Keep repositories focused on persistence access.
- Keep entities simple and data-centric.
- Use mappers for mechanical DTO <-> entity conversion.

## Service style

- Do not introduce service interfaces by default.
- Prefer one concrete `*Service` class per business area.
- Inject dependencies with Lombok `@RequiredArgsConstructor`.
- Prefer `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` for controller, service, and config classes.
- If a service handles password hashing or matching, inject `PasswordEncoder` through the constructor like other dependencies.

## Feature scaffold expectation

For a new main resource, usually create:

- entity
- repository
- request DTOs
- response DTOs
- mapper
- service
- controller

Create only the layers the use case needs. Do not scaffold an empty repository, mapper, or controller to satisfy a checklist.

## What to avoid

- Do not move to hexagonal or clean architecture unless requested.
- Do not create feature modules.
- Do not place business logic in controllers.
- Do not let DTOs replace entities in persistence logic.
- Do not copy old root packages, package names, or template-specific structure into a new project.
- Do not introduce a domain model before its API, persistence, and ownership requirements are known.
