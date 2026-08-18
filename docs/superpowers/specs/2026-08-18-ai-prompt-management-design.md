# 系统设置 - AI 提示词管理设计

## 背景

目前系统中多处 AI 调用仍然把提示词硬编码在 Java 代码里，例如：

- 管理端 `TopicTitleService.java`：爆款标题/选题生成。
- 用户端 `TitleOptimizeServiceImpl.java`：标题优化。
- 用户端 `SkillAnalyzeServiceImpl.java`：文风分析。

这些提示词包含系统角色设定、生成约束、输出格式要求等非变量内容。产品/运营需要能够在不改动代码、不重新发布的情况下调整这些提示词，并且未来新增 AI 调用时先创建提示词配置，再编写调用代码。

> 注意：管理端 13 阶段流水线提示词（`t_prompt_template_stage`）已经在线可编辑，**不在本次范围**。

## 目标

- 在管理后台「系统设置」下新增「AI 提示词管理」菜单。
- 把当前代码中硬编码的 AI 提示词抽取到数据库，支持可视化增删改查。
- 支持变量占位符 `{{variableName}}`，区分变量与非变量内容。
- 支持必填变量校验，缺失时明确报错。
- 管理后台编辑后保存即生效。
- 用户端和管理端共用同一套提示词配置。

## 非目标

- 不做复杂版本历史、回滚、审批流（第一版）。
- 不替换已有的 13 阶段流水线模板管理。
- 不实现 AI 调用本身的抽象，只改造提示词获取与渲染方式。
- 不做提示词 A/B 测试。

## 方案选型

采用 **统一的公共提示词配置表** 方案：

- 新增 `c_ai_prompt` 表统一管理所有代码提示词。
- 每个提示词一个唯一编码 `prompt_code`，代码中按编码读取。
- 优点：扩展性好、规范统一、前端只需一套管理界面。

## 数据库设计

### 表 `c_ai_prompt`

```sql
CREATE TABLE IF NOT EXISTS c_ai_prompt (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    prompt_code VARCHAR(64) NOT NULL COMMENT '唯一编码，如 topic_title_v1',
    prompt_name VARCHAR(128) NOT NULL COMMENT '显示名称',
    module VARCHAR(32) NOT NULL COMMENT '归属端：admin / user',
    category VARCHAR(64) DEFAULT NULL COMMENT '业务分类：topic_title / title_optimize / skill_analyze',
    system_role MEDIUMTEXT COMMENT '系统角色 / AI 身份设定',
    user_prompt MEDIUMTEXT NOT NULL COMMENT '用户提示词主体',
    variable_schema JSON DEFAULT NULL COMMENT '变量元数据：[{name, required, description, example}]',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    description VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_c_ai_prompt_code (prompt_code),
    KEY idx_module_category (module, category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 提示词配置表';
```

### 关键约束

- `prompt_code` 全局唯一，代码通过编码引用提示词。
- `module` 仅用于后台分组展示，不影响调用逻辑。
- `variable_schema` 中的变量名应与 `system_role` / `user_prompt` 中出现的 `{{name}}` 一致。
- 逻辑删除，不物理删除。

### 初始数据

新增 Flyway 迁移脚本，初始化 3 条记录：

| prompt_code | module | category | 说明 |
|-------------|--------|----------|------|
| `topic_title_v1` | `admin` | `topic_title` | 爆款标题/选题生成 |
| `title_optimize_v1` | `user` | `title_optimize` | 标题优化 |
| `skill_analyze_v1` | `user` | `skill_analyze` | 文风分析 |

内容从当前硬编码代码复制，变量统一整理为 `{{xxx}}` 形式。

## API 接口设计

### 管理后台接口（admin/api）

基础路径：`/api/v1/admin/ai-prompts`

#### 1. 查询提示词列表

```
GET /api/v1/admin/ai-prompts
```

**查询参数**

| 参数 | 说明 |
|------|------|
| `module` | 筛选：admin / user |
| `category` | 业务分类 |
| `status` | 状态：0 / 1 |
| `keyword` | 按 code / name 模糊搜索 |
| `pageNum` | 页码 |
| `pageSize` | 页大小 |

**响应数据**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "promptCode": "topic_title_v1",
        "promptName": "爆款标题/选题生成",
        "module": "admin",
        "category": "topic_title",
        "status": 1,
        "updatedAt": "2026-08-18T10:00:00.000"
      }
    ],
    "pageNum": 1,
    "pageSize": 20,
    "total": 3,
    "pages": 1
  }
}
```

#### 2. 查看提示词详情

```
GET /api/v1/admin/ai-prompts/{id}
```

#### 3. 新增提示词

```
POST /api/v1/admin/ai-prompts
```

**请求体**

```json
{
  "promptCode": "topic_title_v1",
  "promptName": "爆款标题/选题生成",
  "module": "admin",
  "category": "topic_title",
  "systemRole": "你是自媒体爆款标题策划。",
  "userPrompt": "请根据以下内容生成 {{count}} 个标题：\n\n{{content}}",
  "variableSchema": [
    { "name": "count", "required": true, "description": "生成数量", "example": "10" },
    { "name": "content", "required": true, "description": "参考内容", "example": "文章正文" }
  ],
  "status": 1,
  "sortOrder": 0,
  "description": "管理端爆款标题生成"
}
```

#### 4. 编辑提示词

```
PUT /api/v1/admin/ai-prompts/{id}
```

**请求体**：同新增。

- `prompt_code` 一旦创建不建议修改；如需修改需校验无代码引用。
- 保存时自动解析 `system_role` 和 `user_prompt` 中的 `{{xxx}}`，与 `variable_schema` 合并更新。

#### 5. 删除提示词

```
DELETE /api/v1/admin/ai-prompts/{id}
```

逻辑删除。

#### 6. 启用/停用

```
POST /api/v1/admin/ai-prompts/{id}/actions/enable
POST /api/v1/admin/ai-prompts/{id}/actions/disable
```

### 内部服务接口

不对外暴露，仅供业务服务调用：

- `AiPromptService.getPromptByCode(String code)`：按编码获取启用中的提示词实体。
- `AiPromptService.render(String code, Map<String, Object> variables)`：渲染提示词。
  - 找不到或已停用 → 抛异常。
  - 必填变量缺失 → 抛异常，返回缺失变量名。
  - 返回 `RenderedPrompt { systemRole, userPrompt }`。

## 提示词渲染规则

1. 从 `c_ai_prompt` 读取 `system_role` 和 `user_prompt`。
2. 使用正则 `\{\{(\w+)\}\}` 匹配所有变量。
3. 校验 `variable_schema` 中 `required=true` 的变量是否在传入参数中存在且非空。
4. 替换变量值；未提供值的非必填变量替换为空字符串。
5. 变量名大小写敏感。

## 后端结构

```
project/admin/api/src/main/java/com/aichuangzuo/admin/modules/system/
├── controller/
│   └── AiPromptController.java
├── service/
│   ├── AiPromptService.java
│   └── impl/
│       └── AiPromptServiceImpl.java
├── entity/
│   └── AiPrompt.java
├── mapper/
│   └── AiPromptMapper.java
├── dto/
│   ├── request/
│   │   ├── AiPromptCreateRequest.java
│   │   ├── AiPromptUpdateRequest.java
│   │   └── AiPromptQueryRequest.java
│   └── AiPromptDTO.java
└── vo/
    ├── AiPromptVO.java
    ├── AiPromptDetailVO.java
    └── RenderedPromptVO.java
```

公共渲染逻辑可下沉到共享模块：

```
project/shared/utils/
└── AIPromptVariableResolver.java   # 变量解析与替换工具
```

改造现有代码：

- `project/admin/api/.../topictitle/service/TopicTitleService.java`
- `project/user/api/.../article/service/impl/TitleOptimizeServiceImpl.java`
- `project/user/api/.../skill/service/impl/SkillAnalyzeServiceImpl.java`

改造后：从硬编码字符串改为 `aiPromptService.render("topic_title_v1", params)`。

## 错误码

新增错误码（`AdminSystemErrorCode`，24xxxx 范围）：

| 错误码 | 含义 |
|--------|------|
| 240101 | 提示词配置不存在 |
| 240102 | 提示词配置已停用 |
| 240103 | 提示词必填变量缺失 |
| 240104 | 提示词编码已存在 |
| 240105 | 提示词渲染异常 |

## 前端结构

### 菜单调整

在 `project/admin/web/src/layouts/AdminLayout.vue` 的「系统设置」下新增二级菜单：

```
系统设置
  └── AI 提示词管理
```

### 新增页面

- `project/admin/web/src/views/AiPromptListView.vue`：提示词列表。
- `project/admin/web/src/views/AiPromptEditView.vue`：新增/编辑提示词。
- `project/admin/web/src/api/aiPrompt.js`：接口封装。
- `project/admin/web/src/composables/useAiPrompt.js`：状态与操作。

### 页面交互

**列表页**

- 表格字段：编码、名称、端（admin/user）、分类、状态、更新时间。
- 筛选：端、分类、状态、关键词搜索。
- 操作：编辑、启用/停用、删除。

**编辑页**

- 基础信息：code、名称、端、分类、排序、状态、备注。
- 系统角色：大文本框。
- 用户提示词：大文本框。
- 变量区域：自动解析 `{{xxx}}`，表格展示，可补充描述、示例、是否必填。
- 快捷插入：常用变量按钮（如 `{{title}}`、`{{content}}`、`{{platform}}`、`{{count}}`）。

## 权限控制

- 所有 `/api/v1/admin/ai-prompts/**` 接口仅允许 `SUPER_ADMIN` 访问。
- 复用已有的超级管理员校验逻辑。

## 测试计划

1. **单元测试**
   - 变量解析：正常提取、嵌套/重复变量、无变量。
   - 必填校验：缺失必填变量时报错并列出名称。
   - 变量替换：正常替换、未提供值替换为空字符串。

2. **API 测试**
   - CRUD、筛选、分页、启用停用、删除。
   - 编码唯一性校验。

3. **回归测试**
   - 管理端爆款标题生成结果格式不变。
   - 用户端标题优化结果格式不变。
   - 用户端文风分析结果格式不变。

## 风险与假设

- 用户端和管理端后端需能访问同一张 `c_ai_prompt` 表。若两库物理隔离，需要额外同步机制。
- 硬编码提示词迁移时，需确保变量命名与现有调用方传入的参数一致。
- 后续新增 AI 调用时，必须先在此表中创建配置，再在代码中引用编码。
