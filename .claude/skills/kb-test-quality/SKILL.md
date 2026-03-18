---
name: kb-test-quality
description: 当前知识库项目的测试技能。用于 JUnit 5 + AssertJ + Mockito + MockMvc 场景下的 Controller、Service、配置、RAG、Redis、Milvus 降级路径测试设计与评审。
triggers:
  - add tests
  - controller test
  - service test
  - RAG test
---

# KB Test Quality

## Purpose

该技能用于为当前项目编写或评审测试，不套用通用示例测试结构，而优先复用仓库里已经存在的测试模式。

## Existing Project Patterns

### Controller tests
- 参考：`src/test/java/com/knowledge/agent/controller/KnowledgeBaseControllerTest.java`
- 使用 `MockMvcBuilders.standaloneSetup(...)`
- 显式挂载 `GlobalExceptionHandler`
- 对统一返回结构断言 `code` / `message` / `data`

### Service tests
- 参考：`src/test/java/com/knowledge/agent/service/KnowledgeQueryServiceImplTest.java`
- 使用 Mockito、伪实现、轻量固定桩对象
- 优先验证主链路与降级路径

### Exception tests
- 参考：`src/test/java/com/knowledge/agent/exception/GlobalExceptionHandlerTest.java`
- 验证统一异常转换是否稳定

### Config tests
- 参考：`src/test/resources/application-test.yml`
- 测试环境默认关闭 Milvus，使用测试模型与测试 key

## What to Test in This Project

### 1. Controller
- 正常响应是否返回 `Result.success`
- 参数非法时是否返回项目既定错误码和消息
- DTO 校验是否真正生效
- 删除、查询、重建索引等接口是否维持统一结构

### 2. Service
- 业务主流程是否正确
- 业务错误是否抛 `ServiceException`
- 降级逻辑是否符合当前实现
- 缓存可用与不可用两种路径是否都覆盖

### 3. RAG pipeline
- 无召回结果时的响应
- LLM 不可用时的降级回答
- metadata filter 非法时的失败路径
- 向量检索结果被截断到 `maxContextChunks` 的行为

### 4. Redis
- 无 Redis Bean 时是否仍可工作
- 缓存读失败是否退回主链路
- 缓存写失败是否不影响主结果

### 5. Milvus / Vector store
- 启用外部向量库时的调用边界
- 关闭时是否走内存实现
- 文档删除 / 重建索引时，向量侧行为是否与数据库状态一致

## Test Style Rules

- 使用 JUnit 5
- 使用 AssertJ
- 使用 `@DisplayName`
- 一个测试只验证一个核心行为
- Happy path 和异常路径至少各覆盖一个
- 不为简单 Lombok 数据类堆砌无价值测试

## Assertion Focus

优先断言：
- `Result.code`
- `Result.message`
- 关键 `data` 字段
- 异常消息是否与项目约定一致
- 降级结果是否包含可识别语义

## Suggested Test Matrix

- [ ] Controller 正常场景
- [ ] Controller 参数非法场景
- [ ] Service 正常场景
- [ ] Service 业务异常场景
- [ ] RAG 无召回场景
- [ ] LLM 降级场景
- [ ] Redis 缺失或失败场景
- [ ] 向量检索实现切换场景

## Anti-patterns

- 只测 happy path
- 断言过多无关字段导致测试脆弱
- 测试框架内部细节而不是业务行为
- 忽略 `Result` 包装，直接断言裸对象结构
- 在测试里复写一套与生产不一致的错误语义

## Related Local Skills

- `kb-spring-boot-rag-patterns`
- `kb-api-contract-review`
- `kb-java-code-review`
