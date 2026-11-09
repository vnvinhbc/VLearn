# ADR-007: Identity Role Model

**Status:** Accepted

## Decision

- Một user có đúng một role: `ADMIN` hoặc `USER`.
- Authority mapping là `ROLE_ADMIN` và `ROLE_USER`.
- Không tạo permission, role-permission hoặc user-permission tables trong Core CRUD.
- Không có API đổi role trong Phase 1.
- Admin không được tự disable và không được disable active admin cuối cùng.
- Mở rộng RBAC chỉ khi có role Teacher/Editor hoặc quyền chi tiết theo module được xác định.

## Consequences

- Authorization đơn giản bằng role checks và method security.
- Thêm role/permission sau này cần migration và contract riêng, không được nhét vào enum âm thầm.
