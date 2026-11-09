# Mapping And DTO Rules

## DTO placement

- Keep request DTOs under `dto.request`.
- Keep response DTOs under `dto.response`.
- Keep shared response envelopes such as `ApiResponse` in `dto.response` or another shared/common DTO package.
- Do not place response wrappers inside `dto.request`.

## Mapper style

- Use MapStruct with `@Mapper(componentModel = "spring")`.
- Keep mapper interfaces small and explicit.
- Use `@Mapping(target = "...", ignore = true)` where service code must fill the field later.
- Use dedicated mapper methods for create, update, and response conversion when needed.

## Responsibility split

- Mapper handles structure conversion.
- Service handles business-dependent transformation:
  - encoding password through injected `PasswordEncoder`
  - assigning default roles
  - loading related entities
  - building security-sensitive values

## Avoid

- Do not do repetitive field-by-field conversion directly inside controller methods.
- Do not put repository lookups inside mappers.
- Do not hide password encoding, token creation, or other security logic inside mappers.
