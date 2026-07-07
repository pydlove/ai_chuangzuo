# 我的账户后端联调与数据库设计

**日期**: 2026-07-06
**状态**: 已确认，待实现
**关联文件**: `project/user/web/src/views/console/EarningsIndex.vue`、`project/user/web/src/composables/useStyleMarket.js`、`project/user/web/src/views/console/MineIndex.vue`、`docs/superpowers/specs/2026-07-02-earnings-leaderboard-design.md`、`docs/superpowers/specs/2026-07-02-withdraw-aml-agreement-design.md`

---

## 1. 功能概述

把「我的账户」页面（`/console/earnings`，入口在 `MineIndex.vue:54`）从 localStorage 假数据迁移到后端持久化。页面 UI 已经定稿（`EarningsIndex.vue`），底层把当前 `useStyleMarket.js` 里的 earnings/balance/settlement 全部换成 API 调用。

**范围**：仅本模块。

- ✅ `GET /summary` / `settlement-list` / `earnings` / `POST /settle-last-month`
- ✅ 新表 `u_earnings_record`（通用收益账本，供其他模块后续写入）
- ✅ `EarningsIndex.vue` 改用新 composable
- ❌ 提现（`WithdrawIndex.vue`，后续 spec）
- ❌ 排行榜（`useLeaderboard.js` 仍读 localStorage）
- ❌ 风格市场（`useStyleMarket.js` 仍读 localStorage，本次不动）

**关键决策**（已与用户确认）：

1. 单表 + 实时聚合（不建汇总表，余额 = SUM(amount WHERE status=1)）
2. 通用账本（其他模块后续通过 `EarningsService.recordEarnings` 写入）
3. 结算单位：**月**（`YYYY-MM`），不是周（与现有 UI 的「按周结算 / 结算上周」按钮文字不符，需同步调整）
4. 余额语义：未结算收益**不**计入余额；点「结算上月」后才计入（与「结算规则」弹框描述一致）
5. 触发方式：**只手动点击**，不做定时任务

---

## 2. 数据模型

### 2.1 u_earnings_record

唯一一张流水表。

```sql
CREATE TABLE IF NOT EXISTS u_earnings_record (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    type VARCHAR(32) NOT NULL
        COMMENT 'USAGE / MILESTONE / LEADERBOARD_REWARD / INVITE_REWARD / OTHER',
    source_type VARCHAR(32) DEFAULT NULL
        COMMENT 'style_market / invite / leaderboard / manual',
    source_id VARCHAR(64) DEFAULT NULL
        COMMENT '上游业务 ID（解耦，不强外键）',
    title VARCHAR(128) NOT NULL
        COMMENT '列表展示标题，如 "「清新」风格被使用"',
    description VARCHAR(255) DEFAULT NULL,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 0
        COMMENT '0=未结算, 1=已结算',
    settlement_month VARCHAR(7) NOT NULL
        COMMENT 'YYYY-MM，归属月份（插入时按 created_at 计算）',
    settled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status_month (user_id, status, settlement_month),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='用户收益流水表（通用账本）';
```

**约定**：

- `settlement_month` 在**插入时**按 `created_at` 所属月份填入（`YYYY-MM`），后续即使跨月也不变。
- 结算动作（status 0→1）只更新 `status` 和 `settled_at`，**不动 settlement_month**，保证历史可追溯。
- 索引 `(user_id, status, settlement_month)` 专门为「查某月未结算」和「按月聚合」两个高频查询服务。
- 不建独立的账户表，所有数字实时从这张表 `SUM` 出来。

**迁移脚本**：`project/user/api/src/main/resources/db/migration/V1.0.0_007__create_earnings_tables.sql`

---

## 3. 后端实现

### 3.1 新建文件

```
project/user/api/src/main/java/com/aichuangzuo/user/modules/earnings/
├── entity/
│   └── EarningsRecord.java          # 实体，与 u_earnings_record 一一对应
├── mapper/
│   └── EarningsRecordMapper.java    # extends BaseMapper，自定义 4 个聚合 SQL
├── enums/
│   ├── EarningsType.java            # USAGE / MILESTONE / LEADERBOARD_REWARD / INVITE_REWARD / OTHER
│   └── EarningsStatus.java          # UNSETTLED(0) / SETTLED(1)
├── dto/request/
│   ├── ListEarningsRequest.java     # status / month / page / pageSize
│   └── RecordEarningsRequest.java   # type / sourceType / sourceId / title / description / amount
├── vo/
│   ├── AccountSummaryVO.java        # balance / totalEarnings / settledEarnings / unsettledEarnings
│   ├── MonthlySettlementVO.java     # month / count / total / settled / unsettled
│   ├── EarningsRecordVO.java        # id / type / typeLabel / title / description / amount / status / statusLabel / settlementMonth / createdAt
│   └── SettleLastMonthResultVO.java # month / settledCount / settledAmount
├── service/
│   ├── EarningsService.java
│   └── impl/EarningsServiceImpl.java
└── controller/
    └── EarningsController.java      # 4 个 HTTP 接口
```

### 3.2 接口

全部挂在 `/api/v1/user/account` 下，JWT 鉴权（沿用 `SecurityConfig` 的 `anyRequest().authenticated()` + `JwtAuthenticationFilter`）。

| 方法 | 路径 | 入参 | 出参 |
|---|---|---|---|
| `GET` | `/summary` | — | `AccountSummaryVO` |
| `GET` | `/settlement-list` | — | `List<MonthlySettlementVO>`（按月倒序） |
| `GET` | `/earnings` | `status`(all/settled/unsettled) / `month`(YYYY-MM) / `page`(默认 1) / `pageSize`(默认 20, ≤100) | `PageResult<EarningsRecordVO>` |
| `POST` | `/settle-last-month` | 空 body | `SettleLastMonthResultVO` |

**响应示例**：

```json
// GET /summary
{ "balance": 12.40, "totalEarnings": 23.80,
  "settledEarnings": 12.40, "unsettledEarnings": 11.40 }

// GET /settlement-list
[
  { "month": "2026-06", "count": 3, "total": 12.40, "settled": 12.40, "unsettled": 0.00 },
  { "month": "2026-05", "count": 5, "total": 11.40, "settled": 0.00,  "unsettled": 11.40 }
]

// GET /earnings?status=all&page=1&pageSize=20
{ "list": [ { "id": 1, "type": "USAGE", "typeLabel": "使用收益",
              "title": "「清新」风格被使用", "amount": 0.20,
              "status": "settled", "statusLabel": "已结算",
              "settlementMonth": "2026-06", "createdAt": "2026-06-12T..." } ],
  "total": 42, "page": 1, "pageSize": 20 }

// POST /settle-last-month
{ "month": "2026-06", "settledCount": 3, "settledAmount": 12.40 }
```

### 3.3 关键实现

#### Summary（4 张统计卡）

```sql
SELECT
  COALESCE(SUM(CASE WHEN status = 1 THEN amount ELSE 0 END), 0) AS settled,
  COALESCE(SUM(amount), 0) AS total
FROM u_earnings_record
WHERE user_id = #{userId}
```

`balance = settled`，`unsettled = total - settled`。

#### Settlement List（按月聚合）

```sql
SELECT
  settlement_month AS month,
  COUNT(*) AS count,
  SUM(amount) AS total,
  SUM(CASE WHEN status = 1 THEN amount ELSE 0 END) AS settled,
  SUM(CASE WHEN status = 0 THEN amount ELSE 0 END) AS unsettled
FROM u_earnings_record
WHERE user_id = #{userId}
GROUP BY settlement_month
ORDER BY settlement_month DESC
```

#### Earnings 列表（分页 + 筛选）

```sql
SELECT * FROM u_earnings_record
WHERE user_id = #{userId}
  [AND status = #{status}]
  [AND settlement_month = #{month}]
ORDER BY created_at DESC
LIMIT #{offset}, #{pageSize}
```

`status=all` 时不加 status 条件；`month` 为空时不加 month 条件。两条 SQL：list + count，组装 `PageResult`。

#### 结算上月（核心，幂等）

**先 SELECT 再 UPDATE**，包在 `@Transactional` 内避免并发：

```java
String lastMonth = LocalDate.now().minusMonths(1)
    .format(DateTimeFormatter.ofPattern("yyyy-MM"));

// 1) 统计待结算金额（无锁，事务内可见）
BigDecimal amount = earningsRecordMapper.sumUnsettledByMonth(userId, lastMonth);

// 2) UPDATE 把 status 0→1，affected rows = 实际结算条数
int rows = earningsRecordMapper.settleByMonth(userId, lastMonth);
```

```sql
-- sumUnsettledByMonth
SELECT COALESCE(SUM(amount), 0)
FROM u_earnings_record
WHERE user_id = #{userId} AND status = 0 AND settlement_month = #{month}

-- settleByMonth
UPDATE u_earnings_record
SET status = 1, settled_at = NOW(), updated_at = NOW()
WHERE user_id = #{userId} AND status = 0 AND settlement_month = #{month}
```

返回 `{ month, settledCount: rows, settledAmount: amount }`。幂等：第二次点 UPDATE 影响 0 行，amount=0。

### 3.4 Service 内部 API（不给 HTTP）

```java
public EarningsRecord recordEarnings(Long userId, RecordEarningsRequest req) {
    // 校验 req：amount > 0，type 合法
    // 计算 settlement_month = LocalDate.now() 的 YYYY-MM
    // INSERT，status=0，settled_at=null
    // 返回插入后的实体（其他模块用 id 做后续关联）
}
```

后续模块（style-market、invite-reward、leaderboard 等）`@Autowired EarningsService` 后调这个方法即可，无需 HTTP。type / sourceType / sourceId 由调用方根据业务传入。

### 3.5 错误码

**不新增**——结算 0 条是正常结果，不抛异常；前端用 `settledCount > 0` 判断消息文案。

---

## 4. 前端改造

### 4.1 新建文件

**`project/user/web/src/api/earnings.js`**

```js
import { api } from '@/api/auth'

export function getAccountSummary() { return api.get('/account/summary') }
export function getSettlementList() { return api.get('/account/settlement-list') }
export function listEarnings(params) { return api.get('/account/earnings', { params }) }
export function settleLastMonth() { return api.post('/account/settle-last-month') }
```

**`project/user/web/src/composables/useEarnings.js`**

参考 `useUserProfile.js` 的「模块级 ref 单例」模式，导出：

```js
const summary = ref(null)        // AccountSummaryVO
const monthlyList = ref([])      // MonthlySettlementVO[]
const records = ref([])          // EarningsRecordVO[]
const total = ref(0)             // 分页总数
const loading = ref(false)

async function loadSummary()    { ... }
async function loadMonthlyList() { ... }
async function loadRecords(query) { ... }
async function settleLastMonth() {
  const r = await settleLastMonthApi()
  // 成功后同时刷新 summary 和 monthlyList
  await Promise.all([loadSummary(), loadMonthlyList()])
  return r
}
```

### 4.2 修改 `EarningsIndex.vue`

| 旧（localStorage） | 新（API） |
|---|---|
| `import ... from '@/composables/useStyleMarket.js'` | `import { useEarnings } from '@/composables/useEarnings.js'` |
| `getCoinBalance()` | `summary.value?.balance ?? 0` |
| `getTotalEarnings()` | `summary.value?.totalEarnings ?? 0` |
| `getSettledEarnings()` | `summary.value?.settledEarnings ?? 0` |
| `getUnsettledEarnings()` | `summary.value?.unsettledEarnings ?? 0` |
| `getWeeklySettlementList()` | `monthlyList.value` |
| `weeklySettle(previousWeek)` | `settleLastMonth()`（不带参） |
| `getPreviousWeek()` | 前端算 `(now.month - 1) → YYYY-MM` |
| Tab `monthly` → 复用 weeklyList | Tab `monthly` → 调 `loadRecords({ month: previousMonth })` |
| 「按周结算」 | 「按月结算」 |
| 「结算上周」按钮 | 「结算上月」按钮 |
| `canSettleLastWeek` 计算 | 改成 `canSettleLastMonth`：上个月那条 `unsettled > 0` |
| `handleWeeklySettle` | 改成 `handleSettleLastMonth`：`if (settledCount > 0) success else info` |

`onMounted` 钩子里并发调 `loadSummary()` + `loadMonthlyList()`。

### 4.3 不动 `useStyleMarket.js`

它还要给 `StylesIndex.vue` / `StyleMarketIndex.vue` / `CreateIndex.vue` 用（仍在 localStorage）。已知不一致：**风格市场产生的收益暂时不会出现在我的账户页**。同样 `useLeaderboard.js` 仍读旧 localStorage，排行榜也不一致。这两处等各自模块迁移 spec 时再处理。

---

## 5. 边界与错误处理

| 场景 | 处理 |
|---|---|
| 未登录 | axios 拦截器 → 跳登录（沿用） |
| 后端 5xx | `message.error('请求失败')` |
| 上月无未结算 | 后端返回 `settledCount=0`，前端 `message.info('上月没有可结算的收益')` |
| 重复结算（幂等） | 第二次也返回 0，无副作用，UI 同样提示 |
| 用户 0 条记录 | 4 张统计卡全 0，结算按钮 disabled |
| 网络断开 | axios reject，loading=false，**不弹错误**（参考 `useUserProfile.loadProfile` 的静默失败） |
| 用户跨时区 | 后端全用 `LocalDate.now()`（应用服务器时区），文档里记一笔「按服务器时区分月」 |

---

## 6. 测试

### 6.1 集成测试 `EarningsServiceTest.java`

沿用 `HotSearchServiceTest` 的 `@SpringBootTest @Transactional @Rollback` 套路。

| # | 用例 | 断言 |
|---|---|---|
| 1 | 新用户 summary | 4 个数字全 0 |
| 2 | 混合记录 summary（3 条 settled + 2 条 unsettled） | balance=3 条 settled 之和，total=全部 5 条之和 |
| 3 | 多月 settlementList | 每个月一条，金额拆分对，按月倒序 |
| 4 | listEarnings `status=settled` | 只返已结算 |
| 5 | listEarnings `month=2026-06` | 只返该月 |
| 6 | settleLastMonth 只动上月 | 上月 status 0→1，更早月份不变 |
| 7 | settleLastMonth 只动未结算 | 已结算的不被改 |
| 8 | settleLastMonth 幂等 | 第二次调返回 `settledCount=0, settledAmount=0` |
| 9 | recordEarnings 默认未结算 | status=0，settlement_month=当前月 |
| 10 | recordEarnings 跨月归属 | 用 SQL 设 `created_at` 为上月最后一天插入，settlement_month 必须等于 created_at 所在月 |

### 6.2 E2E 手动验证

```bash
# 1. 启全栈
./scripts/local/user-full-stack/start.sh
# 2. 注册新账号 → 登录 → 进「我的」 → 点「我的账户」
# 3. 看到 4 张卡全 0；直接点「结算上月」→ 提示「没有可结算的收益」
# 4. 用 SQL 插 2 条记录（一条上月未结算 + 一条当月未结算）：
mysql -uroot -p123456 aichuangzuo -e "
INSERT INTO u_earnings_record
  (user_id, type, title, amount, status, settlement_month, created_at)
VALUES
  (2, 'USAGE', '「清新」风格被使用', 5.00, 0, '2026-06', '2026-06-15 10:00:00'),
  (2, 'USAGE', '「干货」风格被使用', 3.00, 0, '2026-07', '2026-07-01 10:00:00');"
# 5. 刷新页面：余额 0，累计 8，已结算 0，未结算 8；按月列表 2 行
# 6. 点「结算上月」（今天是 7 月）→ 提示「已结算：5.00 创作币」
# 7. 再看：余额 5，累计 8，已结算 5，未结算 3；2026-06 行 settled=5，unsettled=0
# 8. 切到「收益明细」Tab，分别点 全部/已结算/未结算/按月结算 验证 4 个筛选
# 9. 再点一次「结算上月」→ 提示「上月没有可结算的收益」
# 10. 暗色主题检查列表、按钮、统计卡样式
```

---

## 7. 后续工作（明确不在本期）

1. **提现**（`WithdrawIndex.vue`）：单独的 spec；涉及支付宝接入、风控。
2. **风格市场后端**：把 `useMarketStyle` / `simulateExternalUse` 改成调 `EarningsService.recordEarnings`。
3. **排行榜后端**：`useLeaderboard.js` 改为调 `/api/v1/user/account/earnings` 聚合。
4. **邀请返利**：监听新用户注册事件，给邀请人插入一条 `INVITE_REWARD` 记录。
5. **月初自动结算**：目前只手动；后续可加 `@Scheduled(cron="0 0 1 * * ?")`。
6. **历史月份结算**：当前只能结算上月；超出后冻结成永久未结算（待业务确认要不要补一个「补结历史月份」入口）。