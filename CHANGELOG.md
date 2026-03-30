# CHANGELOG

本文件用于记录每个版本的功能变更，便于回溯开发历史。

建议规则：
- 每次提交代码后，同步更新一条版本记录。
- 按时间倒序维护（最新版本放最上面）。
- 重点写清楚：新增功能、优化、修复、影响范围。

---

## [v0.1.4] - 2026-03-30
- Commit: `TBD`
- 标题: `feat: agent-only frontend and router-driven document type`

### 新增
- 路由输出新增 `documentType` 字段，格式统一为：`intentType / reason / documentType`。
- `IntentDecision` 增加 `documentType` 字段，并在 `IntentRouterService` 完成解析。

### 优化
- `AgentWorkflowService#handleActionRequest` 改为直接使用路由结果中的 `documentType` 执行文档工具。
- 前端页面改为仅保留 Agent 问答入口，移除普通对话按钮与 `/api/chat/dialogue` 调用。
- `README.md` 更新为当前 Agent 主流程文档。

### 修复
- `/api/chat/agent/approve` 参数绑定改为 `@RequestParam("token")`，避免未开启 `-parameters` 时反射报错。
- 修复多处中文乱码与 BOM 编码问题，避免 `illegal character: '﻿'`。

### 影响范围
- 路由、工作流、审批接口、前端交互与项目文档。

---

## [v0.1.3] - 2026-03-29
- Commit: `735401d`
- 标题: `refactor: split chat responsibilities and update README`

### 新增
- 拆分聊天职责组件：
  - `ChatInputValidator`（入参校验）
  - `KnowledgeRetriever`（向量检索与上下文组装）
- 新增用户提示词资源文件：`enterprise_user_message.txt`。

### 优化
- `ChatService` 从“大方法”重构为流程编排，职责更清晰。
- `ChatAssistant` 改为 `@UserMessage(fromResource = "enterprise_user_message.txt")`，避免用户提示词硬编码。
- README 按当前实现更新并重构章节。

### 影响范围
- 对话链路（`/api/chat/dialogue`）内部实现结构。
- 项目文档维护方式。

---

## [v0.1.2] - 2026-03-28
- Commit: `7673a5d`
- 标题: `feat: update docs and refine upload/chat workflow`

### 新增
- 新增开发文档与运行说明（README 补充）。
- 新增文档类型模型（`DocumentType`）与用户系统提示词资源。

### 优化
- 文档上传和对话流程细节优化。
- 前端页面交互与提示文案优化。

### 影响范围
- 文档上传链路（`/api/documents/upload`）。
- 前端页面与项目文档。

---

## [v0.1.1] - 2026-03-28
- Commit: `e42677e`
- 标题: `feat: add ai services chat api and frontend chat panel`

### 新增
- 新增基于 LangChain4j AiService 的对话能力。
- 新增对话接口：`POST /api/chat/dialogue`。
- 新增前端对话面板并打通接口。

### 影响范围
- 对话能力从无到有。
- 前后端联调链路初步完成。

---

## 版本记录模板（每次提交后复制）

```markdown
## [vX.Y.Z] - YYYY-MM-DD
- Commit: `xxxxxxxx`
- 标题: `type: summary`

### 新增
- 

### 优化
- 

### 修复
- 

### 影响范围
- 
```

## 推荐提交流程
1. 代码改动完成。
2. 更新 `CHANGELOG.md`（新增一条版本记录）。
3. 执行提交：`git add . && git commit -m "..."`。
4. 推送远程：`git push origin master`。
