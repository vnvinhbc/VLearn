# Security Rules

## Overall approach

- Keep Spring Security close to the authentication business flow.
- Use both HTTP security rules and method-level security.
- Until an authentication mechanism exists, keep application endpoints denied by default and expose only explicit operational probes.

## Configuration style

- Split security-related configuration into focused classes instead of one large config file.
- Introduce focused components such as the following only when the authentication design requires them:
  - `SecurityConfig`
  - `CustomJwtDecoder`
  - `JwtAuthenticationEntryPoint`
  - `ApplicationInitConfig`
- Expose `PasswordEncoder` as a bean and inject it consistently wherever password hashing or matching is needed.

## Access control

- Keep public endpoint lists explicit.
- Use `@PreAuthorize` and `@PostAuthorize` in services for authorization rules tied to business behavior.
- Secure non-public endpoints by default.

## JWT handling

- Do not assume a JWT implementation exists. Define issuer, audience, signing algorithm, key source/rotation, access-token lifetime, clock skew, and authority mapping before enabling JWT authentication.
- Claims should carry scope information for roles and permissions.
- Define and consistently test authority prefixes such as `ROLE_` or `SCOPE_`; do not mix conventions accidentally.
- Never use a committed or fallback signing secret.
- Refresh tokens require an explicit storage, rotation, reuse-detection, revocation, and expiry policy.

## Web and credential protection

- Define CORS allowlists per environment; do not use wildcard origins with credentials.
- Decide CSRF behavior from the authentication transport. Stateless bearer tokens in headers and cookie-based sessions do not share the same CSRF posture.
- Rate-limit or otherwise protect login, password reset, and token refresh endpoints before production exposure.
- Never log passwords, authorization headers, reset tokens, or full access/refresh tokens.
- Keep management endpoints restricted; health is public only when required by infrastructure.

## Practical guidance

- Do not move authorization rules into controllers.
- Do not bypass `AuthenticationService`-like flows with ad-hoc token logic.
- Reuse the existing error model for authentication and authorization failures.
- Do not instantiate `BCryptPasswordEncoder` or similar encoders inline inside services, seeders, or utility classes.
