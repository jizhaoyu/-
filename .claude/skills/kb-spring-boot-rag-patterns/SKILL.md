---
name: kb-spring-boot-rag-patterns
description: 当前知识库项目的后端主技能。用于 Spring Boot 3.5 + Java 17 + Maven + MyBatis-Plus + Redis + Milvus + RAG 场景下的控制层、服务层、配置、异常处理与检索链路实现/评审。
triggers:
  - knowledge-base-agent 后端
  - Spring Boot + MyBatis-Plus
  - RAG
  - Milvus
  - Redis
---

# KB Spring Boot RAG Patterns

## Purpose

这是当前仓库的后端主技能。

它吸收了全局 `backend-design` 与上游 `spring-boot-patterns` 的通用价值，但所有建议都必须以当前项目事实为准，而不是通用示例工程：

- 统一响应：`src/main/java/com/knowledge/agent/common/Result.java`
- 分页响应：`src/main/java/com/knowledge/agent/common/PageResult.java`
- 错误码：`src/main/java/com/knowledge/agent/common/ErrorCodeEnum.java`
- 业务异常：`src/main/java/com/knowledge/agent/exception/ServiceException.java`
- 全局异常：`src/main/java/com/knowledge/agent/exception/GlobalExceptionHandler.java`
- 分层边界：`controller` / `service` / `service/impl` / `mapper` / `entity/dto` / `entity/vo` / `entity/po`
- 基础设施：MyBatis-Plus、可选 Redis、Milvus HTTP、RAG 查询链路

## When to Use

- 新增或修改 Controller / Service / Mapper / Config
- 设计知识库、文档导入、向量检索、问答链路
- 审查后端代码是否符合本仓库分层
- 需要决定 Redis、Milvus、降级链路如何接入

## Project Facts You Must Reuse

### 1. Controller contract
- Controller 只负责参数接收、校验、调用 service、返回 `Result<T>`
- 参考：`src/main/java/com/knowledge/agent/controller/KnowledgeBaseController.java`
- 使用 `@Validated`、`@Valid`、`@Tag`、`@Operation`
- 不要在 Controller 内直接写持久化、向量检索、模型调用逻辑

### 2. Object boundaries
- `entity.dto`：接收输入，带 `@Schema` 和校验注解
- `entity.vo`：对外输出
- `entity.po`：数据库持久化对象
- 禁止把 `PO` 直接返回给前端

### 3. Persistence boundary
- 持久层使用 `mapper` + MyBatis-Plus
- 简单 CRUD 走 `BaseMapper`
- 复杂 SQL 放 XML 或明确的数据访问层，不在业务代码中拼接 SQL
- 参考：`src/main/java/com/knowledge/agent/mapper/KnowledgeDocumentMapper.java`

### 4. Error semantics
- 业务失败抛 `ServiceException`
- 统一由 `GlobalExceptionHandler` 转换为 `Result.fail(...)`
- 不要在 service 返回 `null` 表达业务失败

### 5. Runtime configuration
- MyBatis-Plus：`src/main/java/com/knowledge/agent/config/MybatisPlusConfig.java`
- AI：`src/main/java/com/knowledge/agent/config/AiProperties.java`
- Milvus：`src/main/java/com/knowledge/agent/config/MilvusProperties.java`
- RAG：`src/main/java/com/knowledge/agent/config/RagProperties.java`
- 默认配置：`src/main/resources/application.yml`

## Layering Rules

### Controller
- 返回类型统一为 `Result<T>`
- 列表型接口优先返回 `Result<PageResult<VO>>` 或 `Result<List<VO>>`
- 参数校验失败由全局异常处理，不在 Controller 手写重复兜底
- 允许做少量入参语义校验，但不要承载主业务流程

### Service / ServiceImpl
- 接口定义在 `service`
- 实现放在 `service/impl`
- 事务、降级、编排逻辑放在实现层
- 复杂业务编排优先放在 service，不要下沉到 controller 或 mapper

### Mapper
- 只做数据访问
- 不承载问答编排、缓存降级、文件解析等业务逻辑

### DTO / VO / PO
- DTO 只描述输入契约
- VO 只描述输出契约
- PO 只描述持久化数据
- 若字段需要转换，显式做映射，不偷懒复用同一个对象跨层流动

## RAG Pipeline Rules

当前项目默认按以下边界理解 RAG：

1. parser：文档解析
2. chunker：文本切块
3. embedding：向量生成
4. vector store：Milvus 或内存实现
5. llm：基于召回上下文生成答案
6. query service：拼装 traceId、缓存、检索、降级响应

对应事实来源：
- `src/main/java/com/knowledge/agent/service/impl/DocumentIndexServiceImpl.java`
- `src/main/java/com/knowledge/agent/service/impl/KnowledgeQueryServiceImpl.java`
- `src/main/java/com/knowledge/agent/service/impl/OpenAiEmbeddingService.java`
- `src/main/java/com/knowledge/agent/service/impl/OpenAiLlmService.java`
- `src/main/java/com/knowledge/agent/service/impl/MilvusVectorStoreService.java`
- `src/main/java/com/knowledge/agent/service/impl/InMemoryVectorStoreService.java`

### RAG-specific guidance
- 问答服务要能处理“无召回结果”场景
- LLM 不可用时允许按现有模式返回降级答案
- Redis 是可选加速层，不应成为主链路硬依赖
- Milvus 不可用时要尊重当前项目的启用开关与替代实现边界

## Configuration Rules

- 自定义配置优先使用 `@ConfigurationProperties`
- 配置命名遵循 `application.yml` 现有结构，不另起平行命名体系
- 新配置若涉及模型、向量库、缓存，必须说明默认值、开关方式、缺省行为

## Implementation Checklist

在本仓库写后端代码时，至少检查：

- [ ] Controller 是否只做接口层职责
- [ ] 返回是否统一为 `Result<T>`
- [ ] DTO / VO / PO 边界是否清晰
- [ ] service 是否承担了业务编排
- [ ] mapper 是否只做数据访问
- [ ] 业务错误是否统一走 `ServiceException`
- [ ] 是否复用了已有配置类和错误码
- [ ] Redis / Milvus / LLM 不可用时是否符合当前降级约定
- [ ] 是否避免修改 `src/main/**` 之外不必要的公共规则文件

## Anti-patterns

- 直接把数据库对象返回给接口层
- 在 Controller 中直接调用 mapper
- 在 mapper 中拼接业务分支或外部调用逻辑
- 新造一套与 `Result` / `ErrorCodeEnum` 平行的错误体系
- 无视 `application.yml` 现有配置语义重新命名键
- 把 Redis 或 Milvus 当成永远可用的强依赖

## Related Local Skills

- `mybatis-plus-patterns`
- `kb-api-contract-review`
- `kb-test-quality`
- `kb-logging-patterns`
- `kb-java-code-review`
- `dual-collab`
