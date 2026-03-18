---
name: kb-java-code-review
description: 当前知识库项目的 Java 代码评审技能。重点检查 DTO/VO/PO 边界、Result<T> / ServiceException 约定、Controller/Service/Mapper 分层、Redis/Milvus/RAG 降级语义，以及项目既有规范是否被破坏。
triggers:
  - review Java
  - review PR
  - knowledge-base-agent review
---

# KB Java Code Review

## Purpose

该技能用于审查当前仓库 Java 改动，重点找“项目级不适配”的问题，而不是泛泛而谈的风格建议。

## Review Priorities

### 1. Layer boundaries
- Controller 是否只做接口层职责
- Service 是否承载业务编排
- Mapper 是否只做数据访问
- 是否出现跨层偷跑，例如 controller 直接调 mapper

### 2. DTO / VO / PO correctness
- DTO 是否只用于输入
- VO 是否只用于输出
- PO 是否被错误暴露到接口层
- 是否缺少必要映射导致边界混乱

### 3. API / exception semantics
- 是否统一返回 `Result<T>`
- 是否统一通过 `ServiceException` + `GlobalExceptionHandler` 处理失败
- 是否绕过 `ErrorCodeEnum`
- 是否出现裸 `RuntimeException` 直接向上冒出并改变接口语义

### 4. Persistence and query design
- 是否符合 `mapper + MyBatis-Plus` 模式
- 是否在 Java 中拼 SQL
- 分页与过滤是否受控
- 数据变更后是否考虑缓存/向量侧影响

### 5. Runtime dependency behavior
- Redis 是否被当成可选组件而非硬依赖
- Milvus 开关行为是否被尊重
- LLM / embedding / vector store 失败时是否有符合现有实现的处理方式

### 6. Test gap
- 改动是否缺失主链路测试
- 是否至少覆盖一个异常/降级路径
- 是否复用现有 Controller / Service 测试风格

## Output Format

```markdown
## Findings
- [severity] path:line - finding

## Open Questions
- question

## Suggested Checks
- validation item

## Good Practices Observed
- positive point
```

## Severity Guide

- Critical：会破坏接口契约、数据一致性、错误语义或主链路可用性
- High：明显越层、降级缺失、测试缺口大、配置语义被改坏
- Medium：边界不清、日志上下文不足、局部可维护性差
- Low：小型风格或命名问题

## Local Checklist

- [ ] 遵循项目 `CLAUDE.md` 与 `docs/双端协同统一手册.md`
- [ ] 不越过本轮允许改动范围
- [ ] 不引入 JPA / Repository / EntityGraph 等不适配建议
- [ ] 不破坏当前 `Result<T>` / `ErrorCodeEnum` / `GlobalExceptionHandler` 语义
- [ ] 不把 Redis、Milvus、RAG 依赖写成无降级硬耦合

## Related Local Skills

- `kb-spring-boot-rag-patterns`
- `mybatis-plus-patterns`
- `kb-api-contract-review`
- `kb-test-quality`
- `kb-logging-patterns`
