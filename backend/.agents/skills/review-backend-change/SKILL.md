---
name: review-backend-change
description: Use when reviewing a backend change for API, security, persistence, migration, configuration, observability, and test risks before merge.
---

# Review Backend Change

Read `../../rules/code-review.md` plus every domain rule touched by the diff.

## Review method

1. Inspect the actual diff, surrounding code, configuration, migrations, and relevant tests.
2. Trace public inputs through controller, service, persistence, and response/error handling.
3. Check authorization, transaction/concurrency behavior, database rollout safety, secret exposure, and operational endpoints.
4. Verify focused tests and `./mvnw verify` when execution is available.
5. Report findings first, ordered by severity. Each finding includes concrete impact, location, and the smallest safe fix.

If there are no findings, say so and list residual risks or untested paths. Keep style-only observations separate from correctness findings.
