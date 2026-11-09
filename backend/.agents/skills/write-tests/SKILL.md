---
name: write-tests
description: Use when adding or updating unit, MVC slice, application-context, security, or persistence integration tests in this repository.
---

# Write Backend Tests

Use this skill when adding or updating tests for this codebase or a new project modeled on it.

## Controller tests

- Prefer `@WebMvcTest` for a controller slice.
- Mock the service layer with `@MockitoBean`.
- Import the intended exception and security configuration explicitly.
- Serialize request payloads with Jackson.
- Assert status and key JSON fields such as `code`, `message`, and `result`.
- Use `@SpringBootTest` plus `@AutoConfigureMockMvc` only for behavior that needs full wiring.

## Service tests

- Choose one strategy before writing the class:
  - isolated unit test: instantiate the service directly and mock every collaborator
  - integration test: load Spring wiring and use a real schema or Testcontainers
- Do not mix Spring context plus selective mocked collaborators as the default service-test style.
- Use `@WithMockUser` when service methods depend on Spring Security context.
- If the test needs schema creation, do not keep `ddl-auto=none`.
- Use H2 only for context/web behavior that does not claim PostgreSQL equivalence.
- Use PostgreSQL Testcontainers plus Flyway for SQL, constraints, transactions, migrations, or JPA behavior.

## Naming

- Name test methods as `method_condition_expected`.

## Minimum scenarios

- success path
- validation or bad-request path when applicable
- not-found or business-error path
- authorization path when applicable

## Important caution

Align tests with real dependencies and current behavior rather than blindly copying a nearby test that may exercise a different slice.

## Workflow

1. Name the production change that would make the test fail.
2. Write one focused test and run it to confirm the expected RED result.
3. Implement or update the production behavior minimally.
4. Run the focused test, then the whole `./mvnw verify` suite.
5. Report any skipped integration requirement or environment dependency.
