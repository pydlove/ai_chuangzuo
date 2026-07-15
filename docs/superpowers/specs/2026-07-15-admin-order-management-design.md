# 管理端订单管理设计文档

**Goal:** 在管理端新增「订单管理」模块，管理员可查看订单列表、执行订单操作（标记已支付、退款、取消）、手动调整/发放会员，并查看营收统计数据（卡片 + 图表）。

**Architecture:** 管理端 API 新增 `order` 模块，复用 `u_order` 和 `u_user_membership` 表（新增退款/操作相关字段）；管理端前端新增「订单列表」和「数据统计」两个页面，侧边栏菜单位于「系统设置」上方。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + MySQL 8; Vue 3 + Ant Design Vue + ECharts。

---

## Global Constraints

- 订单和会员数据在用户端 `u_order` / `u_user_membership` 表中，管理端直接读写同库。
- 管理端操作需记录操作人（`operator_id`）和操作备注（`admin_remark`），便于审计。
- 退款操作需回退会员时长，但不自动追回已发放的邀请奖励（测试阶段简化处理）。
- 手动发放会员生成 0 元订单，状态直接为已支付。
- 手动调整会员不创建订单，直接修改会员表。
- 所有 DB 迁移遵循 `docs/architecture/mysql-table-conventions.md`。

---

## 1. 数据库变更

### Flyway 迁移：`V2.0.0_036__add_order_admin_fields.sql`

对 `u_order` 表追加字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `status` | 扩展枚举 | 新增 `2=已退款`, `3=已取消`（现有 0=待支付, 1=已支付 不变） |
| `refunded_at` | `DATETIME(3) NULL` | 退款时间 |
| `refund_reason` | `VARCHAR(256) NULL` | 退款原因 |
| `admin_remark` | `VARCHAR(256) NULL` | 管理员操作备注（如"手动发放""手动调整"） |
| `operator_id` | `BIGINT UNSIGNED NULL` | 执行操作的管理员 ID |

新增索引：

```sql
ALTER TABLE u_order ADD INDEX idx_u_order_status (status);
ALTER TABLE u_order ADD INDEX idx_u_order_paid_at (paid_at);
ALTER TABLE u_order ADD INDEX idx_u_order_created_at (created_at);
```

---

## 2. 管理端 API — `order` 模块

### 包结构

```
com.aichuangzuo.admin.modules.order/
├── controller/
│   └── AdminOrderController.java
├── service/
│   ├── AdminOrderService.java
│   └── impl/
│       └── AdminOrderServiceImpl.java
├── entity/
│   ├── AdminOrder.java          # 映射 u_order（管理端视角）
│   └── AdminUserMembership.java # 映射 u_user_membership
├── mapper/
│   ├── AdminOrderMapper.java
│   └── AdminUserMembershipMapper.java
├── dto/
│   └── request/
│       ├── OrderListRequest.java
│       ├── OrderRefundRequest.java
│       ├── MembershipAdjustRequest.java
│       └── MembershipGrantRequest.java
├── vo/
│   ├── OrderPageVO.java
│   ├── OrderDetailVO.java
│   ├── OrderStatsOverviewVO.java
│   ├── OrderTrendVO.java
│   └── PlanDistributionVO.java
└── enums/
    └── OrderStatus.java
```

### API 端点

#### 2.1 订单列表

```
GET /api/v1/admin/orders
```

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `keyword` | String | 否 | 搜索用户昵称或手机号 |
| `planKey` | String | 否 | 套餐筛选：basic / pro / flagship |
| `status` | Integer | 否 | 状态筛选：0 / 1 / 2 / 3 |
| `startDate` | String | 否 | 开始日期 `yyyy-MM-dd`（按 created_at） |
| `endDate` | String | 否 | 结束日期 `yyyy-MM-dd` |
| `page` | int | 否 | 默认 1 |
| `pageSize` | int | 否 | 默认 10 |

**返回 `OrderPageVO`：**

```json
{
  "total": 128,
  "page": 1,
  "pageSize": 10,
  "list": [
    {
      "id": 1,
      "orderNo": "SUB260715000001",
      "userId": 5,
      "nickname": "张三",
      "email": "zhangsan@test.com",
      "planKey": "pro",
      "planName": "专业版",
      "cycle": "month",
      "cycleName": "月付",
      "amount": 59.90,
      "status": 1,
      "statusName": "已支付",
      "paidAt": "2026-07-15 10:30:00",
      "refundedAt": null,
      "refundReason": null,
      "adminRemark": null,
      "createdAt": "2026-07-15 10:29:55"
    }
  ]
}
```

列表需 JOIN `u_user` 获取昵称和邮箱。

#### 2.2 订单详情

```
GET /api/v1/admin/orders/{id}
```

返回 `OrderDetailVO`，包含订单全部字段 + 用户昵称/邮箱/手机号。

#### 2.3 标记已支付

```
POST /api/v1/admin/orders/{id}/mark-paid
```

**业务逻辑：**

1. 校验订单存在且 `status = 0`（待支付）。
2. 更新 `status = 1`, `paid_at = now`, `operator_id = 当前管理员ID`。
3. 激活/延长会员（复用用户端 `activateOrExtendMembership` 逻辑）：
   - 若用户无会员或已过期，以今天为 `started_at`，按周期加天数。
   - 若会员未过期，以 `expires_at` 为基准加天数，等级覆盖为新等级。
4. 同步更新 `u_user.membership_expire_at` 和 `u_user.membership_plan`。

#### 2.4 退款

```
POST /api/v1/admin/orders/{id}/refund
```

**Request Body：**

```json
{ "reason": "用户申请退款" }
```

**业务逻辑：**

1. 校验订单存在且 `status = 1`（已支付）。
2. 更新 `status = 2`, `refunded_at = now`, `refund_reason = reason`, `operator_id = 当前管理员ID`。
3. 回退会员时长：从 `u_user_membership.expires_at` 减去订单对应周期天数。
4. 若回退后 `expires_at < today`，将 `u_user.membership_expire_at` 设为该日期（会员已过期）。
5. 同步更新 `u_user.membership_expire_at`。

#### 2.5 取消订单

```
POST /api/v1/admin/orders/{id}/cancel
```

**业务逻辑：**

1. 校验订单存在且 `status = 0`（待支付）。
2. 更新 `status = 3`, `operator_id = 当前管理员ID`。

#### 2.6 手动调整会员

```
POST /api/v1/admin/membership/adjust
```

**Request Body：**

```json
{
  "userId": 5,
  "level": "pro",
  "expiresAt": "2027-01-15",
  "remark": "活动补偿调整"
}
```

**业务逻辑：**

1. 校验用户存在，`level` 为有效套餐 key。
2. 若 `u_user_membership` 无记录则插入，有记录则更新 `level` 和 `expires_at`。
3. 同步更新 `u_user.membership_expire_at` 和 `u_user.membership_plan`。
4. 不创建订单记录。

#### 2.7 手动发放会员

```
POST /api/v1/admin/membership/grant
```

**Request Body：**

```json
{
  "userId": 5,
  "planKey": "pro",
  "cycle": "month",
  "remark": "活动赠送"
}
```

**业务逻辑：**

1. 校验用户存在，`planKey` 和 `cycle` 有效。
2. 创建一条 `amount = 0` 的已支付订单，`admin_remark = "手动发放：{remark}"`, `operator_id = 当前管理员ID`。
3. 激活/延长会员（同标记已支付的逻辑）。
4. 同步更新 `u_user.membership_expire_at` 和 `u_user.membership_plan`。

#### 2.8 统计概览

```
GET /api/v1/admin/orders/stats/overview
```

**返回 `OrderStatsOverviewVO`：**

```json
{
  "todayOrderCount": 12,
  "todayRevenue": 718.80,
  "monthOrderCount": 156,
  "monthRevenue": 9344.40,
  "totalOrderCount": 1280,
  "totalRevenue": 76672.00
}
```

仅统计 `status = 1`（已支付）的订单。

#### 2.9 收入趋势

```
GET /api/v1/admin/orders/stats/trend?days=7
```

**Query 参数：** `days`（7 或 30，默认 7）。

**返回 `OrderTrendVO`：**

```json
{
  "dates": ["07-09", "07-10", "07-11", "07-12", "07-13", "07-14", "07-15"],
  "revenues": [120.50, 89.70, 200.00, 150.30, 0, 59.90, 718.80],
  "orderCounts": [2, 1, 3, 2, 0, 1, 12]
}
```

#### 2.10 套餐分布

```
GET /api/v1/admin/orders/stats/plan-distribution
```

**返回 `PlanDistributionVO`：**

```json
{
  "plans": [
    { "planKey": "basic", "planName": "基础版", "count": 45, "revenue": 1345.50 },
    { "planKey": "pro", "planName": "专业版", "count": 80, "revenue": 4792.00 },
    { "planKey": "flagship", "planName": "旗舰版", "count": 31, "revenue": 3096.90 }
  ],
  "cycles": [
    { "cycle": "month", "cycleName": "月付", "count": 90 },
    { "cycle": "quarter", "cycleName": "季付", "count": 40 },
    { "cycle": "year", "cycleName": "年付", "count": 26 }
  ]
}
```

仅统计 `status = 1`（已支付）的订单。

---

## 3. 管理端前端

### 3.1 侧边栏菜单

在 `AdminLayout.vue` 的「用户反馈」和「系统设置」之间插入：

```html
<a-sub-menu key="/console/orders">
  <template #icon><ShoppingCartOutlined /></template>
  <template #title>订单管理</template>
  <a-menu-item key="/console/orders/list">订单列表</a-menu-item>
  <a-menu-item key="/console/orders/stats">数据统计</a-menu-item>
</a-sub-menu>
```

### 3.2 路由

在 `router/index.js` 的 `/console` children 中新增：

```js
{
  path: 'orders/list',
  name: 'AdminOrderList',
  component: () => import('@/views/OrderListView.vue')
},
{
  path: 'orders/stats',
  name: 'AdminOrderStats',
  component: () => import('@/views/OrderStatsView.vue')
}
```

### 3.3 API 层

新增 `src/api/order.js`：

```js
import request from './request.js'

export const getOrderList = (params) => request.get('/orders', { params })
export const getOrderDetail = (id) => request.get(`/orders/${id}`)
export const markOrderPaid = (id) => request.post(`/orders/${id}/mark-paid`)
export const refundOrder = (id, data) => request.post(`/orders/${id}/refund`, data)
export const cancelOrder = (id) => request.post(`/orders/${id}/cancel`)
export const adjustMembership = (data) => request.post('/membership/adjust', data)
export const grantMembership = (data) => request.post('/membership/grant', data)
export const getOrderStatsOverview = () => request.get('/orders/stats/overview')
export const getOrderTrend = (days) => request.get('/orders/stats/trend', { params: { days } })
export const getPlanDistribution = () => request.get('/orders/stats/plan-distribution')
```

### 3.4 订单列表页 — `OrderListView.vue`

**搜索栏：**
- 关键词输入框（昵称/邮箱）
- 套餐下拉：全部 / 基础版 / 专业版 / 旗舰版
- 状态下拉：全部 / 待支付 / 已支付 / 已退款 / 已取消
- 日期范围选择器（创建时间）
- 搜索按钮 + 重置按钮

**快捷操作按钮（搜索栏右侧）：**
- `手动发放会员` — 弹框：选择用户（搜索）、套餐、周期、备注
- `手动调整会员` — 弹框：选择用户（搜索）、等级、到期日期、备注

**表格列：**

| 列 | 说明 |
|---|---|
| 订单号 | `orderNo` |
| 用户 | 昵称 + 邮箱（小字） |
| 套餐 | 标签：基础版（蓝） / 专业版（绿） / 旗舰版（金） |
| 周期 | 月付 / 季付 / 年付 |
| 金额 | `¥{amount}` |
| 状态 | 标签：待支付（橙） / 已支付（绿） / 已退款（红） / 已取消（灰） |
| 支付时间 | `paidAt` 或 `-` |
| 创建时间 | `createdAt` |
| 操作 | 按状态动态显示按钮 |

**操作按钮逻辑：**

| 状态 | 可用操作 |
|---|---|
| 待支付（0) | `标记已支付` `取消` |
| 已支付（1) | `退款` |
| 已退款（2) | `详情` |
| 已取消（3) | `详情` |

- `标记已支付`：二次确认弹框，确认后调用 API，刷新列表。
- `退款`：弹框输入退款原因，确认后调用 API，刷新列表。
- `取消`：二次确认弹框，确认后调用 API，刷新列表。
- `详情`：抽屉展示订单完整信息。

**弹框固定高度**（遵循用户偏好）：所有弹框内容区写死 `height` + 内部滚动，不随内容跳动。

### 3.5 数据统计页 — `OrderStatsView.vue`

**顶部统计卡片（6 个，2 行 x 3 列）：**

| 卡片 | 数据来源 |
|---|---|
| 今日订单 | `todayOrderCount` |
| 今日收入 | `¥{todayRevenue}` |
| 本月订单 | `monthOrderCount` |
| 本月收入 | `¥{monthRevenue}` |
| 累计订单 | `totalOrderCount` |
| 累计收入 | `¥{totalRevenue}` |

**收入趋势图（ECharts 折线图）：**
- 标题：收入趋势
- 切换按钮：`近7天` / `近30天`
- X 轴：日期（MM-DD）
- Y 轴左：收入金额（元）
- Y 轴右：订单数
- 两条线：收入（实线，绿色 `#07c160`）、订单数（虚线，蓝色）

**底部分布图（并排两个饼图）：**
- 左：套餐分布 — 各套餐订单数占比（基础版 / 专业版 / 旗舰版）
- 右：周期分布 — 各周期订单数占比（月付 / 季付 / 年付）

---

## 4. 错误码

复用 `MembershipErrorCode`，新增管理端订单错误码：

| 错误码 | 说明 |
|---|---|
| 117001 | 订单不存在 |
| 117002 | 订单状态不允许此操作 |
| 117003 | 用户不存在 |
| 117004 | 退款原因不能为空 |

---

## 5. 安全与权限

- 所有订单管理接口需登录管理员账号。
- 复用现有 `SecurityAdminContext` 获取当前管理员 ID。
- 订单列表和统计接口需要 `checkSuperAdmin()` 权限校验（与现有用户管理接口一致）。
