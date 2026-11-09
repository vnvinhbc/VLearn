# TOEIC Core CRUD Business Rules

## Identity

- Registration always creates `USER` + `ACTIVE`; client cannot select role/status.
- Email comparison uses canonical trim/lowercase and a database unique constraint.
- Password is stored only as injected PasswordEncoder output.
- Disabled user cannot login, refresh or access protected resources even with an unexpired access token.
- Password change revokes all refresh sessions for that user.
- Admin cannot disable self or the last active admin.
- Core CRUD has no role mutation API and no permission/RBAC tables.

## Tokens

- Access JWT: HS512, 10 minutes, issuer `vlearn`, audience `vlearn-api`, 30-second clock skew.
- HS512 secret must contain at least 512 bits of entropy/key material and never has a source-code default.
- Refresh token: opaque, 15 days, SHA-256 hash at rest, rotate on use.
- Reuse of a rotated/revoked refresh token revokes its entire family.
- Logout revokes refresh family; current access token is not denylisted.

## Media

- Upload accepts only configured size and supported detected file types.
- R2 upload must succeed before metadata becomes ACTIVE.
- Presigned URL is generated on demand and never stored.
- Only ACTIVE media can be referenced.
- In-use media cannot enter delete lifecycle.
- Remote delete failure is retryable from `DELETE_FAILED`.

## Taxonomy

- Slug is canonical and globally unique.
- Parent must exist and be ACTIVE.
- A tag cannot be its own parent or have a descendant as parent.
- Archived tag is excluded from new links and learner content.

## Publishable resources

- Only DRAFT can be edited or have children/order replaced.
- Publish validates the complete aggregate and dependencies in one transaction.
- PUBLISHED is immutable; clone creates a new DRAFT with new owned-child IDs.
- DELETE archives; archived resources are excluded from learner endpoints.
- Archive is blocked when it would break a published dependent resource.

## Question Bank

- Question Part must match group Part when grouped.
- Standalone publish is allowed only for Parts configured as standalone by the Part rule.
- Grouped question cannot publish independently.
- Every published question has exactly one correct option and unique option labels/orders.
- Option count and required group content are validated by Part rule.
- Part 3/4/6/7 group references always preserve the whole group and shared stimuli.
- Learner mapping cannot expose `isCorrect`, explanation or hidden transcript.

## Learning Content

- Lesson block order is contiguous after replacement.
- Rich text must pass the declared schema version and safety limits.
- Media/tag/vocabulary/practice references must be active; publish additionally requires referenced question/group to be published.
- Lesson practice item references exactly one standalone question or whole group.

## Learning Path

- Target score is within `10..990`.
- Module and lesson order is unique and stable.
- A lesson cannot repeat in the same module.
- Published path has at least one non-empty module and all lessons published.
- No enrollment or progress is inferred from path structure.

## Test Management

- Test item references exactly one standalone question or whole group.
- A grouped question cannot be added independently.
- Duplicate effective questions across a test are rejected.
- PART_PRACTICE contains exactly one Part.
- MINI_TEST requires non-empty items and positive configured duration.
- FULL_TEST follows its stored immutable blueprint and section/Part ordering.
- Test preview is read-only content; no attempt, answer or score is persisted.

## Transaction rules

- Question + answer option/tag replacement is atomic.
- Group publish includes group contents, questions and options atomically.
- Lesson block/practice/vocabulary replacement is atomic.
- Module/lesson and section/item reorder is atomic.
- Remote R2 I/O is not performed while holding a long database transaction; failure state/compensation is explicit.

## Endpoint-to-rule traceability

| Endpoint family | Governing rules |
| --- | --- |
| `/auth/**`, `/users/me`, `/admin/users/**` | Identity, Tokens |
| `/admin/media/**` | Media, Transaction rules |
| `/admin/knowledge-tags/**` | Taxonomy, Publishable resources |
| `/admin/questions/**`, `/admin/question-groups/**` | Question Bank, Publishable resources, Transaction rules |
| `/admin/lessons/**`, `/admin/vocabularies/**`, `/lessons/**` | Learning Content, Publishable resources, Transaction rules |
| `/admin/learning-paths/**`, `/admin/modules/**`, `/learning-paths/**` | Learning Path, Publishable resources, Transaction rules |
| `/admin/tests/**`, `/admin/test-sections/**`, `/tests/**` | Test Management, Publishable resources, Transaction rules |

Every collection endpoint also follows the pagination/filter/sort allowlists in `api-contract-v1.md`. Every admin
mutation requires `ADMIN`; `/users/me` and learner catalog endpoints accept `USER` or `ADMIN`.

## Module acceptance scenarios

- Identity: a registered `USER` can login/refresh/logout; a disabled user is rejected; `USER` receives `403` from
  admin APIs; self/last-admin disable returns conflict.
- Media: valid bytes are stored privately in R2 and metadata in PostgreSQL; preview is short-lived; in-use media
  cannot be deleted; a remote delete failure remains retryable.
- Taxonomy: a valid tree reloads unchanged; self/deep cycle and archive of a required tag are rejected.
- Question Bank: all seven Parts can be created/reloaded/previewed; grouped stimuli preserve order; invalid option
  counts, missing dependencies and learner answer leakage are rejected.
- Learning Content: multi-block lesson, vocabulary and whole-group practice references preserve order; unsafe rich
  text and unpublished dependencies block publication.
- Learning Path: a TOEIC 650+ path with multiple modules reloads in stable order; empty/unpublished dependencies
  block publication; learner sees published paths only.
- Test Management: Part Practice, Mini Test and blueprint Full Test keep group stimuli and effective question count;
  duplicates/shape violations block publication; learner preview contains no answer or explanation.
- Integration: the sample chain media -> group/questions -> lesson -> path -> practice test publishes successfully;
  archive/delete conflicts and role boundaries hold across module boundaries.
