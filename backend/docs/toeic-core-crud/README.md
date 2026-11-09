# TOEIC Core CRUD — Execution Roadmap

Tài liệu này là điểm vào cho kế hoạch triển khai Giai đoạn 1 của VLearn. Phạm vi của bộ kế hoạch là **backend Spring Boot hiện tại**. Phase 0 đã bổ sung contract và shared foundation; domain CRUD bắt đầu từ Phase 1.

## Hiện trạng repository

- Java 25, Spring Boot 4.1.2, Maven.
- PostgreSQL + Flyway; hiện chỉ có `V1__baseline.sql` và chưa có domain table.
- Spring Security đang public health check và deny toàn bộ endpoint khác.
- Đã có `ApiResponse`, `ErrorCode`, `AppException`, `GlobalExceptionHandler`.
- Chưa có user, auth, JWT, media, taxonomy hay bất kỳ CRUD nghiệp vụ nào.
- Test web/context dùng H2; mọi test liên quan SQL, constraint, transaction, Flyway hoặc JPA phải dùng PostgreSQL Testcontainers.

## Phạm vi

### Có trong roadmap backend

- M01 Identity & User.
- M02 Media Management với Cloudflare R2.
- M03 Knowledge Taxonomy.
- M04 Question Bank.
- M05 Learning Content.
- M06 Learning Path.
- M07 Test Management.
- Phần backend của M08: publish/archive/preview, learner catalog an toàn, dữ liệu mẫu, hardening và API documentation.

### Không có trong roadmap này

- React/Next.js Admin Dashboard và Learner UI. Repository hiện tại chỉ là backend; frontend cần một plan riêng sau khi API contract được duyệt.
- Làm bài, chấm điểm, attempt/history, progress, timer runtime, analytics, spaced repetition, AI/adaptive learning.
- Microservice, event bus, search engine, versioning nội dung đầy đủ.
- Nội dung TOEIC có bản quyền. Dữ liệu mẫu phải là nội dung tự tạo.

## Tài liệu nguồn

- [Consolidated design](../superpowers/specs/2026-11-10-toeic-core-crud-design.md)
- Đặc tả nghiệp vụ do chủ dự án cung cấp trong yêu cầu ngày 2026-11-10.
- Quy tắc repository trong `AGENTS.md` và `.agents/rules`.

## Thứ tự thực hiện và cổng duyệt

Mỗi phase là một lần triển khai độc lập. Không bắt đầu phase kế tiếp trước khi phase hiện tại đạt acceptance gate và được duyệt.

| Phase | Deliverable chạy được | Phụ thuộc | Plan |
| --- | --- | --- | --- |
| 0 | Chốt contract, ERD, security policy và test foundation | Foundation hiện tại | [Phase 0](../superpowers/plans/2026-11-10-toeic-phase-00-contracts.md) |
| 1 | Đăng ký/đăng nhập/refresh/logout, profile, admin user | Phase 0 | [Phase 1](../superpowers/plans/2026-11-10-toeic-phase-01-identity.md) |
| 2 | Media trên R2 và cây knowledge tag | Phase 1 | [Phase 2](../superpowers/plans/2026-11-10-toeic-phase-02-media-taxonomy.md) |
| 3 | Question Group, Question, Answer Option, publish validation | Phase 2 | [Phase 3](../superpowers/plans/2026-11-10-toeic-phase-03-question-bank.md) |
| 4 | Lesson, block, vocabulary và liên kết question | Phase 3 | [Phase 4](../superpowers/plans/2026-11-10-toeic-phase-04-learning-content.md) |
| 5 | Learning Path, module, thứ tự lesson và learner read API | Phase 4 | [Phase 5](../superpowers/plans/2026-11-10-toeic-phase-05-learning-path.md) |
| 6 | Part Practice, Mini Test, Full Test và blueprint validator | Phase 3 về domain; Phase 5 về migration order | [Phase 6](../superpowers/plans/2026-11-10-toeic-phase-06-test-management.md) |
| 7 | Catalog tích hợp, dữ liệu mẫu, E2E/hardening, API docs | Phases 1–6 | [Phase 7](../superpowers/plans/2026-11-10-toeic-phase-07-integration-hardening.md) |

Phase 5 và Phase 6 độc lập về domain sau khi Phase 3 hoàn tất. Tuy nhiên các plan đã gán migration `V6` cho Learning Path và `V7` cho Test Management, vì vậy mặc định phải triển khai Phase 5 rồi Phase 6. Nếu đổi thứ tự, phải renumber migration trong plan trước khi viết code.

## Ước lượng thận trọng

| Phase | Thời gian dự kiến |
| --- | --- |
| 0 | 3–5 ngày |
| 1 | 7–10 ngày |
| 2 | 6–8 ngày |
| 3 | 10–14 ngày |
| 4 | 7–10 ngày |
| 5 | 4–6 ngày |
| 6 | 7–10 ngày |
| 7 | 5–8 ngày |

Tổng khoảng 49–71 ngày làm việc cho một backend developer, chưa tính frontend. Đây là estimate có test/migration/security gate đầy đủ; có thể rút ngắn sau Phase 0 khi API và security decisions không còn thay đổi.

## Quyết định đã chốt

Các lựa chọn dưới đây đã được chủ dự án phê duyệt ngày 2026-11-10:

1. Giữ `server.servlet.context-path=/vlearn`; controller không dùng thêm prefix chung. Ví dụ controller mapping `/auth/login` có public URL `/vlearn/auth/login`, health là `/vlearn/actuator/health`.
2. Access token JWT `HS512` sống 10 phút; refresh token opaque sống 15 ngày, lưu hash, rotate mỗi lần dùng và phát hiện reuse. Secret HMAC tối thiểu 512 bit, chỉ lấy từ environment/secret store và không có fallback.
3. Refresh token đặt trong cookie `HttpOnly`, `Secure`, `SameSite=Strict`; frontend production đi cùng site qua reverse proxy. Nếu frontend bắt buộc cross-site, phải đổi CSRF/cookie policy trước Phase 1.
   Logout revoke refresh family; access JWT hiện tại chỉ hết hiệu lực khi hết 10 phút trừ khi dự án chấp nhận thêm access-token denylist.
4. Nội dung `PUBLISHED` là immutable. Muốn sửa phải clone sang bản `DRAFT`; `DELETE` của nội dung nghiệp vụ chuyển sang `ARCHIVED`.
5. R2 bucket private; backend upload ở Phase 2 và trả presigned GET URL sống 10 phút. Direct presigned upload chỉ tối ưu ở giai đoạn sau.
6. Rich text dùng JSONB theo subset ProseMirror/Tiptap được allowlist và validate tại backend; không chấp nhận JSON tùy ý hay raw HTML không kiểm soát.
7. Full Test dùng blueprint có mã/version. Blueprint đầu tiên theo TOEIC L&R hiện hành: 100 Listening/45 phút và 100 Reading/75 phút; per-Part count được khóa trong validator của blueprint thay vì rải hardcode trong service.
8. Một user có đúng một role `ADMIN` hoặc `USER` trong Phase 1. Không xây permission table/RBAC mở rộng cho Core CRUD; chỉ xem xét khi xuất hiện Teacher/Editor hoặc quyền chi tiết theo module.
9. Admin không được tự disable chính mình hoặc disable `ACTIVE ADMIN` cuối cùng; Phase 1 chưa có API đổi role.

Nếu một trong các quyết định trên thay đổi, cập nhật ADR, design và plan liên quan trước khi triển khai tiếp.

## Quy trình chung cho mọi phase

1. Đọc design, plan phase và repository state mới nhất.
2. Nêu assumption còn ảnh hưởng API/schema/security.
3. Viết test RED cho một vertical slice.
4. Thêm Flyway migration trước entity khi schema thay đổi; migration test chạy PostgreSQL Testcontainers.
5. Implement tối thiểu để GREEN, format, chạy focused tests.
6. Kết thúc phase bằng `./mvnw verify`, OpenAPI/API examples và báo cáo migration/config/risk.

## Definition of Done chung

- Migration chạy từ database trống và từ schema của phase trước.
- API có validation, authorization, error code và stable pagination/sort khi là collection.
- Controller không serialize entity; DTO admin và learner tách biệt.
- Multi-table write có transaction; invariant quan trọng có DB constraint.
- Published data không bị sửa trực tiếp và archived data không xuất hiện trong learner API.
- Focused test và `./mvnw verify` đều pass.
- Không commit secret, key, R2 credential hay dữ liệu TOEIC sao chép.

## Cách duyệt và triển khai

- Duyệt consolidated design và tám phase plan trước.
- Khi bắt đầu code, chỉ chọn **một phase**.
- Mỗi phase nên được làm trên branch/worktree riêng, review và merge trước phase sau.
- Checklist trong từng plan là nguồn theo dõi thực thi; không dùng checklist 43 mục ban đầu thay cho acceptance gate kỹ thuật.
