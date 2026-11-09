# TOEIC Core CRUD Domain Model

## Shared concepts

- Identifier: Java `UUID`, PostgreSQL `uuid`, do application tạo.
- Publishable status: `DRAFT`, `PUBLISHED`, `ARCHIVED`.
- Difficulty: `EASY`, `MEDIUM`, `HARD`.
- TOEIC Part: `PART_1` đến `PART_7`.
- Proficiency level: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`.
- Audit: aggregate có `created_at`, `updated_at`; resource do admin tạo có `created_by`, `updated_by` khi cần truy vết.
- Ordered children: `display_order` bắt đầu từ 1, unique trong parent.

## Identity aggregate

### User

- Root quản lý credential, role và account status.
- Role: `ADMIN` hoặc `USER`.
- Status: `ACTIVE` hoặc `DISABLED`.
- Email được trim/lowercase trước khi persist và unique trong database.
- Một User sở hữu đúng một UserProfile và nhiều RefreshSession.

### UserProfile

- Thuộc đúng một User.
- Lưu `fullName`, `avatarMediaId`, `targetScore`, `currentLevel`.
- Avatar được thêm khi Media module tồn tại và chỉ tham chiếu ACTIVE IMAGE.

### RefreshSession

- Thuộc một User và một token family.
- Chỉ lưu token hash, expiry, rotated/revoked timestamps và client metadata giới hạn.
- Token raw chỉ xuất hiện khi issue/rotate và không persist/log.

## Media aggregate

### MediaAsset

- Metadata của một R2 object: original/display name, media type, detected MIME, bucket/object key, byte size và status.
- Không lưu permanent public URL.
- Status: `ACTIVE`, `DELETE_PENDING`, `DELETE_FAILED`, `DELETED`.
- MediaUsageService tổng hợp reference từ profile, questions, lessons và vocabulary.

## Taxonomy aggregate

### KnowledgeTag

- Cây adjacency-list qua nullable `parentId`.
- Slug unique toàn hệ thống.
- Status: `ACTIVE`, `ARCHIVED`.
- Không được tự làm parent, không được chuyển parent thành descendant, không archive khi còn reference trái business rule.

## Question Bank aggregates

### QuestionGroup

- Aggregate root cho shared stimuli và grouped questions.
- Có Part, optional group variant, title, admin-only transcript và content status.
- Sở hữu ordered QuestionGroupContent và grouped Questions.
- Grouped question chỉ publish/archive cùng group.

### QuestionGroupContent

- Owned child của QuestionGroup.
- Type: `TEXT`, `RICH_TEXT`, `IMAGE`, `AUDIO`.
- Text/rich-text/media payload tuân theo type và có display order.

### Question

- Standalone aggregate khi `groupId` null; owned child khi có group.
- Có Part, prompt, explanation, difficulty và status.
- Sở hữu 3+ AnswerOption tùy Part và liên kết nhiều KnowledgeTag.

### AnswerOption

- Owned child của Question.
- Có label, text và/hoặc media, correct flag và display order.
- Published question có đúng một correct option.

## Learning Content aggregates

### Lesson

- Aggregate root với title, slug, type, description, difficulty và status.
- Type: `GRAMMAR`, `VOCABULARY`, `LISTENING`, `READING`.
- Sở hữu ordered LessonBlock, LessonPracticeItem; liên kết tags và vocabularies.

### LessonBlock

- Type tối thiểu: `HEADING`, `RICH_TEXT`, `IMAGE`, `AUDIO`.
- Rich text dùng versioned JSONB schema; media block dùng ACTIVE media.

### LessonPracticeItem

- Ordered reference đến đúng một standalone Question hoặc một QuestionGroup.
- Không flatten group thành các question copies.

### Vocabulary

- Resource tái sử dụng: word, phonetic, part of speech, meaning, example, pronunciation media và status.
- LessonVocabulary giữ display order trong lesson.

## Learning Path aggregate

### LearningPath

- Root có title, slug, description, target score, starting level và status.
- Sở hữu ordered LearningModule.

### LearningModule

- Owned child của path, có title/description/order.
- Sở hữu ordered ModuleLesson links.

### ModuleLesson

- Tham chiếu reusable Lesson; một lesson có thể dùng ở nhiều module/path.

## Test Management aggregate

### ToeicTest

- Root có title, description, type, status và optional blueprint code.
- Type: `PART_PRACTICE`, `MINI_TEST`, `FULL_TEST`.
- Sở hữu ordered TestSection.

### TestSection

- Có skill, optional part, duration seconds và display order.
- Sở hữu ordered TestItem.

### TestItem

- Tham chiếu đúng một standalone Question hoặc QuestionGroup.
- Group luôn được lấy toàn bộ shared content và ordered questions.

## Aggregate dependency graph

```text
Identity <- audit ownership
Identity <- profile avatar -> Media
Media <- Question Bank <- Learning Content <- Learning Path
Taxonomy <- Question Bank <- Learning Content
Question Bank <- Test Management
Question Bank <- Learning Content
```

## Mutation ownership

- Controller chỉ nhận/validate transport DTO và gọi service.
- Service sở hữu transaction, state transition, authorization business rule và relation loading.
- Mapper chỉ chuyển đổi cấu trúc; không query repository hoặc xử lý credential/token.
- Repository chỉ truy cập persistence.
- Publication validator không mutate dữ liệu; service publish sau khi validator trả không có violation.
