---
name: kb-api-contract-review
description: 审查当前知识库项目的接口契约。重点检查 Result<T> 包装、ErrorCodeEnum、一致的 DTO/VO 边界、Swagger 注解、分页结构，以及本项目既有错误语义。
triggers:
  - review controller
  - API contract
  - Result<T>
  - Swagger
---

# KB API Contract Review

## Purpose

该技能用于审查当前仓库接口契约是否与项目现有约定保持一致。

注意：本项目已经冻结了自己的接口语义，评审时必须以项目事实为准，而不是套用别的 REST 风格模板。

## Project Contract Baseline

### Response envelope
- 所有接口返回 `Result<T>`
- 分页场景使用 `Result<PageResult<VO>>`
- 成功：`Result.success(data)`
- 失败：通过 `ServiceException` + `GlobalExceptionHandler` 返回 `Result.fail(...)`

### Error semantics
- 错误码来源：`ErrorCodeEnum`
- 异常出口：`GlobalExceptionHandler`
- 参数校验失败与业务失败都回到统一响应结构

### Type boundaries
- 入参：DTO
- 出参：VO
- 持久化：PO
- 禁止把 PO 直接作为 API 输出

### Documentation annotations
- Controller 类使用 `@Tag`
- 方法使用 `@Operation`
- DTO 字段使用 `@Schema`

## When to Use

- 审查 controller 改动
- 审查接口新增、分页、删除、查询、重建索引接口
- 发布前检查接口是否偏离项目现有约定

## Review Focus

### 1. Response consistency
- [ ] 是否统一返回 `Result<T>`
- [ ] 分页是否统一为 `PageResult`
- [ ] 是否复用了 `Result.success` / `Result.fail`
- [ ] 是否绕开统一响应直接返回裸对象

### 2. Error code consistency
- [ ] 是否复用了 `ErrorCodeEnum`
- [ ] 是否使用 `ServiceException` 表达业务失败
- [ ] 是否新增了不必要的平行错误结构
- [ ] 错误消息是否与现有语义一致

### 3. DTO / VO boundary
- [ ] 输入是否使用 DTO
- [ ] 输出是否使用 VO
- [ ] 是否出现直接返回 PO 的情况
- [ ] DTO 是否带校验注解与 `@Schema`

### 4. Controller boundary
- [ ] Controller 是否只做参数接收、校验、调用 service、返回结果
- [ ] 是否把业务判断、数据访问、向量检索直接塞进 controller

### 5. Interface shape
- [ ] 路径命名是否与现有 `/api/kb/**` 风格一致
- [ ] 查询、删除、重建索引等动作是否与现有接口风格一致
- [ ] 分页参数、过滤参数是否清晰可验证

## Output Format

```markdown
## Findings
- [severity] file:line - issue

## Open Questions
- question

## Suggested Checks
- check item
```

## Review Hints for This Project

优先检查这些文件风格是否被复用：
- `src/main/java/com/knowledge/agent/controller/KnowledgeBaseController.java`
- `src/main/java/com/knowledge/agent/common/Result.java`
- `src/main/java/com/knowledge/agent/common/PageResult.java`
- `src/main/java/com/knowledge/agent/exception/GlobalExceptionHandler.java`

## Anti-patterns

- 返回裸 `VO`、`PO`、`List`、`Map`
- 新建另一个 `ApiResponse` / `Response` 包装类
- 在 Controller 中手写大量 try/catch 代替统一异常处理
- DTO 无校验、无 `@Schema`
- 分页接口直接暴露框架内部对象

## Related Local Skills

- `kb-spring-boot-rag-patterns`
- `kb-java-code-review`
- `dual-collab`
