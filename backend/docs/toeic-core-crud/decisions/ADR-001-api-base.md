# ADR-001: API Base And Context Path

**Status:** Accepted

## Decision

- Giữ `server.servlet.context-path=/vlearn`.
- Controller mapping bắt đầu trực tiếp bằng resource/workflow path như `/auth/login`, `/users/me`, `/admin/media`.
- Không thêm prefix `/api` và chưa thêm URI version.
- Public URL bằng context path cộng controller mapping, ví dụ `/vlearn/auth/login`.
- Actuator health giữ tại `/vlearn/actuator/health`; không public management endpoint khác.

## Consequences

- Frontend và tài liệu phải dùng URL có `/vlearn`.
- Mọi security matcher trong Spring nhận path bên trong servlet context, ví dụ `/auth/login`.
- Nếu sau này cần URI versioning, phải có migration contract riêng thay vì đổi ngầm.
