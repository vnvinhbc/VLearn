# TOEIC Core CRUD Backend Design

## Intent

Xây nền tảng quản trị học liệu TOEIC có cấu trúc, kiểm tra được và tái sử dụng được trước khi xây attempt, scoring và progress. Admin phải tạo được chuỗi dữ liệu media → question group/questions → lesson → learning path/test → publish; learner chỉ đọc được dữ liệu đã publish và không nhận đáp án đúng/lời giải.

## Goals

- Quản lý user với hai role `ADMIN` và `USER`; chưa có permission table/RBAC mở rộng.
- Lưu metadata media trong PostgreSQL và object trong Cloudflare R2 private bucket.
- Có taxonomy động dạng cây và không cho chu trình.
- Mô hình hóa đủ Part 1–7 bằng question group, ordered content blocks, question và answer options linh hoạt.
- Dùng chung question bank cho lesson và test.
- Lesson dùng ordered blocks với JSONB có schema; vocabulary là resource tái sử dụng.
- Learning path và test là aggregate có thứ tự, preview được và publish có validation.
- Tách admin DTO khỏi learner DTO; learner không nhận `isCorrect`, explanation hoặc transcript bị ẩn.
- Mọi module có pagination/filter/sort cần thiết, authorization, migration và test.

## Non-goals

- Không tạo attempt, answer submission, score, timer runtime, history hoặc progress.
- Không có automatic test generation, adaptive path, analytics, spaced repetition hay AI.
- Không triển khai frontend trong repository backend này.
- Không xây microservice, generic workflow engine, generic CMS hay full content versioning.

## Constraints from the repository

- Java 25, Spring Boot 4.1.2, Maven và package root `com.bvcgroup.vlearn`.
- Layer-based package: `controller`, `service`, `repository`, `entity`, `dto.request`, `dto.response`, `mapper`, `validator`, `constant`, `configuration`.
- Flyway sở hữu production schema; Hibernate dùng `ddl-auto=validate`.
- PostgreSQL Testcontainers bắt buộc cho migration/constraint/transaction/JPA semantics.
- API body dùng `ApiResponse<T>` và business error dùng `ErrorCode` + `AppException`.
- Security deny-by-default, public endpoint phải liệt kê rõ.

## Approaches considered

### 1. Một plan lớn triển khai toàn bộ module

Ưu điểm là nhìn thấy toàn cảnh; nhược điểm là review rất khó, migration và API contract dễ trôi, không có checkpoint chạy được. Không chọn.

### 2. Tách feature-first package hoặc microservice theo M01–M08

Biên nghiệp vụ rõ nhưng trái convention layer-based hiện tại và quá nặng cho CRUD foundation. Không chọn.

### 3. Modular monolith theo layer, triển khai bằng vertical phase có acceptance gate

Mỗi phase thêm một aggregate hoàn chỉnh qua migration → entity/repository → service → DTO/mapper → controller → tests. Đây là lựa chọn đề xuất vì bám repository, cho phép nghiệm thu sớm và vẫn giữ ranh giới nghiệp vụ qua service/validator rõ ràng.

## Domain boundaries

- **Identity:** `User`, `UserProfile`, `RefreshSession`.
- **Media:** `MediaAsset`; R2 interaction chỉ qua `MediaStorageService`.
- **Taxonomy:** `KnowledgeTag`; adjacency list `parent_id`.
- **Question Bank:** `QuestionGroup` aggregate ordered contents/questions; standalone `Question`; owned `AnswerOption`; `QuestionTag`.
- **Learning Content:** `Lesson`, owned `LessonBlock`, `Vocabulary`, ordered link tables.
- **Learning Path:** `LearningPath`, owned `LearningModule`, ordered `ModuleLesson`.
- **Test Management:** `ToeicTest`, owned `TestSection`, owned `TestItem`; item tham chiếu đúng một question hoặc group.
- **Publishing:** domain validator riêng cho từng aggregate; không tạo generic reflection-based publishing engine.

## Shared model decisions proposed for approval

### Identifiers and audit

- Java type `UUID`, PostgreSQL type `uuid`; application tạo UUID.
- Tất cả aggregate có `created_at`, `updated_at`; resource cần ownership có `created_by`, `updated_by` FK tới users.
- Ordered children dùng `display_order > 0` và unique theo parent; reorder thay toàn bộ danh sách trong một transaction.

### Status and deletion

- Publishable content dùng `DRAFT`, `PUBLISHED`, `ARCHIVED`.
- `PUBLISHED` immutable; clone tạo aggregate `DRAFT` mới.
- `DELETE` của publishable content là archive, không hard delete.
- Owned draft children có thể hard delete trong transaction trước khi aggregate publish.
- User dùng `ACTIVE`, `DISABLED`; media có lifecycle riêng để xử lý object deletion an toàn.

### API and pagination

- Giữ context path `/vlearn`; controller mapping không có prefix chung `/api` và không thêm URI version. Vì vậy `/auth/login` được phục vụ tại public URL `/vlearn/auth/login`.
- Create trả `201`, update/read `200`, delete/archive không body trả `204`.
- Collection mặc định `page=0`, `size=20`, giới hạn `size<=100`, sort ổn định có `id` làm tie-breaker.
- Filter/sort là allowlist theo từng endpoint; không expose arbitrary entity field.
- Ghi aggregate con theo `PUT` replacement khi thứ tự/toàn bộ danh sách cần atomic.

### Security

- Password dùng injected `PasswordEncoder` (BCrypt hiện có).
- JWT access dùng `HS512`, sống 10 phút; issuer/audience được validate. HMAC secret tối thiểu 512 bit lấy từ environment/secret store, không có fallback; rotation thay secret và chấp nhận invalidation tối đa 10 phút của access token cũ.
- Refresh token opaque sống 15 ngày, chỉ lưu SHA-256 hash, rotate và phát hiện reuse; logout revoke session/family.
- Login/refresh phải có rate-limit policy được chốt trong ADR-002; CORS/CSRF phải bám đúng refresh-token transport thay vì disable theo thói quen.
- Trạng thái user được kiểm tra ở mỗi protected request để disable có hiệu lực ngay, không chờ access token hết hạn.
- Role map thống nhất thành `ROLE_ADMIN`, `ROLE_USER`; không tạo permission entities hoặc authority chi tiết trong giai đoạn này.
- Learner/admin DTO tách biệt; mapper learner không có field nhạy cảm ngay từ type level.

### Media

- R2 private bucket dùng S3-compatible AWS SDK.
- Phase 2 upload multipart qua backend; validate extension, declared MIME, detected MIME, byte limit.
- Preview trả short-lived presigned GET URL; URL không được persist.
- Delete: chuyển media sang `DELETE_PENDING`, chặn reference mới, xóa object, sau đó `DELETED`; failure thành `DELETE_FAILED` để retry. Media đang được FK/reference thì trả conflict kèm usage summary.

### Question structure

- `QuestionGroupContent` hỗ trợ `TEXT`, `RICH_TEXT`, `IMAGE`, `AUDIO`, có thứ tự và constraint payload phù hợp type.
- Option count linh hoạt; publish validator áp quy tắc theo Part.
- Grouped question chỉ publish cùng group; standalone question publish độc lập.
- Group publish lock group, content, questions và options trong một transaction.
- Admin DTO có correct answer/explanation/transcript; learner DTO không có các field này và có part-aware visibility cho option text/audio.

### Rich text

- JSONB lưu một subset ProseMirror/Tiptap có version (`schemaVersion`).
- Chỉ allowlist node/mark cần cho bài học TOEIC; link protocol và media reference được validate.
- Không lưu raw editor JSON tùy ý và không render raw HTML từ backend.

### Full Test blueprint

- `ToeicBlueprintCode` định danh một bộ rule bất biến.
- Blueprint đầu tiên kiểm tra 200 câu, Listening 100/45 phút và Reading 100/75 phút, cùng count từng Part.
- Test aggregate lưu blueprint code để rule tương lai không làm đổi nghĩa đề đã publish.
- Vì Phase 1 chưa có timer engine, duration chỉ là metadata nhưng được validate ngay.

## Data integrity rules

- Email canonical lowercase unique; tag/lesson/path slug unique theo loại resource.
- Tree tag không tự làm parent, không tạo ancestor cycle, parent archived không nhận child mới.
- FK media dùng `RESTRICT`; service còn kiểm tra media status.
- Một question có đúng một correct option khi publish.
- `test_items` có XOR check giữa `question_id` và `question_group_id`.
- Reference từ lesson/path/test chỉ được publish khi dependency đã publish và chưa archived.
- Learner query luôn lọc `PUBLISHED`; không dựa vào frontend để ẩn draft/answer.

## Observability and operations

- Log structured identifiers/resource action, không log password/token/presigned URL.
- Actuator health giữ public; detail không public.
- R2/JWT configuration dùng typed `@ConfigurationProperties` và startup validation.
- Mỗi phase ghi rõ migration, environment variables và rollback/forward-fix risk.

## External facts used by the plan

- [ETS mô tả TOEIC Listening & Reading](https://www.ets.org/content/ets-org/language-master/in/home/toeic/test-takers/about/listening-reading.html) hiện tại gồm hai section, mỗi section 100 câu; Listening 45 phút và Reading 75 phút. [ETS research compendium](https://www.ets.org/content/dam/ets-org/pdfs/toeic/research-compendium.pdf) ghi cấu trúc Part hiện hành `6/25/39/30/30/16/54`.
- [Cloudflare R2 S3 API](https://developers.cloudflare.com/r2/get-started/s3/) tương thích S3; [R2 presigned URL guidance](https://developers.cloudflare.com/r2/s3/presigned-urls/) xác nhận truy cập có thời hạn và yêu cầu xem URL như bearer credential.

## Open decisions owned by the project author

Các mục “đề xuất cần chốt” trong roadmap phải được duyệt trước code. Ngoài các mục đó, người triển khai được chọn chi tiết nhỏ không thay đổi API/schema/security, nhưng phải ghi lại trong phase report.
