# TOEIC Phase 1 Identity and User Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cung cấp registration/login/refresh/logout, learner profile và admin user management với disable có hiệu lực ngay.

**Architecture:** Spring Security Resource Server xác thực access JWT HS512 sống 10 phút; backend tự issue token và quản lý opaque refresh token 15 ngày đã hash. User/profile là aggregate riêng; refresh session rotate/revoke trong transaction. Authorization chỉ dùng hai role `ADMIN`/`USER` qua HTTP rule và method security, chưa có permission tables.

**Tech Stack:** Spring Security, OAuth2 JOSE, JPA, Flyway, BCrypt, PostgreSQL Testcontainers, MockMvc

**Spec:** `docs/superpowers/specs/2026-11-10-toeic-core-crud-design.md`

## Global Constraints

- Một user có đúng một role `ADMIN` hoặc `USER`; không scaffold permission/RBAC mở rộng.
- Email được trim + lowercase trước uniqueness check; database unique constraint là nguồn bảo vệ race.
- JWT key chỉ đọc từ validated configuration, không có default secret.
- Không log password, access token, refresh token hoặc cookie.
- Public endpoint chỉ gồm health, register, login, refresh và API docs đã được ADR cho phép.

## Review Focus

- Hai registration đồng thời với cùng email: một request thành công, request còn lại trả conflict.
- Refresh token reuse sau rotation: revoke toàn family và không issue token mới.
- User bị disable khi access JWT còn hạn: request kế tiếp phải bị từ chối ngay.
- Role claim không hợp lệ hoặc audience/issuer sai: trả `401`, không map thành authority.
- Password change/logout phải revoke refresh sessions liên quan nhưng không lộ user enumeration.

---

### Task 1: Identity schema and persistence

**Files:**
- Create: `src/main/resources/db/migration/V2__create_identity_tables.sql`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/User.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/UserProfile.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/RefreshSession.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/UserRole.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/UserStatus.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/ProficiencyLevel.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/UserRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/UserProfileRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/RefreshSessionRepository.java`
- Test: `src/test/java/com/bvcgroup/vlearn/migration/IdentityMigrationIntegrationTest.java`

**Interfaces:**
- Produces: `UserRepository.findByEmailIgnoreCase(String)`, `existsByEmailIgnoreCase(String)`.
- Produces: refresh lookup by token hash, family id and user id; revoke queries scoped explicitly.

- [ ] **Step 1: Write RED migration tests** asserting UUID PK, unique canonical email, one-to-one profile, refresh hash uniqueness, FK/index/check constraints.
- [ ] **Step 2: Run RED:** `./mvnw -Dtest=IdentityMigrationIntegrationTest test`; expected missing V2 tables.
- [ ] **Step 3:** Add V2 SQL and matching JPA entities/repositories; do not use Hibernate schema creation.
- [ ] **Step 4: Run GREEN** migration test and a repository test for canonical email lookup.
- [ ] **Step 5:** Commit `feat: add identity persistence model`.

### Task 2: Registration and password policy

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/UserRegistrationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/UserResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/UserMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/AuthenticationService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AuthenticationController.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/exception/ErrorCode.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/AuthenticationServiceTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AuthenticationControllerTest.java`

**Interfaces:**
- Produces: `AuthenticationService.register(UserRegistrationRequest) -> UserResponse`.
- Endpoint: `POST /auth/register` returns `201`; default role/status are `USER`/`ACTIVE`.

- [ ] **Step 1: Write RED tests** `register_validRequest_createsLearner`, `register_duplicateCanonicalEmail_conflict`, `register_weakPassword_badRequest`.
- [ ] **Step 2:** Run focused tests and confirm RED for missing service/controller.
- [ ] **Step 3:** Implement DTO validation, MapStruct mapping, injected `PasswordEncoder`, transactional user+profile create and duplicate-constraint translation.
- [ ] **Step 4:** Run focused service/MVC tests and expect GREEN.
- [ ] **Step 5:** Commit `feat: add learner registration`.

### Task 3: JWT issuing and login

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/JwtProperties.java`
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/JwtConfig.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/TokenService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/AuthenticationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/AuthenticationResponse.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/AuthenticationService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/controller/AuthenticationController.java`
- Modify: `src/main/resources/application.yaml`
- Test: `src/test/java/com/bvcgroup/vlearn/service/TokenServiceTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AuthenticationControllerTest.java`

**Interfaces:**
- Produces: `TokenService.issueAccessToken(User) -> String` (HS512, 10 minutes), `issueRefreshSession(User, clientMetadata) -> RawRefreshToken` (15 days).
- Endpoint: `POST /auth/login`; exact cookie/body transport follows ADR-002.

- [ ] **Step 1: Write RED tests** for HS512 signature, exact 10-minute access expiry, correct issuer/audience/subject/role, invalid credentials, disabled account, secret shorter than 512 bits and missing secret startup failure.
- [ ] **Step 2:** Run RED focused tests.
- [ ] **Step 3:** Implement typed JWT properties and HMAC encoder/decoder using an environment-provided secret of at least 512 bits, constant-time credential flow, and 15-day refresh token generation with only SHA-256 hash persisted.
- [ ] **Step 4:** Run GREEN focused tests; assert responses never serialize password/hash/raw persisted token.
- [ ] **Step 5:** Commit `feat: add secure login and token issuing`.

### Task 4: Refresh rotation, reuse detection and logout

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/TokenRefreshResponse.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/AuthenticationService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/controller/AuthenticationController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/RefreshTokenIntegrationTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AuthenticationControllerTest.java`

**Interfaces:**
- Endpoints: `POST /auth/refresh`, `POST /auth/logout`.
- Rotation is atomic: old session gets `rotated_at/revoked_at`, new session shares family id.

- [ ] **Step 1: Write RED PostgreSQL integration tests** for valid rotation, expired token, revoked token, reuse detection and concurrent refresh.
- [ ] **Step 2:** Run RED; confirm no refresh implementation.
- [ ] **Step 3:** Implement row locking/transaction boundaries and generic unauthenticated errors that do not reveal token state.
- [ ] **Step 4:** Run GREEN integration and MVC tests.
- [ ] **Step 5:** Commit `feat: rotate and revoke refresh sessions`.

### Task 5: Protected requests and immediate disabled-user enforcement

**Files:**
- Modify: `src/main/java/com/bvcgroup/vlearn/configuration/SecurityConfig.java`
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/JwtAuthorityConverter.java`
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/ActiveUserFilter.java`
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/JwtAuthenticationEntryPoint.java`
- Test: `src/test/java/com/bvcgroup/vlearn/configuration/IdentitySecurityIntegrationTest.java`

**Interfaces:** Produces `ROLE_ADMIN`/`ROLE_USER` authorities and rejects non-active/missing subjects on every protected request.

- [ ] **Step 1: Write RED tests** for missing/expired/bad issuer/bad audience token, admin-vs-learner `403`, disabled user with still-valid JWT `401`, accepted/rejected Origin, CSRF posture chosen by ADR and login/refresh rate limit behavior.
- [ ] **Step 2:** Run RED security integration tests.
- [ ] **Step 3:** Implement converter/filter/entry point and explicit public endpoint allowlist; keep all unspecified routes authenticated by default.
- [ ] **Step 4:** Run GREEN security tests and verify error envelope consistency.
- [ ] **Step 5:** Commit `feat: enforce JWT roles and active user status`.

### Task 6: Profile, password and admin user APIs

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/UserProfileUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/PasswordChangeRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/UserStatusUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/UserProfileResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/UserService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/UserController.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminUserController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/UserServiceTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/UserControllerTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AdminUserControllerTest.java`

**Interfaces:**
- Learner: `GET/PATCH /users/me`, `PATCH /users/me/password`.
- Admin: `GET /admin/users`, `GET /admin/users/{id}`, `PATCH /admin/users/{id}/status`.
- Admin collection filters: `email`, `role`, `status`, `createdFrom`, `createdTo`; stable sort allowlist.

- [ ] **Step 1: Write RED tests** for self read/update, target score range/current level, password mismatch, session revocation, paginated admin search, learner forbidden admin API, self-disable rejection and last-active-admin protection.
- [ ] **Step 2:** Run RED focused tests.
- [ ] **Step 3:** Implement transactional service methods, explicit DTOs/mappers and method-level `@PreAuthorize`.
- [ ] **Step 4:** Run GREEN focused tests.
- [ ] **Step 5:** Commit `feat: add profile and admin user management`.

### Task 7: One-time admin bootstrap and phase verification

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/AdminBootstrapProperties.java`
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/AdminBootstrapConfig.java`
- Modify: `src/main/resources/application-local.yaml`
- Modify: `README.md`
- Test: `src/test/java/com/bvcgroup/vlearn/configuration/AdminBootstrapConfigTest.java`

**Interfaces:** Explicit opt-in bootstrap; idempotent by canonical email; no password in migration/log.

- [ ] **Step 1: Write RED tests** proving disabled-by-default, missing required secret fails only when enabled, repeat run is idempotent.
- [ ] **Step 2:** Implement opt-in bootstrap using the same password encoder and user service invariant.
- [ ] **Step 3:** Document JWT/admin environment variables without values.
- [ ] **Step 4:** Run `./mvnw spotless:apply` then `./mvnw verify`.
- [ ] **Step 5:** Commit `feat: complete identity foundation`.

## Phase 1 Acceptance Gate

- Register/login/refresh/logout/profile/admin user flow chạy end-to-end.
- Learner bị `403` ở admin API; disabled user bị chặn ngay với token còn hạn.
- Refresh rotation/reuse/concurrency được test bằng PostgreSQL.
- V2 migration chạy từ DB trống; JWT/bootstrap config không có secret mặc định.
- `./mvnw verify` pass.
