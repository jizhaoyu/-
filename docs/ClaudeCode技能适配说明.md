# ClaudeCode 技能适配说明

## 1. 目的

本说明文档用于记录：

- 上游参考仓库 `decebals/claude-code-java` 在当前项目中的保留方式
- 上游 skills 到本地 skills 的映射关系
- 为什么某些上游 skill 只保留副本、不直接启用
- 当前项目技能应以哪些项目事实为准

> 结论先行：当前项目 `docs/` 与项目根 `CLAUDE.md` 才是最终规范来源（source of truth）。
> 上游仓库只作为参考，不替代当前项目规范。

---

## 2. 上游副本保留策略

上游仓库内容保留在：

- `external/claude-code-java`

保留原则：

- 保留 `.claude/skills`
- 保留 `docs/`
- 保留 `templates/`
- 保留 `scripts/`
- 不把上游目录平铺到当前项目根目录

这样做是为了：

1. 避免覆盖或混淆当前仓库已有 `.claude/`、`docs/`、脚本语义
2. 方便后续与上游做比对
3. 满足“拉到当前项目根目录下并保留副本”的要求

---

## 3. 当前项目的本地 skill 映射

| 上游 skill / 来源 | 当前处理方式 | 本地 skill |
| --- | --- | --- |
| `spring-boot-patterns` + 全局 `backend-design` | 吸收并重写 | `kb-spring-boot-rag-patterns` |
| `jpa-patterns` | 不直接采用，改写为 MyBatis-Plus 版 | `mybatis-plus-patterns` |
| `api-contract-review` | 按项目接口约定本地化 | `kb-api-contract-review` |
| `test-quality` + 全局 `backend-design` 测试偏好 | 按项目测试风格本地化 | `kb-test-quality` |
| `logging-patterns` | 按 traceId / Redis / Milvus / RAG 场景本地化 | `kb-logging-patterns` |
| `java-code-review` | 按本项目分层与降级规则本地化 | `kb-java-code-review` |

---

## 4. 为什么不直接启用 JPA 相关技能

当前项目不是通用 JPA / Spring Data JPA 示例，而是：

- Spring Boot 3.5
- Java 17
- Maven
- MyBatis-Plus
- Redis
- Milvus HTTP
- RAG 查询链路

因此，上游以下主张不适合作为当前项目默认建议：

- `JpaRepository`
- `@EntityGraph`
- `LazyInitializationException`
- JPA Entity 关系建模作为主要持久层策略
- Open Session in View 等 JPA 语境问题

这些内容保留在 `external/claude-code-java` 中，仅作参考，不作为当前仓库正式技能。

---

## 5. 当前项目技能必须复用的事实来源

### 协作规范
- `CLAUDE.md`
- `docs/双端协同统一手册.md`

### API / 错误语义
- `src/main/java/com/knowledge/agent/common/Result.java`
- `src/main/java/com/knowledge/agent/common/PageResult.java`
- `src/main/java/com/knowledge/agent/common/ErrorCodeEnum.java`
- `src/main/java/com/knowledge/agent/exception/ServiceException.java`
- `src/main/java/com/knowledge/agent/exception/GlobalExceptionHandler.java`

### 分层与对象边界
- `src/main/java/com/knowledge/agent/controller/KnowledgeBaseController.java`
- `src/main/java/com/knowledge/agent/entity/dto/*`
- `src/main/java/com/knowledge/agent/entity/vo/*`
- `src/main/java/com/knowledge/agent/entity/po/*`
- `src/main/java/com/knowledge/agent/mapper/*`
- `src/main/java/com/knowledge/agent/service/*`
- `src/main/java/com/knowledge/agent/service/impl/*`

### 基础设施与运行时
- `src/main/java/com/knowledge/agent/config/MybatisPlusConfig.java`
- `src/main/java/com/knowledge/agent/config/AiProperties.java`
- `src/main/java/com/knowledge/agent/config/MilvusProperties.java`
- `src/main/java/com/knowledge/agent/config/RagProperties.java`
- `src/main/resources/application.yml`

### RAG / Redis / Milvus
- `src/main/java/com/knowledge/agent/service/impl/KnowledgeQueryServiceImpl.java`
- `src/main/java/com/knowledge/agent/service/impl/DocumentIndexServiceImpl.java`
- `src/main/java/com/knowledge/agent/service/impl/MilvusVectorStoreService.java`
- `src/main/java/com/knowledge/agent/service/impl/InMemoryVectorStoreService.java`
- `src/main/java/com/knowledge/agent/service/impl/OpenAiEmbeddingService.java`
- `src/main/java/com/knowledge/agent/service/impl/OpenAiLlmService.java`

### 测试风格
- `src/test/java/com/knowledge/agent/controller/KnowledgeBaseControllerTest.java`
- `src/test/java/com/knowledge/agent/service/KnowledgeQueryServiceImplTest.java`
- `src/test/java/com/knowledge/agent/exception/GlobalExceptionHandlerTest.java`
- `src/test/resources/application-test.yml`

---

## 6. 只保留上游副本、不直接本地化的技能

以下技能当前仅保留在 `external/claude-code-java` 中作为参考：

- `clean-code`
- `design-patterns`
- `solid-principles`
- `concurrency-review`
- `performance-smell-detection`
- `java-migration`
- `git-commit`
- `issue-triage`
- `changelog-generator`
- `architecture-review`
- `maven-dependency-audit`
- `security-audit`

原因：

- 这些技能大多是跨项目通用能力
- 本轮目标是建立与当前项目后端实现直接相关的本地技能
- 先避免技能数量膨胀和重复维护

---

## 7. scripts / templates / docs 的使用原则

### 上游 scripts
- 仅保留副本，不直接作为当前项目正式脚本使用
- 特别是会修改用户级 Claude 配置的脚本，不直接启用
- 后续若需要，优先新增当前项目本地脚本，而不是直接引用上游 Bash 脚本

### 上游 templates
- 仅作模板参考
- 不直接替代当前项目 `CLAUDE.md`、`docs/` 中的规则文档

### 上游 docs
- 仅作技能编写参考
- 当前项目的最终规范仍由本仓库 `docs/` 负责维护

---

## 8. 与 dual-collab 的关系

`dual-collab` 负责：

- 冻结边界
- 分配职责
- 生成提示词
- 输出交接信息与验收清单

新增本地后端技能负责：

- 在已冻结边界下，给出当前项目可执行的后端实现、评审、测试、日志与持久层建议

因此，建议配合方式为：

1. 先用 `dual-collab` 冻结边界
2. 再按任务类型使用：
   - `kb-spring-boot-rag-patterns`
   - `mybatis-plus-patterns`
   - `kb-api-contract-review`
   - `kb-test-quality`
   - `kb-logging-patterns`
   - `kb-java-code-review`

---

## 9. 当前状态说明

本次适配已完成以下落地：

- 保留上游参考副本到 `external/claude-code-java`
- 在 `.claude/skills` 新增本地技能：
  - `kb-spring-boot-rag-patterns`
  - `mybatis-plus-patterns`
  - `kb-api-contract-review`
  - `kb-test-quality`
  - `kb-logging-patterns`
  - `kb-java-code-review`

后续若需要扩展更多本地 skill，应继续遵守：

- 先看项目 `docs/`
- 再看当前代码事实
- 最后决定是否吸收上游通用技能
