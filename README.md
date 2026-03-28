# Enterprise Knowledge Assistant

一个基于 **Spring Boot + LangChain4j + Pinecone + MiniMax(Anthropic 协议)** 的企业知识问答系统。

当前项目支持两条主链路：
- 文档上传与向量化入库
- 多轮会话问答（带会话记忆）

## 1. 功能概览

- 文档上传接口：`/api/documents/upload`
  - 上传文本文件
  - 必须传入文档类型：`POLICY` / `TECH_TYPE` / `SOP` / `FAQ`
  - 后端会将文档类型写入文档元数据：`document_type`（值为 `policy/tech_type/sop/faq`）
- 对话接口：`/api/chat/dialogue`
  - 支持 `userId` 维度的多轮会话记忆（MySQL 持久化）
  - 先做向量召回，再拼接上下文调用大模型回答
- 简单前端页面：`src/main/resources/static/index.html`
  - 文档上传面板（含文档类型下拉）
  - 模型对话面板

## 2. 技术栈

- Java 17
- Spring Boot 3.2.6
- LangChain4j 1.3.0
- Pinecone（向量数据库）
- MiniMax（通过 Anthropic 协议接入）
- Hutool DB（会话消息存储）

## 3. 项目结构

```text
src/main/java/org/weihua
├─ Application.java
├─ assistant
│  └─ ChatAssistant.java
├─ config
│  ├─ EmbeddingModelConfig.java
│  ├─ EmbeddingStoreConfig.java
│  └─ MemoryProviderConfig.java
├─ controller
│  ├─ ChatController.java
│  └─ DocumentController.java
├─ model
│  ├─ chat
│  │  ├─ ChatRequest.java
│  │  └─ ChatResponse.java
│  └─ document
│     └─ DocumentType.java
├─ repository
│  ├─ ChatMemoryRepository.java
│  ├─ DocumentRepository.java
│  └─ DocumentRepositoryImpl.java
└─ service
   ├─ ChatService.java
   └─ DocumentService.java
```

## 4. 环境准备

### 4.1 必备软件

- JDK 17+
- Maven 3.9+
- MySQL 8+

### 4.2 环境变量

在系统环境变量中配置：

- `MINIMAX_API_KEY`：MiniMax API Key
- `PINECONE_API_KEY`：Pinecone API Key

### 4.3 应用配置

`src/main/resources/application.properties`：

```properties
service.port=8080

langchain4j.anthropic.chat-model.api-key=${MINIMAX_API_KEY}
langchain4j.anthropic.chat-model.base-url=https://api.minimaxi.com/anthropic/v1
langchain4j.anthropic.chat-model.model-name=MiniMax-M2.7

pine.cone.api.key=${PINECONE_API_KEY}
```

说明：Spring Boot 常用端口配置键是 `server.port`。如果你发现端口没有生效，请把 `service.port` 改成 `server.port`。

### 4.4 数据库配置

项目使用 `src/main/resources/db.setting`（Hutool DB）连接 MySQL，默认：

```properties
url=jdbc:mysql://localhost:3306/langchain4j
user=root
pass=root
```

请提前创建数据库与会话表（示例）：

```sql
CREATE DATABASE IF NOT EXISTS langchain4j DEFAULT CHARACTER SET utf8mb4;

USE langchain4j;

CREATE TABLE IF NOT EXISTS chat_msg (
  uid VARCHAR(128) PRIMARY KEY,
  message LONGTEXT NOT NULL
);
```

## 5. 启动项目

在项目根目录执行：

```bash
mvn spring-boot:run
```

启动后访问：
- 前端页面：`http://localhost:8080/`

## 6. 接口说明

### 6.1 上传文档

- Method: `POST`
- URL: `/api/documents/upload`
- Content-Type: `multipart/form-data`
- 参数：
  - `file`：上传文件
  - `docType`：`POLICY` / `TECH_TYPE` / `SOP` / `FAQ`

示例：

```bash
curl -X POST "http://localhost:8080/api/documents/upload" \
  -F "file=@./demo.txt" \
  -F "docType=POLICY"
```

示例响应：

```json
{
  "message": "Document uploaded and ingested successfully",
  "fileName": "demo.txt",
  "docType": "POLICY"
}
```

### 6.2 模型对话

- Method: `POST`
- URL: `/api/chat/dialogue`
- Content-Type: `application/json`

请求体：

```json
{
  "userId": "666",
  "message": "请总结一下请假流程"
}
```

响应体：

```json
{
  "answer": "..."
}
```

## 7. 核心实现说明（按当前代码）

- 文档入库流程
  - `DocumentController` 接收 `file + docType`
  - `DocumentService` 将 `docType` 转成元数据字段：`document_type=documentType.getDesc()`
  - 使用 `Document.from(text, Metadata.from(...))` 构建文档
  - `DocumentRepositoryImpl` 负责切分（`recursive(100,30)`）+ embedding + 写入 Pinecone

- 向量存储配置
  - `EmbeddingStoreConfig` 当前提供统一 `EmbeddingStore<TextSegment>` Bean（索引 `enterprise-index`，命名空间 `enterprise-namespace`）
  - 当前代码未在 `embeddingStore()` 中按文档类型动态切换 `metadataTextKey`

- 对话流程
  - `ChatService` 先将用户问题 embedding，再在向量库检索（`maxResults=1`, `minScore=0.6`）
  - 取召回片段拼接 prompt 后调用 `ChatAssistant.chat(userId, prompt)`
  - `ChatAssistant` 使用 `@AiService` 显式绑定 `anthropicChatModel` 和 `chatMySqlMemoryProvider`
  - 系统提示词来自 `src/main/resources/enterprise_system_message.txt`

- 会话记忆
  - `MemoryProviderConfig` 使用 `MessageWindowChatMemory(maxMessages=10)`
  - `ChatMemoryRepository` 将会话消息持久化到 MySQL `chat_msg` 表

## 8. 常见问题

- Q: 启动报 API Key 缺失？
  - A: 检查 `MINIMAX_API_KEY`、`PINECONE_API_KEY` 是否已设置。

- Q: 对话无上下文记忆？
  - A: 检查 MySQL 是否可连接、`chat_msg` 表是否存在，并确认 `userId` 一致。

- Q: 上传成功但检索不到内容？
  - A: 检查 Pinecone 索引/命名空间配置、文档内容是否为空、`minScore` 阈值是否过高。

## 9. 后续建议

- 增加统一异常处理（返回标准错误码与错误信息）
- 完善接口文档（Knife4j/OpenAPI）
- 增加单元测试与集成测试
- 为上传文档增加格式校验（txt/pdf/docx）与大小限制