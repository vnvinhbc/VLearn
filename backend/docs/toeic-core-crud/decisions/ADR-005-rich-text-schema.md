# ADR-005: Rich Text Storage

**Status:** Accepted

## Decision

- Lesson rich text lưu PostgreSQL JSONB theo versioned subset của ProseMirror/Tiptap.
- Payload có `schemaVersion`; Phase 4 bắt đầu với version 1.
- Backend allowlist node/mark, giới hạn depth/size và protocol link.
- Không chấp nhận JSON editor tùy ý hoặc raw HTML không kiểm soát.
- Frontend render bằng schema tương ứng, không dùng unsanitized `innerHTML`.

## Consequences

- Thay đổi schema cần backward-compatible reader hoặc migration/version mới.
- Editor frontend phải bám schema backend thay vì tự lưu extensions bất kỳ.
