# TOEIC Phase 4 Learning Content Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cho phép Admin tạo/publish lesson nhiều block, vocabulary và ordered practice references dùng lại Question Bank.

**Architecture:** Lesson là aggregate root, owned `LessonBlock` được replace/reorder trong transaction. Rich text JSONB được validate bằng versioned schema. Practice item dùng XOR reference đến standalone question hoặc whole question group để không làm mất ngữ liệu Part 3/4/6/7.

**Tech Stack:** JPA, Flyway, PostgreSQL JSONB, Jackson, JSON Schema validator selected in ADR-005, MapStruct, Testcontainers

**Spec:** `docs/superpowers/specs/2026-11-10-toeic-core-crud-design.md`

## Global Constraints

- Lesson type: `GRAMMAR`, `VOCABULARY`, `LISTENING`, `READING`.
- Lesson block tối thiểu: `HEADING`, `RICH_TEXT`, `IMAGE`, `AUDIO`.
- Published lesson immutable; clone mới được sửa.
- Publish chỉ nhận ACTIVE media/tag và PUBLISHED practice question/group/vocabulary.
- Learner response render được nhưng không có answer/explanation từ practice item.

## Review Focus

- Malformed/unknown rich-text node, unsafe link protocol hoặc oversized JSON phải bị từ chối.
- Reorder blocks/practice items phải atomic và không để duplicate order.
- Group practice reference phải trả toàn group, không flatten mất shared media.
- Archive dependency sau khi lesson publish phải không làm learner payload hỏng âm thầm; dependency archive phải bị chặn bằng usage rule.
- Clone lesson phải deep-copy blocks/ordered links nhưng không duplicate media/question/vocabulary.

---

### Task 1: Learning content schema

**Files:**
- Create: `src/main/resources/db/migration/V5__create_learning_content_tables.sql`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/Lesson.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/LessonBlock.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/LessonPracticeItem.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/LessonTag.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/Vocabulary.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/LessonVocabulary.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/LessonType.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/LessonBlockType.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/PartOfSpeech.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/LessonRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/LessonBlockRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/LessonPracticeItemRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/LessonTagRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/VocabularyRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/LessonVocabularyRepository.java`
- Test: `src/test/java/com/bvcgroup/vlearn/migration/LearningContentMigrationIntegrationTest.java`

**Interfaces:**
- `lesson_practice_items` has XOR question/group check.
- JSONB block payload and optional media are constrained by block type; ordered link tables have parent/order uniqueness.

- [ ] **Step 1: Write RED migration tests** for slug uniqueness, JSONB column, all FK/XOR/order/check constraints and delete behavior.
- [ ] **Step 2:** Run RED missing V5 tables.
- [ ] **Step 3:** Add V5 and matching entities/repositories.
- [ ] **Step 4:** Run GREEN migration/repository round-trip tests.
- [ ] **Step 5:** Commit `feat: add learning content persistence`.

### Task 2: Versioned rich-text validator

**Files:**
- Create: `src/main/resources/schema/lesson-rich-text-v1.json`
- Create: `src/main/java/com/bvcgroup/vlearn/validator/RichTextValidator.java`
- Create: `src/main/java/com/bvcgroup/vlearn/exception/RichTextValidationException.java` only if existing error model needs structured violations
- Test: `src/test/java/com/bvcgroup/vlearn/validator/RichTextValidatorTest.java`

**Interfaces:** Produces `validate(JsonNode content, int schemaVersion)` with allowlisted nodes/marks, size/depth limits and safe `https`, `mailto` link protocols.

- [ ] **Step 1: Write RED tests** for valid paragraph/heading/list, unknown node, raw script/html, `javascript:` link, excessive depth/size and unknown schema version.
- [ ] **Step 2:** Add only the approved JSON Schema dependency and v1 schema.
- [ ] **Step 3:** Implement validator/error mapping and run GREEN tests.
- [ ] **Step 4:** Commit `feat: validate lesson rich text`.

### Task 3: Lesson aggregate CRUD

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/LessonCreationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/LessonUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/LessonBlockRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/AdminLessonResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/LessonMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/LessonService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminLessonController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/LessonServiceTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AdminLessonControllerTest.java`

**Interfaces:**
- Endpoints: `POST/GET /admin/lessons`, `GET/PATCH/DELETE /admin/lessons/{id}`, `PUT /admin/lessons/{id}/blocks`, `PUT .../{id}/tags`.
- Filters: `lessonType`, `difficulty`, `status`, `tagId`, `q`; stable sort allowlist.

- [ ] **Step 1: Write RED tests** for metadata CRUD, block types, active media, valid JSONB, reorder rollback, immutable published lesson and authorization.
- [ ] **Step 2:** Implement mapper/service/controller with transactional block replacement and explicit filtering.
- [ ] **Step 3:** Run GREEN focused tests and commit `feat: manage lessons and content blocks`.

### Task 4: Vocabulary CRUD and lesson links

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/VocabularyCreationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/VocabularyUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/VocabularyResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/VocabularyMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/VocabularyService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminVocabularyController.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/LessonService.java`
- Test: vocabulary service/controller tests and lesson link integration test

**Interfaces:**
- Vocabulary endpoints: `POST/GET /admin/vocabularies`, `GET/PATCH/DELETE /admin/vocabularies/{id}`.
- Lesson links: `PUT /admin/lessons/{id}/vocabularies`, ordered replacement.

- [ ] **Step 1: Write RED tests** for vocabulary validation, duplicate normalized word+meaning policy, active pronunciation audio and ordered lesson links.
- [ ] **Step 2:** Implement minimal CRUD/link behavior and run GREEN.
- [ ] **Step 3:** Commit `feat: manage vocabulary learning content`.

### Task 5: Practice references, publish, preview and clone

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/LessonPracticeItemRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/validator/LessonPublicationValidator.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/LearnerLessonResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/LearnerLessonMapper.java`
- Modify: `LessonService.java`, `AdminLessonController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/LessonPublicationIntegrationTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/mapper/LearnerLessonMapperTest.java`

**Interfaces:**
- `PUT /admin/lessons/{id}/practice-items`.
- `POST /admin/lessons/{id}/publish`, `POST .../{id}/clone`, `GET .../{id}/preview`.

- [ ] **Step 1: Write RED tests** for XOR practice refs, full group preservation, dependency status, publish violations, learner answer redaction and deep clone behavior.
- [ ] **Step 2:** Implement ordered references, publication validator, learner mapper and clone transaction.
- [ ] **Step 3:** Run GREEN tests and commit `feat: publish and preview lessons`.

### Task 6: Usage hooks and phase verification

**Files:**
- Modify: `src/main/java/com/bvcgroup/vlearn/service/MediaUsageService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/KnowledgeTagService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/QuestionService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/QuestionGroupService.java`
- Modify: `docs/toeic-core-crud/api-contract-v1.md`
- Modify: `docs/toeic-core-crud/business-rules.md`

- [ ] **Step 1:** Register lesson block/vocabulary media usage, lesson tag usage and lesson practice question/group usage; test archive conflicts.
- [ ] **Step 2:** Add representative Grammar, Vocabulary, Listening and Reading payload examples.
- [ ] **Step 3:** Run `./mvnw spotless:apply` and `./mvnw verify`.
- [ ] **Step 4:** Commit `feat: complete learning content management`.

## Phase 4 Acceptance Gate

- Admin tạo/reload lesson nhiều block và reorder đúng.
- JSONB schema chặn unsafe/arbitrary content.
- Lesson giữ nguyên whole question group khi làm practice.
- Publish/clone/archive/learner-safe preview được test.
- V5 migration và `./mvnw verify` pass.
