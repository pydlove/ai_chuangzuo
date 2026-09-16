# 模拟运营（机器人用户旅程模拟）设计文档

- 日期：2026-09-16
- 状态：已确认设计，待实施
- 涉及端：admin/api、admin/web、user/api

## 1. 背景与目标

管理端「运营管理」下新增「模拟运营」功能。第一个 tab「模拟新用户」：运营人员手动创建批次，生成指定数量的机器人用户，并让每个机器人按概率走完一段模拟真实用户的旅程，每个阶段都通过 HTTP 调用用户端真实接口完成（模拟登录、带机器人 token 调公开接口），从而在生产链路中产生运营数据（注册用户、抽奖记录、会员订单、创作收益、约稿投稿、邀请返佣）。

核心要求：

- 机器人归类：用户表已有 `u_user.user_type`（0=机器人，1=真实用户），机器人写 0。
- 公开业务链路零改动：用户端只新增受内部密钥保护的内部接口，以及在支付校验处加一处「内部密钥放行」。
- 支付绿通：复用现有 test_mode 分支（payCode=123456 直接确认激活），通过内部密钥在请求级强制进入该分支；全局开关 `a_payment_config.test_mode` 保持关闭，不受影响。

## 2. 整体架构

```
管理端 web（运营管理 → 模拟运营）
   │ 创建批次 / 查进度 / 取消
管理端 admin/api  simulation 模块
   │  MySQL 任务表(a_simulation_*) + @Scheduled 消费器（复用现有调度模式）
   │  RestTemplate + X-Internal-Key + 机器人 JWT
用户端 user/api
   │  内部绿通：POST /internal/simulation/robots（注册机器人）
   │  其余全部走公开真实接口（带机器人 token）
MySQL（user 库：u_user / u_order / u_earnings_record / ... 真实业务数据）
```

管理端通过 RestTemplate 调用用户端（复用现有 `user.api.base-url` 配置与 X-Internal-Key 内部调用机制，端口：user 25050 / admin 26060）。

## 3. 机器人旅程

每阶段独立概率，命中才执行，未命中记 SKIPPED：

| 顺序 | 阶段 | 调用的接口 | 说明 |
|---|---|---|---|
| 1 | 注册 | `POST /api/v1/user/internal/simulation/robots`（内部） | 虚拟邮箱、跳过验证码、user_type=0、随机携带一个现有机器人的邀请码（走真实邀请返佣链路） |
| 2 | 登录 | `POST /api/v1/user/auth/login` | 获取机器人 token，后续请求均携带 |
| 3 | 资料 | `PUT /me/nickname`、`PUT /me/profile`、`POST /me/avatar` | 昵称/简介由 admin 侧 AiProviderClient 生成；头像从 picsum.photos 随机拉图后走真实上传接口 |
| 4 | 抽奖 | `GET /lottery/campaigns/current` → `POST /lottery/draw` | — |
| 5 | 买会员 | `POST /membership/subscribe`（payCode=123456 + X-Internal-Key） | 内部密钥强制走 test_mode 分支，confirmOrder 真实激活 |
| 6 | 自由创作 | `GET /market-skills?publisherType=…` → `POST /generation-tasks` → 轮询完成 | 按配置的发布者范围（ROBOT/ALL/REAL）随机选提示词；发布者经 SkillMarketUsageService 获得真实收益 |
| 7 | 约稿 | `GET /commission/tasks` → `POST /commission/tasks/{taskId}/submissions` | 随机选进行中的约稿任务，投第 6 步生成的文章 |
| 8 | 邀请绑定 | （在第 1 步注册时完成） | — |

**执行节奏**：消费器每 5 秒 tick 一次，每次只推进一个机器人的一个阶段；阶段间隔与机器人间隔均为可配置随机范围，通过 `next_run_at` 控制。进程重启后从任务表恢复。

**机器人邮箱格式**：`bot{batchNo后8位}{seq}@{随机单词}.simrobot.com`，格式合法、全局唯一、一眼可识别为模拟数据。

## 4. 数据表（admin 库）

Flyway 新文件：`V2.0.0_105__create_simulation_tables.sql`（版本号以当时实际最新为准顺延）。

### 4.1 `a_simulation_batch`（批次）

| 字段 | 说明 |
|---|---|
| id, batch_no | 主键 + 业务编号（SIM+日期+序号） |
| user_count | 生成机器人数（1-500） |
| plan_id, plan_name | 会员版本快照（默认专业版） |
| stage_config | JSON：各阶段启用+概率（lottery/membership/create/commission）、prompt_scope（ROBOT/ALL/REAL）、user_interval_min/max、stage_interval_min/max |
| status | PENDING / RUNNING / COMPLETED / CANCELED |
| total_count, completed_count, failed_count | 进度冗余，便于列表展示 |
| remark, created_by, created_at, started_at, finished_at | 审计字段 |

### 4.2 `a_simulation_robot`（批次内每个机器人一行，执行状态机载体）

| 字段 | 说明 |
|---|---|
| id, batch_id, seq | seq = 批次内序号 |
| email, password | 虚拟邮箱 + 初始密码（AES 加密存储） |
| user_id | 用户端注册成功后回写的 u_user.id |
| invite_code | 绑定的邀请码（冗余） |
| status | WAITING / IN_PROGRESS / COMPLETED / FAILED / CANCELED |
| current_stage | REGISTER / PROFILE / LOTTERY / MEMBERSHIP / CREATE / COMMISSION |
| next_run_at | 下一阶段允许执行时间（间隔控制） |
| fail_stage, fail_reason | 失败现场 |
| started_at, finished_at | — |

### 4.3 `a_simulation_robot_log`（阶段执行流水）

id、robot_id、batch_id、stage、status（SUCCESS/FAILED/SKIPPED）、detail（JSON：generation_task_id、market_skill_biz_no、commission_task_id、耗时等）、error_msg、created_at。

## 5. 状态机

```
批次：PENDING ──首个 robot 被领取──▶ RUNNING ──全部 robot 终态──▶ COMPLETED
                              └──▶ CANCELED（手动取消，未终态 robot 标记 CANCELED）

robot：WAITING ──▶ IN_PROGRESS（逐阶段推进，next_run_at 控制节奏）
         │                ├─ 阶段失败 ──▶ FAILED（批次继续跑其他 robot）
         │                └─ 全部阶段完成 ──▶ COMPLETED
         └─ 批次取消 ──▶ CANCELED
```

按「阶段」而非「整个用户」调度：重启恢复精确到阶段、间隔控制精确、单 tick 工作量小不阻塞调度线程。消费器按 `current_stage` switch 分发。

## 6. 接口清单

### 6.1 管理端 admin/api（新模块 `modules/simulation`）

| 接口 | 说明 |
|---|---|
| `POST /api/v1/admin/simulation/batches` | 创建批次；服务端校验：数量 1-500、概率 0-100、interval min≤max |
| `GET /api/v1/admin/simulation/batches` | 分页列表（带进度、状态） |
| `GET /api/v1/admin/simulation/batches/{id}` | 批次详情 + robot 列表 |
| `GET /api/v1/admin/simulation/batches/{id}/logs` | 按 robot/阶段过滤的分页日志 |
| `POST /api/v1/admin/simulation/batches/{id}/cancel` | 取消：批次 → CANCELED，未终态 robot → CANCELED |

### 6.2 用户端 user/api 改动点

**新增 `internal/controller/SimulationInternalController`**（X-Internal-Key + 内部 JWT，与 UserApiClient 同套校验）：

- `POST /api/v1/user/internal/simulation/robots`：入参 email、password、inviteCode（可选）。复用 AuthServiceImpl 注册核心（邮箱/密码校验、handleInviteRelation 邀请返佣），跳过邮箱验证码、强制 user_type=0。返回 userId、email。
- `GET /api/v1/user/internal/simulation/robot-invite-codes?count=N`：随机返回 N 个现有机器人的邀请码；无机器人时返回空（首个批次自然跳过绑定）。

**技能市场列表加可选参数**：`GET /market-skills?publisherType=ROBOT|REAL`（不传=全部），列表 SQL 按 `u_user.user_type` 过滤。公开参数、无副作用。

**PaymentServiceImpl 一处放行**：subscribe 的 testMode 校验条件改为 `testMode || 内部密钥有效`（从请求上下文读 X-Internal-Key 校验）。生产无密钥时行为与现状完全一致；payCode 仍要求 123456，走原有 confirmOrder 激活链路（含会员待生效 Job）。

**配置**：复用 admin 侧 `user.api.base-url` 与现有内部密钥配置项，不新增。

## 7. 前端（admin/web）

路由 `/operation/simulation`，运营管理菜单加「模拟运营」入口，页面顶部 tabs（为后续扩展留位）。

**Tab 1：模拟新用户**

- 创建表单（弹窗）：生成数量（默认 10，上限 500）、会员版本（下拉，调管理端 plan 列表，默认专业版）、提示词范围（单选：仅机器人/所有人/仅真实用户）、各阶段启用+概率（抽奖/买会员/自由创作/约稿，默认 100/100/100/50）、用户间隔秒（min-max，默认 10-30）、阶段间隔秒（min-max，默认 3-8）。
- 批次列表：批次号、数量、进度条（完成/失败/总数）、状态标签、创建时间、操作（详情、取消）。
- 详情抽屉：robot 列表（邮箱、状态、当前阶段、失败原因）+ 展开阶段日志（阶段、结果、耗时、detail）。

风格遵循管理端现有 Ant Design Vue 页面，不加新依赖。

## 8. 错误处理

- **阶段失败**：记录 fail_stage/fail_reason + 日志，robot → FAILED，批次继续跑其余 robot；不做自动重试（需要更多数据时人工再发一批）。
- **创建校验失败**：概率越界、interval min>max、数量超上限，创建接口 400，沿用全局异常规范。
- **取消**：仅 PENDING/RUNNING 可取消；执行中 robot 当前阶段跑完即停，不中断半截请求。
- **幂等**：机器人邮箱唯一约束兜底，重复注册返回已有账号（消费器视为成功继续）。
- **依赖前置不满足**：抽奖无进行中活动、无进行中约稿任务、提示词范围内无可选提示词时，该阶段记 SKIPPED 并在 detail 注明原因，不算失败，旅程继续。

## 9. 验证方式

1. 管理端创建 3-5 人小批次，观察日志逐阶段推进、间隔生效；
2. 用户端库核对：`u_user`（user_type=0）、`u_user_invite_relation`、抽奖记录、`u_order`+`u_user_membership`、`u_earnings_record`（提示词发布者收益）、`u_commission_submission`；
3. 管理端统计/看板能看到机器人产生的运营数据；
4. 确认生产 test_mode 开关关闭时：带内部密钥的订阅成功、无密钥请求行为不变。
