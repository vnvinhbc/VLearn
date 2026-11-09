# TOEIC Phase 6 Test Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Quản lý Part Practice, Mini Test và Full Test bằng references có thứ tự tới published Question Bank mà không làm mất shared stimuli.

**Architecture:** `ToeicTest` là aggregate root với owned sections/items. Một item tham chiếu đúng một standalone question hoặc whole question group; effective question count được tính từ reference. Publication validator được tách theo `TestType` và versioned `ToeicBlueprintCode`.

**Tech Stack:** Spring MVC, JPA, Flyway, MapStruct, Bean Validation, PostgreSQL Testcontainers

**Spec:** `docs/superpowers/specs/2026-11-10-toeic-core-crud-design.md`

## Global Constraints

- Test type: `PART_PRACTICE`, `MINI_TEST`, `FULL_TEST`.
- Published test immutable và chỉ tham chiếu published, non-archived question/group.
- Một question thuộc group không được add như standalone item.
- Không có attempt/answer/score/timer runtime trong phase này.
- Learner preview không có correct answer, explanation hoặc hidden transcript.

## Review Focus

- Cùng question xuất hiện trực tiếp và qua group phải được phát hiện duplicate effective question.
- Reorder items phải giữ whole group và không đổi effective question order.
- Full Test count phải tính theo số question trong group, không theo số item row.
- Publish đang chạy đồng thời với archive dependency phải không tạo broken test.
- Blueprint mới trong tương lai không được đổi semantics của test đã publish với blueprint cũ.

---

### Task 1: Test management schema

**Files:**
- Create: `src/main/resources/db/migration/V7__create_test_management_tables.sql`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/ToeicTest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/TestSection.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/TestItem.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/TestType.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/ToeicSkill.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/ToeicBlueprintCode.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/ToeicTestRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/TestSectionRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/TestItemRepository.java`
- Test: `src/test/java/com/bvcgroup/vlearn/migration/TestManagementMigrationIntegrationTest.java`

**Interfaces:**
- `test_items` has XOR question/group check, positive order and unique reference/order per section.
- `test_sections` stores skill, optional part, duration seconds and display order.
- `toeic_tests` stores type, status and nullable blueprint code required for FULL_TEST.

- [ ] **Step 1: Write RED migration tests** for XOR, FK/index/order/enum/duration constraints and blueprint/type compatibility.
- [ ] **Step 2:** Run RED missing V7 tables.
- [ ] **Step 3:** Add V7 and matching JPA model/repositories.
- [ ] **Step 4:** Run GREEN migration/repository tests.
- [ ] **Step 5:** Commit `feat: add test management persistence`.

### Task 2: Test and section CRUD

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/ToeicTestCreationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/ToeicTestUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/TestSectionCreationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/TestSectionUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/AdminToeicTestResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/AdminTestSectionResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/ToeicTestMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/ToeicTestService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminToeicTestController.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminTestSectionController.java`
- Test: service/controller tests

**Interfaces:**
- `POST/GET /admin/tests`, `GET/PATCH/DELETE /admin/tests/{id}`.
- `POST /admin/tests/{id}/sections`, `PATCH/DELETE /admin/test-sections/{id}`.
- Filters: `testType`, `status`, `skill`, `part`, `q`.

- [ ] **Step 1: Write RED tests** for three types, duration/blueprint rules, pagination/filter, authorization and published immutability.
- [ ] **Step 2:** Implement minimal DTO/mapper/service/controller behavior and run GREEN.
- [ ] **Step 3:** Commit `feat: manage tests and sections`.

### Task 3: Atomic test item selection and ordering

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/TestItemRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/TestItemListRequest.java`
- Modify: `ToeicTestService.java`, admin section controller
- Test: `src/test/java/com/bvcgroup/vlearn/service/TestItemOrderingIntegrationTest.java`

**Interfaces:** `PUT /admin/test-sections/{id}/items` replaces the full ordered list.

- [ ] **Step 1: Write RED PostgreSQL tests** for question item, group item, XOR, grouped-question direct reference, part mismatch, duplicate effective question, inactive dependency, reorder and rollback.
- [ ] **Step 2:** Implement row locking and transactional replacement; resolve each group to ordered questions only for validation/preview, never persist flattened copies.
- [ ] **Step 3:** Run GREEN and commit `feat: select and order test items`.

### Task 4: Versioned publication validators

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/validator/TestPublicationValidator.java`
- Create: `src/main/java/com/bvcgroup/vlearn/validator/PartPracticePublicationRule.java`
- Create: `src/main/java/com/bvcgroup/vlearn/validator/MiniTestPublicationRule.java`
- Create: `src/main/java/com/bvcgroup/vlearn/validator/ToeicLrBlueprint2026.java`
- Modify: `ToeicTestService.java`, admin controller
- Test: `src/test/java/com/bvcgroup/vlearn/validator/TestPublicationValidatorTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/TestPublicationIntegrationTest.java`

**Interfaces:** `POST /admin/tests/{id}/publish` returns structured validation failures or atomically publishes aggregate.

- [ ] **Step 1: Write RED rule tests**:
  - PART_PRACTICE: exactly one Part and every effective question matches it.
  - MINI_TEST: non-empty sections/items and configured positive duration.
  - FULL_TEST blueprint: Listening 45 minutes/100 questions and Reading 75 minutes/100 questions; Part counts `6,25,39,30,30,16,54`; section/part order fixed; any additional group-shape rules from ADR-006.
- [ ] **Step 2:** Run RED validator tests.
- [ ] **Step 3:** Implement type dispatch and immutable blueprint class; persist blueprint code on the test.
- [ ] **Step 4:** Run GREEN unit/integration tests including one exact 200-question generated fixture.
- [ ] **Step 5:** Commit `feat: validate and publish TOEIC tests`.

### Task 5: Preview, learner projection and clone

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/LearnerTestSummaryResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/LearnerTestPreviewResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/LearnerToeicTestMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/TestCatalogController.java`
- Modify: `ToeicTestService.java`, admin controller
- Test: learner serialization/controller and clone integration tests

**Interfaces:**
- Admin: `GET /admin/tests/{id}/preview`, `POST /admin/tests/{id}/clone`.
- Learner: `GET /tests`, `GET /tests/{id}/preview`; no attempt endpoint.

- [ ] **Step 1: Write RED tests** for stable expanded question order, whole group content, published-only learner access, answer redaction and deep clone with new section/item ids.
- [ ] **Step 2:** Implement DTO/mapping/fetch plan/clone transaction.
- [ ] **Step 3:** Run GREEN and commit `feat: preview and clone TOEIC tests`.

### Task 6: Usage hooks and phase verification

**Files:**
- Modify: `src/main/java/com/bvcgroup/vlearn/service/QuestionService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/QuestionGroupService.java`
- Modify: `docs/toeic-core-crud/api-contract-v1.md`
- Modify: `docs/toeic-core-crud/business-rules.md`
- Test: `src/test/java/com/bvcgroup/vlearn/persistence/FullTestPreviewQueryCountTest.java`

- [ ] Register question/group usage by test items; block archive when referenced by active draft/published test per business rules.
- [ ] Add Part Practice, Mini Test and Full Test examples to API contract.
- [ ] Run query-count test for a representative Full Test preview to catch N+1.
- [ ] Run `./mvnw spotless:apply` and `./mvnw verify`.
- [ ] Commit `feat: complete TOEIC test management`.

## Phase 6 Acceptance Gate

- Part Practice/Mini/Full CRUD, reorder, preview, clone và publish chạy đúng.
- Group item luôn giữ shared content và count đúng số questions.
- Full Test validator dùng versioned blueprint, không rải magic counts.
- Learner preview không lộ answers/explanations.
- V7 migration và `./mvnw verify` pass.
