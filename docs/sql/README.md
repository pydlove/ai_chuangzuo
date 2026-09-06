# 数据库表结构按业务聚合
本目录将 `aichuangzuo` 库全部表按业务域聚合，每个文件一个业务域。
DDL 来自本地开发库（Flyway 全部迁移执行后的最终状态），仅供查阅，不用于执行。

| 文件 | 业务域 | 表数 |
|---|---|---|
| `01_user_account__用户与账号.sql` | 用户与账号 | 11 |
| `02_membership_benefit__会员与权益.sql` | 会员与权益 | 7 |
| `03_order_payment_assets__订单支付与资产.sql` | 订单支付与资产 | 11 |
| `04_creation_generation__AI 创作与生成.sql` | AI 创作与生成 | 15 |
| `05_style_skill_market__风格与风格市场.sql` | 风格与风格市场 | 5 |
| `06_topic_title__选题与标题.sql` | 选题与标题 | 5 |
| `07_message_notify__消息与通知.sql` | 消息与通知 | 5 |
| `08_hot_search__热搜抓取.sql` | 热搜抓取 | 4 |
| `09_learn_academy__创作学院.sql` | 创作学院 | 3 |
| `10_lottery__抽奖活动.sql` | 抽奖活动 | 7 |
| `11_commission__约稿征稿.sql` | 约稿征稿 | 2 |
| `12_self_media_plan__自媒体运营方案.sql` | 自媒体运营方案 | 5 |
| `13_admin_system__管理端与系统配置.sql` | 管理端与系统配置 | 22 |
