---
name: style-review
description: Use when checking Java or Spring code specifically for compliance with this repository's package, layering, DTO, mapping, and naming conventions.
---

# Review Project Style

Use this skill when reviewing whether code matches the style defined in this repository.

## Review dimensions

- package placement follows the expected layers
- controller remains thin
- service owns orchestration
- repository stays simple
- DTOs and mapper are separated from entity
- `ApiResponse` lives in a shared/common place or `dto.response`, not `dto.request`
- create/delete endpoints use sensible REST statuses such as `201` and `204`
- password-related code injects `PasswordEncoder` consistently
- errors use `ErrorCode` and `ApiResponse`
- generic exception handling stays aligned with `ErrorCode` status mapping
- validation sits on request DTOs
- security rules are explicit and service-aware
- service tests clearly choose unit or integration strategy
- test schema setup matches the selected test strategy
- production readiness considers migration tooling for schema changes
- every referenced pattern and dependency actually exists in the repository

## Common deviations to flag

- feature-first package structure
- unnecessary service interfaces
- controller-level business logic
- manual mapping duplicated across methods
- ad-hoc JSON error payloads
- `ApiResponse` misplaced under request DTO packages
- create/delete still flattened to `200 OK` without intent
- inline `new BCryptPasswordEncoder()` or equivalent
- service tests mixing Spring context with random mocks
- test profile set to `ddl-auto=none` while tests still expect schema generation
- custom query infrastructure introduced too early
- rich domain methods added into otherwise anemic entities
- inconsistent endpoint naming or response wrapping

## Output style

When reviewing, report:

1. concrete mismatches with the local style
2. risk level or likely maintenance cost
3. the minimal change needed to realign with the project pattern

Do not treat a personal preference as a violation. For security, persistence, rollout, or correctness review, use `../review-backend-change/SKILL.md`.
