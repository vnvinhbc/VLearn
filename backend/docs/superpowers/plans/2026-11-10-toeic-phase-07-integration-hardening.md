# TOEIC Phase 7 Integration and Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện learner catalog, API documentation, dữ liệu mẫu xuyên suốt và E2E/security/integrity verification cho toàn Core CRUD.

**Architecture:** Không thêm domain mới. Phase này kết nối projections/services đã có, tạo local-only sample data qua application service/fixture, kiểm tra end-to-end qua HTTP + PostgreSQL + fake R2 tương thích contract, và đóng các gap quan sát được.

**Tech Stack:** Spring Boot, MockMvc, PostgreSQL Testcontainers, WireMock hoặc S3-compatible test double được duyệt, springdoc-openapi 3.2.0, Maven Verify

**Spec:** `docs/superpowers/specs/2026-11-10-toeic-core-crud-design.md`

## Global Constraints

- Không thêm attempt/score/progress dưới danh nghĩa integration.
- Sample content phải tự tạo, không sao chép câu hỏi/audio/image TOEIC có bản quyền.
- Seed development không chạy trong production profile và không dùng production migration location.
- API docs production disabled/restricted theo configuration; security scheme mô tả bearer access token và refresh transport thực tế.
- Mọi bug phát hiện trong E2E phải có regression test ở module sở hữu behavior.

## Review Focus

- Learner traversal từ path/lesson/test không được lộ draft/archived dependency hoặc correct answer.
- Local seed chạy lặp lại không nhân bản dữ liệu và không chứa credential.
- Media-delete vs content-reference race phải giữ invariant.
- Tất cả collection có max page size/stable sort và không query N+1 rõ rệt.
- OpenAPI phải khớp status code/error/payload thật, đặc biệt multipart và cookie/header auth.

---

### Task 1: Unified learner content catalog

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/controller/LessonCatalogController.java`
- Modify: learner services/mappers from phases 4–6
- Test: `src/test/java/com/bvcgroup/vlearn/controller/LearnerCatalogIntegrationTest.java`

**Interfaces:**
- `GET /lessons`, `GET /lessons/{slug}`.
- Existing `GET /learning-paths...` and `GET /tests...` use one pagination/status/security convention.

- [ ] **Step 1: Write RED tests** for authenticated learner/admin, anonymous behavior from ADR, published-only filtering, nested answer redaction and media preview URL generation.
- [ ] **Step 2:** Implement missing catalog endpoints/projections without generic entity serializer.
- [ ] **Step 3:** Run GREEN and commit `feat: complete learner content catalog`.

### Task 2: Local-only connected sample dataset

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/SampleContentProperties.java`
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/SampleContentLoader.java`
- Create: `src/test/resources/fixtures/toeic-sample/` metadata and tiny generated fixtures
- Modify: `src/main/resources/application-local.yaml`
- Test: `src/test/java/com/bvcgroup/vlearn/configuration/SampleContentLoaderIntegrationTest.java`

**Interfaces:** Explicit opt-in, idempotent sample loader using application services; stable external keys kept only for seed lookup.

- [ ] **Step 1: Write RED tests** for disabled-by-default, idempotent rerun and complete chain: media → Part 3 group + 3 questions → Listening lesson → TOEIC 650+ path → Part Practice.
- [ ] **Step 2:** Add original/synthetic fixture content and loader; do not bypass validators or repositories with raw insert.
- [ ] **Step 3:** Run GREEN twice on same PostgreSQL container and commit `feat: add connected local TOEIC sample data`.

### Task 3: End-to-end acceptance scenario

**Files:**
- Create: `src/test/java/com/bvcgroup/vlearn/e2e/ToeicContentLifecycleE2ETest.java`
- Create: `src/test/java/com/bvcgroup/vlearn/support/FakeR2Server.java` or approved equivalent

**Interfaces:** Executes real HTTP/security/service/JPA/Flyway behavior; external R2 is a contract-faithful test double.

- [ ] **Step 1:** Implement the 11-step acceptance flow from the product specification as one readable scenario with helper methods, not one opaque giant assertion.
- [ ] **Step 2:** Add failure branches: missing correct answer, bad upload, duplicate order, partial aggregate update rollback, published mutation conflict and learner calling admin API.
- [ ] **Step 3:** Run `./mvnw -Dtest=ToeicContentLifecycleE2ETest test` and fix only failures backed by regression tests in owning modules.
- [ ] **Step 4:** Commit `test: verify TOEIC content lifecycle end to end`.

### Task 4: OpenAPI and deployment documentation

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/OpenApiConfig.java`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/main/resources/application-local.yaml`
- Create: `docs/toeic-core-crud/configuration.md`
- Create: `docs/toeic-core-crud/deployment.md`
- Test: `src/test/java/com/bvcgroup/vlearn/configuration/OpenApiContractTest.java`

**Interfaces:** Uses [`org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0`](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.2.0), compatible with Spring Boot 4.1.x; generates `/v3/api-docs` and Swagger UI only where enabled.

- [ ] **Step 1: Write RED tests** asserting all planned paths/status/security schemes exist and learner schemas contain no answer fields.
- [ ] **Step 2:** Add dependency/config/annotations only where generated contract lacks required detail.
- [ ] **Step 3:** Document all DB/JWT/R2/CORS/bootstrap/sample variables, local startup, migration and deploy ordering without secret values.
- [ ] **Step 4:** Run GREEN OpenAPI test and commit `docs: publish TOEIC core API and deployment guide`.

### Task 5: Security and authorization matrix

**Files:**
- Create: `src/test/java/com/bvcgroup/vlearn/security/AuthorizationMatrixIntegrationTest.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/configuration/SecurityConfig.java` only if tests expose gaps

- [ ] **Step 1:** Parameterize every endpoint family across anonymous/USER/ADMIN and expected `200/201/204/401/403` behavior.
- [ ] **Step 2:** Test disabled user, expired/bad issuer/bad audience token, CORS allowlist and no sensitive token/media URL logging.
- [ ] **Step 3:** Fix gaps minimally and commit `test: enforce TOEIC API authorization matrix`.

### Task 6: Persistence, transaction and query hardening

**Files:**
- Create: `src/test/java/com/bvcgroup/vlearn/persistence/ContentIntegrityIntegrationTest.java`
- Create: `src/test/java/com/bvcgroup/vlearn/persistence/ContentQueryPerformanceTest.java`
- Modify: repositories/fetch plans/index migration only when a failing test demonstrates need
- Create: `src/main/resources/db/migration/V8__harden_toeic_content_constraints.sql` only if required

- [ ] **Step 1:** Exercise soft archive, all FK restrictions, media/tag usage, XOR constraints, concurrent reorder/reference and rollback paths.
- [ ] **Step 2:** Establish bounded query expectations for group preview, lesson detail, path detail and full-test preview; inspect plans before adding indexes.
- [ ] **Step 3:** If schema change is proven, add V8 plus migration test; never edit V2–V7.
- [ ] **Step 4:** Commit `fix: harden TOEIC content integrity` only when changes are necessary.

### Task 7: Final verification and handoff

**Files:**
- Modify: `README.md`
- Create: `docs/toeic-core-crud/verification-report.md`
- Modify: `docs/toeic-core-crud/README.md`

- [ ] **Step 1:** Start from empty PostgreSQL, run all migrations, enable sample loader and manually call the primary admin/learner flow.
- [ ] **Step 2:** Run `./mvnw spotless:apply`.
- [ ] **Step 3:** Run focused E2E/security/migration suites, then `./mvnw verify`.
- [ ] **Step 4:** Record exact commands/results, migration list, config variables, known risks and intentionally deferred Phase 2 learning/attempt features.
- [ ] **Step 5:** Commit `docs: complete TOEIC core CRUD handoff`.

## Phase 7 Acceptance Gate

- 11-step acceptance scenario pass với PostgreSQL và R2 test double.
- Learner catalog chỉ trả published data, không lộ đáp án/lời giải/transcript nhạy cảm.
- Sample dataset xuyên suốt, local-only và idempotent.
- OpenAPI/config/deployment docs khớp runtime.
- Migrations V1–V7 (và V8 nếu thực sự cần) chạy từ DB trống.
- `./mvnw verify` pass và verification report được lưu.
