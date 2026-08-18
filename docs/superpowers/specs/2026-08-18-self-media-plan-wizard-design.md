# 制定你的自媒体方案（AI 驱动版）设计

## 背景

用户端已有一个「制定你的自媒体方案」向导页面（`/console/onboarding`），目前：

- 平台卡片数据来自管理端 `c_platform` 表，但平台推荐靠前端硬编码打分。
- 目标、赛道、人设、内容支柱全部写死在前端 `OnboardingIndex.vue` 里。
- 方案只保存在 `localStorage`（`aichuangzuo_onboarding_done`），没有后端持久化，`WorkbenchIndex.vue` 也使用硬编码的运营方案。

随着 AI 提示词管理系统（`c_ai_prompt` + `AiPromptRenderService`）上线，需要把这个向导改造成**全流程 AI 驱动**：选平台 → AI 推荐目标 → 选目标 → AI 推荐赛道 → 选赛道 → AI 推荐人设 → 选人设法 → 保存方案，并且所有 AI 提示词都可在管理后台可视化配置。

## 目标

1. 把「制定自媒体方案」的 AI 推理逻辑全部抽到后端，前端只负责展示和用户选择。
2. 新增 4 条 `c_ai_prompt` 提示词配置，覆盖平台推荐、目标推荐、赛道推荐、人设推荐。
3. 新增用户端后端能力：存储每个用户的自媒体运营方案。
4. 前端向导改为调用后端 LLM 接口生成选项，最终方案保存到后端。
5. 工作台（Workbench）从后端读取当前用户的运营方案。

## 非目标

- 不做历史版本/方案对比/多方案管理（第一版只保留每个用户最新一份方案）。
- 不做 AI 推荐结果的缓存（每次进入步骤重新调用）。
- 不替换已有的 13 阶段流水线模板。
- 不做 AI 模型调用底层抽象，沿用现有 `SkillAnalyzeAiService` 模式封装一个专用调用器。

## 方案选型

采用「后端集中式 AI 推理 + 统一提示词配置」方案：

- 后端新增 `SelfMediaPlanService`，按步骤调用 LLM，解析 JSON 后返回结构化选项。
- LLM 提示词全部走 `AiPromptRenderService.render(promptCode, variables)`，提示词内容写入 `c_ai_prompt`。
- 前端保持现有 5 步向导 UI，仅把选项来源从本地常量替换为后端接口。
- 方案持久化到新增表 `u_self_media_plan`。

## 数据库设计

### 1. 用户自媒体方案表 `u_self_media_plan`

```sql
CREATE TABLE IF NOT EXISTS u_self_media_plan (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    platform_key VARCHAR(64) NOT NULL COMMENT '主攻平台key，如 xiaohongshu',
    platform_name VARCHAR(128) NOT NULL COMMENT '主攻平台显示名，如 小红书',
    goal VARCHAR(128) NOT NULL COMMENT '用户选择的核心目标',
    background VARCHAR(128) DEFAULT NULL COMMENT '用户职业/经验领域',
    has_product TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否有可变现产品/服务：0-否，1-是',
    product_desc VARCHAR(512) DEFAULT NULL COMMENT '可变现产品/服务描述',
    niche_key VARCHAR(64) NOT NULL COMMENT '细分赛道key',
    niche_name VARCHAR(128) NOT NULL COMMENT '细分赛道显示名',
    persona_key VARCHAR(64) NOT NULL COMMENT '人设key',
    persona_name VARCHAR(128) NOT NULL COMMENT '人设显示名',
    content_pillars_json JSON NOT NULL COMMENT '内容支柱比例 [{"name":"干货复盘","percent":60},...]',
    recommendation_context_json JSON DEFAULT NULL COMMENT 'AI推荐问卷上下文（工作类型、投入时间、收入目标等）',
    is_recommended_by_ai TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '平台是否由AI推荐：0-否，1-是',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_self_media_plan_user_id (user_id),
    KEY idx_u_self_media_plan_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自媒体运营方案表';
```

设计说明：

- 每个用户只有一份方案，用 `user_id` 唯一索引。
- 问卷上下文以 JSON 保留，方便后续重新推荐或分析。
- 内容支柱用 JSON 存储，结构清晰且前端可直接渲染。

### 2. AI 提示词种子数据

在 `c_ai_prompt` 中新增 4 条用户端提示词。迁移脚本见下方「迁移文件」。

| prompt_code | category | 用途 |
|-------------|----------|------|
| `self_media_recommend_platform_v1` | `self_media_plan` | 根据问卷推荐平台 |
| `self_media_recommend_goals_v1` | `self_media_plan` | 根据平台和上下文推荐目标 |
| `self_media_recommend_niches_v1` | `self_media_plan` | 根据平台、目标、背景推荐赛道 |
| `self_media_recommend_personas_v1` | `self_media_plan` | 根据前面选择推荐人设和默认内容支柱 |

## API 接口设计

基础路径：`/api/v1/user/self-media-plans`（用户端登录后可访问）。

### 1. 查询当前方案

```
GET /api/v1/user/self-media-plans/current
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "platformKey": "xiaohongshu",
    "platformName": "小红书",
    "goal": "靠生活经验/好物分享变现",
    "background": "职场/管理",
    "hasProduct": false,
    "productDesc": "",
    "nicheKey": "zhichangzhuanxing",
    "nicheName": "35+ 职场转型",
    "personaKey": "experiencer",
    "personaName": "实战记录者",
    "pillars": [
      { "name": "干货复盘", "percent": 60 },
      { "name": "个人故事", "percent": 20 },
      { "name": "热点解读", "percent": 20 }
    ],
    "isRecommendedByAI": true,
    "recommendationContext": {
      "workType": "副业",
      "timePerWeek": "3 - 10 小时",
      "incomeGoal": "月入过万",
      "breakEvenPeriod": "3 个月",
      "contentType": "图文笔记",
      "audience": "职场人",
      "identity": "职场人",
      "onCamera": "不想做视频",
      "note": ""
    }
  }
}
```

未创建方案时 `data` 为 `null`。

### 2. AI 推荐平台

```
POST /api/v1/user/self-media-plans/actions/recommend-platform
```

请求体：

```json
{
  "workType": "副业",
  "timePerWeek": "3 - 10 小时",
  "incomeGoal": "月入过万",
  "breakEvenPeriod": "3 个月",
  "contentType": "图文笔记",
  "audience": "职场人",
  "identity": "职场人",
  "onCamera": "不想做视频",
  "note": ""
}
```

响应：

```json
{
  "code": 0,
  "data": {
    "platformKey": "xiaohongshu",
    "platformName": "小红书",
    "reason": "图文笔记形式与你的内容类型、投入时间和目标受众匹配度最高。"
  }
}
```

### 3. AI 推荐目标选项

```
POST /api/v1/user/self-media-plans/actions/recommend-goals
```

请求体：

```json
{
  "platformKey": "xiaohongshu",
  "background": "职场/管理",
  "workType": "副业",
  "timePerWeek": "3 - 10 小时",
  "incomeGoal": "月入过万",
  "breakEvenPeriod": "3 个月",
  "contentType": "图文笔记",
  "audience": "职场人",
  "identity": "职场人",
  "onCamera": "不想做视频",
  "note": ""
}
```

响应：

```json
{
  "code": 0,
  "data": [
    { "key": "knowledge", "name": "靠专业知识/技能变现", "description": "输出职场方法论，适合建立专业信任后做咨询/课程。" },
    { "key": "experience", "name": "靠生活经验/好物分享变现", "description": "用真实经历种草，容易起号，变现路径短。" },
    { "key": "product", "name": "靠产品/服务变现", "description": "如果你有职场相关产品，可通过内容引流到私域成交。" }
  ]
}
```

### 4. AI 推荐赛道选项

```
POST /api/v1/user/self-media-plans/actions/recommend-niches
```

请求体：

```json
{
  "platformKey": "xiaohongshu",
  "goal": "靠生活经验/好物分享变现",
  "background": "职场/管理",
  "hasProduct": false,
  "productDesc": "",
  "workType": "副业",
  "timePerWeek": "3 - 10 小时",
  "incomeGoal": "月入过万",
  "breakEvenPeriod": "3 个月",
  "contentType": "图文笔记",
  "audience": "职场人",
  "identity": "职场人",
  "onCamera": "不想做视频",
  "note": ""
}
```

响应：

```json
{
  "code": 0,
  "data": [
    {
      "key": "zhichangzhuanxing",
      "name": "35+ 职场转型",
      "audience": "30-45 岁职场人",
      "monetization": "咨询/课程/社群",
      "riskLabel": "同质化风险低",
      "riskColor": "success",
      "caseCount": 12,
      "reason": "细分人群明确，你的职场背景是最佳差异化。"
    }
  ]
}
```

### 5. AI 推荐人设选项

```
POST /api/v1/user/self-media-plans/actions/recommend-personas
```

请求体：

```json
{
  "platformKey": "xiaohongshu",
  "goal": "靠生活经验/好物分享变现",
  "background": "职场/管理",
  "nicheKey": "zhichangzhuanxing",
  "nicheName": "35+ 职场转型",
  "workType": "副业",
  "timePerWeek": "3 - 10 小时",
  "incomeGoal": "月入过万",
  "breakEvenPeriod": "3 个月",
  "contentType": "图文笔记",
  "audience": "职场人",
  "identity": "职场人",
  "onCamera": "不想做视频",
  "note": ""
}
```

响应：

```json
{
  "code": 0,
  "data": {
    "personas": [
      { "key": "experiencer", "name": "实战记录者", "desc": "分享亲身经历，真实感强，容易建立信任。" },
      { "key": "expert", "name": "干货专家", "desc": "输出方法论和深度分析，适合专业赛道。" },
      { "key": "curator", "name": "经验总结者", "desc": "整理归纳信息，适合工具/清单类内容。" }
    ],
    "defaultPillars": [
      { "name": "干货复盘", "percent": 60 },
      { "name": "个人故事", "percent": 20 },
      { "name": "热点解读", "percent": 20 }
    ]
  }
}
```

### 6. 保存/更新方案

```
POST /api/v1/user/self-media-plans
```

请求体：

```json
{
  "platformKey": "xiaohongshu",
  "platformName": "小红书",
  "goal": "靠生活经验/好物分享变现",
  "background": "职场/管理",
  "hasProduct": false,
  "productDesc": "",
  "nicheKey": "zhichangzhuanxing",
  "nicheName": "35+ 职场转型",
  "personaKey": "experiencer",
  "personaName": "实战记录者",
  "pillars": [
    { "name": "干货复盘", "percent": 60 },
    { "name": "个人故事", "percent": 20 },
    { "name": "热点解读", "percent": 20 }
  ],
  "isRecommendedByAI": true,
  "recommendationContext": {
    "workType": "副业",
    "timePerWeek": "3 - 10 小时",
    "incomeGoal": "月入过万",
    "breakEvenPeriod": "3 个月",
    "contentType": "图文笔记",
    "audience": "职场人",
    "identity": "职场人",
    "onCamera": "不想做视频",
    "note": ""
  }
}
```

响应：同「查询当前方案」。后端按 `user_id` 插入或更新（`user_id` 唯一）。

## AI 提示词设计

### 公共要求

所有提示词统一要求：

- `system_role`：设定 AI 角色，并强调「只输出合法 JSON」。
- `user_prompt`：把上下文变量以 `{{variableName}}` 注入。
- 输出必须是单个 JSON 对象，不要 markdown 代码围栏，不要任何前言/后记。
- 第一个字符必须是 `{`，最后一个字符必须是 `}`。

变量统一规范：

- 问卷字段：`workType`、`timePerWeek`、`incomeGoal`、`breakEvenPeriod`、`contentType`、`audience`、`identity`、`onCamera`、`note`。
- 平台字段：`platformKey`、`platformName`、`platformTagline`、`platformContentForm`、`platformMonetization`、`platformBestFor`。
- 业务字段：`background`、`goal`、`hasProduct`、`productDesc`、`nicheKey`、`nicheName`。

### 1. 平台推荐 `self_media_recommend_platform_v1`

**变量 schema：**

```json
[
  {"name":"workType","required":true,"description":"主业/副业/想转主业/不明确","example":"副业"},
  {"name":"timePerWeek","required":true,"description":"每周投入时间","example":"3 - 10 小时"},
  {"name":"incomeGoal","required":true,"description":"期望月收入","example":"月入过万"},
  {"name":"breakEvenPeriod","required":true,"description":"可接受不盈利周期","example":"3 个月"},
  {"name":"contentType","required":true,"description":"倾向内容形式","example":"图文笔记"},
  {"name":"audience","required":true,"description":"目标受众","example":"职场人"},
  {"name":"identity","required":true,"description":"身份标签","example":"职场人"},
  {"name":"onCamera","required":true,"description":"是否愿意出镜/做视频","example":"不想做视频"},
  {"name":"note","required":false,"description":"补充说明","example":""},
  {"name":"platformsJson","required":true,"description":"当前启用平台列表 JSON","example":"[{\"platformKey\":\"xiaohongshu\",\"platformName\":\"小红书\",\"tagline\":\"...\"}]"}
]
```

**system_role：**

```
你是一位自媒体平台匹配顾问。你只输出合法 JSON，不输出任何解释、免责声明或 markdown 代码围栏。
```

**user_prompt 示例：**

```
请根据用户画像，从以下平台中推荐最合适的一个平台，并说明理由。

可选平台（JSON 数组）：
{{platformsJson}}

用户画像：
- 主业/副业：{{workType}}
- 每周投入时间：{{timePerWeek}}
- 期望月收入：{{incomeGoal}}
- 可接受不盈利周期：{{breakEvenPeriod}}
- 倾向内容形式：{{contentType}}
- 目标受众：{{audience}}
- 身份标签：{{identity}}
- 是否愿意出镜/做视频：{{onCamera}}
- 补充说明：{{note}}

要求：
1. platformKey 必须从可选平台的 platformKey 中选取。
2. reason 用 1-2 句话说明匹配原因，控制在 120 字以内。
3. 只输出 JSON：{"platformKey":"...","reason":"..."}。
4. 不要代码围栏，不要任何额外文字。
```

### 2. 目标推荐 `self_media_recommend_goals_v1`

**变量 schema：**

```json
[
  {"name":"platformKey","required":true,"description":"平台key","example":"xiaohongshu"},
  {"name":"platformName","required":true,"description":"平台名","example":"小红书"},
  {"name":"platformTagline","required":true,"description":"平台一句话卖点","example":"图文种草社区，女性用户多"},
  {"name":"platformContentForm","required":true,"description":"内容形式，逗号分隔","example":"图文笔记,短视频"},
  {"name":"platformMonetization","required":true,"description":"主要收益，逗号分隔","example":"品牌广告,带货分佣,私域引流"},
  {"name":"platformBestFor","required":true,"description":"适合谁","example":"有生活经验、愿意分享好物/干货的人"},
  {"name":"background","required":false,"description":"职业/经验领域","example":"职场/管理"},
  {"name":"workType","required":true},
  {"name":"timePerWeek","required":true},
  {"name":"incomeGoal","required":true},
  {"name":"breakEvenPeriod","required":true},
  {"name":"contentType","required":true},
  {"name":"audience","required":true},
  {"name":"identity","required":true},
  {"name":"onCamera","required":true},
  {"name":"note","required":false}
]
```

**system_role：**

```
你是一位自媒体变现路径规划师。你只输出合法 JSON。
```

**user_prompt 示例：**

```
请为以下用户推荐 3-5 个适合该平台的运营目标（变现方向）。

平台信息：
- 平台：{{platformName}}（{{platformKey}}）
- 卖点：{{platformTagline}}
- 内容形式：{{platformContentForm}}
- 主要收益：{{platformMonetization}}
- 适合人群：{{platformBestFor}}

用户背景：
- 职业/经验领域：{{background}}
- 主业/副业：{{workType}}
- 每周投入时间：{{timePerWeek}}
- 期望月收入：{{incomeGoal}}
- 可接受不盈利周期：{{breakEvenPeriod}}
- 倾向内容形式：{{contentType}}
- 目标受众：{{audience}}
- 身份标签：{{identity}}
- 是否出镜/做视频：{{onCamera}}
- 补充说明：{{note}}

输出 JSON 结构：
{"goals":[{"key":"英文标识","name":"中文目标名称","description":"一句话说明为什么适合，不超过60字"}]}

要求：
1. key 使用英文小写+下划线，如 knowledge、experience、product。
2. name 必须是中文，直观可懂。
3. 推荐的选项必须贴合平台变现路径和用户背景。
4. 只输出 JSON，不要代码围栏和额外说明。
```

### 3. 赛道推荐 `self_media_recommend_niches_v1`

**变量 schema：** 在前者基础上新增 `goal`、`background`、`hasProduct`、`productDesc`。

**system_role：**

```
你是一位自媒体赛道分析师。你只输出合法 JSON。
```

**user_prompt 示例：**

```
请基于已选的平台和目标，推荐 3 个细分赛道。

已选信息：
- 平台：{{platformKey}}
- 目标：{{goal}}
- 职业/经验领域：{{background}}
- 是否有可变现产品/服务：{{hasProduct}}
- 产品/服务描述：{{productDesc}}

用户画像：
- 主业/副业：{{workType}}
- 每周投入时间：{{timePerWeek}}
- 期望月收入：{{incomeGoal}}
- 可接受不盈利周期：{{breakEvenPeriod}}
- 倾向内容形式：{{contentType}}
- 目标受众：{{audience}}
- 身份标签：{{identity}}
- 是否出镜/做视频：{{onCamera}}
- 补充说明：{{note}}

输出 JSON 结构：
{"niches":[
  {
    "key":"英文标识",
    "name":"中文赛道名",
    "audience":"目标人群，20字以内",
    "monetization":"主要变现方式，20字以内",
    "riskLabel":"同质化风险低/中/高",
    "riskColor":"success/warning/error",
    "caseCount":10,
    "reason":"推荐理由，80字以内"
  }
]}

要求：
1. key 使用英文小写+下划线。
2. riskColor 只能是 success、warning、error 之一，与 riskLabel 对应。
3. caseCount 为 5-20 之间的整数，用于展示「近7天低粉高赞案例」。
4. 三个赛道之间要有差异化，不能只是换关键词。
5. 只输出 JSON，不要代码围栏和额外说明。
```

### 4. 人设推荐 `self_media_recommend_personas_v1`

**变量 schema：** 在前者基础上新增 `nicheKey`、`nicheName`。

**system_role：**

```
你是一位自媒体人设定位顾问。你只输出合法 JSON。
```

**user_prompt 示例：**

```
请为以下用户推荐 3-4 个适合的人设定位，并给出默认内容支柱比例。

已选信息：
- 平台：{{platformKey}}
- 目标：{{goal}}
- 职业/经验领域：{{background}}
- 细分赛道：{{nicheName}}（{{nicheKey}}）

用户画像：
- 主业/副业：{{workType}}
- 每周投入时间：{{timePerWeek}}
- 期望月收入：{{incomeGoal}}
- 可接受不盈利周期：{{breakEvenPeriod}}
- 倾向内容形式：{{contentType}}
- 目标受众：{{audience}}
- 身份标签：{{identity}}
- 是否出镜/做视频：{{onCamera}}
- 补充说明：{{note}}

输出 JSON 结构：
{
  "personas":[
    {"key":"英文标识","name":"中文人设名","desc":"一句话说明，40字以内"}
  ],
  "defaultPillars":[
    {"name":"支柱一名称","percent":60},
    {"name":"支柱二名称","percent":20},
    {"name":"支柱三名称","percent":20}
  ]
}

要求：
1. key 使用英文小写+下划线。
2. 人设名要中文，且与平台/赛道调性匹配。
3. defaultPillars 中 percent 之和必须等于 100，建议给出 3 项。
4. 支柱名称要与赛道和人设一致（如干货复盘、个人故事、热点解读、案例拆解、工具清单等）。
5. 只输出 JSON，不要代码围栏和额外说明。
```

## 后端结构

```
project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/
├── controller/
│   └── SelfMediaPlanController.java
├── service/
│   ├── SelfMediaPlanService.java
│   ├── impl/
│   │   └── SelfMediaPlanServiceImpl.java
│   └── SelfMediaPlanAiService.java
├── entity/
│   └── SelfMediaPlan.java
├── mapper/
│   └── SelfMediaPlanMapper.java
├── dto/
│   ├── request/
│   │   ├── SavePlanRequest.java
│   │   ├── RecommendPlatformRequest.java
│   │   ├── RecommendGoalsRequest.java
│   │   ├── RecommendNichesRequest.java
│   │   └── RecommendPersonasRequest.java
│   └── SelfMediaRecommendationContext.java
├── vo/
│   ├── SelfMediaPlanVO.java
│   ├── RecommendPlatformResultVO.java
│   ├── GoalOptionVO.java
│   ├── NicheOptionVO.java
│   ├── PersonaOptionVO.java
│   ├── PillarVO.java
│   └── RecommendPersonasResultVO.java
└── enums/
    └── SelfMediaPlanErrorCode.java
```

公共 JSON 解析工具下沉：

```
project/shared/src/main/java/com/aichuangzuo/shared/utils/LlmJsonParser.java
```

提取 `SkillAnalyzeServiceImpl` 中已有的 `stripCodeFence`、`extractJsonObject`、修复 prompt 内未转义引号等逻辑，供 `SelfMediaPlanAiService` 复用。

## 前端结构

新增/修改：

```
project/user/web/src/api/selfMediaPlan.js
project/user/web/src/composables/useSelfMediaPlan.js
project/user/web/src/views/console/OnboardingIndex.vue
project/user/web/src/views/console/WorkbenchIndex.vue
```

`OnboardingIndex.vue` 改造点：

1. 保留平台选择 UI，但 AI 推荐弹窗改为调用 `POST /actions/recommend-platform`。
2. Step 2（定目标）保留背景/产品收集，新增「获取 AI 目标推荐」按钮，调用 `POST /actions/recommend-goals`，用返回的 `goals` 渲染选项按钮。
3. Step 3（选赛道）调用 `POST /actions/recommend-niches` 渲染赛道卡片。
4. Step 4（做人设）调用 `POST /actions/recommend-personas` 渲染人设卡片，并用返回的 `defaultPillars` 初始化 sliders。
5. Step 5（出方案）点击确认调用 `POST /` 保存方案；成功后设置 `localStorage.setItem('aichuangzuo_onboarding_done', '1')` 并跳转工作台。

`WorkbenchIndex.vue` 改造点：

1. `plan` 改为 `ref`，在 `onMounted` 中调用 `GET /current` 获取真实方案。
2. 若接口返回 `null` 且当前未在向导页，可跳转 `/console/onboarding`（保留 `localStorage` 作为轻量缓存）。

## 错误码

新增 `SelfMediaPlanErrorCode`（段 113xxx）：

| 错误码 | 含义 |
|--------|------|
| 113001 | 运营方案不存在 |
| 113002 | AI 推荐失败，请重试 |
| 113003 | 请选择自媒体平台 |
| 113004 | 请选择运营目标 |
| 113005 | 请选择细分赛道 |
| 113006 | 请选择人设定位 |

## 迁移文件

1. 用户端建表：

```
project/user/api/src/main/resources/db/migration/V1.0.0_086__create_self_media_plan_table.sql
```

2. 管理端/公共提示词种子：

```
project/admin/api/src/main/resources/db/migration/V2.0.0_088__seed_self_media_plan_prompts.sql
```

该脚本向 `c_ai_prompt` 插入 4 条用户端提示词（SQL 与上述「AI 提示词设计」一致）。

## 测试计划

1. **单元测试**
   - `LlmJsonParser`：代码围栏剥离、JSON 对象提取、prompt 内未转义引号修复、非法输入兜底。
   - `AiPromptRenderService` 对新增提示词的变量渲染和必填校验。

2. **API 测试**
   - 4 个推荐接口在缺失必填变量时返回正确错误码。
   - 保存方案后，`GET /current` 能完整返回。
   - 同一用户重复保存为更新而非新增。

3. **集成/手动测试**
   - 前端向导从平台选择到保存方案走通。
   - AI 推荐平台、目标、赛道、人设返回有效 JSON 并能渲染。
   - 工作台正确显示已保存方案。
   - 管理后台修改 `c_ai_prompt` 中对应提示词后，用户端下次调用生效。

## 风险与假设

- 假设 `c_ai_prompt` 表对用户端后端可读（共用数据库或已同步）。
- LLM 输出 JSON 可能不稳定，必须通过 `LlmJsonParser` 做围栏剥离、对象提取和失败兜底。
- 若 LLM 调用失败，前端应给出友好提示，允许用户重试；第一版不强制要求本地硬编码兜底。
- 该功能会触发多次 LLM 调用（最多 4 次），需观察模型成本和响应时长，必要时后续加缓存或合并 prompt。
