# 运营抽奖活动设计文档

## 1. 背景与目标

为「爱创作」平台新增运营抽奖活动能力，支持按轮次开启/关闭。用户可消耗抽奖次数抽取兑换码，兑换创作币、会员权益或订阅折扣券。管理端可配置活动、奖项、概率、库存上限、兑换码规则，并可查看兑换/抽奖记录。

## 2. 范围

本期支持：
- 活动轮次管理（开始/结束时间、状态、每人免费次数）。
- 奖项配置：特等奖、一等奖、二等奖、三等奖、四等奖、谢谢回顾，每个奖项独立配置概率与全局库存上限。
- 奖励类型：创作币、会员天数/周期、订阅折扣券、谢谢回顾。
- 用户端抽奖、兑换码展示、兑换奖励。
- 邀请新用户注册成功获得额外抽奖次数。
- 中奖展示墙，支持运营配置机器人中奖记录。
- 管理端兑换记录、抽奖记录菜单。
- 基础风控与并发防超抽。

本期不支持：
- 实物奖品、第三方权益。
- 现金/可提现余额奖励。
- 复杂的设备指纹/行为风控（仅做基础限制）。

## 3. 奖励类型与叠加规则

| 奖励类型 | 示例 | 叠加规则 |
|---|---|---|
| 创作币 | 100 创作币 | 可叠加，直接入账余额，购买时可抵扣。 |
| 会员天数 | pro 3 天 | 同档位会员：剩余天数叠加。不同档位：不能重叠，进入待生效队列，当前会员到期后自动切换。 |
| 会员周期 | pro 1 个月 | 同档位会员：剩余天数叠加。不同档位：进入待生效队列。 |
| 折扣券 | pro 年费 8 折券 | 不可叠加，一笔订单只能用一张。在现有折扣（新人折扣、升级抵扣、创作币抵扣）基础上再打折/减免。 |
| 谢谢回顾 | 无 | 无奖励。 |

## 4. 数据模型

所有表建在用户端数据库，前缀 `u_`。

### 4.1 `u_lottery_campaign` — 活动轮次

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| name | VARCHAR(64) | 活动名称 |
| description | VARCHAR(256) | 活动描述 |
| start_time | DATETIME(3) | 开始时间 |
| end_time | DATETIME(3) | 结束时间 |
| status | TINYINT | 0-draft, 1-ongoing, 2-ended, 3-disabled |
| free_draws_per_user | INT | 每轮免费次数，默认 1 |
| created_at / updated_at | DATETIME(3) | 审计字段 |
| created_by / updated_by | BIGINT UNSIGNED | 审计字段 |

### 4.2 `u_lottery_prize_tier` — 奖项配置

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| campaign_id | BIGINT UNSIGNED FK | 所属活动 |
| tier_key | VARCHAR(32) | 奖项标识，如 grand/first/second/third/fourth/thanks |
| tier_name | VARCHAR(64) | 展示名，如「特等奖」 |
| probability | DECIMAL(10,8) | 中奖概率，所有启用项之和 ≤ 1 |
| max_win_count | INT | 全局可中次数上限，null 表示不限 |
| remaining_win_count | INT | 剩余可中次数 |
| reward_type | VARCHAR(16) | coin / membership / coupon / none |
| reward_value_json | JSON | 奖励参数 |
| code_prefix | VARCHAR(16) | 兑换码前缀 |
| code_length | INT | 兑换码总字符数（含前缀），如 prefix=GRAND, length=12，则生成 GRANDxxxxxx |
| code_validity_days | INT | 兑换码有效期天数 |
| sort_order | INT | 展示排序 |
| status | TINYINT | 0-停用，1-启用 |

`reward_value_json` 示例：

```json
// 创作币
{"amount": 100}

// 会员天数
{"plan_key": "pro", "days": 3}

// 会员周期
{"plan_key": "pro", "cycle": "month"}

// 折扣券
{
  "coupon_type": "percent",
  "discount_value": 0.8,
  "applicable_cycle": "year",
  "applicable_plan": "pro"
}
```

### 4.3 `u_lottery_draw_chance` — 抽奖次数池

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| campaign_id | BIGINT UNSIGNED FK | 所属活动 |
| user_id | BIGINT UNSIGNED FK | 用户 |
| chance_type | VARCHAR(16) | free / invite |
| source_invite_relation_id | BIGINT UNSIGNED | invite 次数来源，free 时为 null |
| status | VARCHAR(16) | available / used |
| created_at / used_at | DATETIME(3) | 创建/使用时间 |

唯一键：`(campaign_id, user_id, chance_type)`，业务上保证每轮每人只有一条 `free` 记录，邀请类型可有多条。

### 4.4 `u_lottery_draw_record` — 抽奖记录

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| biz_no | VARCHAR(64) | 业务唯一编号 |
| campaign_id | BIGINT UNSIGNED FK | 活动 |
| user_id | BIGINT UNSIGNED FK | 用户 |
| tier_id | BIGINT UNSIGNED FK | 命中奖项 |
| code_id | BIGINT UNSIGNED FK | 生成的兑换码，未中奖为 null |
| draw_type | VARCHAR(16) | free / invite |
| invite_relation_id | BIGINT UNSIGNED | invite 来源 |
| created_at | DATETIME(3) | 抽奖时间 |

### 4.5 `u_lottery_redemption_code` — 兑换码

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| code | VARCHAR(64) | 兑换码，唯一 |
| campaign_id | BIGINT UNSIGNED FK | 活动 |
| tier_id | BIGINT UNSIGNED FK | 奖项 |
| drawer_user_id | BIGINT UNSIGNED | 中奖人，仅追溯不限制兑换 |
| reward_type / reward_value_json | 同奖项 | 快照奖励内容 |
| status | VARCHAR(16) | unused / used / expired |
| used_by | BIGINT UNSIGNED | 兑换人 |
| used_at | DATETIME(3) | 兑换时间 |
| expires_at | DATETIME(3) | 过期时间 |
| created_at | DATETIME(3) | 创建时间 |

### 4.6 `u_user_coupon` — 用户优惠券

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| user_id | BIGINT UNSIGNED FK | 用户 |
| coupon_code | VARCHAR(64) | 券码，唯一 |
| coupon_type | VARCHAR(16) | percent / fixed_amount |
| discount_value | DECIMAL(10,4) | 折扣值，如 0.8 或 50 |
| applicable_cycle | VARCHAR(16) | month / quarter / year / all |
| applicable_plan | VARCHAR(16) | basic / pro / flagship / all |
| status | VARCHAR(16) | unused / used / expired |
| valid_start / valid_end | DATETIME(3) | 有效期 |
| used_order_id | BIGINT UNSIGNED | 使用订单 |
| created_at | DATETIME(3) | 创建时间 |

### 4.7 `u_lottery_display_winner` — 中奖展示墙

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| campaign_id | BIGINT UNSIGNED FK | 活动 |
| tier_id | BIGINT UNSIGNED FK | 奖项 |
| user_id | BIGINT UNSIGNED | 关联用户 |
| nickname / avatar_url | VARCHAR | 展示信息，支持覆盖 |
| prize_name | VARCHAR(64) | 展示奖品名 |
| win_time | DATETIME(3) | 展示时间 |
| is_real | TINYINT | 0-机器人/运营配置，1-真实中奖 |
| sort_order | INT | 排序 |
| status | TINYINT | 0-隐藏，1-展示 |

### 4.8 `u_user_membership_pending` — 待生效会员

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| user_id | BIGINT UNSIGNED FK | 用户 |
| plan_key | VARCHAR(32) | basic / pro / flagship |
| days | INT | 天数 |
| planned_start_at | DATE | 计划生效日期 |
| status | VARCHAR(16) | pending / activated / expired |
| source_code_id | BIGINT UNSIGNED | 来源兑换码 |
| created_at / activated_at | DATETIME(3) | 创建/激活时间 |

### 4.9 `u_lottery_risk_log` — 风控日志（MVP 可延后）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| campaign_id | BIGINT UNSIGNED FK | 活动 |
| user_id | BIGINT UNSIGNED FK | 用户 |
| action | VARCHAR(16) | draw / redeem / invite |
| risk_type | VARCHAR(32) | ip_limit / frequency 等 |
| detail_json | JSON | 详情 |
| created_at | DATETIME(3) | 时间 |

## 5. 活动与奖项配置

### 5.1 活动生命周期

- `draft`：管理端可编辑配置，用户不可见。
- `ongoing`：到达开始时间或手动开启，用户可参与。
- `ended`：到达结束时间或手动关闭，停止新抽奖。
- `disabled`：手动关闭，保留数据。

### 5.2 奖项概率校验

保存奖项时校验：所有 `status=1` 的奖项 `probability` 之和 ≤ 1。若 < 1，差额自动视为「谢谢回顾」概率。

### 5.3 库存上限

每个奖项配置 `max_win_count`。抽奖命中该奖项时，先原子扣减 `remaining_win_count`：

```sql
UPDATE u_lottery_prize_tier
SET remaining_win_count = remaining_win_count - 1
WHERE id = ? AND remaining_win_count > 0;
```

若影响行数为 0，则该奖项库存耗尽，命中结果降级为「谢谢回顾」。`max_win_count` 为 null 时表示不限制。

## 6. 抽奖流程

### 6.1 获取抽奖次数

用户进入活动页，后端返回：
- 免费次数：每轮 1 次，未使用则显示可用。
- 邀请次数：该用户本轮 `chance_type=invite` 且 `status=available` 的记录数。

免费次数在首次抽奖时懒创建，使用 `INSERT IGNORE` 或唯一键处理并发。被邀请人注册成功后，`InviteRewardService.rewardAfterRegister()` 若检测到存在进行中的活动，同步为邀请人创建一条 `chance_type=invite` 的 `u_lottery_draw_chance` 记录。

### 6.2 抽奖接口

1. 校验活动进行中。
2. 消费一条可用抽奖次数：
   ```sql
   UPDATE u_lottery_draw_chance
   SET status = 'used', used_at = NOW()
   WHERE id = ? AND status = 'available';
   ```
   影响行数 ≠ 1 则返回次数不足。
3. 按概率命中奖项（内存随机数）。
4. 若命中非 thanks 奖项，原子扣减该奖项 `remaining_win_count`；若库存不足则降级为 thanks。
5. 生成兑换码并写入 `u_lottery_redemption_code`。
6. 写入 `u_lottery_draw_record`。
7. 真实中奖写入 `u_lottery_display_winner`（is_real=1）。
8. 返回奖项信息、兑换码、过期时间。

### 6.3 并发控制

- 次数消费：数据库行锁（UPDATE WHERE status='available'）。
- 库存扣减：数据库原子 UPDATE WHERE remaining_win_count > 0。
- 兑换码唯一性：code 字段唯一索引，冲突重试。
- 免费次数创建：唯一键 `(campaign_id, user_id, chance_type)` 防并发重复。

## 7. 邀请获得次数

### 7.1 触发条件

被邀请人通过邀请码完成注册，`InviteRewardService.rewardAfterRegister()` 成功后，若存在进行中的活动，则为邀请人创建一条 `chance_type=invite` 的 `u_lottery_draw_chance`。

### 7.2 复用现有风控

邀请关系本身的风控复用现有注册逻辑：
- IP 注册限制（`u_ip_register_limit`）。
- 邮箱验证码。
- 不能自邀请、不能循环邀请。
- 7 天补绑窗口。

本期不额外增加设备指纹、邮箱域校验。

## 8. 兑换与奖励发放

### 8.1 兑换流程

1. 用户输入兑换码。
2. 校验码存在、未使用、未过期。
3. 根据 `reward_type` 发放奖励，码状态变为 used，记录 `used_by` 和 `used_at`。

### 8.2 创作币

调用 `CoinRecordService.grant()`，biz_type = `lottery_coin_reward`。

### 8.3 会员权益

- 无当前会员或同档会员：立即生效，调用 `MembershipService.extendMembership()`。
- 不同档会员：写入 `u_user_membership_pending`，`planned_start_at` 为当前会员到期日。当前会员到期后由定时任务按 `created_at` 先后顺序激活；若同一到期日有多条待生效记录，先创建的先激活。

### 8.4 折扣券

写入 `u_user_coupon`，兑换时即绑定到当前用户，不可转让。订阅下单时：
- 用户从可用券中选择一张。
- 校验 `applicable_cycle` 与 `applicable_plan`：`all` 表示匹配任意周期/套餐，否则必须完全匹配。
- 券在现有折扣后应用：
  - percent：最终价 × discount_value。
  - fixed_amount：最终价 - discount_value，最低 0。
- 一笔订单只能用一张券。

> 注：折扣券需要与现有订阅下单流程（`MembershipService.subscribe` / 订单预览接口）集成，订单表需支持 coupon 抵扣字段。

## 9. 中奖展示墙

- 真实用户中奖后自动写入 `u_lottery_display_winner`（is_real=1）。
- 运营可在管理端新增/编辑机器人记录（is_real=0）。
- 前端接口按 `win_time` 倒序返回，运营通过 `status` 和 `sort_order` 控制展示。

## 10. 风控

- 邀请层：复用现有 IP/邮箱/循环邀请限制。
- 抽奖层：单用户次数通过数据库行锁控制；同 IP 高频抽奖记录风控日志。
- 兑换层：兑换码一人一码，绑定 redeemer；同码不可重复兑换。
- 展示墙：机器人账号需为 `u_user` 中真实存在的用户。

## 11. 管理端 API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/v1/admin/lottery/campaigns | 活动列表 |
| POST | /api/v1/admin/lottery/campaigns | 创建活动 |
| PUT | /api/v1/admin/lottery/campaigns/{id} | 编辑活动 |
| POST | /api/v1/admin/lottery/campaigns/{id}/open | 开启活动 |
| POST | /api/v1/admin/lottery/campaigns/{id}/close | 关闭活动 |
| GET | /api/v1/admin/lottery/campaigns/{id}/tiers | 奖项列表 |
| POST | /api/v1/admin/lottery/campaigns/{id}/tiers | 新增/更新奖项 |
| DELETE | /api/v1/admin/lottery/campaigns/{id}/tiers/{tierId} | 删除奖项 |
| GET | /api/v1/admin/lottery/redemption-codes | 兑换记录（支持筛选导出） |
| GET | /api/v1/admin/lottery/draw-records | 抽奖记录 |
| GET | /api/v1/admin/lottery/display-winners | 展示墙列表 |
| POST | /api/v1/admin/lottery/display-winners | 新增机器人展示记录 |
| PUT | /api/v1/admin/lottery/display-winners/{id} | 编辑展示记录 |
| DELETE | /api/v1/admin/lottery/display-winners/{id} | 删除展示记录 |

## 12. 用户端 API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/v1/user/lottery/campaigns/current | 当前活动及奖项 |
| GET | /api/v1/user/lottery/chances | 我的剩余次数 |
| POST | /api/v1/user/lottery/draw | 抽奖 |
| POST | /api/v1/user/lottery/redeem | 兑换奖励 |
| GET | /api/v1/user/lottery/my-codes | 我的兑换码 |
| GET | /api/v1/user/lottery/my-coupons | 我的优惠券 |
| GET | /api/v1/user/lottery/display-winners | 中奖展示墙 |

## 13. 前端页面

- 用户端：新增「抽奖活动」页面（可复用 console 或 landing 入口）。
- 管理端：在「运营活动」下新增「抽奖活动」「兑换记录」「抽奖记录」菜单。

## 14. 非目标

- 实物奖品物流管理。
- 复杂的反欺诈算法（设备指纹、行为分析）。
- 多活动同时在线（本期先支持一个进行中的活动）。
- 积分体系。

## 15. 关键决策

- 概率池 + 每奖项全局库存上限，兼顾灵活性与成本可控。
- 兑换码动态生成、可转让，兑换时绑定 redeemer。
- 会员不同档位进入待生效队列，到期自动切换。
- 折扣券不可叠加，在现有折扣基础上再打折。
- 邀请风控复用现有注册/IP 限制，不做额外加重。
