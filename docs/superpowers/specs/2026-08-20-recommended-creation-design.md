# 按小爱推荐的方式创作（AI 驱动版）设计

## 背景

用户端工作台「开始今日创作」提供两种创作方式：

- **按小爱推荐的方式创作**：当前弹框 `CreateFlowModal.vue` 的选题、观点、提示词、模板均为前端硬编码，点击「生成文章」后仅在前端模拟进度条，未调用真实生成接口。
- **自由创作**：`FreeCreateModal.vue` 已接入真实后端，调用 `/api/v1/user/generation-tasks` 提交生成任务。

本设计把「按小爱推荐的方式创作」改造为**全流程 AI 驱动**：每步调用 AI，下一步基于上一步结果；AI 结果按用户维度持久化到数据库，生成成功后清除会话。

## 目标

1. 把「小爱推荐创作」的 AI 推理逻辑抽到后端，前端只负责展示和用户选择。
2. 新增 2 条 `c_ai_prompt` 提示词配置：选题推荐、观点推荐。
3. 新增用户端后端能力：每个用户维护一份进行中的「推荐创作会话」。
4. 前端向导改为调用后端 LLM 接口生成选项，最终调用真实创作任务接口。
5. 生成任务提交成功后，自动清除该用户的推荐创作会话，下次进入重新开始。

## 非目标

- 不做历史会话版本管理（每个用户只保留一条进行中的会话）。
- 不做多设备实时同步（以数据库最新记录为准）。
- 不替换已有的 5 步 UI 结构，仅在现有 `CreateFlowModal.vue` 上替换数据源。
- 不改动 `GenerationTaskService` 的提交流程和额度扣减逻辑。

## 方案选型

采用「后端集中式 AI 推理 + 统一提示词配置 + 每用户一份会话」方案：

- 后端新增 `RecommendedCreationService`，按步骤调用 LLM，解析 JSON 后返回结构化选项。
- LLM 提示词走 `AiPromptRenderService.render(promptCode, variables)`，提示词内容写入 `c_ai_prompt`。
- 前端保持现有 5 步向导 UI，仅把选项来源从本地常量替换为后端接口。
- 会话持久化到新增表 `u_recommended_creation_session`。

## 数据库设计

### 1. 用户推荐创作会话表 `u_recommended_creation_session`

```sql
CREATE TABLE IF NOT EXISTS u_recommended_creation_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    current_step TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '当前步骤：1选题 2观点 3字数 4提示词 5模板',
    topics_json JSON DEFAULT NULL COMMENT 'AI生成的选题列表',
    selected_topic_json JSON DEFAULT NULL COMMENT '用户选中的选题',
    angles_json JSON DEFAULT NULL COMMENT 'AI生成的观点角度列表',
    selected_angles_json JSON DEFAULT NULL COMMENT '用户选中的观点角度（最多3个）',
    word_count INT UNSIGNED DEFAULT NULL COMMENT '用户设置的字数',
    prompt VARCHAR(512) DEFAULT NULL COMMENT '用户选中的创作提示词',
    template VARCHAR(64) DEFAULT NULL COMMENT '用户选中的导出模板key',
    status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '会话状态：draft',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_recommended_creation_session_user_id (user_id),
    KEY idx_u_recommended_creation_session_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户推荐创作会话表';
```

设计说明：

- 每个用户只有一条进行中的会话，用 `user_id` 唯一索引。
- 选题、观点等 AI 结果以 JSON 保留，方便恢复和继续编辑。
- `status` 字段预留，第一版仅使用 `draft`。

### 2. AI 提示词种子数据

在 `c_ai_prompt` 中新增 2 条用户端提示词。迁移脚本见下方「迁移文件」。

| prompt_code | category | 用途 |
|-------------|----------|------|
| `recommend_creation_topics_v1` | `recommended_creation` | 基于运营方案生成今日创作选题 |
| `recommend_creation_angles_v1` | `recommended_creation` | 基于选题生成差异化观点角度 |

## API 接口设计

基础路径：`/api/v1/user/recommended-creation`（用户端登录后可访问）。

### 1. 查询当前会话

```
GET /api/v1/user/recommended-creation/session
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "currentStep": 1,
    "topics": [
      {
        "id": "t1",
        "title": "35+ 被优化后，我用 3 个月找到 Remote 工作的真实路径",
        "risk": "low",
        "riskLabel": "同质化风险低",
        "caseCount": 12,
        "recommendedAngle": "真实复盘"
      }
    ],
    "selectedTopic": null,
    "angles": [],
    "selectedAngles": [],
    "wordCount": 1500,
    "prompt": "",
    "template": ""
  }
}
```

说明：无会话时返回 `data: null`，前端进入第 1 步并调用生成选题接口。

### 2. 生成选题

```
POST /api/v1/user/recommended-creation/topics
```

请求体：无（从当前用户 `u_self_media_plan` 取运营方案）。

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "topics": [
      {
        "id": "t1",
        "title": "35+ 被优化后，我用 3 个月找到 Remote 工作的真实路径",
        "risk": "low",
        "riskLabel": "同质化风险低",
        "caseCount": 12,
        "recommendedAngle": "真实复盘"
      }
    ]
  }
}
```

后端逻辑：

1. 查询当前用户 `u_self_media_plan`。
2. 调用 AI prompt `recommend_creation_topics_v1`，传入平台、赛道、人设、内容支柱。
3. 使用 `LlmJsonParser.parseLenient()` 解析 JSON，兜底补全字段。
4. 写入/更新 `u_recommended_creation_session`（`topics_json`、`current_step=1`）。

### 3. 生成观点角度

```
POST /api/v1/user/recommended-creation/angles
```

请求体：

```json
{
  "topicId": "t1"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "angles": [
      { "id": "a1", "text": "数字具体化：把关键数字提炼到标题" },
      { "id": "a2", "text": "反常识冲突：写一个与大众认知相反的标题" }
    ]
  }
}
```

后端逻辑：

1. 根据 `topicId` 从 `topics_json` 中找到对应选题，写入 `selected_topic_json`。
2. 调用 AI prompt `recommend_creation_angles_v1`，传入选题、运营方案。
3. 使用 `LlmJsonParser.parseLenient()` 解析 JSON，兜底补全字段。
4. 更新会话（`selected_topic_json`、`angles_json`、`current_step=2`）。

### 4. 更新中间选择（字数 / 提示词 / 模板）

```
PATCH /api/v1/user/recommended-creation/session
```

请求体：

```json
{
  "currentStep": 3,
  "wordCount": 1500
}
```

或：

```json
{
  "currentStep": 4,
  "prompt": "用亲身经历+反同质化角度，写一篇有信息增量的文章"
}
```

或：

```json
{
  "currentStep": 5,
  "template": "xiaohongshu-default"
}
```

后端逻辑：校验步骤连续性，更新会话对应字段。

### 5. 提交生成任务

```
POST /api/v1/user/recommended-creation/submit
```

请求体：无（所有必要字段从会话中读取）。

响应：与 `GenerationTaskController.submit` 一致，返回 `GenerationTaskVO`。

后端逻辑：

1. 从会话读取完整性数据：已选选题、观点、字数、提示词、模板。
2. 组装标题和描述：
   - 标题：所选选题的 `title`。
   - 描述：所选观点文本 + 选中提示词 + 运营方案摘要。
3. 调用 `GenerationTaskService.submit()` 提交真实生成任务。
4. **生成任务提交成功后**，删除 `u_recommended_creation_session` 中该用户记录。

### 6. 放弃/清除会话

```
DELETE /api/v1/user/recommended-creation/session
```

说明：用户点击明确的「放弃本次创作」按钮时调用，删除该用户会话。普通关闭弹框不调用此接口。

## 前端改造点

### 1. `CreateFlowModal.vue`

- 打开弹框时显示 loading，调用 `GET /session`。
- 无会话：调用 `POST /topics`，渲染选题。
- 有会话：按 `currentStep` 恢复界面，回填已选数据。
- 第 1 步选中选题后，调用 `POST /angles` 进入第 2 步。
- 第 2 步选中观点后，调用 `PATCH /session` 保存 `selectedAngles`。
- 第 3/4/5 步分别调用 `PATCH /session` 保存 `wordCount`、`prompt`、`template`。
- 第 5 步点击「生成文章」：调用 `POST /submit`（无请求体）；成功后 `emit('success', task)` 并关闭弹框。
- 弹框关闭（非完成）时，保留会话，不调用清除接口。
- 提供「放弃本次创作」入口，调用 `DELETE /session` 清除会话。

### 2. `WorkbenchIndex.vue`

- `onCreateStart` 改为接收真实任务对象，刷新生成记录列表，标记今日完成。
- 移除 `simulateGeneration()` 前端模拟逻辑。

### 3. 新增 API 文件

`project/user/web/src/api/recommendedCreation.js`：封装上述 6 个接口。

## AI Prompt 设计

### recommend_creation_topics_v1

**system_role**：

```
你是一位资深的自媒体选题顾问，擅长根据账号定位生成低粉高赞、差异化的今日创作选题。请严格按 JSON 格式返回，不要输出额外说明。
```

**user_prompt**：

```
用户运营方案如下：
- 主攻平台：{{platform}}
- 细分赛道：{{niche}}
- 人设定位：{{persona}}
- 内容支柱：{{pillars}}

请基于以上方案，推荐 6 个适合今日创作的选题。要求：
1. 选题贴合赛道和人设，有爆款潜质。
2. 给出同质化风险等级（low/medium/high）及标签。
3. 给出参考案例数量和推荐切入角度。

返回 JSON 数组，每个元素包含字段：id、title、risk、riskLabel、caseCount、recommendedAngle。
```

### recommend_creation_angles_v1

**system_role**：

```
你是一位爆款文章角度策划，擅长为一个选题生成多个可组合使用的差异化观点。请严格按 JSON 格式返回，不要输出额外说明。
```

**user_prompt**：

```
用户运营方案如下：
- 主攻平台：{{platform}}
- 细分赛道：{{niche}}
- 人设定位：{{persona}}
- 内容支柱：{{pillars}}

今日选题：{{topicTitle}}

请围绕该选题生成 7 个观点/切入角度，用户可从中选择 1-3 个组合使用。每个角度要具体、可执行、有网感。

返回 JSON 数组，每个元素包含字段：id、text。
```

## 错误处理

- **AI 调用失败**：前端显示「重新生成」按钮，保留当前步骤，不删除会话。
- **生成任务提交失败**（额度不足、队列满、字数超限等）：按现有 `GenerationTaskService` 抛出的业务异常提示用户；不删除会话，允许用户修改后重试。
- **用户关闭弹框**：保留会话，下次进入可恢复。
- **生成任务提交成功**：必须删除会话，下次进入重新从第 1 步开始。

## 安全与权限

- 所有接口必须登录，从 `SecurityUserContext` 获取 `user_id`。
- 只能操作当前用户的会话，禁止横向越权。
- AI 返回内容需做 XSS 过滤，前端渲染时使用 `v-text` 或转义输出。

## 验证方式

1. 后端单元测试：
   - 生成选题后会话正确写入。
   - 选题→观点流程中，观点基于所选选题生成。
   - 提交生成任务成功后，会话被删除。
   - 清除接口正确删除会话。

2. 前端验证：
   - 打开「按小爱推荐的方式创作」，无会话时展示 loading 并生成选题。
   - 选中选题进入第 2 步，生成观点。
   - 刷新页面后，能恢复到当前步骤和已选数据。
   - 点击「生成文章」成功后，再次打开弹框，重新从第 1 步开始。

## 依赖与影响

- 依赖 `u_self_media_plan` 表已有数据（用户已制定运营方案）。
- 依赖 `AiPromptRenderService` 和现有 LLM 调用器模式。
- 复用 `GenerationTaskService.submit()`，不影响自由创作逻辑。

## 迁移文件

新增 Flyway 迁移文件：

- `V2.0.0_0xx__create_recommended_creation_session_table.sql`：创建 `u_recommended_creation_session` 表。
- `V2.0.0_0xx__insert_recommended_creation_prompts.sql`：插入 2 条 `c_ai_prompt` 种子数据。

> 具体版本号按当前 migration 顺序递增确定。
