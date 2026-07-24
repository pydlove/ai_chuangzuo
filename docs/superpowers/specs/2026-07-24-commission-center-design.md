# 约稿中心 设计规格

> 创建日期: 2026-07-24
> 状态: 已批准(用户批准推荐方案,跳过详细规格审查,实施完成后整体审查)

## 1. 概述

新增控制台功能 **约稿中心**,允许用户发布/承接稿件征集任务,以创作币结算。

### 核心流程

1. **发布者** 发布约稿任务,设置标题、要求、字数、奖励(≥5 创作币)、截止时间
2. 奖励即时从发布者余额**冻结**(escrow)
3. **投稿人** 选取自己**平台已生成的文章**(必须为 `bizNo` 引用,不可外链)进行投递
4. **同一用户对同一任务仅可投递一次**
5. 截止时间到期前,发布者从投稿中**选择一名获胜者**
6. 平台抽成 10%:获奖者获得 `奖励 × 90%`,平台账户入账 `奖励 × 10%`
7. 未中标的投稿不获得任何奖励,但投稿记录保留可查看
8. 截止 + 24 小时宽限期后发布者仍未选人 → 任务 **EXPIRED**,奖励全额退还发布者
9. 截止前若有投稿:**发布者不可单方面撤销**(避免白嫖)

### 范围(本次交付)

- 前端: 完整实现
- 后端: 预留接口与 TODO 注释(由调用层包裹 `try/catch`,localStorage 兜底)
- 数据存储: 当前阶段用 `localStorage`,key 分别为 `aichuangzuo_commission_tasks`、`aichuangzuo_commission_submissions`

## 2. 菜单与导航

- 侧边栏新增 `约稿中心`,icon 选用 `FileTextOutlined`(投稿=文件投递)
- 路由 `/console/commission`,详情页 `/console/commission/:id`
- 手机端 TabBar 不动(只有 4 个高频入口),通过 `/console/mine` 二级入口可触达

## 3. 业务规则

| 规则 | 值 |
|---|---|
| 最低奖励 | 5 创作币 |
| 最高奖励 | 10000 创作币 |
| 平台抽成 | 10% |
| 截止时间选项 | 3 / 7 / 15 天(下拉三选一) |
| 投稿限额 | 每用户每任务 1 篇 |
| 撤回投稿 | 截止前可撤回(刷新可改投其它稿件) |
| 发布者撤销 | 无投稿时允许;有投稿后**禁止** |
| 余额不足 | 弹 toast,跳提现页 |
| 自动结算 | 截止后 24h 宽限期,逾期 → EXPIRED 全额退款 |

## 4. 数据模型(前端 localStorage 形态)

### Task

```js
{
  id: 'cmt_<timestamp>_<rand>',
  publisherId: 'userId',
  publisherNickname: '昵称',
  title: 'string (≤30字)',
  description: 'string (≤500字)',
  requirements: { minWordCount: number, maxWordCount: number, styleHint?: string },
  rewardCoin: number,            // 发布者出的总奖励(含平台抽成)
  platformFeeRate: 0.10,         // 写死 10%
  deadlineAt: ISOString,         // 截止时间
  graceDeadlineAt: ISOString,    // 截止 + 24h
  status: 'OPEN' | 'SETTLED' | 'EXPIRED' | 'CANCELLED',
  winnerSubmissionId: string | null,
  settledAt: ISOString | null,
  createdAt: ISOString,
}
```

### Submission

```js
{
  id: 'cms_<timestamp>_<rand>',
  taskId: string,
  submitterId: string,
  submitterNickname: string,
  articleBizNo: string,          // 引用 articles 表
  articleTitle: string,
  wordCount: number,
  submittedAt: ISOString,
  withdrawnAt: ISOString | null,
}
```

## 5. 页面结构

### 5.1 `/console/commission` 主列表页

布局: 顶部标题 + 三个 Tab(全部任务 / 我发布的 / 我投稿的),每个 Tab 内有子筛选(全部 / 进行中 / 已截止)。

卡片项字段:

- 标题
- 发布者昵称 + 发布时间
- 奖励徽章(红色高亮,如 "30 创作币")
- 字数要求范围
- 截止倒计时(剩余 X 天 Y 小时,过期显示"已结束")
- 投稿人数(`已有 N 人投稿`)
- 状态徽章(OPEN/SETTLED/EXPIRED/CANCELLED)
- 操作按钮:
  - 别人任务且进行中 → "去投稿"
  - 自己发布且 OPEN → "查看投稿"
  - 自己发布且已结算 → "查看结果"
  - 我投稿过 → "查看我的投稿"

浮动按钮: 右下角"发布约稿"(`PlusOutlined`,仅在"我发布的"Tab 也显示,所有 Tab 都可发)。

### 5.2 `/console/commission/publish` 发布页(Modal 或独立路由)

表单字段:

- 标题(必填,≤30字)
- 需求描述(必填,≤500字)
- 字数下限 / 上限(必填,下限≥100,上限≤5000)
- 风格提示(可选,≤50字)
- 奖励(必填,数字输入,5-10000)
- 截止时间(必填,下拉 3 / 7 / 15 天)
- 实时展示:
  - 平台抽成预览: "平台抽成 X 创作币,投稿者实得 Y"
  - 当前余额: "本次需冻结 X 创作币,余额 Z"

提交: 余额校验 → 冻结 → 写任务 → 跳详情页。

### 5.3 `/console/commission/:id` 详情页

布局: 顶部任务信息卡 + 投稿列表 + 操作区。

**作为发布者视角:**

- 任务信息(只读)
- 状态: 进行中显示"截止倒计时",宽限期内显示"还剩 X 小时",过期显示"已截止"
- 投稿列表(每条):
  - 投递人昵称、投递时间
  - 文章标题、字数
  - 投递人本人头像首字 + 头像色块(用昵称首字生成 hash → 选 ant-design 预设色)
  - 右侧按钮:
    - OPEN 状态 → "查看稿件"(跳我的作品预览) + "选用TA"(主按钮)
    - SETTLED / EXPIRED → 显示中标/未中标

- 底部操作:
  - OPEN 且无人投稿 → "撤回任务(全额退款)"
  - OPEN 且有人投稿 → "去选择获胜者"按钮(强调必须选)
  - 宽限期内 → "立即结算"按钮(必选)

**作为投稿人视角:**

- 任务信息(只读)
- 自己投稿状态卡(已投递文章标题 / 字数 / 投递时间 / 中标状态)
- "查看其他人的投稿"折叠面板(只显示昵称、字数,不暴露文章正文)
- 操作:
  - 未投递 & OPEN → "立即投稿"按钮(打开我的作品选择器)
  - 已投递 & 未被选 & OPEN → "撤回投递"按钮
  - SETTLED 后如自己中标 → 显示"+ X 创作币(已扣除平台抽成)"绿色高亮
  - EXPIRED → 显示"任务已流局"

**作为旁观者视角:**

- 任务信息 + 投稿人数 + 自己的状态(未投递 / 已投递)

### 5.4 投稿选择器(Modal)

打开条件: 详情页"立即投稿"按钮触发。

内容: 调用 `listArticles({ page: 1, pageSize: 50 })` 拉取我的作品,按时间倒序;每条卡片显示标题、字数、平台;点击 → 选中 → 底部"确认投递"。要求字数落在任务字数范围内(超出范围提示且禁用)。

## 6. 状态机

```
OPEN ──(发布者选人)──> SETTLED
OPEN ──(截止后 24h 未选)──> EXPIRED
OPEN ──(无人投稿时发布者撤回)──> CANCELLED
```

## 7. 状态持久化与可恢复性

- localStorage 主存,key:
  - `aichuangzuo_commission_tasks` (Array<CommissionTask>)
  - `aichuangzuo_commission_submissions` (Array<CommissionSubmission>)
- 创作币余额变更: 复用现有的 `useInviteStats().coinBalance`,但其当前为只读 — 本次扩展其在 localStorage 中的写入,新增 key `aichuangzuo_coin_balance`(默认 100)
- 启动时扫描 OPEN 任务,把过期的统一刷为 EXPIRED 并退款
- "渲染时倒计时"用 `setInterval(60s)` 触发刷新

## 8. API 接口(占位)

后端未来实现时建议路径(本次只写 TODO 注释,不实现):

```
GET    /api/commission/tasks?scope=all|mine-published|mine-submitted&status=...
GET    /api/commission/tasks/:id
POST   /api/commission/tasks                       (创建,自动冻结 rewardCoin)
DELETE /api/commission/tasks/:id                   (无人投稿时撤销)
GET    /api/commission/tasks/:id/submissions       (发布者看全部投稿)
POST   /api/commission/tasks/:id/submissions       (投递)
DELETE /api/commission/submissions/:id             (撤回投递)
POST   /api/commission/tasks/:id/settle            (发布者选人,触发结算)
```

后端未来实现时数据表建议:

```
commission_task (
  id, publisher_id, title, description,
  min_word_count, max_word_count, style_hint,
  reward_coin, platform_fee_rate,
  deadline_at, grace_deadline_at,
  status, winner_submission_id, settled_at,
  created_at, updated_at,
  deleted, created_by, updated_by
)
commission_submission (
  id, task_id, submitter_id,
  article_biz_no, article_title, word_count,
  submitted_at, withdrawn_at
)
```

## 9. 端到端验证(手动)

起项目后,本地打开 `/console/commission`:

1. **空态**: 没任务时显示空插画 + "去发布"按钮
2. **发布**: 模拟余额 100,发一个 30 币的任务,余额应变为 70
3. **投稿**: 切换用户(改 localStorage `aichuangzuo_user_id`),从我的作品选择一篇投递
4. **重复投稿**: 同用户再投 → 弹 toast"你已投递过此任务"
5. **选择获胜者**: 发布者视角看投稿列表,点"选用TA",余额 +27(90% × 30)
6. **截止流局**: 改任务 deadlineAt 为过去时间 + 25h,刷新页面 → 状态变 EXPIRED,余额 +30 退还
7. **无稿件投递**: 详情页"立即投稿"→ 我的作品为空 → 显示空态提示
8. **撤回任务**: 无投稿时点"撤回任务"→ 状态变 CANCELLED,余额 +X 退还
9. **移动端**: ≤768px 显示正常,TabBar 不动

## 10. 文件改动清单(实施阶段)

新增:

- `project/user/web/src/views/console/CommissionIndex.vue` 列表页
- `project/user/web/src/views/console/CommissionDetail.vue` 详情页
- `project/user/web/src/views/console/CommissionPublish.vue` 发布页(Modal 也可)
- `project/user/web/src/api/commission.js` API 占位文件
- `project/user/web/src/composables/useCommission.js` 业务 store
- `project/user/web/src/data/commissionSeed.js` 演示数据(3-5 个任务覆盖各状态)
- `project/user/web/src/styles/commission.css` 或 Vue scoped 内

修改:

- `project/user/web/src/router/index.js` 注册 3 条路由
- `project/user/web/src/views/console/ConsoleLayout.vue` `navItems` 增加一项 + 注册 `consoleActions`
- `project/user/web/src/composables/useInviteStats.js` 增加 `coinBalance` 的 setter 能力(若仅有 getter)
- `project/user/web/src/composables/useWorks.js` 暴露 listArticles 调用(若没有)

## 11. 设计令牌复用

- 主色: `var(--color-primary)` = `#FF2442`
- 字体: 默认 sans-serif
- 卡片圆角: 12px
- 状态色: 成功 `#07c160` / 警告 `#fa8c16` / 危险 `#ff4d4f`
- Modal: width 640, footer: null, 居中
- 倒计时显示: 「X 天 Y 小时」「X 小时 Y 分」「即将截止」(< 2h)