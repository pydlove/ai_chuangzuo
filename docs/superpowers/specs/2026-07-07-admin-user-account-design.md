# 管理端收益排行榜菜单设计

**日期**: 2026-07-07
**状态**: 已确认，待实现
**关联规格**:
- `docs/superpowers/specs/2026-07-06-my-account-backend-design.md`（结算中心数据源）
- `docs/superpowers/specs/2026-07-07-leaderboard-backend-design.md`（自媒体审核 / 榜单发奖数据源）

---

## 0. 术语对照

| 管理端菜单 | 对应数据库 | 说明 |
|---|---|---|
| 账户明细 | `u_user` + `u_earnings_record` + `u_user_coin_record` + `u_leaderboard_reward_record` | 用户账户综合查询 |
| 结算中心 | `u_earnings_record.status` | 把未结算收益改为已结算（0→1） |
| 自媒体审核 | `u_leaderboard_income_submission` | 审核用户提交的外部自媒体收入 |
| 榜单发奖 | `u_user_coin_record` + `u_leaderboard_reward_record` | 给 TOP 10 用户发放创作币奖励 |

---

## 1. 目标

在管理端新增一个顶级菜单「收益排行榜」，包含 4 个子页面，满足运营人员查看用户账户、完成月度结算、审核自媒体收入、发放榜单奖励的需求。

**核心原则**：
- 用户端只读，所有写操作（结算 / 审核 / 发奖）都在管理端完成。
- 管理端可以直接读用户侧表（共用 MySQL），但写 `u_user` / `u_user_coin_record` 必须走用户端 HTTP 内部接口。
- 结算（`u_earnings_record.status` 0→1）与发奖（`u_user_coin_record`）是两个独立概念，不互相替代。

**不做**：
- 提现审批 / 打款状态管理（本期只到「结算」和「发奖」，提现另开 spec）。
- 榜单自动发奖（保持手动触发）。
- 用户端申报以外的排行榜编辑能力。

---

## 2. 设计决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 数据读取 | 管理端直接读用户侧表 | 共用 MySQL，实时一致；按 java-package-conventions 在 admin 端建自己的 entity/mapper |
| 余额/流水写入 | 走用户端内部 HTTP 接口 | 与 `2026-07-07-leaderboard-backend-design.md` 保持一致，避免 admin 端越权写用户核心资产 |
| 结算范围 | 按自然月批量结算 | 与 `2026-07-06-my-account-backend-design.md` 一致，用户端「我的账户」也是按月展示 |
| 榜单发奖 | 手动选择榜单类型 + 月份后触发 | 按钮触发，单事务幂等，方便演示 |
| 自媒体审核 | 通过 / 拒绝二态 | 与现有用户端 UI 的 pending / approved / rejected 三态对应 |

---

## 3. 管理端菜单结构

```
收益排行榜
├── 账户明细          /admin/earnings/accounts
├── 结算中心          /admin/earnings/settlements
├── 自媒体审核        /admin/earnings/self-media-review
└── 榜单发奖          /admin/earnings/leaderboard-awards
```

> 前端路径统一以 `/admin/earnings/` 为前缀，与现有 `/admin/users`、`/admin/model-configs` 风格一致。

---

## 4. 账户明细

### 4.1 功能

按用户维度查询账户综合信息：
- 基础信息：用户 ID、昵称、手机号/邮箱（脱敏）、注册时间
- 我的账户：累计收益、已结算、未结算（来自 `u_earnings_record`）
- 创作币：当前余额、累计收入/支出（来自 `u_user_coin_record`）
- 榜单奖励：历史获奖次数、最近获奖月份/排名（来自 `u_leaderboard_reward_record`）
- 本月排名：创作币榜第几名、自媒体收入榜第几名（实时聚合）

### 4.2 页面交互

- 顶部筛选：用户 ID / 昵称 / 手机号（模糊）/ 邮箱（模糊）
- 表格列：用户 ID、昵称、注册时间、累计收益、未结算收益、创作币余额、本月创作币排名、本月自媒体排名、操作
- 操作列：「查看详情」
- 详情抽屉：展示该用户 4 类数据的汇总卡 + 最近 10 条收益流水 + 最近 10 条创作币流水 + 榜单奖励历史

### 4.3 管理端 API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/accounts` | 用户账户列表（分页 + 筛选） |
| GET | `/api/v1/admin/accounts/{userId}` | 单个用户账户详情 |

**请求参数** `/accounts?page=&size=&userId=&nickname=&phone=&email=`

**响应示例**:

```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "userId": 10001,
        "nickname": "创作者小王",
        "phone": "138****1234",
        "email": "w***@example.com",
        "registeredAt": "2026-05-10T12:00:00",
        "totalEarnings": 128.50,
        "unsettledEarnings": 36.00,
        "coinBalance": 250.00,
        "coinRankThisMonth": 3,
        "incomeRankThisMonth": 5
      }
    ],
    "total": 42,
    "page": 1,
    "size": 20
  }
}
```

### 4.4 聚合逻辑

- `totalEarnings` / `unsettledEarnings`: `SUM(amount)` from `u_earnings_record`
- `coinBalance`: `u_user.coin_balance`
- `coinRankThisMonth`: 实时聚合 `u_user_coin_record` 当月收入 TOP N 后定位当前用户排名
- `incomeRankThisMonth`: 实时聚合 `u_leaderboard_income_submission` 当月已审核记录 TOP N 后定位当前用户排名

---

## 5. 结算中心

### 5.1 功能

批量把 `u_earnings_record` 中 `status=0` 的记录改为 `status=1`，按自然月结算。

### 5.2 页面交互

- 顶部选择月份（默认上个月）
- 展示该月份待结算汇总：用户数、记录数、总金额
- 列表展示每个有待结算记录的用户（用户 ID、昵称、未结算金额、记录数）
- 支持单用户结算和「全部结算」批量操作
- 结算后刷新列表，已结算用户从列表移除

### 5.3 管理端 API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/accounts/settlements/pending-summary?month=YYYY-MM` | 某月待结算汇总 |
| GET | `/api/v1/admin/accounts/settlements/pending-users?month=YYYY-MM` | 待结算用户列表 |
| POST | `/api/v1/admin/accounts/settlements/actions/settle` | 执行结算 |

**结算请求**:

```json
{
  "month": "2026-06",
  "userIds": [10001, 10002]
}
```

`userIds` 为空数组时表示结算该月份全部待结算用户。

**响应示例**:

```json
{
  "code": 0,
  "data": {
    "month": "2026-06",
    "settledUserCount": 2,
    "settledRecordCount": 5,
    "settledAmount": 36.00
  }
}
```

### 5.4 结算逻辑

1. 校验月份格式。
2. 查询该月份 `status=0` 的记录。
3. 若传了 `userIds`，只结算这些用户；否则结算全部。
4. 在 `@Transactional` 内批量 `UPDATE u_earnings_record SET status=1, settled_at=NOW()`。
5. 返回实际结算用户数 / 记录数 / 金额。
6. 幂等：已结算记录 status 已是 1，UPDATE 影响 0 行，无副作用。

> 与用户端 `/api/v1/user/account/settle-last-month` 是同一套数据，只是管理端可以指定任意月份和任意用户。

---

## 6. 自媒体审核

### 6.1 功能

审核用户在用户端提交的自媒体收入申报（`u_leaderboard_income_submission`）。

### 6.2 页面交互

- Tab 切换：待审核 / 已通过 / 已拒绝
- 表格列：申报 ID、用户 ID、昵称、月份、平台、金额、截图、提交时间、状态、操作
- 操作：
  - 通过：行内按钮，二次确认后通过
  - 拒绝：弹框输入拒绝原因（必填）
- 点击截图缩略图可放大查看

### 6.3 管理端 API

沿用 `2026-07-07-leaderboard-backend-design.md` §5 已定义的接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/leaderboards/income-submissions` | 申报列表（分页 + 状态筛选） |
| POST | `/api/v1/admin/leaderboards/income-submissions/{id}/approve` | 通过 |
| POST | `/api/v1/admin/leaderboards/income-submissions/{id}/reject` | 拒绝 |

**列表请求参数**: `?status=0&periodMonth=2026-07&page=&size=`

**拒绝请求**:

```json
{
  "rejectReason": "截图无法识别收益金额"
}
```

### 6.4 审核逻辑

1. 加载申报记录，校验 `audit_status=0`。
2. 更新 `audit_status`（1 或 2）、`audited_by`（当前管理员 ID）、`audited_at`。
3. 拒绝时写入 `reject_reason`。
4. 写 `a_operation_log` 审计日志。
5. 审核通过**不写余额/流水**，仅决定该申报是否参与榜单聚合。

---

## 7. 榜单发奖

### 7.1 功能

针对某月某榜单（创作币榜 / 自媒体收入榜）的前 10 名用户发放创作币奖励。

### 7.2 页面交互

- 顶部选择：榜单类型（创作币榜 / 自媒体收入榜）、月份
- 中部展示该榜单 TOP 10：排名、用户 ID、昵称、金额、发奖状态
- 发奖状态：已发（绿标）/ 待发（灰标）
- 底部按钮：「发奖」（已发用户自动跳过）
- 发奖成功后 toast 提示成功条数 + 跳过条数，刷新列表

### 7.3 管理端 API

沿用 `2026-07-07-leaderboard-backend-design.md` §5 已定义的接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/leaderboards/rewards/preview` | 预览某榜单 TOP 10 |
| POST | `/api/v1/admin/leaderboards/rewards/actions/grant` | 执行发奖 |
| GET | `/api/v1/admin/leaderboards/rewards` | 奖励发放历史 |

**预览请求**: `GET /rewards/preview?leaderboardType=1&periodMonth=2026-06`

**发奖请求**:

```json
{
  "leaderboardType": 1,
  "periodMonth": "2026-06"
}
```

### 7.4 发奖逻辑

1. 计算该榜单 TOP 10（复用聚合逻辑）。
2. 查询 `u_leaderboard_reward_record` 中已发记录，用于跳过。
3. 对每个未发奖用户：
   - INSERT `u_leaderboard_reward_record`（幂等，依赖唯一索引 `(leaderboard_type, period_month, user_id)`）
   - 通过 HTTP 调用用户端内部接口 `POST /api/v1/user/coin-records/internal-grant` 写入 `u_user_coin_record` 并更新 `u_user.coin_balance`
4. 返回成功条数 + 跳过条数。

> 用户端内部接口 `internal-grant` 需要单独实现，接收 `userId`、`amount`、`bizType='leaderboard_reward'`、`refId`（reward record biz_no）、`remark`。

---

## 8. 跨端调用：用户端内部发币接口

### 8.1 接口定义

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/v1/user/coin-records/internal-grant` | Admin JWT + IP/Header 白名单 | 给指定用户加创作币 |

**请求**:

```json
{
  "userId": 10001,
  "amount": 100.00,
  "bizType": "leaderboard_reward",
  "refId": "LR202607010001",
  "remark": "2026-06 创作币榜第 1 名奖励"
}
```

**响应**: 返回 `coinRecordBizNo`。

### 8.2 安全

- 仅允许管理端服务 IP 或带特定内部 Key 的请求访问（通过 Spring Security 配置）。
- 接口不在用户端前端开放。
- `bizType` 必须来自白名单（`leaderboard_reward`、`admin_adjust` 等）。

---

## 9. 管理端后端分层

```
com.aichuangzuo.admin.modules.earnings
├── controller/
│   ├── AccountAdminController.java          # 账户明细
│   ├── SettlementAdminController.java       # 结算中心
│   └── LeaderboardAdminController.java      # 自媒体审核 + 榜单发奖（可与 user-side 规格对齐）
├── service/
│   ├── AccountAdminService.java
│   ├── SettlementAdminService.java
│   ├── LeaderboardReviewService.java
│   └── LeaderboardAwardService.java
├── mapper/
│   ├── AccountAdminMapper.java              # 读 u_user / u_earnings_record / u_user_coin_record / u_leaderboard_reward_record
│   ├── SettlementAdminMapper.java           # 更新 u_earnings_record
│   ├── IncomeSubmissionAdminMapper.java
│   └── RewardRecordMapper.java
├── entity/
│   ├── UserAccountView.java                 # 仅查询用 POJO，不必须映射全表
│   ├── EarningsRecord.java                  # 管理端视图实体
│   ├── UserCoinRecord.java                  # 管理端视图实体
│   ├── IncomeSubmission.java
│   └── RewardRecord.java
├── vo/
│   ├── UserAccountVO.java
│   ├── UserAccountDetailVO.java
│   ├── PendingSettlementSummaryVO.java
│   ├── PendingSettlementUserVO.java
│   ├── SettlementResultVO.java
│   ├── IncomeSubmissionAdminVO.java
│   ├── RewardRecordAdminVO.java
│   └── LeaderboardTop10PreviewVO.java
├── dto/request/
│   ├── AccountQueryRequest.java
│   ├── SettlementRequest.java
│   ├── LeaderboardRejectRequest.java
│   └── LeaderboardGrantRequest.java
└── client/
    └── UserCoinRecordClient.java            # 调用户端 internal-grant
```

> 按 java-package-conventions，管理端 entity 不与用户端共享，即使是同一张表也各自维护。

---

## 10. 管理端前端结构

```
project/admin/web/src/
├── api/
│   └── earnings.js              # 4 个页面共用 API 封装
├── composables/
│   ├── useAccountQuery.js       # 账户明细
│   ├── useSettlement.js         # 结算中心
│   ├── useSelfMediaReview.js    # 自媒体审核
│   └── useLeaderboardAward.js   # 榜单发奖
├── views/
│   ├── AccountQueryView.vue
│   ├── SettlementView.vue
│   ├── SelfMediaReviewView.vue
│   └── LeaderboardAwardView.vue
└── router/index.js              # 注册 4 条路由
```

### 10.1 菜单注册

在 `AdminLayout.vue`（或实际菜单配置文件）新增：

```js
{
  key: 'earnings',
  icon: 'DollarOutlined',
  label: '收益排行榜',
  children: [
    { key: '/admin/earnings/accounts', label: '账户明细' },
    { key: '/admin/earnings/settlements', label: '结算中心' },
    { key: '/admin/earnings/self-media-review', label: '自媒体审核' },
    { key: '/admin/earnings/leaderboard-awards', label: '榜单发奖' }
  ]
}
```

---

## 11. 错误处理

### 11.1 管理端错误码（追加到 `AdminErrorCode`）

| 错误码 | 场景 |
|---|---|
| `300201` | 用户不存在 |
| `300202` | 结算月份格式错误 |
| `300203` | 所选月份无待结算记录 |
| `300204` | 自媒体申报记录不存在或已审核 |
| `300205` | 拒绝原因不能为空 |
| `300206` | 榜单类型非法 |
| `300207` | 榜单周期格式错误 |
| `300208` | 发奖时跨端调用失败 |
| `300209` | 重复发奖（唯一索引冲突） |

---

## 12. 测试计划

### 12.1 后端集成测试

| # | 用例 | 断言 |
|---|---|---|
| 1 | 账户明细列表 | 分页、筛选、聚合数值正确 |
| 2 | 账户详情 | 4 类汇总数据齐全 |
| 3 | 结算中心汇总 | 某月待结算用户数 / 金额正确 |
| 4 | 结算操作 | 指定用户 status 0→1，其他用户不变 |
| 5 | 结算幂等 | 再次结算同月同用户，影响 0 行，返回 0 |
| 6 | 自媒体通过 | `audit_status` 变为 1，榜单聚合金额变化 |
| 7 | 自媒体拒绝 | `audit_status` 变为 2，拒绝原因写入 |
| 8 | 榜单发奖 | TOP 10 写入 reward_record，用户余额增加 |
| 9 | 发奖幂等 | 再次发奖跳过已发用户 |
| 10 | 跨端调用失败 | 用户端接口 500 时，admin 端事务回滚 |

### 12.2 前端 E2E（Playwright）

1. 登录管理端 → 进入「账户明细」→ 搜索用户 → 查看详情抽屉 → 断言 4 张汇总卡存在
2. 进入「结算中心」→ 选择上个月 → 点击「全部结算」→ 断言列表刷新为空
3. 进入「自媒体审核」→ 对待审核记录点「通过」→ 断言状态变为已通过
4. 进入「榜单发奖」→ 选择创作币榜 / 上个月 → 点击发奖 → 断言已发标签出现
5. 重复发奖 → 断言 toast 提示跳过已发用户

---

## 13. 文件清单

### 13.1 管理端后端（Java）

| 路径 | 说明 |
|---|---|
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/controller/AccountAdminController.java` | 账户明细 API |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/controller/SettlementAdminController.java` | 结算中心 API |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/controller/LeaderboardAdminController.java` | 自媒体审核 + 榜单发奖 API |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/service/AccountAdminService.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/service/impl/AccountAdminServiceImpl.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/service/SettlementAdminService.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/service/impl/SettlementAdminServiceImpl.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/service/LeaderboardReviewService.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/service/impl/LeaderboardReviewServiceImpl.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/service/LeaderboardAwardService.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/service/impl/LeaderboardAwardServiceImpl.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/mapper/AccountAdminMapper.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/mapper/SettlementAdminMapper.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/mapper/IncomeSubmissionAdminMapper.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/mapper/RewardRecordMapper.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/entity/*.java` | 管理端视图实体 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/vo/*.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/dto/request/*.java` | |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/earnings/client/UserCoinRecordClient.java` | 调用户端 internal-grant |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/common/exception/AdminErrorCode.java` | 追加 9 个错误码 |

### 13.2 管理端前端（Vue）

| 路径 | 说明 |
|---|---|
| `project/admin/web/src/api/earnings.js` | 4 个页面 API 封装 |
| `project/admin/web/src/composables/useAccountQuery.js` | |
| `project/admin/web/src/composables/useSettlement.js` | |
| `project/admin/web/src/composables/useSelfMediaReview.js` | |
| `project/admin/web/src/composables/useLeaderboardAward.js` | |
| `project/admin/web/src/views/AccountQueryView.vue` | |
| `project/admin/web/src/views/SettlementView.vue` | |
| `project/admin/web/src/views/SelfMediaReviewView.vue` | |
| `project/admin/web/src/views/LeaderboardAwardView.vue` | |
| `project/admin/web/src/router/index.js` | 注册 4 条路由 |
| `project/admin/web/src/layouts/AdminLayout.vue` | 新增菜单 |

### 13.3 用户端后端（Java，补充 internal-grant）

| 路径 | 说明 |
|---|---|
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/controller/CoinRecordInternalController.java` | internal-grant 端点 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/dto/request/InternalGrantRequest.java` | |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/service/CoinRecordService.java` | 提供 grant 方法 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/service/impl/CoinRecordServiceImpl.java` | |

### 13.4 测试

| 路径 | 说明 |
|---|---|
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/earnings/service/AccountAdminServiceTest.java` | |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/earnings/service/SettlementAdminServiceTest.java` | |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/earnings/service/LeaderboardAwardServiceTest.java` | |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/earnings/controller/EarningsAdminControllerIT.java` | |
| `tests/e2e/verify_admin_earnings.py` | 管理端收益菜单 E2E |

---

## 14. 实施顺序（粗）

> 详细任务拆分交给 writing-plans skill。

1. 用户端补充 `internal-grant` 接口 + `CoinRecordService`
2. 管理端账户明细：mapper / entity / VO / controller / service / 前端页面
3. 管理端结算中心：mapper / service / controller / 前端页面
4. 管理端自媒体审核：复用 leaderboard 规格已有设计，完成 admin 端实现
5. 管理端榜单发奖：复用 leaderboard 规格已有设计，完成跨端调用
6. 管理端菜单 + 路由注册
7. 后端集成测试 + Playwright E2E

---

## 15. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-07-07 | v1.0 | 初稿：整合用户端「我的账户」与「收益排行榜」规格，明确管理端 4 个子页面与跨端调用方案 |
