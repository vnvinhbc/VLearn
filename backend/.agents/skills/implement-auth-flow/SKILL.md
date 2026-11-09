---
name: implement-auth-flow
description: Use when adding or changing authentication, JWT validation or issuing, token refresh, logout, password handling, or authorization behavior.
---

# Implement Auth Flow

Use this skill when asked to add or modify authentication, token, introspection, refresh, logout, or authorization-related behavior.

## Read first

- `../../rules/project-state.md`
- `../../rules/security.md`
- `../../rules/configuration-and-secrets.md`
- `../../rules/api.md`
- `../../rules/error-handling.md`
- `../../rules/testing.md`

## Discovery gate

Locate existing auth classes and flows before reusing them. The foundation does not itself prove that token issuing, refresh, invalidation, roles, or permissions exist.

Before implementing JWT or refresh behavior, make explicit decisions for issuer, audience, algorithm, key source and rotation, lifetimes, clock skew, authority mapping, refresh storage/rotation/reuse detection, revocation, CORS/CSRF posture, and rate limiting. Stop for domain clarification when these choices materially change the public contract.

## Workflow

1. Write tests for success, invalid credentials/token, expiry, unauthorized, and forbidden behavior as applicable.
2. Keep `/auth/...` controllers thin and token/password orchestration in services.
3. Inject the configured `PasswordEncoder`; never instantiate one inline.
4. Update the explicit public endpoint list and keep all other endpoints secure by default.
5. Reuse `ApiResponse`, `ErrorCode`, and `AppException`; keep authentication entry-point output consistent.
6. Never log credentials or tokens and never commit signing material.
7. Run focused security tests, format, then `./mvnw verify`.

The final report must identify the chosen token/security policies, configuration variables, public endpoints, and tests.
