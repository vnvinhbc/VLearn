# ADR-004: Media Storage

**Status:** Accepted

## Decision

- PostgreSQL lưu metadata; Cloudflare R2 private bucket lưu bytes.
- Phase 2 upload multipart qua backend; direct presigned upload để giai đoạn tối ưu sau.
- Preview/download dùng presigned GET URL sống 10 phút; URL không persist và không log query string.
- Chỉ nhận MP3, JPEG/JPG, PNG, WebP; kiểm tra declared MIME, detected MIME và configurable byte limit.
- Delete lifecycle: `ACTIVE -> DELETE_PENDING -> DELETED`; remote failure thành `DELETE_FAILED` để retry.
- Media đang được tham chiếu không được delete; usage API trả các resource đang dùng.

## Consequences

- PostgreSQL và R2 không có distributed transaction; service phải compensation/retry rõ ràng.
- Mọi content service chỉ nhận media trạng thái `ACTIVE`.
