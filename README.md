# 知识库 Agent

一个基于 `Java 17 + Spring Boot 3` 的轻量知识库问答示例项目，内置文档上传、向量化索引、检索问答、运行态探测和静态管理页面，适合做本地 RAG 原型、接口联调和后端集成演示。

## 项目能力

- 上传 `txt`、`md`、`pdf` 文档并触发索引
- 支持 `IN_MEMORY` 与 `MILVUS` 两种向量存储模式
- 基于 `embedding + 检索 + LLM` 完成知识库问答
- 支持文档详情页与正文查看
- 提供运行态检查接口，展示 `MySQL`、`Redis`、`Milvus`、`AI Provider` 状态
- 当知识未命中时，可降级为普通 AI 聊天
- 当模型暂时不可用时，返回可人工核对的引用片段

## 技术栈

- `Java 17`
- `Spring Boot 3.5.10`
- `MyBatis-Plus`
- `MySQL 8`
- `Redis`
- `Milvus 2.4`
- `SpringDoc + Knife4j`
- `Hutool`
- `Apache PDFBox`

## 目录结构

```text
.
├─ src/main/java            后端业务代码
├─ src/main/resources
│  ├─ application.yml       主配置
│  ├─ db/schema.sql         初始化表结构
│  └─ static                内置前端页面与静态资源
├─ src/test                 单元测试与接口测试
├─ docs                     协作文档、环境准备、执行清单
├─ docker-compose.yml       Milvus 依赖编排
└─ README.md                当前说明文档
```

## 快速开始

### 1. 环境准备

请先准备以下环境：

- `JDK 17`
- `Maven 3.9+`
- 本机 `MySQL 8`
- 本机 `Redis`
- `Docker Desktop`，用于启动 Milvus 依赖

### 2. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS kb_agent
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 3. 配置 AI Key

至少需要配置：

- `AI_CHAT_API_KEY`
- `AI_EMBEDDING_API_KEY`

PowerShell 示例：

```powershell
$env:AI_CHAT_API_KEY="your-chat-key"
$env:AI_EMBEDDING_API_KEY="your-embedding-key"
```

按需覆盖的可选项：

```powershell
$env:AI_CHAT_BASE_URL="https://api.moonshot.cn/v1"
$env:AI_CHAT_MODEL="kimi-k2.5"
$env:AI_EMBEDDING_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:AI_EMBEDDING_MODEL="text-embedding-v2"
```

### 4. 启动最小可运行模式

先用内存向量库跑通整条主链路：

```powershell
mvn -DskipTests spring-boot:run "-Dspring-boot.run.arguments=--app.milvus.enabled=false"
```

启动后访问：

- 首页：`http://localhost:8080/`
- 运行态：`http://localhost:8080/api/kb/runtime`
- Knife4j：`http://localhost:8080/doc.html`

### 5. 启动 Milvus 真实模式

先拉起依赖服务：

```powershell
docker compose up -d
```

再启动应用：

```powershell
mvn -DskipTests spring-boot:run
```

默认 `local` 配置会启用 Milvus，此时运行态中应看到：

- `vectorStoreMode = MILVUS`
- `Milvus = UP`

## 运行模式说明

### IN_MEMORY

适合本地调试和最小验证：

- 不依赖 Milvus
- 向量仅保存在当前进程内存中
- 服务重启后索引数据会丢失

### MILVUS

适合真实检索与联调验证：

- 使用 `docker-compose.yml` 中的 `etcd + minio + milvus`
- 向量持久化到 Milvus
- 更接近正式 RAG 链路

## 默认配置说明

主配置位于 [src/main/resources/application.yml](src/main/resources/application.yml)。

默认约定：

- MySQL：`localhost:3306/kb_agent`
- Redis：`localhost:6379`
- Chat Model：`kimi-k2.5`
- Embedding Model：`text-embedding-v2`
- Milvus：`http://localhost:19530`

注意事项：

- `application-local.yml` 仅用于本地调试，不应提交明文密钥
- 当前 `.gitignore` 已忽略本地密钥文件、日志和 Docker 数据目录

## 核心接口

- `GET /api/kb/runtime`：查看运行态摘要
- `POST /api/kb/documents`：上传文档并触发索引
- `GET /api/kb/documents`：分页查询文档
- `GET /api/kb/documents/{id}`：查看文档详情
- `GET /api/kb/documents/{id}/content`：读取文档正文
- `POST /api/kb/query`：执行知识库问答
- `POST /api/kb/reindex/{id}`：重建单个文档索引

页面路由：

- `/`：首页
- `/details-{id}`：文档详情页

## 问答策略

当前问答流程大致为：

1. 对问题生成向量
2. 在向量库中召回相关片段
3. 将问题和上下文交给聊天模型生成答案
4. 如果没有命中知识，降级为普通 AI 聊天
5. 如果模型不可用，返回引用片段供人工核对

## 常见问题

### 文档一直处于 `FAILED`

优先检查：

- `AI_CHAT_API_KEY`
- `AI_EMBEDDING_API_KEY`
- Embedding 服务地址与模型名

### 运行态里 Redis 是 `WARN`

表示缓存不可用，但不会阻塞页面和主流程。

### 运行态里 Milvus 是 `DISABLED`

说明当前使用的是内存向量模式，通常是启动参数中显式传了：

```powershell
--app.milvus.enabled=false
```

### 模型服务偶发返回 429

这通常表示上游模型过载，不一定是本地配置错误。检索链路可能仍然成功，只是最终答案生成失败。

## 测试与验证

编译：

```powershell
mvn -q -DskipTests compile
```

测试：

```powershell
mvn test
```

建议优先使用 `JDK 17` 跑测试，避免较新 JDK 与 Mockito agent 的兼容问题。

## 相关文档

- [测试与环境准备](docs/测试与环境准备.md)
- [执行清单](docs/执行清单.md)
- [双端协同统一手册](docs/双端协同统一手册.md)
- [文档详情联调接口清单](docs/文档详情联调接口清单.md)
