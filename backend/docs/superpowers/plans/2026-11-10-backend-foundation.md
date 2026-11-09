# Backend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align the agent guidance and create a runnable Spring Boot foundation matching it.

**Architecture:** A root guide routes tasks into concise project skills and rules, with repository state as the source of truth. Cross-cutting application code supplies API errors, secure defaults, migrations, environment-backed configuration, and test support without inventing domain features.

**Tech Stack:** Java 25, Spring Boot 4.1.2, Maven, PostgreSQL, Flyway, Spring Security Resource Server, Validation, Lombok, MapStruct, Actuator, JUnit, H2, Testcontainers

**Spec:** `docs/superpowers/specs/2026-11-10-backend-foundation-design.md`

## Global Constraints

- Keep packages under `com.bvcgroup.vlearn` and layer-based.
- Do not implement a user/auth domain in this foundation.
- Production schema changes require Flyway; Hibernate does not create production schema.
- Default configuration contains no production credentials.
- Production behavior changes use test-first development.

## Review Focus

- Missing database environment variables should fail configuration clearly, not silently target an unintended database.
- Unknown validation keys must produce the standard bad-request error.
- Unhandled exceptions must return the generic internal error without leaking exception details.
- Health probes must remain public while all unspecified application endpoints remain denied.
- The context test must run without Docker or a developer PostgreSQL instance.

---

### Task 1: Agent contract

**Files:** `AGENTS.md`, `../../../.agents`, `../../../.agents`

**Interfaces:** Produces task routing and enforceable repository conventions for all later work.

- [ ] Capture the missing entrypoint, invalid skill metadata, and stale reference as failing static checks.
- [ ] Add the root guide, current-state rule, missing operational rules, and corrected task skills.
- [ ] Re-run static checks and verify every skill has valid frontmatter and every linked local file exists.

### Task 2: Build and configuration foundation

**Files:** `pom.xml`, `.mvn/wrapper/maven-wrapper.properties`, `application*.yaml`, `db/migration/V1__baseline.sql`

**Interfaces:** Produces the dependencies and runtime profiles consumed by application code and tests.

- [ ] Record the current wrapper/test failure.
- [ ] Add the required managed dependencies, annotation processors, formatter, profiles, and migration baseline.
- [ ] Run Maven compilation and dependency resolution.

### Task 3: API and error foundation

**Files:** `ApiResponse.java`, `ErrorCode.java`, `AppException.java`, `GlobalExceptionHandler.java`, focused tests

**Interfaces:** Produces `ApiResponse<T>`, `ErrorCode`, and `AppException` for controllers and services.

- [ ] Write tests for the response envelope and business/generic exception mappings; confirm RED.
- [ ] Implement the minimal production types and handler; confirm focused tests GREEN.

### Task 4: Secure default and application verification

**Files:** `SecurityConfig.java`, `VlearnApplicationTests.java`, security tests

**Interfaces:** Produces `PasswordEncoder` and a health-only public HTTP baseline.

- [ ] Write security tests proving public health and denied unspecified endpoints; confirm RED.
- [ ] Implement the minimal filter chain and encoder; confirm GREEN.
- [ ] Run `./mvnw spotless:apply` and `./mvnw verify`.
