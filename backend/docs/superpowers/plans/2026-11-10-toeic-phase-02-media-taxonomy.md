# TOEIC Phase 2 Media and Taxonomy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cho phép Admin quản lý audio/image trên R2 và cây knowledge tag an toàn để các module nội dung tham chiếu.

**Architecture:** PostgreSQL giữ media metadata, R2 giữ bytes sau abstraction `MediaStorageService`. Taxonomy dùng adjacency list; cycle/status validation nằm trong service và FK/unique/check nằm trong PostgreSQL. Hai submodule cùng phase nhưng triển khai tuần tự, không dùng transaction xuyên PostgreSQL–R2.

**Tech Stack:** Spring MVC multipart, AWS SDK S3-compatible client, Cloudflare R2, Apache Tika (nếu ADR-004 chấp thuận), JPA, Flyway, Testcontainers

**Spec:** `docs/superpowers/specs/2026-11-10-toeic-core-crud-design.md`

## Global Constraints

- Chỉ `ADMIN` được mutate/list metadata quản trị.
- Bucket private; không persist presigned URL.
- Accept MP3, JPG/JPEG, PNG, WebP với size limit từ typed configuration.
- Tag slug global unique; archived tag không nhận child/link mới.
- Media deletion lifecycle phải recoverable/retryable và không làm mất reference hợp lệ.

## Review Focus

- File giả MIME/extension hoặc vượt size phải bị từ chối trước khi metadata ACTIVE.
- R2 upload thành công nhưng DB insert fail phải có compensation; ngược lại không được trả ACTIVE khi object upload fail.
- Presigned URL hết hạn và không bị log/cache như URL vĩnh viễn.
- Tag parent đổi sang descendant phải bị chặn, kể cả cây sâu.
- Xóa media/tag đang được tham chiếu phải trả conflict và không thay đổi trạng thái.

---

### Task 1: Media and taxonomy schema

**Files:**
- Create: `src/main/resources/db/migration/V3__create_media_and_taxonomy_tables.sql`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/MediaAsset.java`
- Create: `src/main/java/com/bvcgroup/vlearn/entity/KnowledgeTag.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/MediaType.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/MediaStatus.java`
- Create: `src/main/java/com/bvcgroup/vlearn/constant/TaxonomyStatus.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/MediaAssetRepository.java`
- Create: `src/main/java/com/bvcgroup/vlearn/repository/KnowledgeTagRepository.java`
- Test: `src/test/java/com/bvcgroup/vlearn/migration/MediaTaxonomyMigrationIntegrationTest.java`

**Interfaces:** Produces UUID entities, media object-key uniqueness and self-referencing tag FK/index.

- [ ] **Step 1: Write RED migration tests** for unique `object_key`/`slug`, valid enum checks, positive `file_size`, tag parent FK `RESTRICT`, audit FKs and nullable `user_profiles.avatar_media_id` FK to active media metadata.
- [ ] **Step 2:** Run RED missing V3 tables.
- [ ] **Step 3:** Add migration/entities/repositories with matching column names and status enum mapping.
- [ ] **Step 4:** Run GREEN migration/repository tests.
- [ ] **Step 5:** Commit `feat: add media and taxonomy persistence`.

### Task 2: R2 client and validated upload

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/R2Properties.java`
- Create: `src/main/java/com/bvcgroup/vlearn/configuration/R2Config.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/MediaStorageService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/R2MediaStorageService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/MediaService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/MediaUploadRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/MediaResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/MediaMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminMediaController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/MediaServiceTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AdminMediaControllerTest.java`

**Interfaces:**
- Produces: `MediaStorageService.put(objectKey, contentType, contentLength, InputStream)`, `presignGet(objectKey, Duration)`, `delete(objectKey)`.
- Endpoint: `POST /admin/media/upload` multipart returns `201` `MediaResponse` without credential/object URL.

- [ ] **Step 1: Write RED tests** for valid types, spoofed MIME, zero/oversize file, R2 failure, DB failure compensation and admin authorization.
- [ ] **Step 2:** Run RED focused tests.
- [ ] **Step 3:** Add pinned S3/Tika dependencies, validated properties and minimal storage/service/controller implementation.
- [ ] **Step 4:** Run GREEN; ensure logs redact credential and presigned query string.
- [ ] **Step 5:** Commit `feat: upload validated media to R2`.

### Task 3: Media list/detail/update/preview

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/MediaUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/MediaDetailResponse.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/MediaService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/controller/AdminMediaController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AdminMediaControllerTest.java`

**Interfaces:**
- Endpoints: `GET /admin/media`, `GET/PATCH /admin/media/{id}`, `GET /admin/media/{id}/preview`.
- Filters: `mediaType`, `status`, `createdFrom`, `createdTo`, `q`; sort allowlist `createdAt`, `displayName`, `fileSize`.

- [ ] **Step 1: Write RED MVC/service tests** for pagination/filter/stable sort, metadata update and 10-minute preview URL only for ACTIVE media.
- [ ] **Step 2:** Implement explicit filter query and DTOs; do not return entity or bucket credential.
- [ ] **Step 3:** Run GREEN focused tests and commit `feat: browse and preview media`.

### Task 4: Usage registry and safe media deletion

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/service/MediaUsageService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/MediaUsageResponse.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/MediaService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/controller/AdminMediaController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/MediaDeletionIntegrationTest.java`

**Interfaces:**
- Endpoints: `GET /admin/media/{id}/usage`, `DELETE /admin/media/{id}`.
- Later phases register query contributors in `MediaUsageService`; delete succeeds only with zero usage.

- [ ] **Step 1: Write RED tests** for in-use conflict, `DELETE_PENDING -> DELETED`, storage failure to `DELETE_FAILED`, retry and concurrent reference attempt.
- [ ] **Step 2:** Implement lifecycle without holding a DB transaction open during remote R2 I/O; block new references to non-ACTIVE media.
- [ ] **Step 3:** Run GREEN integration tests and commit `feat: delete unused media safely`.

### Task 5: User avatar media binding

**Files:**
- Modify: `src/main/java/com/bvcgroup/vlearn/entity/UserProfile.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/dto/request/UserProfileUpdateRequest.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/dto/response/UserProfileResponse.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/UserService.java`
- Modify: `src/main/java/com/bvcgroup/vlearn/service/MediaUsageService.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/UserAvatarIntegrationTest.java`

**Interfaces:** `PATCH /users/me` may set/clear `avatarMediaId`; only ACTIVE IMAGE is accepted and profile reference appears in media usage.

- [ ] **Step 1: Write RED tests** for active image, audio rejection, inactive media rejection, clear avatar and delete-in-use conflict.
- [ ] **Step 2:** Implement profile/media validation and usage hook; run GREEN.
- [ ] **Step 3:** Commit `feat: link profile avatars to media`.

### Task 6: Knowledge tag CRUD and cycle prevention

**Files:**
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/KnowledgeTagCreationRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/request/KnowledgeTagUpdateRequest.java`
- Create: `src/main/java/com/bvcgroup/vlearn/dto/response/KnowledgeTagResponse.java`
- Create: `src/main/java/com/bvcgroup/vlearn/mapper/KnowledgeTagMapper.java`
- Create: `src/main/java/com/bvcgroup/vlearn/service/KnowledgeTagService.java`
- Create: `src/main/java/com/bvcgroup/vlearn/controller/AdminKnowledgeTagController.java`
- Test: `src/test/java/com/bvcgroup/vlearn/service/KnowledgeTagServiceTest.java`
- Test: `src/test/java/com/bvcgroup/vlearn/controller/AdminKnowledgeTagControllerTest.java`

**Interfaces:**
- Endpoints: `POST/GET /admin/knowledge-tags`, `GET/PATCH/DELETE /admin/knowledge-tags/{id}`.
- List supports flat paginated view and `view=tree` bounded tree response.

- [ ] **Step 1: Write RED tests** for create root/child, duplicate slug, self-parent, descendant-parent cycle, archived parent and archive-in-use conflict hook.
- [ ] **Step 2:** Implement traversal validation in service, mapper and admin-only controller.
- [ ] **Step 3:** Run GREEN focused tests and commit `feat: manage knowledge taxonomy`.

### Task 7: Configuration and phase verification

**Files:**
- Modify: `src/main/resources/application.yaml`
- Modify: `src/main/resources/application-local.yaml`
- Modify: `README.md`
- Modify: `docs/toeic-core-crud/api-contract-v1.md`

- [ ] **Step 1:** Document `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, upload limits and preview TTL without real values.
- [ ] **Step 2:** Add a fake/in-memory storage adapter only in test scope; production must fail startup clearly when media feature is enabled without config.
- [ ] **Step 3:** Run `./mvnw spotless:apply` and `./mvnw verify`.
- [ ] **Step 4:** Commit `feat: complete media and taxonomy foundation`.

## Phase 2 Acceptance Gate

- Admin upload/list/update/preview/delete được media; learner không gọi admin API.
- Invalid/spoofed media bị chặn; R2/DB partial failure có trạng thái hoặc compensation rõ.
- Tag tree không thể có cycle; slug/parent/archive invariant được bảo vệ.
- V3 migration và PostgreSQL tests pass.
- `./mvnw verify` pass.
