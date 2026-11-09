# Testing Rules

## Test types in this codebase

- Controller tests use Spring Boot test support with `MockMvc`.
- Service tests must choose one clear strategy per class:
  - isolated unit test with all dependencies mocked
  - integration test with real persistence wiring, real schema, or Testcontainers
- Do not mix a half-integration Spring context with selectively mocked service dependencies unless there is a deliberate slice-test reason.

## Controller test style

- Prefer:
  - `@WebMvcTest` for focused controller behavior
  - `@MockitoBean` for the service under test
  - an explicit imported security configuration or deliberate filter setup
- Use `@SpringBootTest` plus `@AutoConfigureMockMvc` only when the behavior depends on full application wiring.
- Assert both HTTP status and response payload fields.

## Service test style

- Unit tests should instantiate the service directly and mock every dependency the method touches.
- Integration tests should bootstrap the required Spring context and use a real schema strategy.
- If persistence behavior matters, prefer integration tests backed by a real database setup or Testcontainers instead of over-mocking repository behavior.
- If tests still need Hibernate to create schema automatically, do not leave `ddl-auto=none`; use a fitting test value such as `create`, `create-drop`, or run migrations before the test.
- Context and web-only tests may use the `test` profile with H2.
- Tests for PostgreSQL SQL, constraints, migrations, transactions, or JPA behavior must use PostgreSQL Testcontainers and run Flyway.

## Naming

- Use the pattern `method_condition_expected`.
- Examples:
  - `createUser_validRequest_success`
  - `createUser_usernameInvalid_fail`
  - `getMyInfo_userNotFound_error`

## Practical cautions

- Prefer writing tests that align with actual service dependencies rather than copying a nearby test without checking its strategy.
- Be explicit in the test class name, annotations, and setup about whether the class is unit or integration focused.
- Run the focused test first and `./mvnw verify` before completion.
