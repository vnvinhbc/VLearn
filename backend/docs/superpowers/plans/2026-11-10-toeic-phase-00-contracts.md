# TOEIC Phase 0 Contracts and Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chốt domain/API/security contract và chuẩn bị test foundation trước khi tạo domain table đầu tiên.

**Architecture:** Giữ modular monolith theo layer hiện tại, nhưng mô tả ranh giới aggregate trong tài liệu và OpenAPI. Phase này chỉ thay đổi cross-cutting foundation cần thiết để các phase sau cùng dùng một contract.

**Tech Stack:** Java 25, Spring Boot 4.1.2, Maven, PostgreSQL 16, Flyway, Spring Security, JUnit 5, Testcontainers

**Spec:** `docs/superpowers/specs/2026-11-10-toeic-core-crud-design.md`

## Global Constraints

- Chưa tạo user/media/question/lesson/path/test table trong phase này.
- Không commit secret hoặc signing key.
- Giữ layer-based package dưới `com.bvcgroup.vlearn`.
- Mọi quyết định thay đổi public API/schema/security phải được duyệt trong roadmap trước khi code.

## Review Focus

- Context path `/vlearn` phải tạo đúng URL public mà không làm lộ actuator ngoài ý muốn; controller không có prefix `/api`.
- Testcontainers phải chạy Flyway thật, không để Hibernate tạo schema.
- Pagination contract phải có max size và stable sort.
- Error contract phải phân biệt validation `400`, unauthenticated `401`, forbidden `403`, not found `404`, conflict `409`.
- Tài liệu ERD/API không được mô tả entity hoặc endpoint khác với phase plans.

---

### Task 1: Chốt các quyết định kiến trúc

**Files:**
- Modify: `docs/toeic-core-crud/README.md`
- Modify: `docs/superpowers/specs/2026-11-10-toeic-core-crud-design.md`
- Create: `docs/toeic-core-crud/decisions/ADR-001-api-base.md`
- Create: `docs/toeic-core-crud/decisions/ADR-002-auth-token-policy.md`
- Create: `docs/toeic-core-crud/decisions/ADR-003-publishing-lifecycle.md`
- Create: `docs/toeic-core-crud/decisions/ADR-004-media-storage.md`
- Create: `docs/toeic-core-crud/decisions/ADR-005-rich-text-schema.md`
- Create: `docs/toeic-core-crud/decisions/ADR-006-full-test-blueprint.md`
- Create: `docs/toeic-core-crud/decisions/ADR-007-identity-role-model.md`

**Interfaces:** Produces immutable architecture decisions consumed by every later phase.

- [x] **Step 1:** Review từng proposal trong roadmap; ghi `Accepted`, `Rejected` hoặc phương án thay thế và consequence trong ADR tương ứng. ADR-002 phải chốt logout semantics, CORS, CSRF và login/refresh rate limiting.
- [x] **Step 2:** Cập nhật design để không còn câu mâu thuẫn với ADR đã accepted.
- [x] **Step 3:** Kiểm tra chéo URL, token transport, published mutation, R2 access, rich-text format và Full Test count giữa ADR và tất cả phase plan.
- [x] **Step 4:** Không tạo commit theo chỉ đạo của chủ dự án; thay đổi được ghi trong execution ledger.

### Task 2: Hoàn thành bốn tài liệu kỹ thuật trước API code

**Files:**
- Create: `docs/toeic-core-crud/domain-model.md`
- Create: `docs/toeic-core-crud/erd-v1.md`
- Create: `docs/toeic-core-crud/api-contract-v1.md`
- Create: `docs/toeic-core-crud/business-rules.md`

**Interfaces:** Produces entity/relationship names, exact endpoint contract and lifecycle rules used by migrations and tests.

- [x] **Step 1:** Viết domain model với aggregate ownership, allowed references và state transitions; không lặp column-level ERD.
- [x] **Step 2:** Viết Mermaid ERD gồm PK/FK/unique/check/index đã được từng phase plan yêu cầu; đánh dấu table thuộc migration nào.
- [x] **Step 3:** Viết API contract gồm request/response DTO, status code, pagination/filter/sort allowlist và error code cho mọi endpoint trong phases 1–7.
- [x] **Step 4:** Viết business rules theo resource: create/update/archive/clone/publish và dependency validation.
- [x] **Step 5:** Review traceability: mỗi endpoint trỏ đến rule, mỗi FK/constraint trỏ đến invariant, mỗi module có acceptance scenario.
- [x] **Step 6:** Không tạo commit theo chỉ đạo của chủ dự án; thay đổi được ghi trong execution ledger.

### Task 3: Chuẩn hóa API base và shared pagination/error types

**Files:**
- Modify: `src/main/resources/application.yaml`
- Modify: `src/main/java/com/bvcgroup/vlearn/exception/ErrorCode.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/common/PageResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/ApiConstants.java`
- Test: `src/test/java/com/bvcgroup/vlearn/dto/common/PageResponseTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/exception/ErrorCodeTest.java`
- Modify: `src/test/java/com/bvcgroup/vlearn/configuration/SecurityConfigTest.java`

**Interfaces:**
- Produces: `PageResponse<T>` with `content`, `page`, `size`, `totalElements`, `totalPages`, `sort`; `ApiConstants.MAX_PAGE_SIZE = 100`.
- Produces: stable errors including `RESOURCE_CONFLICT` (`409`) and domain-specific codes added by later phases.

- [x] **Step 1: Write RED tests** for `PageResponse.from(Page<T>)`, max-page validation contract and public health path `/vlearn/actuator/health` while unspecified routes under `/vlearn` remain protected.
- [x] **Step 2: Run RED:** focused tests failed first because shared type/error did not exist.
- [x] **Step 3: Implement minimal shared contract** and align `server.servlet.context-path` exactly with ADR-001.
- [x] **Step 4: Run GREEN:** focused tests passed.
- [x] **Step 5:** Update `README.md` URLs; commit waived by project owner.

### Task 4: PostgreSQL migration test foundation

**Files:**
- Create: `src/test/java/com/bvcgroup/vlearn/support/PostgresIntegrationTest.java`
- Create: `src/test/java/com/bvcgroup/vlearn/migration/BaselineMigrationIntegrationTest.java`
- Modify: `src/test/resources/application-test.yaml`

**Interfaces:** Produces a reusable PostgreSQL 16 Testcontainers base/configuration with Flyway enabled and Hibernate `validate`.

- [x] **Step 1: Write RED test** `migrate_emptyDatabase_baselineSucceeds` asserting Flyway schema version `1` and application context startup against PostgreSQL.
- [x] **Step 2: Run RED:** first compile/context runs exposed missing base wiring and missing Flyway auto-configuration.
- [x] **Step 3:** Implement `@Testcontainers`, `@ServiceConnection`, real PostgreSQL 16 and separate H2 lightweight-test behavior.
- [x] **Step 4: Run GREEN** focused migration test and full verify on clean disposable containers.
- [x] **Step 5:** Run format and full verify; commit waived by project owner.

### Task 5: Local, staging and CI baseline

**Files:**
- Create: `src/main/resources/application-staging.yaml`
- Modify: `docker-compose.yml`
- Create: `.github/workflows/backend-ci.yml` at the actual Git repository root after confirming whether `backend/` remains a subdirectory
- Modify: `README.md`

**Interfaces:** Produces secret-free staging config and CI that runs `./mvnw verify` with Java 25; no deployment side effect.

- [x] **Step 1:** Confirm Git root `/home/vinh/projects/vlearn` and create workflow in its `.github/workflows` directory.
- [x] **Step 2:** Add staging profile with inherited environment-backed datasource and safe operational overrides only.
- [x] **Step 3:** Add Java 25 CI verify, Maven cache and test-report artifact retention; Testcontainers uses runner Docker.
- [x] **Step 4:** Run the equivalent local format/verify commands and inspect workflow syntax.
- [x] **Step 5:** Commit waived by project owner.

## Phase 0 Acceptance Gate

- Bảy architecture decisions có trạng thái rõ ràng.
- Bốn tài liệu kỹ thuật tồn tại và trace được sang phase tasks.
- URL/API envelope/pagination/error contract thống nhất.
- Testcontainers chạy Flyway V1 trên PostgreSQL.
- Local/staging config và CI không chứa secret, dùng cùng Maven verification gate.
- `./mvnw verify` pass trước khi Phase 1 bắt đầu.
