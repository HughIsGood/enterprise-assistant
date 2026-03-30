# Enterprise Knowledge Assistant

## 项目简介
企业知识助手系统，支持文档入库、RAG 检索问答、以及 Agent 工具调用与审批流。

## 当前能力
1. 文档上传入库
- 接口：`POST /api/documents/upload`
- 支持文档类型：`POLICY` / `TECH_TYPE` / `SOP` / `FAQ`

2. Agent 问答（主入口）
- 接口：`POST /api/chat/agent/ask`
- 前端仅保留 Agent 问答流程
- 根据路由结果执行知识问答或动作请求

3. 动作审批执行
- 接口：`POST /api/chat/agent/approve?token=...`
- 当 `ACTION_REQUEST` 需要审批时，前端弹窗确认并调用该接口

4. 路由决策输出文档类型
- `RouterAssistant` 输出：`intentType` / `reason` / `documentType`
- `IntentRouterService` 解析后写入 `IntentDecision`
- `AgentWorkflowService#handleActionRequest` 直接消费 `documentType`

## API 说明
1. 文档上传
- `POST /api/documents/upload`
- `multipart/form-data`：`file`, `docType`

2. Agent 问答
- `POST /api/chat/agent/ask`
- 请求体：`{ "userId": "666", "message": "..." }`

3. 审批执行
- `POST /api/chat/agent/approve?token=xxx`

4. 兼容接口（已废弃）
- `POST /api/chat/dialogue`
- 后端已标记 `@Deprecated`，前端不再使用

## 关键实现
1. 路由与工作流
- `org.weihua.assistant.RouterAssistant`
- `org.weihua.service.IntentRouterService`
- `org.weihua.service.workflow.AgentWorkflowService`

2. 工具执行
- `org.weihua.service.tools.ToolExecutionService`
- `org.weihua.service.tools.DocumentTool`
- `org.weihua.service.tools.TicketTool`

3. 前端页面
- `src/main/resources/static/index.html`
- 仅保留 Agent 问答按钮与审批交互

## 运行
1. 环境要求
- JDK 17+
- Maven 3.9+
- MySQL 8+

2. 启动
```bash
mvn spring-boot:run
```

3. 访问
- `http://localhost:8080/`

## 说明
- Java 源码统一使用 UTF-8（无 BOM），避免 `illegal character: '﻿'` 编译问题。
