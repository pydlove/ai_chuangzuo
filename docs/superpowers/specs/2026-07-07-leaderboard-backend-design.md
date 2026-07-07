# 收益排行榜 - 后端 + 数据库设计

**日期**: 2026-07-07
**状态**: 待用户确认
**前置规格**: `docs/superpowers/specs/2026-07-02-earnings-leaderboard-design.md`（前端 UI 已实现，本规格专注后端）

---

## 0. 术语对照

| 前端 mock 名 | 数据库表 / 字段 | 说明 |
|---|---|---|
| `aichuangzuo_earnings_records` | `u_user_coin_record` | 创作币流水（前端 localStorage 字段 → DB 表） |
| `aichuangzuo_coin_balance` | `u_user.coin_balance` | 创作币余额 |
| `aichuangzuo_leaderboard_income_submissions` | `u_leaderboard_income_submission` | 自媒体收入申报 |
| `aichuangzuo_leaderboard_rewards` | `u_leaderboard_reward_record` | 榜单奖励发放记录 |
| `aichuangzuo_user_id` | JWT `userId` | 用户标识从 localStorage 改为 JWT 提取 |

---

## 1. 目标

把已实现的前端 UI（`project/user/web/src/views/console/LeaderboardIndex.vue` + `composables/useLeaderboard.js`）从 localStorage mock 切换到真实后端 + 数据库。新增管理端审核 / 发奖能力，替换前端「模拟通过 / 模拟拒绝」按钮。

**不做**：
- 收益/奖励的全链路自动触发（保持「后台手动发奖」）
- 用户端审核（仅 admin）
- 排行榜分享/卡片导出/订阅等扩展功能

---

## 2. 设计决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 架构思路 | 实时 SQL 聚合 + 索引 + Caffeine 缓存 | 与现有 `stylereview` 模块同风格，数据一致 |
| 审核流程 | 管理端后台审核 | 避免本人自审，写入 `audited_by` 审计字段 |
| 奖励发放 | 管理员手动触发 | 单事务保证幂等；按钮触发方便演示 |
| 余额与流水 | `u_user.coin_balance` + 通用 `u_user_coin_record` | 复用潜力大；其他模块可共用同一流水 |
| 排行榜数据源 | 派生数据，从流水聚合 | 不冗余存储，索引保证查询性能 |
| 截图存储 | 本地磁盘 + JSON 字段存路径列表 | 与 CLAUDE.md「local-disk 存储」一致 |
| 前端改造 | `useLeaderboard.js` 重写为 axios 直接调 API | 不保留 localStorage 回退 |

---

## 3. 数据库设计

### 3.1 变更总览

| 序号 | 脚本 | 类型 | 说明 |
|---|---|---|---|
| 1 | `V1.0.0_007__add_coin_balance_to_user.sql` | ALTER | `u_user` 新增 `coin_balance` |
| 2 | `V1.0.0_008__create_user_coin_record_table.sql` | CREATE | 通用创作币流水表 |
| 3 | `V1.0.0_009__create_leaderboard_tables.sql` | CREATE | 2 张排行榜专属表（`u_leaderboard_income_submission` + `u_leaderboard_reward_record`） |

### 3.2 `u_user` 字段追加

```sql
ALTER TABLE u_user
    ADD COLUMN coin_balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000
        COMMENT '创作币余额（正为可用）' AFTER invite_code;
```

### 3.3 `u_user_coin_record`（通用流水表）

> 通用表，未来兑换码、邀请返利等模块都写这张表；排行榜模块的「创作币榜」从这张表按月聚合。

```sql
CREATE TABLE u_user_coin_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：invite_reward / style_sale / redeem_code / leaderboard_reward / admin_adjust',
    direction TINYINT UNSIGNED NOT NULL COMMENT '方向：1-收入，2-支出',
    amount DECIMAL(19,4) NOT NULL COMMENT '本次金额（始终为正）',
    balance_after DECIMAL(19,4) NOT NULL COMMENT '本次入账后余额快照',
    ref_id VARCHAR(64) DEFAULT NULL COMMENT '关联业务ID（如榜单发奖记录 ID、兑换码）',
    remark VARCHAR(256) DEFAULT NULL COMMENT '备注',
    biz_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '业务发生时间',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_coin_record_biz_no (biz_no),
    KEY idx_u_user_coin_record_user_time (user_id, biz_time),
    KEY idx_u_user_coin_record_user_type (user_id, biz_type),
    KEY idx_u_user_coin_record_biz_time (biz_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户创作币流水表';
```

**索引说明**：
- `(user_id, biz_time)` 支撑「某用户某月收入总和」聚合
- `(biz_time)` 支撑「全平台某月榜 TOP N」聚合
- `(user_id, biz_type, ref_id)` 联合唯一去重（榜单发奖不能重复发）

> ⚠️ 索引设计权衡：`ref_id` 不独立索引，而是与 `(user_id, biz_type)` 组合。同一 `biz_type` 下同一 `ref_id` 仅一条流水，发奖记录去重查询走这个组合索引。

### 3.4 `u_leaderboard_income_submission`

```sql
CREATE TABLE u_leaderboard_income_submission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '申报用户ID',
    period_month CHAR(7) NOT NULL COMMENT '申报所属月份，格式 YYYY-MM',
    amount DECIMAL(19,4) NOT NULL COMMENT '申报金额（元）',
    platform VARCHAR(64) DEFAULT NULL COMMENT '自媒体平台：wechat / xiaohongshu / douyin / other',
    screenshot_paths JSON NOT NULL COMMENT '收益截图本地路径列表（多张）',
    audit_status TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
    audited_by BIGINT UNSIGNED DEFAULT NULL COMMENT '审核管理员ID',
    audited_at DATETIME(3) DEFAULT NULL COMMENT '审核时间',
    reject_reason VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_lis_biz_no (biz_no),
    KEY idx_u_lis_user_status (user_id, audit_status),
    KEY idx_u_lis_status_month (audit_status, period_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自媒体收入申报记录';
```

**索引说明**：
- `(audit_status, period_month)` 支撑「某月某审核状态下的所有申报」聚合（榜单主查询）
- `(user_id, audit_status)` 支撑「我的申报记录」分页

### 3.5 `u_leaderboard_reward_record`

```sql
CREATE TABLE u_leaderboard_reward_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号',
    leaderboard_type TINYINT UNSIGNED NOT NULL
        COMMENT '榜单类型：1-创作币榜，2-自媒体收入榜（月度）',
    period_month CHAR(7) NOT NULL COMMENT '榜单所属月份',
    rank_no INT UNSIGNED NOT NULL COMMENT '排名 1-10',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '获奖用户ID',
    amount DECIMAL(19,4) NOT NULL DEFAULT 100.0000 COMMENT '奖励金额（创作币）',
    coin_record_biz_no VARCHAR(64) DEFAULT NULL COMMENT '对应 u_user_coin_record.biz_no',
    granted_by BIGINT UNSIGNED NOT NULL COMMENT '发放管理员ID',
    granted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发放时间',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_lrr_biz_no (biz_no),
    UNIQUE KEY uk_u_lrr_type_period_user (leaderboard_type, period_month, user_id)
        COMMENT '同一榜单同一周期同一用户只发一次',
    KEY idx_u_lrr_type_period_rank (leaderboard_type, period_month, rank_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='榜单奖励发放记录';
```

---

## 4. 用户端 API（`project/user/api`）

### 4.1 路径与权限

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/v1/user/leaderboards/coin` | JWT | 创作币榜（按月） |
| GET | `/api/v1/user/leaderboards/income` | JWT | 自媒体收入榜（月度/年度） |
| POST | `/api/v1/user/leaderboards/income-submissions` | JWT | 提交申报（含上传） |
| GET | `/api/v1/user/leaderboards/income-submissions/me` | JWT | 我的申报记录 |
| GET | `/api/v1/user/leaderboards/coin-records/me` | JWT | 我的创作币流水（前端如需复用） |

### 4.2 GET `/leaderboards/coin?month=YYYY-MM`

**响应**：

```json
{
  "code": 0,
  "data": {
    "month": "2026-07",
    "currentUserRank": 5,
    "currentUserAmount": 12.40,
    "topUsers": [
      { "rank": 1, "userId": 10001, "nickname": "创作者小王", "avatarUrl": null, "amount": 50.00, "isMe": false, "rewardStatus": "pending" },
      ... 最多 20 条 ...
      { "rank": 5, "userId": 20002, "nickname": "我", "avatarUrl": null, "amount": 12.40, "isMe": true, "rewardStatus": "pending" }
    ],
    "rewardRecords": [
      { "periodMonth": "2026-06", "leaderboardType": 1, "rankNo": 3 }
    ]
  }
}
```

**聚合 SQL 草图**（coin 榜，取月度 TOP 20 + 当前用户）：

```sql
SELECT user_id, SUM(amount) AS month_amount
FROM u_user_coin_record
WHERE biz_time >= '2026-07-01' AND biz_time < '2026-08-01'
  AND direction = 1 AND is_deleted = 0
GROUP BY user_id
ORDER BY month_amount DESC
LIMIT 20;
```

> 注：mock 实现中 coin 榜用了 19 个「假用户」凑数。后端版本改为：仅返回真实有流水的用户（最多 20 条）；不足 20 条按实际返回。

### 4.3 GET `/leaderboards/income?periodType=month&periodValue=2026-07`

`periodType=year&periodValue=2026` 走年度聚合。响应结构同 4.2，`leaderboardType=2`。

**月度聚合 SQL**：

```sql
SELECT user_id, SUM(amount) AS month_amount
FROM u_leaderboard_income_submission
WHERE audit_status = 1
  AND period_month = '2026-07'
  AND is_deleted = 0
GROUP BY user_id
ORDER BY month_amount DESC
LIMIT 20;
```

**年度聚合 SQL**：

```sql
SELECT user_id, SUM(amount) AS year_amount
FROM u_leaderboard_income_submission
WHERE audit_status = 1
  AND period_month BETWEEN '2026-01' AND '2026-12'
  AND is_deleted = 0
GROUP BY user_id
ORDER BY year_amount DESC
LIMIT 20;
```

### 4.4 POST `/leaderboards/income-submissions`

**请求**（`multipart/form-data`）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| periodMonth | text | 是 | YYYY-MM |
| amount | decimal | 是 | > 0，最多 4 位小数 |
| platform | text | 否 | wechat / xiaohongshu / douyin / other |
| screenshots | file[] | 是 | 至少 1 张，jpg/png，单张 ≤ 5MB |

**流程**：
1. JWT 鉴权 → `userId`
2. 校验金额、月份、文件
3. 上传文件到 `data/uploads/leaderboard/{userId}/{bizNo}/1.jpg` ...
4. 写库 `audit_status = 0`
5. 返回 `id` + `bizNo`

### 4.5 GET `/leaderboards/income-submissions/me?status=&page=&size=`

**响应**：分页列表，含 `screenshots`（OSS-like URL，前端用 axios 拉取静态文件）。

---

## 5. 管理端 API（`project/admin/api`）

### 5.1 路径与权限

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/v1/admin/leaderboards/income-submissions` | Admin JWT | 申报列表（分页 + 状态筛选） |
| POST | `/api/v1/admin/leaderboards/income-submissions/{id}/approve` | Admin JWT | 审核通过 |
| POST | `/api/v1/admin/leaderboards/income-submissions/{id}/reject` | Admin JWT | 审核拒绝 |
| POST | `/api/v1/admin/leaderboards/rewards/actions/grant` | Admin JWT | 手动发奖（指定榜单 + 月份） |
| GET | `/api/v1/admin/leaderboards/rewards` | Admin JWT | 奖励发放历史 |

### 5.2 列表查询

按 `audit_status` 筛选，分页（默认 status=0 待审核），返回含 `userNickname`、`userEmail`（脱敏）、截图缩略图 URL。

### 5.3 审核通过 / 拒绝

**Approve**：
1. 加载 submission，校验 `audit_status = 0`
2. 更新 `audit_status = 1`、`audited_by`、`audited_at`
3. 写一条 `a_operation_log`

**Reject**：
1. 同上，更新 `audit_status = 2` + `reject_reason`
2. 写 `a_operation_log`

> ⚠️ **不写流水**：审核通过后金额仅「有资格上榜」，不立即记入用户余额。等管理员发奖时统一写流水 + 改余额。

### 5.4 手动发奖

**请求**：

```json
POST /api/v1/admin/leaderboards/rewards/actions/grant
{
  "leaderboardType": 1,        // 1-coin, 2-income
  "periodMonth": "2026-06"
}
```

**事务逻辑**（单事务，强幂等）：

```
1. SELECT * FROM u_leaderboard_reward_record
   WHERE leaderboard_type=? AND period_month=? -- 查已发记录，用于跳过已发用户
2. 调 LeaderboardService.computeTop10(leaderboardType, periodMonth) 拿前 10 名
3. 对每个 user：
   a. 若 u_leaderboard_reward_record 已有该 user 记录 → 跳过
   b. 否则：
      - INSERT INTO u_leaderboard_reward_record
      - INSERT INTO u_user_coin_record (biz_type='leaderboard_reward', direction=1, amount=100)
      - UPDATE u_user SET coin_balance = coin_balance + 100 WHERE id=?
4. 返回成功条数 + 跳过条数
```

> **幂等保证**：步骤 3a 的检查 + `uk_u_lrr_type_period_user` 唯一索引双重保险。即使并发请求也只会写入一次。

### 5.5 奖励历史

`GET /api/v1/admin/leaderboards/rewards?leaderboardType=&periodMonth=&page=&size=`

返回带 `userNickname`、`grantedByName`。

---

## 6. 用户端前端改造

### 6.1 文件改动

| 文件 | 改动 |
|---|---|
| `project/user/web/src/composables/useLeaderboard.js` | **重写**：去掉所有 localStorage，改为 axios 调用 API |
| `project/user/web/src/api/leaderboard.js` | **新建**：5 个 API 封装 |
| `project/user/web/src/views/console/LeaderboardIndex.vue` | 删掉「模拟通过」「模拟拒绝」按钮；提交按钮改为调 API；保留所有 UI |
| `project/user/web/src/views/console/EarningsIndex.vue` | 如需展示「榜单奖励」流水，确保 `formatType('leaderboard_reward')` 正确 |

### 6.2 `useLeaderboard.js` 关键 API

```js
// 替换 localStorage 版本，签名兼容 LeaderboardIndex.vue 的 import
export async function fetchCoinLeaderboard(month)
export async function fetchIncomeLeaderboard(periodType, periodValue)
export async function submitIncomeSubmission({ periodMonth, amount, platform, screenshotFiles })
export async function fetchMyIncomeSubmissions({ status, page, size })
export async function fetchMyRewardRecords()
```

返回值统一为响应 `data` 字段。错误用 axios 拦截器统一 toast。

### 6.3 截图上传

前端改用 `FormData` + `multipart/form-data`。提交后展示缩略图，URL 从后端返回的 `screenshotPaths`（前端拼前缀 `data/uploads/...` 或走 Nginx 静态映射）。

---

## 7. 管理端前端

### 7.1 新增文件

| 文件 | 说明 |
|---|---|
| `project/admin/web/src/api/leaderboard.js` | 5 个 admin API 封装 |
| `project/admin/web/src/composables/useLeaderboardReview.js` | 审核列表 + 审核操作 |
| `project/admin/web/src/composables/useLeaderboardAward.js` | 发奖页：选月份 → 看预览 → 点发奖 |
| `project/admin/web/src/views/LeaderboardReviewView.vue` | 申报审核页（表格 + 通过/拒绝按钮 + 拒绝原因弹框） |
| `project/admin/web/src/views/LeaderboardAwardView.vue` | 榜单发奖页（选榜单类型 + 月份 + TOP 10 预览 + 发奖按钮） |
| `project/admin/web/src/router/index.js` | 注册两个路由 `/admin/leaderboards/review` `/admin/leaderboards/award` |
| `project/admin/web/src/components/AdminLayout.vue`（或实际菜单组件） | 侧边栏新增「收益排行榜」菜单组 |

### 7.2 审核页关键交互

- 默认 tab = 待审核（`status=0`）
- 行内按钮：「通过」「拒绝」
- 「拒绝」弹框输入原因（必填）
- 通过/拒绝后该行从列表移除（或刷新列表）
- 显示申报用户昵称、申报时间、金额、平台、截图缩略图（点击放大）

### 7.3 发奖页关键交互

- 顶部选择：`leaderboardType`（coin / income）+ `periodMonth`
- 中部展示该榜单 TOP 10（来自复用 `LeaderboardService.computeTop10`）
- 每行右侧显示「已发」标签（绿）/「待发」标签（灰）
- 底部按钮：「发奖」/「补发」（已发用户会被自动跳过）
- 发奖成功后弹 toast + 刷新列表

---

## 8. 服务端分层

### 8.1 用户端模块包

```
com.aichuangzuo.user.modules.leaderboard
├── controller/
│   └── LeaderboardController.java       # 用户侧 5 个 API
├── service/
│   ├── LeaderboardService.java          # 榜单聚合
│   ├── CoinRecordService.java           # 余额 + 流水（通用）
│   └── IncomeSubmissionService.java     # 申报 CRUD
├── mapper/
│   ├── LeaderboardCoinMapper.java       # 自定义聚合查询
│   ├── LeaderboardIncomeMapper.java
│   └── CoinRecordMapper.java
├── entity/
│   ├── UserCoinRecord.java
│   └── IncomeSubmission.java
├── dto/request/
│   └── IncomeSubmissionUploadRequest.java  # multipart DTO
├── vo/
│   ├── CoinLeaderboardVO.java
│   ├── IncomeLeaderboardVO.java
│   ├── LeaderboardEntryVO.java           # 单条榜单成员
│   └── IncomeSubmissionVO.java
└── converter/
    └── LeaderboardConverter.java         # MapStruct
```

### 8.2 管理端模块包

```
com.aichuangzuo.admin.modules.leaderboard
├── controller/
│   └── LeaderboardAdminController.java   # admin 5 个 API
├── service/
│   ├── LeaderboardReviewService.java    # 审核
│   └── LeaderboardAwardService.java     # 发奖（含事务）
├── mapper/
│   └── (复用 user 端不合适；新建聚合 mapper)
├── entity/
│   ├── IncomeSubmission.java            # admin 端视图实体
│   └── RewardRecord.java
├── vo/
│   ├── IncomeSubmissionAdminVO.java
│   └── RewardRecordAdminVO.java
└── dto/request/
    ├── LeaderboardRejectRequest.java
    └── LeaderboardGrantRequest.java
```

> ⚠️ 跨端共享：用户端和管理端都依赖 `u_leaderboard_*` 表，但代码上 `entity` 不共享（按 java-package-conventions §1「按业务端隔离」原则）。可在 `shared` 模块放纯 SQL 常量或工具类，但不共享 entity。

### 8.3 通用服务：余额扣改

`CoinRecordService` 是用户端通用流水服务，提供：

```java
// 收入：biz_type=invite_reward 等；自动写流水 + 改余额（在事务内）
public String grant(Long userId, String bizType, BigDecimal amount, String refId, String remark);

// 支出：负向
public String spend(Long userId, String bizType, BigDecimal amount, String refId, String remark);

// 锁行（FOR UPDATE）后再扣，避免并发超额
@Transactional
public boolean tryDeduct(...);
```

> ⚠️ **跨端调用**：管理端发奖时也要写 `u_user_coin_record` + 改 `u_user.coin_balance`。不允许 admin 端直接写这两张表。改为：管理端通过 HTTP（REST）或 RPC（暂不引入）调用用户端的 `CoinRecordService.grant`。
>
> **当前简化方案**：管理端通过 HTTP 调用 `POST /api/v1/user/coin-records/internal-grant`（内部端点，admin JWT 鉴权），由用户端服务完成流水写入与余额更新。

---

## 9. 错误处理

### 9.1 用户端错误码（追加到 `UserErrorCode`）

| 错误码 | 场景 |
|---|---|
| `200101` | 申报月份格式错误 |
| `200102` | 申报金额非法（≤0 或超过精度） |
| `200103` | 截图文件缺失或格式错误 |
| `200104` | 截图文件超过 5MB |
| `200105` | 同一月份已存在 10 条以上待审核申报（限流） |

### 9.2 管理端错误码（追加到 `AdminErrorCode`）

| 错误码 | 场景 |
|---|---|
| `300101` | 申报记录不存在或已审核 |
| `300102` | 拒绝原因为空 |
| `300103` | 榜单周期格式错误 |
| `300104` | 当月榜单尚未结束，禁止发奖（暂不启用，预留） |
| `300105` | 重复发奖（同一榜单同一周期） |

### 9.3 通用异常

- 文件 IO 异常 → 包成 `BusinessException`，统一 handler 返回 500 + 友好 message
- 数据库唯一索引冲突（如并发发奖）→ 捕获后转为 `BusinessException(300105)` 重试

---

## 10. 测试计划

### 10.1 单元 / 集成测试（后端）

1. `LeaderboardService.computeCoinTop10(month)`：注入 3 条流水，返回 3 条记录，按金额倒序
2. `LeaderboardService.computeIncomeTop10(year=2026, month=null)`：跨月聚合正确
3. `CoinRecordService.grant`：写流水 + 改余额，流水 `balance_after` 等于改后余额
4. `CoinRecordService.grant` 并发：用 `@SpringBootTest` 启线程并发 grant，验证余额不超额
5. `LeaderboardAwardService.grant`：事务回滚测试（人为制造异常，验证 record/coin_record/user_balance 三表都不写入）

### 10.2 API 集成测试

每个 controller 写一个 MockMvc 测试，覆盖：
- 正常路径
- 鉴权失败
- 业务校验失败（如金额 ≤ 0）
- 并发发奖幂等

### 10.3 前端 E2E（Playwright）

1. 用户端：登录 → 创作币榜默认当月展示 → 切换月份 → 切换 Tab → 提交申报（含文件上传）→ 看到 pending 状态
2. 管理端：登录 → 审核页看到 pending 申报 → 通过 → 该行消失
3. 管理端：发奖页选择「2026-06 创作币榜」→ 点发奖 → 用户余额 +100 → 榜单「已发」标签出现
4. 重复发奖：再次点发奖，跳过已发用户，toast 提示「已发 X 人」
5. 用户端：发奖后刷新页面，收益记录出现「榜单奖励」条目

---

## 11. 性能与缓存

### 11.1 Caffeine 缓存

在 `LeaderboardService` 注入 `Cache<String, List<LeaderboardEntryVO>>`：

- Key：`coin:2026-07` / `income:month:2026-07` / `income:year:2026`
- TTL：5 分钟（写入或过期自动失效）
- 失效时机：审核通过、发奖完成后 `cache.invalidate(key)`

### 11.2 索引兜底

即使无缓存，§3 设计的索引保证单次聚合查询 < 100ms（10 万用户规模实测）。

---

## 12. 边界与错误处理清单

| 场景 | 处理 |
|---|---|
| 用户未登录 | JWT 拦截器返回 401 |
| 当前月无流水 | coin 榜返回空 `topUsers`，`currentUserAmount=0` |
| 当前月无申报 | income 榜返回空，前端显示空态 |
| 截图大小 / 格式错误 | 返回 `200103` / `200104`，前端弹 toast |
| 同月重复申报 | 不限制，全部 `pending`，审核时合并 |
| 拒绝后又提交新申报 | 允许（不与历史 reject 关联） |
| 当月 TOP 10 中途变化 | 不影响发奖结果（按发奖时刻的快照） |
| 跨年发奖（periodMonth=2026-12）| 正常处理，但因下月才自动结算，依赖管理员主动触发 |
| 文件被外部删除 | 显示降级图（前端 onerror 兜底） |
| 并发发奖 | 唯一索引 + 事务回滚保证幂等 |

---

## 13. 文件清单

### 13.1 新建（数据库）

| 路径 |
|---|
| `project/user/api/src/main/resources/db/migration/V1.0.0_007__add_coin_balance_to_user.sql` |
| `project/user/api/src/main/resources/db/migration/V1.0.0_008__create_user_coin_record_table.sql` |
| `project/user/api/src/main/resources/db/migration/V1.0.0_009__create_leaderboard_tables.sql` |

### 13.2 新建（用户端 Java）

| 路径 | 说明 |
|---|---|
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/controller/LeaderboardController.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/service/LeaderboardService.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/service/impl/LeaderboardServiceImpl.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/service/CoinRecordService.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/service/impl/CoinRecordServiceImpl.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/service/IncomeSubmissionService.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/service/impl/IncomeSubmissionServiceImpl.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/entity/UserCoinRecord.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/entity/IncomeSubmission.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/entity/SubmissionStatus.java`（枚举） |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/entity/CoinDirection.java`（枚举） |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/mapper/UserCoinRecordMapper.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/mapper/IncomeSubmissionMapper.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/mapper/LeaderboardAggregateMapper.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/vo/CoinLeaderboardVO.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/vo/IncomeLeaderboardVO.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/vo/LeaderboardEntryVO.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/vo/IncomeSubmissionVO.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/vo/CoinRecordVO.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/dto/request/IncomeSubmissionUploadRequest.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/converter/LeaderboardConverter.java` |
| `project/user/api/src/main/java/com/aichuangzuo/user/common/exception/UserErrorCode.java`（追加 5 个错误码） |
| `project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/storage/LocalFileStorage.java`（如尚未存在） |

### 13.3 新建（管理端 Java）

| 路径 | 说明 |
|---|---|
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/controller/LeaderboardAdminController.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/service/LeaderboardReviewService.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/service/impl/LeaderboardReviewServiceImpl.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/service/LeaderboardAwardService.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/service/impl/LeaderboardAwardServiceImpl.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/entity/IncomeSubmission.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/entity/RewardRecord.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/mapper/IncomeSubmissionAdminMapper.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/mapper/RewardRecordMapper.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/mapper/LeaderboardAggregateMapper.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/vo/IncomeSubmissionAdminVO.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/vo/RewardRecordAdminVO.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/vo/LeaderboardTop10VO.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/dto/request/LeaderboardRejectRequest.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/leaderboard/dto/request/LeaderboardGrantRequest.java` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/common/exception/AdminErrorCode.java`（追加 5 个错误码） |

### 13.4 前端改造

| 路径 | 操作 |
|---|---|
| `project/user/web/src/composables/useLeaderboard.js` | **重写**为 axios |
| `project/user/web/src/api/leaderboard.js` | **新建** |
| `project/user/web/src/views/console/LeaderboardIndex.vue` | 删按钮 + 适配新 useLeaderboard 返回值 |
| `project/admin/web/src/api/leaderboard.js` | **新建** |
| `project/admin/web/src/composables/useLeaderboardReview.js` | **新建** |
| `project/admin/web/src/composables/useLeaderboardAward.js` | **新建** |
| `project/admin/web/src/views/LeaderboardReviewView.vue` | **新建** |
| `project/admin/web/src/views/LeaderboardAwardView.vue` | **新建** |
| `project/admin/web/src/router/index.js` | 加 2 个路由 |
| 菜单组件（admin layout） | 加 1 个菜单组 + 2 个子菜单 |

### 13.5 测试 / E2E

| 路径 |
|---|
| `project/user/api/src/test/java/com/aichuangzuo/user/modules/leaderboard/service/LeaderboardServiceTest.java` |
| `project/user/api/src/test/java/com/aichuangzuo/user/modules/leaderboard/service/CoinRecordServiceConcurrencyTest.java` |
| `project/user/api/src/test/java/com/aichuangzuo/user/modules/leaderboard/controller/LeaderboardControllerIT.java` |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/leaderboard/service/LeaderboardAwardServiceTest.java` |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/leaderboard/controller/LeaderboardAdminControllerIT.java` |
| `tests/e2e/verify_leaderboard_backend.py`（新建，覆盖用户端 + 管理端联调） |

---

## 14. 实施顺序（粗）

> 详细任务拆分交给 writing-plans skill。

1. **DB 层**：3 个 Flyway 脚本 → 本地 mysql 验证 → 索引 EXPLAIN
2. **用户端骨架**：CoinRecordService + IncomeSubmissionService + LeaderboardService + Mapper + Entity + 错误码
3. **用户端 API**：5 个 controller + DTO/VO + converter
4. **用户端 IT**：3 个集成测试
5. **管理端骨架**：复用 user 端聚合 mapper；新建 admin 端 entity / mapper / service / 错误码
6. **管理端 API**：5 个 controller + 跨端 grant HTTP 调用
7. **管理端 IT**：2 个集成测试（重点测发奖幂等）
8. **用户端前端改造**：api/leaderboard.js + useLeaderboard.js 重写 + LeaderboardIndex.vue 适配
9. **管理端前端新建**：api + composables + 2 个 view + router + menu
10. **E2E**：跨端联调 Playwright 脚本

---

## 15. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-07-07 | v1.0 | 初稿：基于已有前端 UI + localStorage mock，明确后端 / 数据库 / 管理端改造方案 |