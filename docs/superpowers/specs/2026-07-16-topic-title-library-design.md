# 选题标题库 设计

> 用户端创作页「没灵感？点一个快速开始」胶囊从写死的 6 条改为真实标题库；管理端新增「标题管理」菜单，支持 AI 批量生成标题 + 概要入库。用过的标题按用户隔离，不再出现。

## 背景与目标

- 现状：CreateIndex.vue 写死 6 条选题，`used` 仅前端内存态，「换一批」只是前端洗牌。
- 目标：
  - 管理端可持续用 AI 生产标题（标题 + 概要）入库
  - 用户端胶囊从库中随机拉取，点胶囊 = 标题填入标题框 + 概要填入要求框
  - 使用记录按用户隔离：A 用过的标题 A 不再看到，B 不受影响

## 数据模型

两张新表，Flyway 迁移脚本放用户端（`project/user/api/src/main/resources/db/migration`）。

### u_topic_title（标题库）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| title | VARCHAR(128) NOT NULL | 标题 |
| summary | VARCHAR(512) NOT NULL | 标题概要（写作方向） |
| direction | VARCHAR(256) DEFAULT '' | 生成时用的方向提示词（追溯用） |
| use_count | INT NOT NULL DEFAULT 0 | 全站累计使用次数（管理端列表展示） |
| is_deleted | TINYINT NOT NULL DEFAULT 0 | 逻辑删除 |
| tenant_id / created_at / updated_at / created_by / updated_by | — | 标准审计列 |

索引：`idx_topic_title_deleted_id (is_deleted, id)`

删除走逻辑删除：已被使用记录引用的标题物理删除会破坏「我的已用」排除逻辑。

### u_topic_title_usage（用户使用记录）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id | BIGINT NOT NULL | |
| title_id | BIGINT NOT NULL | |
| created_at / updated_at 等 | — | 标准审计列 |

索引：`uk_topic_usage_user_title (user_id, title_id)` 唯一（同一用户对同一标题只记一次，use 幂等的基石）；`idx_topic_usage_title (title_id)`

## 管理端

### 菜单与页面

「创作管理」子菜单新增「标题管理」，路由 `/console/topic-titles`。

- 顶部操作条：「AI 生成标题」按钮 + 标题关键字搜索框
- 列表列：标题、概要、方向提示词、使用次数、生成时间、操作（删除）
- 生成弹框：数量（1-20，默认 10）+ 方向提示词 textarea（占位示例「职场效率类，面向 25-35 岁打工人」）。确认后 loading 等待，成功提示「已生成 N 条入库」并刷新列表。

### 接口 `/api/v1/admin/topic-titles`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/` | 分页列表：keyword + page/pageSize |
| POST | `/generate` | body `{count, direction}`，同步调 AI，返回 `{generated: N}` |
| DELETE | `/{id}` | 逻辑删除 |

### AI 生成流程（TopicTitleService.generate）

1. 取当前 active 模型配置，复用 `GenerationAiService.call(modelConfigId, systemMsg, userMsg, modelParams)`
2. systemMsg：固定角色「你是自媒体爆款标题策划」
3. userMsg 拼装：管理员填的方向提示词 + 数量要求 + JSON 结构说明 + 以下 4 条强约束（原文内置后端代码）：

```
最终输出要求（覆盖以上所有说明，必须严格遵守）：
1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。
2. 不要用 ```json 或任何代码围栏包裹。
3. 第一个字符必须是 {，最后一个字符必须是 }。
4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。
```

4. JSON 结构说明（后端内置，告知 AI 的输出格式）：

```json
{"titles": [{"title": "标题文字", "summary": "这篇文章的核心观点和写作方向"}]}
```

5. 解析与入库：清洗可能的前言/代码围栏 → Jackson 解析 → 校验 titles 数组非空、每条 title/summary 非空、截断超长（title 128 / summary 512）→ 批量 insert（direction 落库）
6. 解析失败：抛业务异常，不入库，管理员可重试

## 用户端

### 接口 `/api/v1/user/topics`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/random?count=6` | 排除「我已用过 + 已删除」，`ORDER BY RAND() LIMIT count`，返回 `[{id, title, summary}]` |
| POST | `/{id}/use` | 插 usage（唯一键冲突忽略，幂等）+ `use_count + 1` |

### 创作页改造（CreateIndex.vue）

- 删除写死的 6 条 topics；`onMounted` 调 `/random?count=6`
- 点胶囊：标题填入标题框、概要填入「核心观点/要求」框（替换原"追加通用提示"逻辑），同时调 `/use` 上报，该胶囊置 used 禁用
- 「换一批」：重新调 `/random` 整体替换列表
- 库中可用标题不足 6 条有多少显示多少；0 条时胶囊区整体隐藏
- 已用排除在后端做，刷新页面后状态一致

## 错误处理

- 管理端：无 active 模型 → `GENERATION_MODEL_UNAVAILABLE`；AI 返回无法解析或 titles 为空 → 新错误码 `TOPIC_TITLE_GENERATE_FAILED`（"AI 生成失败，请重试"）；count 超 1-20 → 参数校验 400
- 用户端 `/use`：标题不存在或已删除 → 404；重复 use → 静默成功
- 用户端 `/random`：库为空 → 空数组，前端隐藏胶囊区

## 测试

- 管理端 `TopicTitleServiceTest`（@SpringBootTest，mock GenerationAiService 不真调 AI）：
  - generate 成功路径（合法 JSON 数组 → 入库）
  - JSON 带前言/代码围栏杂质 → 清洗后解析成功
  - 纯垃圾返回 → 抛业务异常不入库
  - 列表分页 + 关键字过滤
  - 删除后不再出现在用户端随机池
- 用户端 `TopicTitleServiceTest`（@SpringBootTest）：
  - random 排除已用标题
  - random 排除已删除标题
  - use 幂等（重复调用不报错且只记一次）
  - use_count 正确累加
  - 库为空返回空数组

## YAGNI 边界（明确不做）

- 不做标题分类/标签、不做手动新增、不做编辑、不做使用记录查询页
- 不做生成历史批次记录（direction 字段已够追溯）
- 不做异步生成队列（同步一次 AI 调用足够）
