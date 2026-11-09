# ADR-006: Full Test Blueprint

**Status:** Accepted

## Decision

- Full Test lưu immutable `ToeicBlueprintCode`.
- Blueprint đầu tiên là `TOEIC_LR_2026`:
  - Listening: 45 phút, 100 câu; Part 1–4 lần lượt `6, 25, 39, 30`.
  - Reading: 75 phút, 100 câu; Part 5–7 lần lượt `30, 16, 54`.
- Validator tính effective question count: standalone item bằng 1, group item bằng số question published trong group.
- Rule tập trung trong một blueprint class, không rải magic number trong service.

## Consequences

- Format mới tạo blueprint code/class mới; không đổi semantics của đề đã publish.
- Phase 1 chưa chạy timer nhưng duration metadata đã được validate.
