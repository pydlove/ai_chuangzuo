# 用户端立即订阅（测试支付）设计文档

**Goal:** 在 Pricing 页实现「立即订阅」按钮，点击后弹出测试支付输入框，输入 `123456` 即视为支付成功；成功后为用户开通/续期会员，若用户存在邀请人则给邀请人发放创作币奖励，并通过消息中心向双方发送订阅成功和奖励到账通知。

**Architecture:** 新增订单与会员两张表，新增订阅 API 统一处理支付校验、会员开通、邀请奖励、消息通知；前端 Pricing 页通过弹框收集测试支付码并调用 API，成功后刷新本地会员状态并跳转创作页。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + MySQL 8; Vue 3 + Ant Design Vue + Pinia; Playwright + pymysql E2E。

---

## Global Constraints

- 测试阶段不接入真实支付，支付码固定为 `123456`。
- 会员周期映射：月 = 30 天，季 = 90 天，年 = 365 天。
- 会员等级由 Pricing 页选择的套餐决定：基础版 / 专业版 / 旗舰版。
- 邀请关系以 `u_user_invite_relation` 表为准，`effective_status = 1` 表示有效。
- 消息通知复用现有 `u_message` + `MessageService.pushPersonal`。
- 所有 DB 迁移必须遵循 `docs/architecture/mysql-table-conventions.md`。

---

## 数据模型

### 1. `u_order`（测试阶段简版订单）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT UNSIGNED PK | 自增 |
| `order_no` | VARCHAR(32) UNIQUE | 订单编号，生成规则 `SUB{yyMMdd}{6位随机}` |
| `user_id` | BIGINT UNSIGNED | 下单用户 |
| `plan_key` | VARCHAR(32) | `basic` / `pro` / `flagship` |
| `cycle` | VARCHAR(16) | `month` / `quarter` / `year` |
| `amount` | DECIMAL(10,2) | 订单金额（取自 Pricing 页当前价格） |
| `status` | TINYINT | `0 pending`, `1 paid` |
| `paid_at` | DATETIME(3) | 支付时间 |
| `created_at` / `updated_at` | DATETIME(3) | 标准时间戳 |
| `tenant_id` | BIGINT | 固定 `0` |

### 2. `u_user_membership`（用户会员状态）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT UNSIGNED PK | 自增 |
| `user_id` | BIGINT UNSIGNED UNIQUE | 用户 ID |
| `level` | VARCHAR(32) | 当前等级：`basic` / `pro` / `flagship` |
| `started_at` | DATE | 本次会员开始日期 |
| `expires_at` | DATE | 会员到期日期 |
| `created_at` / `updated_at` | DATETIME(3) | 标准时间戳 |
| `tenant_id` | BIGINT | 固定 `0` |

**续期规则：**
- 若当前无会员或已过期，以今天为 `started_at`，按周期加天数。
- 若当前会员未过期，以 `expires_at` 为基准往后加天数，等级覆盖为新购买的等级。

---

## API 契约

### POST `/api/v1/user/membership/subscribe`

**Request Body:**
```json
{
  "planKey": "pro",
  "cycle": "year",
  "payCode": "123456",
  "amount": 503.2
}
```

**Response (success):**
```json
{
  "code": 200,
  "data": {
    "orderNo": "SUB260708A1B2C3",
    "level": "pro",
    "days": 365,
    "expiresAt": "2027-07-08",
    "inviterRewarded": true,
    "rewardAmount": 5
  }
}
```

**Response (error):**
- `payCode` 错误 → `400` + 错误码 `INVALID_PAY_CODE`
- 用户已存在有效同等级或更高级会员（可选拦截，本期不强制）→ 按续期处理

### GET `/api/v1/user/membership/me`

返回当前用户会员状态：
```json
{
  "code": 200,
  "data": {
    "hasMembership": true,
    "level": "pro",
    "levelName": "专业版",
    "expiresAt": "2027-07-08"
  }
}
```

---

## 后端处理流程

1. **校验支付码** `payCode == "123456"`，否则抛异常。
2. **生成订单** `u_order`，状态 `paid`，`paid_at = now()`。
3. **计算会员天数** `cycle → days (30/90/365)`。
4. **开通/续期会员** 写入 `u_user_membership`。
5. **发送订阅成功通知** 给用户本人：`MessageService.pushPersonal(userId, "membership", "订阅成功", ..., subType="subscribed")`。
6. **查询邀请人** `UserInviteRelationMapper.selectByInviteeId(userId)`，若存在且有效：
   - 按等级给邀请人加创作币：`basic=3`, `pro=5`, `flagship=10`。
   - 写 `u_user_coin_record` 流水，`biz_type = INVITE_REWARD`，`ref_id = order.id`。
   - 给邀请人发奖励通知：`MessageService.pushPersonal(inviterId, "reward", "邀请奖励到账", ..., subType=null)`。

---

## 前端流程

### Pricing.vue 改动

1. 给每个 `plan-btn` 绑定 `click`：
   ```js
   const handleSubscribe = (planKey) => { selectedPlan.value = planKey; modalVisible.value = true }
   ```
2. 新增 `a-modal` 弹框：
   - 标题：确认订阅 {planName}
   - 输入框：请输入测试支付码 `123456`
   - 按钮：取消 / 确认支付
3. 调用 `POST /api/v1/user/membership/subscribe`。
4. 成功后：
   - `message.success('订阅成功')`
   - 刷新本地会员状态（可复用现有 localStorage `aichuangzuo_membership` 或新增 Pinia store）
   - `router.push('/console/create')`

### 会员状态展示

- ConsoleLayout 顶部会员徽章、个人中心会员卡读取 `/api/v1/user/membership/me` 或本地 store。
- 本期优先保证本地 `localStorage` + API 刷新，后续可迁移到 Pinia。

---

## 消息通知文案

### 订阅成功（发给购买者）

- `msg_type`: `membership`
- `sub_type`: `subscribed`
- `title`: 订阅成功
- `summary`: 您已成功开通 {levelName}，有效期至 {expiresAt}
- `content`: 完整欢迎/权益文案

### 邀请奖励到账（发给邀请人）

- `msg_type`: `reward`
- `title`: 邀请奖励到账
- `summary`: 好友 {nickname} 订阅 {levelName}，您获得 {reward} 创作币
- `content`: 详细说明

---

## 安全与测试

- 支付码写死为 `123456`，生产环境必须替换为真实支付网关。
- 订单 `order_no` 唯一索引，防止重复提交。
- 邀请奖励幂等：同一订单只发一次，通过 `u_user_coin_record.ref_id` 去重。

### 测试覆盖

1. **后端单元测试**
   - `MembershipServiceTest`: 支付码错误、开通新会员、续期会员、邀请人奖励、消息通知。
2. **Flyway 迁移验证**
   - 重启 user-api 自动应用 `V1.0.0_020__add_order_and_membership.sql`。
3. **前端构建**
   - `npm run build` 通过。
4. **E2E**
   - `tests/e2e/verify_subscription.py`: 有邀请人/无邀请人两条路径，验证会员开通、消息通知、奖励到账。

---

## 关键文件清单

- 后端：
  - `db/migration/V1.0.0_020__add_order_and_membership.sql`
  - `modules/membership/entity/Order.java`
  - `modules/membership/entity/UserMembership.java`
  - `modules/membership/mapper/*.java` + `*.xml`
  - `modules/membership/service/MembershipService.java` + impl
  - `modules/membership/controller/MembershipController.java`
  - `modules/message/enums/MessageSubType.java`（新增 `REWARD` 类型讨论）
  - `modules/membership/service/MembershipServiceTest.java`
- 前端：
  - `views/Pricing.vue`
  - `api/membership.js`
  - `views/console/ConsoleLayout.vue`（会员状态读取）
- E2E：
  - `tests/e2e/verify_subscription.py`

---

## 风险与回滚

- **写死支付码**：测试专用，生产必须替换。
- **邀请奖励写死金额**：后续应从配置表读取。
- **会员等级覆盖**：续期时新购买等级覆盖旧等级，符合业务预期。
- 迁移涉及新增表，回滚只需删除 `V1.0.0_020` 迁移并清理 Flyway 记录。