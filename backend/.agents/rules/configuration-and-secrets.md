# Configuration And Secrets Rules

- Production-capable configuration reads credentials and signing material from environment variables or an approved secret store.
- Never commit real passwords, tokens, private keys, or connection strings.
- Development defaults belong in an explicit `local` profile and must match `docker-compose.yml`.
- Tests use an explicit `test` profile. A context or web test may use H2; persistence semantics must use PostgreSQL Testcontainers.
- Add typed `@ConfigurationProperties` for a related group of application settings instead of scattering `@Value` fields.
- Validate required security configuration at startup. Never fall back to a hard-coded JWT key.
- Do not log credentials, authorization headers, reset links, or full tokens.
