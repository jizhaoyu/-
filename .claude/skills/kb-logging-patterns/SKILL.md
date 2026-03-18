---
name: kb-logging-patterns
description: 当前知识库项目的日志技能。用于 traceId、文档导入、索引构建、Redis 缓存降级、Milvus HTTP 请求、RAG 问答链路的日志设计与评审。
triggers:
  - logging
  - traceId
  - Milvus logs
  - Redis fallback
---

# KB Logging Patterns

## Purpose

该技能面向当前项目的日志实践，而不是通用电商或订单系统。

重点是让日志能定位以下链路：
- 文档上传与索引构建
- 向量写入与删除
- RAG 查询与降级
- Redis 缓存命中/失败
- Milvus HTTP 请求失败
- traceId 贯穿链路

## Project Facts

- 查询链路：`src/main/java/com/knowledge/agent/service/impl/KnowledgeQueryServiceImpl.java`
- 索引链路：`src/main/java/com/knowledge/agent/service/impl/DocumentIndexServiceImpl.java`
- 向量存储：`src/main/java/com/knowledge/agent/service/impl/MilvusVectorStoreService.java`
- traceId 工具：`src/main/java/com/knowledge/agent/util/TraceIdUtil.java`（若存在调用，以实际代码为准）

## Logging Goals

### 1. Traceability
- 同一个查询或索引动作要能通过 `traceId` 串起来
- 关键事件应携带 `documentId`、`kbId`、`chunk count`、`duration` 等上下文

### 2. Degradation visibility
- Redis 不可用、LLM 不可用、Milvus 请求失败时，日志应能说明发生在哪一层
- 降级日志优先 `warn` 或 `debug`，避免正常降级刷满 `error`

### 3. Boundary logging
- 在 service 边界、外部 HTTP 调用边界、异步索引边界记录关键日志
- 不在每一层重复打印同一个异常

## Recommended Events

### Document ingest / index
- 索引开始：documentId、traceId
- 解析完成：文档类型、切块数
- 向量写入完成：chunk 数量、collection 名称
- 索引失败：documentId、traceId、失败阶段

### Query
- 查询开始：kbId、topK、traceId
- 缓存命中/未命中：cacheKey 或关键摘要
- 召回完成：retrieved chunk 数、过滤条件
- LLM 降级：失败原因摘要，不泄露敏感配置
- 查询完成：latencyMs、traceId

### External integrations
- Milvus：请求动作、collection、文档 id、失败 code/message
- AI：区分 embedding 与 chat 调用失败
- Redis：读失败与写失败分开记录

## Log Level Guidance

- `error`：真正导致本次请求或索引失败的异常
- `warn`：已处理的业务失败、降级、外部服务不可用但有替代路径
- `info`：关键业务节点与完成事件
- `debug`：缓存读写失败、序列化细节、排查型上下文

## Review Checklist

- [ ] 是否带了 traceId / documentId / kbId 等关键上下文
- [ ] 是否把同一个异常重复记录多次
- [ ] 是否区分了失败与已处理降级
- [ ] 是否避免记录 token、apiKey、原始敏感内容
- [ ] 是否能从日志看出 RAG 卡在 parser / chunker / embedding / vector / llm 哪一层

## Anti-patterns

- `System.out.println`
- 只打 “error occurred” 这类无上下文日志
- 把完整 token / key / 请求密文写进日志
- Redis 失败直接打 error 但链路其实已降级成功
- 同一异常在 service、controller、全局异常处理各打一次 full stack

## Related Local Skills

- `kb-spring-boot-rag-patterns`
- `kb-java-code-review`
- `kb-test-quality`
