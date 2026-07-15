# 会员权益系统设计文档

**Goal:** 设计数据驱动的会员权益系统，覆盖 Pricing 页承诺的全部 15 项权益，支持运行时校验和灵活扩展。

**Architecture:** 两张核心表（权益定义 + 套餐权益值）+ 一张用量表，用户端 API 新增 benefit 模块提供权益查询和校验，前端启动时加载权益到 Pinia store，页面按权益值控制功能显隐。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + MySQL 8 + Caffeine; Vue 3 + Pinia + Ant Design Vue。

---

## Global Constraints

- 权益定义和套餐权益值通过 Flyway 迁移插入初始数据，管理端暂不做配置页面。
- 权益校验在用户端 API 进行，管理端不涉及。
- `plan_key → List<PlanBenefit>` 使用 Caffeine 缓存，TTL 10 分钟。
- quota 类型权益需要 `u_benefit_usage` 表跟踪用量，按自然月统计。
- `history_days` 中 -1 表示永久。
- 所有 DB 迁移遵循 `docs/architecture/mysql-table-conventions.md`。

---

## 1. 数据模型

### 1.1 `u_benefit`（权益定义表）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT UNSIGNED PK | 自增 |
| `code` | VARCHAR(64) UNIQUE NOT NULL | 权益编码，如 `ai_article_quota` |
| `name` | VARCHAR(64) NOT NULL | 权益名称，如"AI 文章生成" |
| `type` | VARCHAR(16) NOT NULL | `boolean` / `quota` / `tier` |
| `description` | VARCHAR(256) | 描述 |
| `sort_order` | INT NOT NULL DEFAULT 0 | 排序 |
| `status` | TINYINT UNSIGNED NOT NULL DEFAULT 1 | 0=停用， 1=启用 |
| `created_at` / `updated_at` | DATETIME(3) | 标准时间戳 |
| `created_by` / `updated_by` | BIGINT UNSIGNED | 标准审计字段 |
| `tenant_id` | BIGINT UNSIGNED | 固定 0 |

### 1.2 `u_plan_benefit`（套餐权益值表）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT UNSIGNED PK | 自增 |
| `plan_key` | VARCHAR(32) NOT NULL | 套餐：basic / pro / flagship |
| `benefit_code` | VARCHAR(64) NOT NULL | 关联 `u_benefit.code` |
| `benefit_value` | VARCHAR(128) NOT NULL | 值：boolean 存 "true"/"false"，quota 存数字，tier 存等级标识 |
| `created_at` / `updated_at` | DATETIME(3) | 标准时间戳 |
| `created_by` / `updated_by` | BIGINT UNSIGNED | 标准审计字段 |
| `tenant_id` | BIGINT UNSIGNED | 固定 0 |

联合唯一索引：`uk_plan_benefit (plan_key, benefit_code)`

### 1.3 `u_benefit_usage`（权益用量表）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT UNSIGNED PK | 自增 |
| `user_id` | BIGINT UNSIGNED NOT NULL | 用户 ID |
| `benefit_code` | VARCHAR(64) NOT NULL | 权益编码 |
| `period` | VARCHAR(16) NOT NULL | 周期标识，如 `2026-07`（月度） |
| `used_count` | INT UNSIGNED NOT NULL DEFAULT 0 | 已用量 |
| `created_at` / `updated_at` | DATETIME(3) | 标准时间戳 |
| `created_by` / `updated_by` | BIGINT UNSIGNED | 标准审计字段 |
| `tenant_id` | BIGINT UNSIGNED | 固定 0 |

联合唯一索引：`uk_benefit_usage (user_id, benefit_code, period)`

---

## 2. 初始 15 项权益数据

| code | name | type | basic | pro | flagship | sort_order |
|---|---|---|---|---|---|---|
| `ai_article_quota` | AI 文章生成 | quota | 30 | 100 | 300 | 1 |
| `export_word` | 导出 Word | boolean | true | true | true | 2 |
| `copy_text` | 复制正文 | boolean | true | true | true | 3 |
| `ai_topic` | AI 选题灵感 | boolean | true | true | true | 4 |
| `ai_title_optimize` | AI 标题优化 | boolean | false | true | true | 5 |
| `online_edit` | 在线编辑 | boolean | false | true | true | 6 |
| `style_custom` | 写作风格定制 | tier | none | preset | custom | 7 |
| `seo_keywords` | SEO 关键词建议 | boolean | false | false | true | 8 |
| `template_access` | 文章模板 | tier | basic_8 | all_20 | all_custom | 9 |
| `sticker_quota` | 贴图生成 | quota | 5 | 30 | 100 | 10 |
| `batch_generate` | 批量生成/改写 | boolean | false | false | true | 11 |
| `batch_export` | 批量导出 | boolean | false | false | true | 12 |
| `history_days` | 历史记录 | quota | 30 | -1 | -1 | 13 |
| `queue_priority` | 生成队列优先级 | tier | standard | priority | express | 14 |
| `queue_max_tasks` | 队列任务数 | quota | 1 | 5 | 10 | 15 |

---

## 3. 权益校验流程

```
用户请求 → JWT 解析 userId → 查 u_user_membership 得 plan_key
→ 查 u_plan_benefit 得该套餐所有权益 → Caffeine 缓存
→ 校验具体权益值
```

- **boolean 校验**：值为 `"true"` 放行，`"false"` 返回 403 + 错误码。
- **quota 校验**：查 `u_benefit_usage` 当前周期已用量，`used_count < benefit_value` 放行并 +1，否则返回 403 + 额度不足。
- **tier 校验**：返回值给业务逻辑自行判断（如队列优先级决定任务排队策略）。

### 缓存策略

- **缓存 key**：`plan_benefits:{plan_key}`
- **缓存内容**：`List<PlanBenefit>`（该套餐的所有权益值）
- **TTL**：10 分钟
- **失效**：自然过期，不主动失效（管理端暂无配置页面）

---

## 4. 用户端 API — `benefit` 模块

### 包结构

```
com.aichuangzuo.user.modules.benefit/
├── controller/
│   └── BenefitController.java
├── service/
│   ├── BenefitService.java
│   └── impl/
│       └── BenefitServiceImpl.java
├── entity/
│   ├── Benefit.java
│   ├── PlanBenefit.java
│   └── BenefitUsage.java
├── mapper/
│   ├── BenefitMapper.java
│   ├── PlanBenefitMapper.java
│   └── BenefitUsageMapper.java
├── enums/
│   └── BenefitErrorCode.java
└── vo/
    └── UserBenefitVO.java
```

### API 端点

#### 4.1 查询当前用户权益

```
GET /api/v1/user/benefits/me
```

**返回：**

```json
{
  "planKey": "pro",
  "planName": "专业版",
  "expiresAt": "2026-08-14",
  "benefits": [
    { "code": "ai_article_quota", "name": "AI 文章生成", "type": "quota", "value": "100" },
    { "code": "ai_title_optimize", "name": "AI 标题优化", "type": "boolean", "value": "true" },
    { "code": "queue_max_tasks", "name": "队列任务数", "type": "quota", "value": "5" }
  ]
}
```

无会员时 `planKey` 返回 `"free"`，`benefits` 返回空列表（前端按默认无权限处理）。

#### 4.2 校验单项权益

```
POST /api/v1/user/benefits/check/{code}
```

**返回（boolean 通过）：**

```json
{ "allowed": true, "code": "ai_title_optimize", "type": "boolean", "value": "true" }
```

**返回（quota 通过）：**

```json
{ "allowed": true, "code": "ai_article_quota", "type": "quota", "value": "100", "used": 28, "remaining": 72 }
```

**返回（不通过）：**

```json
{ "allowed": false, "code": "ai_title_optimize", "type": "boolean", "value": "false", "message": "当前套餐不支持此功能，请升级" }
```

#### 4.3 消费配额（内部调用）

```
POST /api/v1/user/benefits/consume/{code}
```

quota 类型专用。校验通过后 `used_count + 1`。返回剩余额度。

**返回：**

```json
{ "allowed": true, "code": "ai_article_quota", "remaining": 71 }
```

### 错误码

| 错误码 | 说明 |
|---|---|
| 118001 | 权益不存在 |
| 118002 | 当前套餐不支持此功能 |
| 118003 | 额度已用完 |

---

## 5. 前端接入

### 5.1 Pinia Store

新增 `src/stores/benefits.js`：

```js
// 登录后调用 loadBenefits() 加载
// benefits = { ai_article_quota: '100', ai_title_optimize: 'true', ... }
// hasBenefit(code) → boolean
// benefitValue(code) → string
// benefitRemaining(code) → number (quota 类型)
```

### 5.2 页面控制

**功能开关类（boolean）：**
- `v-if="hasBenefit('ai_title_optimize')"` 控制 AI 标题优化按钮显隐
- `v-if="hasBenefit('online_edit')"` 控制在线编辑器入口
- 不通过时也可显示但置灰 + 提示"升级会员解锁"

**额度类（quota）：**
- 生成页显示"本月还可生成 X 篇"
- 超额时弹窗提示升级会员

**等级类（tier）：**
- 模板库按 `template_access` 值过滤可显示的模板
- 队列优先级影响提交任务时的提示文案

---

## 6. 扩展方式

新增一项权益（如"AI 封面图生成"）：

1. `INSERT INTO u_benefit (code, name, type, sort_order) VALUES ('ai_cover_image', 'AI封面图', 'boolean', 16)`
2. `INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES ('basic','ai_cover_image','false'), ('pro','ai_cover_image','true'), ('flagship','ai_cover_image','true')`
3. 前端用 `hasBenefit('ai_cover_image')` 控制入口

不改表结构、不改后端代码。

---

## 7. 不在本次范围

- 管理端权益配置页面（后续需要时再补）
- 权益用量统计报表
- 权益变更历史记录
