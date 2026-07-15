# 表结构总览

爱创作项目 MySQL 数据字典。按业务域拆分，每份文档列出该域下所有表的用途、字段含义、索引与关联关系。

## 命名约定

| 前缀 | 含义 | 归属 |
| --- | --- | --- |
| `u_` | 用户端业务表 | 主要由 user-api 读写，admin-api 只读或运营操作 |
| `a_` | 管理端业务表 | 主要由 admin-api 读写 |
| `t_` | 共享业务表 | 两端都可能读写（如提示词模板、学院文章） |
| 无前缀 | 跨端配置表 | 如 `hot_search_*`、`reminder_config` |

通用列约定：每张表都有 `id` 主键；多数业务表带 `tenant_id`（预留多租户，当前固定 0）、`is_deleted`（逻辑删除）、`created_at` / `updated_at` / `created_by` / `updated_by` 审计列。详细规范见 `docs/architecture/mysql-table-conventions.md`。

## 域索引

### [01-用户与认证](./01-用户与认证.md) — 4 张表

| 表 | 用途 |
| --- | --- |
| `u_user` | C 端用户主表：账号、邀请码、创作币余额、会员快照 |
| `u_user_login_log` | 用户登录/注册行为流水，安全审计用 |
| `u_user_invite_relation` | 邀请人 ↔ 被邀请人绑定关系，邀请奖励依据 |
| `u_ip_register_limit` | 按 IP 累计注册数，防批量注册 |

### [02-会员订单与收益榜单](./02-会员订单与收益榜单.md) — 6 张表

| 表 | 用途 |
| --- | --- |
| `u_user_membership` | 用户会员状态真源表（`u_user` 上是冗余快照） |
| `u_order` | 会员购买订单，含支付状态机 |
| `u_user_coin_record` | 创作币流水（充值、消费、奖励等） |
| `u_earnings_record` | 用户收益通用账本 |
| `u_leaderboard_income_submission` | 自媒体收入申报，用于榜单排名 |
| `u_leaderboard_reward_record` | 榜单奖励发放记录 |

### [03-风格与热搜](./03-风格与热搜.md) — 5 张表

| 表 | 用途 |
| --- | --- |
| `u_user_style` | 用户写作风格（自建 + 预设），含审核字段 |
| `u_style_market` | 风格市场，用户间风格分享/交易 |
| `hot_search_platform` | 热搜平台配置（微博、抖音、知乎等） |
| `hot_search_daily` | 每日热搜榜单数据 |
| `hot_search_config` | 热搜抓取任务配置 |

### [04-消息文章反馈](./04-消息文章反馈.md) — 5 张表

| 表 | 用途 |
| --- | --- |
| `u_message` | 站内消息（系统通知、运营消息） |
| `u_message_read` | 用户消息已读记录 |
| `u_article` | 用户已生成的文章作品 |
| `u_draft` | 用户创作草稿 |
| `u_feedback` | 用户反馈与建议 |

### [05-管理端基础](./05-管理端基础.md) — 9 张表

| 表 | 用途 |
| --- | --- |
| `a_admin_user` | 管理员账号 |
| `a_admin_login_log` | 管理员登录日志 |
| `a_role` | RBAC 角色 |
| `a_permission` | RBAC 权限点 |
| `a_admin_user_role_rel` | 管理员 ↔ 角色关联 |
| `a_role_permission_rel` | 角色 ↔ 权限关联 |
| `a_model_config` | AI 模型配置（API Key、模型名、单价等） |
| `reminder_config` | 会员到期提醒规则配置 |
| `u_reminder_send_log` | 到期提醒发送记录 |

### [06-AI生成](./06-AI生成.md) — 7 张表

| 表 | 用途 |
| --- | --- |
| `a_generation_task` | 文章生成任务队列，worker FIFO 拉取跑 12 阶段流水线 |
| `a_generation_history` | 终态任务归档冷存储 |
| `a_generation_config` | 生成运行时单行配置（worker 池、重试、AI 默认参数） |
| `a_generation_call_log` | 每阶段 AI 调用日志（请求/响应/token/耗时） |
| `t_prompt_template` | 提示词模板主表 |
| `t_prompt_template_stage` | 模板 12 阶段配置（每阶段 prompt + 模型参数） |
| `t_prompt_template_version` | 模板版本快照，任务按版本锁定 |

### [07-创作学院与Banner](./07-创作学院与Banner.md) — 4 张表

| 表 | 用途 |
| --- | --- |
| `t_article` | 创作学院文章内容 |
| `t_article_category` | 学院分类，支持推荐位 |
| `t_learn_banner` | 学院页 Banner |
| `a_home_banner` | C 端首页 Banner |

## 总计

40 张表：用户端 `u_` 18 张、管理端 `a_` 14 张、共享 `t_` 5 张、跨端配置 3 张。

## 维护说明

- 表结构真源：`project/user/api/src/main/resources/db/migration/` 与 `project/admin/api/src/main/resources/db/migration/` 下的 Flyway 迁移。
- 新增/修改表后，请同步更新对应域文档与本索引。
- 本文档由迁移文件 + 实体类反向整理，最后更新：2026-07-15。
