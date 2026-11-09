# TOEIC Phase 5 Learning Path Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Quản lý và publish lộ trình có ordered modules và reusable lessons; learner đọc được path đã publish.

**Architecture:** `LearningPath` là aggregate root, `LearningModule` là owned child và `ModuleLesson` là ordered link. Admin có thể xây path bằng draft lessons nhưng publish chỉ khi mọi lesson đã published. Learner projection chỉ query published path/dependencies.

**Tech Stack:** Spring MVC, JPA, Flyway, MapStruct, PostgreSQL Testcontainers

**Spec:** `docs/superpowers/specs/2026-11-10-toeic-core-crud-design.md`

## Global Constraints

- Target score trong TOEIC range được business rules chốt; starting level dùng enum đã document.
- Một lesson được tái sử dụng ở nhiều module/path.
- Module/order update là atomic; không có progress/enrollment trong phase này.
- Published path immutable; lesson/path archive bị usage rules bảo vệ.

## Review Focus

- Một lesson lặp hai lần trong cùng module phải bị chặn, nhưng được phép ở module khác nếu business rule cho phép.
- Reorder module và lesson đồng thời phải không mất link.
- Publish path có empty module hoặc draft/archived lesson phải fail rõ.
- Learner slug không được đọc draft/archived path.
- Query detail path không được gây N+1 theo số module/lesson.

---

### Task 1: Learning path schema

**Files:**
- Create: `src/main/resources/db/migration/V6__create_learning_path_tables.sql`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/LearningPath.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/LearningModule.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/ModuleLesson.java`
- Reuse: `src/main/java/com/bvcgroup/vlearn/constant/ProficiencyLevel.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/LearningPathRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/LearningModuleRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/ModuleLessonRepository.java`
- Test: `src/test/java/com/bvcgroup/vlearn/migration/LearningPathMigrationIntegrationTest.java`

**Interfaces:** Ordered unique modules per path and lessons per module; FK lessons uses `RESTRICT`.

- [ ] **Step 1: Write RED migration tests** for slug, score/check constraints, FK/index/order/duplicate lesson constraints.
- [ ] **Step 2:** Add V6 and entities/repositories; run GREEN migration tests.
- [ ] **Step 3:** Commit `feat: add learning path persistence`.

### Task 2: Path and module CRUD

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/LearningPathCreationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/LearningPathUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/LearningModuleCreationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/LearningModuleUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/AdminLearningPathResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/AdminLearningModuleResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/LearningPathMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/LearningPathService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminLearningPathController.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminLearningModuleController.java`
- Test: service and controller tests

**Interfaces:**
- `POST/GET /admin/learning-paths`, `GET/PATCH/DELETE /admin/learning-paths/{id}`.
- `POST /admin/learning-paths/{id}/modules`, `PATCH/DELETE /admin/modules/{id}`.
- Filters: `targetScore`, `startingLevel`, `status`, `q`.

- [ ] **Step 1: Write RED tests** for CRUD, validation, authorization, pagination and immutable published path.
- [ ] **Step 2:** Implement minimal services/controllers/mappers; run GREEN.
- [ ] **Step 3:** Commit `feat: manage learning paths and modules`.

### Task 3: Atomic ordering and lesson assignment

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/ModuleOrderRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/ModuleLessonListRequest.java`
- Modify: learning path service/controllers
- Test: `src/test/java/com/bvcgroup/vlearn/service/LearningPathOrderingIntegrationTest.java`

**Interfaces:**
- `PUT /admin/learning-paths/{id}/module-order`.
- `PUT /admin/modules/{id}/lessons` replaces full ordered list.

- [ ] **Step 1: Write RED PostgreSQL tests** for valid reorder, duplicate/missing module id, duplicate lesson, unknown lesson and rollback on one invalid item.
- [ ] **Step 2:** Implement row locking/transactional replacement without temporary unique-order collisions.
- [ ] **Step 3:** Run GREEN and commit `feat: order path modules and lessons`.

### Task 4: Publish, learner catalog, preview and clone

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/validator/LearningPathPublicationValidator.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/LearnerLearningPathSummaryResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/LearnerLearningPathDetailResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/LearnerLearningPathMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/LearningPathCatalogController.java`
- Modify: learning path service/admin controller
- Test: publication, learner MVC and query-count tests

**Interfaces:**
- Admin: `POST /admin/learning-paths/{id}/publish`, `POST .../{id}/clone`, `GET .../{id}/preview`.
- Learner: `GET /learning-paths`, `GET /learning-paths/{slug}`.

- [ ] **Step 1: Write RED tests** for empty structure, unpublished lesson, target metadata, published-only learner list/detail, deep clone and answer-safe nested lesson summaries.
- [ ] **Step 2:** Implement validator/projections/fetch plan; learner endpoints require authenticated `USER` or `ADMIN` as ADR specifies.
- [ ] **Step 3:** Run GREEN including bounded query-count assertion.
- [ ] **Step 4:** Commit `feat: publish and browse learning paths`.

### Task 5: Usage hooks and phase verification

**Files:**
- Modify: `src/main/java/com/bvcgroup/vlearn/service/LessonService.java`
- Modify: `docs/toeic-core-crud/api-contract-v1.md`
- Modify: `docs/toeic-core-crud/business-rules.md`
- Test: `src/test/java/com/bvcgroup/vlearn/e2e/LearningPathFixtureIntegrationTest.java`

- [ ] Register lesson usage by modules; archived lesson with path reference returns conflict.
- [ ] Build an integration fixture for a `TOEIC 650+` path with at least three modules and stable lesson order.
- [ ] Update API/business-rule docs.
- [ ] Run `./mvnw spotless:apply` and `./mvnw verify`.
- [ ] Commit `feat: complete learning path management`.

## Phase 5 Acceptance Gate

- TOEIC 650+ path giữ đúng module/lesson order sau reload.
- Publish chặn empty/draft/archived dependency.
- Learner chỉ xem published path qua learner-safe DTO.
- V6 migration và `./mvnw verify` pass.
