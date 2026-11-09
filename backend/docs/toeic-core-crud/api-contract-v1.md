# TOEIC Core CRUD API Contract v1

## Base URL and envelope

- Servlet context path: `/vlearn`.
- Controller mappings below do not contain `/api`.
- Example: controller `POST /auth/login` is public URL `POST /vlearn/auth/login`.
- Successful body envelope: `{ "code": 1000, "message": null, "result": ... }`.
- `204 No Content` has no envelope/body.
- Collection defaults: `page=0`, `size=20`, maximum `size=100`; stable sort always adds `id` tie-breaker.

## Error statuses

| HTTP | Meaning |
| --- | --- |
| 400 | DTO/query validation or publish validation failure |
| 401 | Missing/invalid/expired token, invalid credential, inactive subject |
| 403 | Authenticated role cannot perform operation |
| 404 | Resource or published learner view not found |
| 409 | Uniqueness/state/reference conflict |
| 500 | Generic internal error without internal detail |

Error body uses the same envelope with a non-success code and no domain object:

```json
{
  "code": 1001,
  "message": "Invalid request",
  "result": {
    "violations": [{ "path": "email", "reason": "must be a valid email" }]
  }
}
```

Stable shared codes are `1001 INVALID_REQUEST`, `1002 UNAUTHENTICATED`, `1003 UNAUTHORIZED`,
`1004 RESOURCE_NOT_FOUND`, `1005 RESOURCE_CONFLICT` and `9999 INTERNAL_ERROR`. Later phases may add
domain-specific codes without changing these meanings. Publish validation uses `400`; duplicate canonical data,
illegal lifecycle transitions and in-use archive/delete use `409`.

## Shared transport contract

- UUID fields are canonical hyphenated strings; timestamps are UTC ISO-8601 instants.
- JSON field names use `camelCase`; enum values use the uppercase values documented here.
- `PATCH` distinguishes an omitted field from an explicit `null`; only fields explicitly documented as clearable
  accept `null`.
- `PUT` ordered-list endpoints replace the full list. Every item has `displayOrder >= 1`, orders must be unique and
  contiguous, and an empty list is accepted only while the aggregate is `DRAFT`.
- Collection request: `page` default `0`, `size` default `20` and max `100`, repeated `sort=field,asc|desc`.
- Collection result: `{content, page, size, totalElements, totalPages, sort}`. The service appends `id,asc` as a
  deterministic tie-breaker when `id` is not explicitly supplied.
- Create and clone return `201`; successful update/get/publish return `200`; delete/archive, logout and password
  change return `204`.

## Field-level DTO contract

This section fixes the minimum fields and validation boundary. Admin response types additionally include `id`,
`status`, `createdAt`, `updatedAt` and ordered owned children where the endpoint is a detail/preview endpoint.

### Identity DTOs

| DTO | Fields and validation |
| --- | --- |
| `UserRegistrationRequest` | `email` required, canonicalized, max 254; `password` required, 8..72 characters; `fullName` required, 1..120 |
| `AuthenticationRequest` | `email` required; `password` required; neither is logged |
| `AuthenticationResponse` | `accessToken`, `tokenType="Bearer"`, `expiresInSeconds=600`, `user: UserResponse`; refresh token is cookie-only |
| `TokenRefreshResponse` | `accessToken`, `tokenType="Bearer"`, `expiresInSeconds=600`; rotated refresh token is cookie-only |
| `UserProfileUpdateRequest` | optional `fullName` 1..120, clearable `avatarMediaId`, clearable `targetScore` 10..990, clearable `currentLevel` |
| `PasswordChangeRequest` | `currentPassword`, `newPassword` 8..72; values must differ |
| `UserStatusUpdateRequest` | `status: ACTIVE|DISABLED` only |
| `UserResponse` | `id`, `email`, `role: ADMIN|USER`, `status: ACTIVE|DISABLED`, `createdAt`, `updatedAt` |
| `UserProfileResponse` | user fields plus `fullName`, `avatarMediaId`, `targetScore`, `currentLevel` |

Refresh cookie name is `vlearn_refresh`, path `/vlearn/auth`, `HttpOnly`, `SameSite=Strict`, and `Secure` outside
local development. Register/login/refresh never accept role or status from the client.

### Media and taxonomy DTOs

| DTO | Fields and validation |
| --- | --- |
| `MediaUploadRequest` | multipart `file` required; optional `displayName` 1..255 and `description` max 1000; detected type must be MP3/JPEG/PNG/WebP within configured size |
| `MediaUpdateRequest` | optional `displayName` 1..255, clearable `description` max 1000 |
| `MediaResponse` | `id`, names/description, `mediaType: AUDIO|IMAGE`, `mimeType`, `fileSize`, `status`, audit fields; never bucket credential/object URL |
| `MediaPreviewResponse` | `mediaId`, `url`, `expiresAt`; URL lifetime 600 seconds |
| `MediaUsageResponse` | `mediaId`, bounded list of `{resourceType, resourceId, field}` and `totalReferences` |
| `KnowledgeTagCreationRequest` | `name` 1..120, `slug` canonical 1..140, optional `description` max 1000, optional `parentId` |
| `KnowledgeTagUpdateRequest` | optional name/slug/description/parentId using create constraints; explicit null clears parent/description |
| `KnowledgeTagResponse` | `id`, `parentId`, `name`, `slug`, `description`, `status: ACTIVE|ARCHIVED`, audit fields |

### Question Bank DTOs

| DTO | Fields and validation |
| --- | --- |
| `QuestionGroupCreationRequest` | `part`, optional `variant` max 50, `title` 1..255, optional admin-only `transcript` max 50000 |
| `QuestionGroupUpdateRequest` | optional mutable creation fields; draft only |
| `QuestionGroupContentRequest` | `contentType: TEXT|RICH_TEXT|IMAGE|AUDIO`, type-matching `textContent`/`contentJson`/`mediaId`, `displayOrder` |
| `QuestionCreationRequest` | optional `groupId`, `part`, `questionText` 1..10000, optional `explanation` max 20000, `difficulty`, `options`, `tagIds` |
| `QuestionUpdateRequest` | optional metadata plus full replacement `options`/`tagIds` when supplied; draft standalone or draft owning group only |
| `AnswerOptionRequest` | `label` 1..10, optional `content` max 10000, optional `mediaId`, `correct`, `displayOrder`; text or media required |
| Admin question/group response | full stimuli, transcript, explanation and `correct`; used only below `/admin` |
| Learner question/group response | prompt, visible stimuli and options without `correct`, explanation or hidden transcript |

`PUT /admin/question-groups/{id}/contents` body is `{ "items": QuestionGroupContentRequest[] }`.

### Learning content DTOs

| DTO | Fields and validation |
| --- | --- |
| `LessonCreationRequest` | `title` 1..255, `slug` 1..280, `lessonType`, optional `description` max 2000, `difficulty` |
| `LessonUpdateRequest` | optional mutable creation fields; draft only |
| `LessonBlockRequest` | `blockType: HEADING|RICH_TEXT|IMAGE|AUDIO`, `schemaVersion` for rich text, type-matching `contentJson`/`mediaId`, `displayOrder` |
| `VocabularyCreationRequest` | `word` 1..255, optional `phonetic` max 255, `partOfSpeech`, `meaning` 1..1000, optional `example` max 2000, optional audio `mediaId` |
| `VocabularyUpdateRequest` | optional mutable creation fields; archived vocabulary is immutable |
| `LessonPracticeItemRequest` | exactly one of `questionId` or `questionGroupId`, plus `displayOrder` |
| `AdminLessonResponse` | lesson metadata plus blocks, tag IDs, ordered vocabulary and practice references |
| `LearnerLessonResponse` | published renderable blocks/vocabulary and learner-safe practice projections only |

Ordered replacement bodies are `{ "items": [...] }`; tag replacement uses `{ "tagIds": UUID[] }`; vocabulary
replacement uses `{ "items": [{"vocabularyId": UUID, "displayOrder": number}] }`.

### Learning path DTOs

| DTO | Fields and validation |
| --- | --- |
| `LearningPathCreationRequest` | `title` 1..255, `slug` 1..280, optional `description` max 2000, `targetScore` 10..990, `startingLevel` |
| `LearningPathUpdateRequest` | optional mutable creation fields; draft only |
| `LearningModuleCreationRequest` | `title` 1..255, optional `description` max 2000, `displayOrder` |
| `LearningModuleUpdateRequest` | optional title/description; order changes through the dedicated reorder endpoint |
| `ModuleOrderRequest` | `moduleIds` contains every current module exactly once |
| `ModuleLessonListRequest` | `items: [{lessonId, displayOrder}]`, unique lesson and order |
| Admin path/module response | metadata plus ordered modules and lesson references |
| Learner path summary/detail | published metadata and ordered published lesson summaries; no progress/enrollment fields |

### Test Management DTOs

| DTO | Fields and validation |
| --- | --- |
| `ToeicTestCreationRequest` | `title` 1..255, optional `description` max 2000, `testType`, optional `blueprintCode`; FULL_TEST requires a supported code |
| `ToeicTestUpdateRequest` | optional mutable creation fields; draft only |
| `TestSectionCreationRequest` | `skill: LISTENING|READING`, optional `part`, `durationSeconds >= 0`, `displayOrder` |
| `TestSectionUpdateRequest` | optional skill/part/duration; order changes through aggregate ordering |
| `TestItemRequest` | exactly one of `questionId` or `questionGroupId`, plus `displayOrder` |
| `TestItemListRequest` | `items: TestItemRequest[]` replacing a section atomically |
| Admin test/section response | test metadata, blueprint and ordered sections/items with admin question projections |
| Learner test summary/preview | published metadata and ordered learner-safe stimuli/questions; no answer, explanation, attempt or score fields |

## Sort allowlists

- Users: `email`, `role`, `status`, `createdAt`, `id`.
- Media: `displayName`, `mediaType`, `status`, `createdAt`, `id`.
- Tags: `name`, `slug`, `createdAt`, `id`.
- Groups/questions: `title`/`questionText`, `part`, `difficulty`, `status`, `createdAt`, `id`.
- Lessons/vocabularies: `title`/`word`, `difficulty`, `status`, `createdAt`, `id`.
- Paths: `title`, `targetScore`, `startingLevel`, `status`, `createdAt`, `id`.
- Tests: `title`, `testType`, `status`, `createdAt`, `id`.

Unsupported filter/sort fields or directions return `400 INVALID_REQUEST` rather than being passed to persistence.

## Authentication and users

| Method | Controller path | Access | Result |
| --- | --- | --- | --- |
| POST | `/auth/register` | Public | `201 UserResponse` |
| POST | `/auth/login` | Public | `200 AuthenticationResponse` + refresh cookie |
| POST | `/auth/refresh` | Refresh cookie | `200 TokenRefreshResponse` + rotated cookie |
| POST | `/auth/logout` | Refresh cookie | `204` |
| GET | `/users/me` | USER/ADMIN | `200 UserProfileResponse` |
| PATCH | `/users/me` | USER/ADMIN | `200 UserProfileResponse` |
| PATCH | `/users/me/password` | USER/ADMIN | `204` |
| GET | `/admin/users` | ADMIN | `200 PageResponse<UserResponse>` |
| GET | `/admin/users/{id}` | ADMIN | `200 UserResponse` |
| PATCH | `/admin/users/{id}/status` | ADMIN | `200 UserResponse` |

## Media and taxonomy

| Method | Path | Access |
| --- | --- | --- |
| POST | `/admin/media/upload` | ADMIN |
| GET | `/admin/media` | ADMIN |
| GET/PATCH/DELETE | `/admin/media/{id}` | ADMIN |
| GET | `/admin/media/{id}/preview` | ADMIN |
| GET | `/admin/media/{id}/usage` | ADMIN |
| POST/GET | `/admin/knowledge-tags` | ADMIN |
| GET/PATCH/DELETE | `/admin/knowledge-tags/{id}` | ADMIN |

## Question Bank

| Method | Path | Access |
| --- | --- | --- |
| POST/GET | `/admin/question-groups` | ADMIN |
| GET/PATCH/DELETE | `/admin/question-groups/{id}` | ADMIN |
| PUT | `/admin/question-groups/{id}/contents` | ADMIN |
| POST | `/admin/question-groups/{id}/publish` | ADMIN |
| POST | `/admin/question-groups/{id}/clone` | ADMIN |
| GET | `/admin/question-groups/{id}/preview` | ADMIN |
| POST/GET | `/admin/questions` | ADMIN |
| GET/PATCH/DELETE | `/admin/questions/{id}` | ADMIN |
| POST | `/admin/questions/{id}/publish` | ADMIN |
| POST | `/admin/questions/{id}/clone` | ADMIN |
| GET | `/admin/questions/{id}/preview` | ADMIN |

Admin question DTO includes explanation/correct option/transcript. Learner DTO types do not declare those fields.

## Learning content

| Method | Path | Access |
| --- | --- | --- |
| POST/GET | `/admin/lessons` | ADMIN |
| GET/PATCH/DELETE | `/admin/lessons/{id}` | ADMIN |
| PUT | `/admin/lessons/{id}/blocks` | ADMIN |
| PUT | `/admin/lessons/{id}/tags` | ADMIN |
| PUT | `/admin/lessons/{id}/vocabularies` | ADMIN |
| PUT | `/admin/lessons/{id}/practice-items` | ADMIN |
| POST | `/admin/lessons/{id}/publish` | ADMIN |
| POST | `/admin/lessons/{id}/clone` | ADMIN |
| GET | `/admin/lessons/{id}/preview` | ADMIN |
| POST/GET | `/admin/vocabularies` | ADMIN |
| GET/PATCH/DELETE | `/admin/vocabularies/{id}` | ADMIN |
| GET | `/lessons` | USER/ADMIN |
| GET | `/lessons/{slug}` | USER/ADMIN |

## Learning paths

| Method | Path | Access |
| --- | --- | --- |
| POST/GET | `/admin/learning-paths` | ADMIN |
| GET/PATCH/DELETE | `/admin/learning-paths/{id}` | ADMIN |
| POST | `/admin/learning-paths/{id}/modules` | ADMIN |
| PUT | `/admin/learning-paths/{id}/module-order` | ADMIN |
| PATCH/DELETE | `/admin/modules/{id}` | ADMIN |
| PUT | `/admin/modules/{id}/lessons` | ADMIN |
| POST | `/admin/learning-paths/{id}/publish` | ADMIN |
| POST | `/admin/learning-paths/{id}/clone` | ADMIN |
| GET | `/admin/learning-paths/{id}/preview` | ADMIN |
| GET | `/learning-paths` | USER/ADMIN |
| GET | `/learning-paths/{slug}` | USER/ADMIN |

## Tests

| Method | Path | Access |
| --- | --- | --- |
| POST/GET | `/admin/tests` | ADMIN |
| GET/PATCH/DELETE | `/admin/tests/{id}` | ADMIN |
| POST | `/admin/tests/{id}/sections` | ADMIN |
| PATCH/DELETE | `/admin/test-sections/{id}` | ADMIN |
| PUT | `/admin/test-sections/{id}/items` | ADMIN |
| POST | `/admin/tests/{id}/publish` | ADMIN |
| POST | `/admin/tests/{id}/clone` | ADMIN |
| GET | `/admin/tests/{id}/preview` | ADMIN |
| GET | `/tests` | USER/ADMIN |
| GET | `/tests/{id}/preview` | USER/ADMIN |

There is deliberately no attempt, answer submission, scoring, history, progress or timer endpoint in this contract.

## Collection filter allowlists

- Users: `email`, `role`, `status`, `createdFrom`, `createdTo`.
- Media: `q`, `mediaType`, `status`, `createdFrom`, `createdTo`.
- Tags: `q`, `status`, `parentId`; `view=tree` is bounded and non-paginated only for active taxonomy tree.
- Groups/questions: `q`, `part`, `variant`, `difficulty` where applicable, `status`, `tagId`.
- Lessons: `q`, `lessonType`, `difficulty`, `status`, `tagId`.
- Paths: `q`, `targetScore`, `startingLevel`, `status`.
- Tests: `q`, `testType`, `status`, `skill`, `part`.

Arbitrary entity-field sorting/filtering is not supported.
