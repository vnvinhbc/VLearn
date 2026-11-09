# ADR-003: Publishing Lifecycle

**Status:** Accepted

## Decision

- Publishable resource dùng `DRAFT`, `PUBLISHED`, `ARCHIVED`.
- Chỉ `DRAFT` được sửa nội dung và quan hệ.
- `PUBLISHED` immutable; thay đổi nội dung phải clone thành aggregate `DRAFT` mới.
- `DELETE` API của publishable resource chuyển sang `ARCHIVED`, không hard delete.
- Owned children của draft có thể hard delete trong transaction.
- Publish chạy validator theo aggregate và kiểm tra toàn bộ dependency đang active/published.

## Consequences

- Không cần full versioning trong Core CRUD nhưng nội dung learner đang dùng không bị thay đổi ngầm.
- Slug/identifier của archived record vẫn giữ uniqueness trừ khi business rule sau này cho phép tái sử dụng.
