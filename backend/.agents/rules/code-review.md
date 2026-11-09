# Backend Review Rules

Review behavior and risk, not only formatting.

Check:

- API compatibility, validation, status codes, pagination, and idempotency where relevant
- authorization at the service boundary and secure defaults
- transaction boundaries, race conditions, uniqueness, locking, and retry behavior
- JPA fetch behavior, N+1 queries, unbounded reads, and entity serialization leaks
- migrations, constraints, indexes, rollout safety, and data backfills
- error mappings without sensitive internal details
- configuration/secrets and management endpoint exposure
- focused tests plus the full `./mvnw verify` result

Report each finding with severity, concrete impact, file/location, and the smallest safe correction. Do not report preferences as correctness defects.
