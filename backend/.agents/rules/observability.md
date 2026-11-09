# Observability Rules

- Actuator health is the only public management endpoint by default.
- Keep management exposure explicit; do not expose environment, beans, config properties, heap dumps, or metrics publicly.
- Use structured, parameterized logging. Do not use `System.out` or print stack traces directly.
- Include a request/correlation identifier when distributed tracing or cross-service debugging becomes necessary.
- Log unexpected failures server-side while returning a generic error message to clients.
- Never log secrets, passwords, authorization headers, or full JWT/refresh tokens.
- Add metrics for meaningful business or operational behavior, not arbitrary method counts.
