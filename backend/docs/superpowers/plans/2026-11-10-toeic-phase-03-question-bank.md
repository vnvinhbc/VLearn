# TOEIC Phase 3 Question Bank Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Quản lý và publish chính xác câu hỏi Part 1–7, gồm standalone question và group có nhiều ordered stimuli.

**Architecture:** `QuestionGroup` là aggregate root cho grouped questions; standalone `Question` tự làm aggregate root. Content blocks, questions và answer options được ghi theo transaction; publish dùng part-specific validator. Admin projection đầy đủ, learner projection loại field nhạy cảm ở type level.

**Tech Stack:** Spring MVC, JPA, MapStruct, Flyway, PostgreSQL JSONB, Bean Validation, Testcontainers

**Spec:** `docs/superpowers/specs/2026-11-10-toeic-core-crud-design.md`

## Global Constraints

- Part là enum `PART_1` … `PART_7`; difficulty là `EASY`, `MEDIUM`, `HARD`.
- Status là `DRAFT`, `PUBLISHED`, `ARCHIVED`; published immutable, archive thay hard delete.
- Grouped question không publish độc lập; group publish atomically cùng active children.
- Mỗi published question có đúng một correct option; learner type không có `isCorrect` hoặc `explanation`.
- Mọi media reference phải ACTIVE; mọi tag phải ACTIVE.

## Review Focus

- Atomic update bị fail giữa question/options phải rollback toàn bộ.
- Duplicate/missing `display_order` hoặc option label phải bị chặn bởi DB và service.
- Publish group khi một media vừa bị archive/delete phải fail.
- Part 2 learner payload không được vô tình lộ text transcript của spoken options.
- Published aggregate clone phải tạo ID mới cho toàn bộ owned children nhưng giữ thứ tự/nội dung.

---

### Task 1: Question bank schema

**Files:**
- Create: `src/main/resources/db/migration/V4__create_question_bank_tables.sql`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/QuestionGroup.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/QuestionGroupContent.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/Question.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/AnswerOption.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/QuestionTag.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/ToeicPart.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/Difficulty.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/ContentStatus.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/QuestionGroupContentType.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/QuestionGroupVariant.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/QuestionGroupRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/QuestionGroupContentRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/QuestionRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/AnswerOptionRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/QuestionTagRepository.java`
- Test: `src/test/java/com/bvcgroup/vlearn/migration/QuestionBankMigrationIntegrationTest.java`

**Interfaces:**
- Produces ordered group contents/questions/options and join-table tags.
- `question_group_contents` enforces payload-by-type; `answer_options` requires text or media; order/label are unique per parent.

- [ ] **Step 1: Write RED migration tests** for all FKs, check constraints, unique order/label, content payload shape, positive display order and media `RESTRICT`.
- [ ] **Step 2:** Run RED missing V4 tables.
- [ ] **Step 3:** Implement V4 and matching entities/repositories; use explicit fetch methods for detail views, never controller entity serialization.
- [ ] **Step 4:** Run GREEN migration test and repository round-trip for multi-content Part 7 group.
- [ ] **Step 5:** Commit `feat: add question bank persistence model`.

### Task 2: Question group CRUD and ordered content replacement

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/QuestionGroupCreationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/QuestionGroupUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/QuestionGroupContentRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/AdminQuestionGroupResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/QuestionGroupMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/QuestionGroupService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminQuestionGroupController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/QuestionGroupServiceTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AdminQuestionGroupControllerTest.java`

**Interfaces:**
- Endpoints: `POST/GET /admin/question-groups`, `GET/PATCH/DELETE /admin/question-groups/{id}`, `PUT /admin/question-groups/{id}/contents`.
- Filters: `part`, `variant`, `status`, `q`; stable sort allowlist.

- [ ] **Step 1: Write RED tests** for create/list/detail/update/archive, ordered mixed content, invalid content payload and inactive media.
- [ ] **Step 2:** Run RED focused tests.
- [ ] **Step 3:** Implement service transactions, media validation, explicit pageable filtering (Specification is allowed here because optional multi-filter search is a stated use case).
- [ ] **Step 4:** Run GREEN; verify repeated GET preserves content order.
- [ ] **Step 5:** Commit `feat: manage question groups and stimuli`.

### Task 3: Question, answer options and tags CRUD

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/QuestionCreationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/QuestionUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/AnswerOptionRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/AdminQuestionResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/QuestionMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/QuestionService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminQuestionController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/QuestionServiceTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AdminQuestionControllerTest.java`

**Interfaces:**
- Endpoints: `POST/GET /admin/questions`, `GET/PATCH/DELETE /admin/questions/{id}`.
- Create/update request owns the full ordered option list and tag id set; service replaces both atomically.

- [ ] **Step 1: Write RED tests** for standalone/grouped create, 3/4/flexible options, duplicate labels/order, invalid group Part mismatch, inactive tag/media and transaction rollback.
- [ ] **Step 2:** Run RED focused tests.
- [ ] **Step 3:** Implement relation loading in service, MapStruct mechanical mapping and atomic option/tag replacement.
- [ ] **Step 4:** Run GREEN service/MVC tests and PostgreSQL rollback test.
- [ ] **Step 5:** Commit `feat: manage questions answers and tags`.

### Task 4: Part-specific publication validators

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/validator/QuestionPublicationValidator.java`
- Create: `src/main/java/com/bvcgroup/vlearn/validator/QuestionGroupPublicationValidator.java`
- Create: `src/main/java/com/bvcgroup/vlearn/validator/PartPublicationRule.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/QuestionService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/QuestionGroupService.java`
- Modify: admin controllers
- Test: `src/test/java/com/bvcgroup/vlearn/validator/QuestionPublicationValidatorTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/QuestionPublicationIntegrationTest.java`

**Interfaces:**
- Endpoints: `POST /admin/questions/{id}/publish`, `POST /admin/question-groups/{id}/publish`.
- Produces validation violations as stable error code + field/resource path list, not a generic 500.

- [ ] **Step 1: Write a parameterized RED matrix** covering Parts 1–7: required group/standalone form, media/content type, option count policy, exactly one correct, active dependencies and group question count rules from `business-rules.md`.
- [ ] **Step 2:** Run RED validator tests.
- [ ] **Step 3:** Implement rule registry keyed by `ToeicPart`; keep rule values in one place and group publish one transaction.
- [ ] **Step 4:** Run GREEN unit + PostgreSQL integration tests.
- [ ] **Step 5:** Commit `feat: validate and publish TOEIC questions`.

### Task 5: Preview, learner-safe projection and clone

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/LearnerQuestionResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/LearnerQuestionGroupResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/LearnerQuestionMapper.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/QuestionService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/QuestionGroupService.java`
- Modify: admin controllers
- Test: `src/test/java/com/bvcgroup/vlearn/mapper/LearnerQuestionMapperTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/QuestionCloneIntegrationTest.java`

**Interfaces:**
- Admin preview: `GET /admin/questions/{id}/preview`, `GET /admin/question-groups/{id}/preview`.
- Clone: `POST /admin/questions/{id}/clone`, `POST /admin/question-groups/{id}/clone` returns `201 DRAFT`.
- Learner mapper used later by lesson/test catalog.

- [ ] **Step 1: Write RED serialization tests** asserting learner JSON has no `isCorrect`, `explanation`, hidden transcript or internal storage metadata for every Part.
- [ ] **Step 2: Write RED clone test** asserting new aggregate/child UUIDs, copied order/tags/media references and `DRAFT` status.
- [ ] **Step 3:** Implement explicit learner DTO/part-aware mapping and deep clone transaction.
- [ ] **Step 4:** Run GREEN preview/serialization/clone tests.
- [ ] **Step 5:** Commit `feat: preview and clone questions safely`.

### Task 6: Usage hooks and phase verification

**Files:**
- Modify: `src/main/java/com/bvcgroup/vlearn/service/MediaUsageService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/KnowledgeTagService.java`
- Modify: `docs/toeic-core-crud/api-contract-v1.md`
- Modify: `docs/toeic-core-crud/business-rules.md`

- [ ] **Step 1:** Register question/group-content/option media usage and question tag usage; write conflict tests.
- [ ] **Step 2:** Add API examples for Part 1–7 create and preview payloads.
- [ ] **Step 3:** Run focused Part 1–7 scenario suite, `./mvnw spotless:apply`, then `./mvnw verify`.
- [ ] **Step 4:** Commit `feat: complete TOEIC question bank`.

## Phase 3 Acceptance Gate

- Tạo/reload/preview chính xác representative data của cả bảy Part.
- Group nhiều content blocks và questions giữ đúng thứ tự.
- Publish matrix chặn thiếu option/correct answer/media/content/inactive reference.
- Learner DTO không thể serialize answer/explanation/transcript nhạy cảm.
- V4 migration và `./mvnw verify` pass.
