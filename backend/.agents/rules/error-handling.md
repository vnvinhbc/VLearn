# Error Handling Rules

## Central model

- Use `ErrorCode` as the source of truth for business and API errors.
- Each error should define:
  - internal numeric code
  - message
  - HTTP status
- Throw `AppException` for expected business failures.

## Exception handling

- Handle common application exceptions in `GlobalExceptionHandler`.
- Keep authentication-entry failures handled separately in the security layer when needed.
- Return errors inside `ApiResponse` when the endpoint returns a body, not ad-hoc JSON objects.
- Keep the generic exception fallback aligned with `ErrorCode` conventions so status codes do not drift between `400` and `500`.
- Generic exceptions map to a stable `500` response and must not expose exception messages or stack traces to clients.
- Prefer deriving the HTTP status from `ErrorCode` instead of duplicating status decisions in multiple branches.

## Validation error mapping

- Map validation message keys from DTO annotations back to `ErrorCode`.
- If validation messages need constraint values such as a minimum length, format them from the constraint arguments without exposing rejected sensitive values.
- If a validation key is not recognized, map it to a consistent fallback `ErrorCode` for bad requests instead of returning an arbitrary status.

## Consistency rules

- Prefer adding a new `ErrorCode` before hardcoding any new message.
- Do not throw raw `RuntimeException` for normal business errors.
- Keep error payloads aligned with the existing contract:

```json
{
  "code": 1005,
  "message": "User not existed"
}
```

- Periodically verify that every handled exception path still maps to the intended `ErrorCode` and HTTP status.
- Log unexpected failures server-side with request context; expected business failures do not require error-level stack traces.
