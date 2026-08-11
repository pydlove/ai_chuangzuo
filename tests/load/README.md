# 压测脚本使用说明

本目录存放 k6 压测脚本，用于测试爱创作用户端 API 性能。

## 目录结构

```text
tests/load/
├── config/                # 配置文件与公共逻辑
│   ├── config.js          # 服务器、账号、接口路径配置
│   ├── accounts.json      # 压测用的用户账号池
│   ├── auth.js            # 登录逻辑（内部使用）
│   ├── summary.js         # 报告生成（内部使用）
│   └── fetch_users.js     # 从管理端拉取用户到 accounts.json
├── case/                  # 按业务域划分的压测脚本
│   ├── auth/              # 认证业务压测脚本
│   │   ├── login.js           # 登录压测
│   │   ├── register.js        # 注册压测（依赖 test profile 抓取验证码）
│   │   ├── forgot_password.js # 忘记密码压测（依赖 test profile 抓取验证码）
│   │   ├── send_email_code.js # 发送邮箱验证码压测
│   │   └── auth_mix.js        # 认证混合场景
│   └── creation/          # 创作业务压测脚本
│       ├── smoke.js           # 冒烟测试
│       ├── loadtest.js        # 阶梯负载测试
│       ├── stress.js          # 压力测试
│       ├── capacity.js        # 容量测试
│       ├── users.js           # 指定并发用户数测试
│       ├── api_mix.js         # 混合场景测试
│       ├── mixed_read_write.js# 读写混合测试（读文章 + 提交创作任务）
│       └── with_login.js      # 已合并到其它脚本，保留兼容
├── run                    # 包装脚本，支持 ./run script.js 1000 写法
└── report/                # 压测报告输出目录
    ├── report-latest.md       # 最新压测报告
    └── report-*.md            # 归档报告
```

## 快速开始

### 1. 配置服务器

编辑 `config/config.js`：

```javascript
const SERVER_IP = '101.126.15.58';
const SERVER_PORT = '22345';   // 用户端 Nginx 入口
```

### 2. 准备用户账号

方式一：手动填写 `config/accounts.json`

```json
[
  { "email": "user1@example.com", "password": "123456" },
  { "email": "user2@example.com", "password": "123456" }
]
```

方式二：从管理端自动拉取（推荐）

```bash
# 拉 50 个用户，默认设置 pro 套餐、2026-12-31 到期
node config/fetch_users.js

# 拉 100 个用户
USER_COUNT=100 node config/fetch_users.js
```

### 3. 跑压测

```bash
# 50 并发读写混合压测
./run mixed_read_write.js 50

# 100 并发只读
./run users.js 100

# 认证压测
./run case/auth/login.js 100
./run case/auth/send_email_code.js 100
./run case/auth/register.js 50
./run case/auth/forgot_password.js 50
./run case/auth/auth_mix.js 100

# 创作压测
./run mixed_read_write.js 50

# 阶梯加压到 100 并发
./run loadtest.js
```

### 4. 看报告

```bash
cat report/report-latest.md
```

## 各脚本说明

| 脚本 | 用途 | 典型用法 |
|------|------|----------|
| `case/creation/smoke.js` | 5 并发快速验证接口通不通 | `k6 run case/creation/smoke.js` |
| `case/creation/loadtest.js` | 10→50→100 并发阶梯加压 | `k6 run case/creation/loadtest.js` |
| `case/creation/stress.js` | 100→800 并发找到崩溃边界 | `k6 run case/creation/stress.js` |
| `case/creation/capacity.js` | 50→1000 并发容量测试 | `k6 run case/creation/capacity.js` |
| `case/creation/users.js` | 指定并发数，简单直接 | `./run users.js 500` |
| `case/creation/api_mix.js` | 多接口混合：列表/详情/创作 | `./run api_mix.js 100` |
| `case/creation/mixed_read_write.js` | 70% 读 + 30% 写，用真实标题生成文章 | `./run mixed_read_write.js 100` |
| `case/auth/login.js` | 多账号循环登录 | `./run case/auth/login.js 100` |
| `case/auth/login_capacity.js` | 登录阶梯容量测试：5→10→20→30→50 | `k6 run case/auth/login_capacity.js` |
| `case/auth/send_email_code.js` | 发送邮箱验证码 | `./run case/auth/send_email_code.js 100` |
| `case/auth/register.js` | 发送验证码 + 注册完整流程（依赖 test profile） | `./run case/auth/register.js 50` |
| `case/auth/forgot_password.js` | 发送验证码 + 重置密码完整流程（依赖 test profile） | `./run case/auth/forgot_password.js 50` |
| `case/auth/auth_mix.js` | 登录/注册/忘记密码混合场景 | `./run case/auth/auth_mix.js 100` |

## 命令行传并发数

所有支持 `TARGET_VUS` 的脚本都可以用 `./run` 包装脚本：

```bash
./run users.js 100
./run mixed_read_write.js 500
./run api_mix.js 200
```

等价于：

```bash
TARGET_VUS=100 k6 run case/creation/users.js
TARGET_VUS=500 k6 run case/creation/mixed_read_write.js
```

## 报告文件

每次运行会生成在 `report/` 目录下：

```text
report/report-latest.md                  # 最新报告，直接看这个
report/report-latest.json                # 最新原始数据
report/report-smoke-2026-08-07T...md    # 带脚本名和时间戳的归档
report/report-smoke-2026-08-07T...json  # 归档原始数据
```

## 核心指标解读

打开 `report-latest.md` 看这几个数：

| 指标 | 健康标准 |
|------|----------|
| 错误率 | < 0.1% 优秀，< 1% 可接受，> 1% 必须优化 |
| P95 响应时间 | < 200ms 优秀，< 500ms 可用，> 1s 临界，> 2s 差 |
| QPS | 看业务预期 |

## 常见问题

### 1. 登录返回 403

- 确认 `config/config.js` 里 `SERVER_PORT` 是用户端端口（22345），不是管理端
- 确认账号密码是用户端账号

### 2. 注册/忘记密码压测报错「验证码错误」

- `register.js` 和 `forgot_password.js` 依赖 `/__test/email-code` 抓取 GreenMail 里的验证码
- 该端点只在 `test` profile 启用；生产/预发环境没有，这两个脚本无法直接跑通
- 如需在部署环境压测完整注册链路，需要服务端临时开启 test profile 或提供验证码 bypass 能力

### 3. 创作任务提交后后端生成不了文章

- 说明任务队列积压，worker 处理不过来
- 降低 `mixed_read_write.js` 里的写比例（默认 30%）
- 或减少并发数

### 3. 账号不够

- 用 `config/fetch_users.js` 从管理端多拉一些用户
- 建议并发数 : 账号数 ≈ 10 : 1

### 4. 报告持续时间显示 0 秒

- 不影响结果判断，是 k6 版本差异

## 压测建议流程

```bash
# 1. 准备账号
USER_COUNT=100 node config/fetch_users.js

# 2. 冒烟测试
k6 run case/creation/smoke.js

# 3. 读写混合压测，逐步加压
./run mixed_read_write.js 50
./run mixed_read_write.js 100
./run mixed_read_write.js 200

# 4. 看报告
ls -lt report/report-*.md | head -5
```

## 修改写比例

编辑 `case/creation/mixed_read_write.js` 里的这行：

```javascript
if (Math.random() < 0.7) {   // 0.7 = 70% 读，30% 写
```

改成 `0.9` 就是 90% 读 + 10% 写。
