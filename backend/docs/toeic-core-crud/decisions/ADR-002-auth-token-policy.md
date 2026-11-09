# ADR-002: Authentication And Token Policy

**Status:** Accepted

## Decision

- Access token là JWT ký `HS512`, lifetime 10 phút, clock skew 30 giây.
- JWT issuer là `vlearn`, audience là `vlearn-api`, subject là user UUID.
- Claim role map thành đúng một authority `ROLE_ADMIN` hoặc `ROLE_USER`.
- HMAC secret tối thiểu 512 bit, đọc từ environment/secret store, không có fallback trong source.
- `HS521` trong feedback được chuẩn hóa thành `HS512`; RFC 7518 không đăng ký thuật toán `HS521`.
- Rotation thay secret khi deploy; access token ký bằng secret cũ bị invalid ngay. Lifetime ngắn giới hạn ảnh hưởng, chưa hỗ trợ multi-key overlap.
- Refresh token là opaque random token, lifetime 15 ngày, database chỉ lưu SHA-256 hash.
- Refresh token rotate mỗi lần dùng; reuse token cũ revoke toàn token family.
- Refresh token truyền bằng cookie `HttpOnly`, `Secure` ở production, `SameSite=Strict`, path giới hạn cho auth endpoints.
- Access token truyền qua `Authorization: Bearer` header.
- Logout revoke refresh family; access JWT hiện tại tiếp tục hợp lệ tối đa 10 phút. Client phải xóa access token ngay.
- CSRF không áp dụng cho bearer-protected resource endpoints; refresh/logout kiểm tra same-site cookie và allowed Origin theo deployment contract.
- CORS dùng allowlist theo environment, không dùng wildcard với credentials.
- Login/refresh rate limit là production deployment gate tại reverse proxy/API gateway; không xây distributed rate limiter trong Core CRUD.

## Consequences

- Mọi instance biết HMAC secret đều có thể ký token; secret phải được bảo vệ chặt.
- Rotate secret làm invalid access token đang còn hạn.
- Không có access-token denylist trong Phase 1.
- Production chưa được expose login/refresh nếu edge rate limit chưa cấu hình.
