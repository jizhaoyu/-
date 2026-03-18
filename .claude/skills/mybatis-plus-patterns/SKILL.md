---
name: mybatis-plus-patterns
description: 当前知识库项目的 MyBatis-Plus 数据访问技能。用于 BaseMapper、分页、条件查询、XML 复杂 SQL、安全边界，以及与 Redis / Milvus / RAG 一致性的实现与评审。
triggers:
  - MyBatis-Plus
  - BaseMapper
  - LambdaQueryWrapper
  - mapper
---

# MyBatis-Plus Patterns

## Purpose

该技能替代上游面向另一类持久层的通用数据访问建议，统一为当前仓库的 MyBatis-Plus 实践。

核心事实：
- `mapper` 继承 `BaseMapper`
- 分页能力来自 `MybatisPlusConfig`
- 复杂 SQL 不在 Java 字符串里拼接
- 数据层服务于知识库文档、检索元数据与索引状态，不直接承担问答编排

## When to Use

- 新增或修改 `mapper`
- 实现条件查询、分页查询、存在性校验
- 评审查询是否安全、是否越层
- 处理文档删除、重建索引、状态更新的一致性问题

## Project Facts

- Mapper 示例：`src/main/java/com/knowledge/agent/mapper/KnowledgeDocumentMapper.java`
- 分页配置：`src/main/java/com/knowledge/agent/config/MybatisPlusConfig.java`
- PO 示例：`src/main/java/com/knowledge/agent/entity/po/KnowledgeDocument.java`
- 文档索引编排：`src/main/java/com/knowledge/agent/service/impl/DocumentIndexServiceImpl.java`

## Core Patterns

### 1. Base CRUD
- 简单单表查询优先使用 `BaseMapper`
- 常用方法：`selectById`、`insert`、`updateById`、`deleteById`
- 查询结果先回到 service，再映射为 VO

### 2. Conditional query
- 条件查询优先使用 `LambdaQueryWrapper`
- 只追加明确、可预测的条件
- 空条件要显式忽略，不拼接含糊的兜底逻辑

### 3. Pagination
- 分页查询优先走 MyBatis-Plus 分页能力
- 当前项目最大分页上限来自 `MybatisPlusConfig` 中的 `setMaxLimit(50L)`
- 对外输出时转成 `PageResult<T>`，不要把框架分页对象直接暴露给接口层

### 4. Complex SQL placement
- 复杂查询、聚合、联表、批量更新：放 XML 或独立清晰的数据访问层
- 禁止在 Java 代码中用字符串拼接 SQL
- 若查询已超出单一 mapper 职责，先回到 service 重新拆边界

## Safety Rules

- 不把前端原始字段直接拼成查询条件
- 对排序字段、过滤字段做白名单约束
- 对文档 id、kbId 这类关键条件保持显式校验
- 只在 service 中做业务存在性判断与错误码映射，不在 mapper 中抛业务语义异常

## Consistency Rules with Redis / Milvus

本项目的数据一致性不是只有数据库：

- 文档删除：数据库记录、缓存、向量索引都要考虑影响面
- 文档重建索引：先确认文档状态，再处理向量删除/重建
- 查询缓存：缓存 key 与过滤条件要稳定、可复现

评审时重点检查：
- 数据库状态变更后，缓存是否需要失效或绕过
- 文档删除后，向量存储是否同步清理
- 查询条件标准化后，缓存键是否一致

## Query Review Checklist

- [ ] mapper 是否只做数据访问
- [ ] 查询是否使用了明确字段而不是模糊拼接
- [ ] 分页是否受最大限制保护
- [ ] 复杂 SQL 是否放在 XML
- [ ] service 是否负责业务异常转换
- [ ] 输出是否映射为 VO / PageResult
- [ ] 是否考虑了 Redis 缓存与向量索引的一致性影响

## Anti-patterns

- 直接在 controller 中构造 `LambdaQueryWrapper`
- 在 service 中拼接原始 SQL
- 把 MyBatis-Plus Page 对象直接作为接口返回体
- 以“查不到”返回 `null` 代替业务异常
- 数据库删了，缓存和向量索引完全不管

## Related Local Skills

- `kb-spring-boot-rag-patterns`
- `kb-api-contract-review`
- `kb-java-code-review`
