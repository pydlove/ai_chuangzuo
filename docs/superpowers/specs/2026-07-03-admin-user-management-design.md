# 管理控制台用户管理设计规格

> 日期：2026-07-03  
> 范围：`project/admin/web/` 管理控制台前端 - 注册用户管理功能  
> 状态：待实现

---

## 1. 概述

为爱创作（AI Creation）管理控制台实现「注册用户管理」功能。管理员登录后可查看平台注册用户列表，进行搜索、启用/禁用、重置密码、查看详情等操作。功能采用 Ant Design Vue 经典后台布局 + 表格方式实现，数据来自前端 mock。

---

## 2. 目标与约束

### 2.1 目标

- 提供标准后台用户管理界面，符合「传统管理控制台」预期。
- 使用 Ant Design Vue 现成组件（a-table、a-modal、a-drawer、a-popconfirm），保证成熟度与一致性。
- 数据层 mock，便于前端独立演进，后续可平滑替换为真实接口。

### 2.2 约束

- 技术栈：Vue 3 + Vite + Ant Design Vue 4.2.0 + Pinia + Vue Router 4。
- 主色：`#ff2442`，遵循 `docs/design/design-system.md`。
- 文件目录与命名遵循 `docs/architecture/frontend-code-conventions.md`。
- 现有 `/login` 与 `/console` 路由保留；`/console` 重定向到 `/console/users`。

---

## 3. 设计方向

采用「Ant Design Vue 经典侧边栏布局 + Table」方案：

- 左侧固定 200px 侧边栏，顶部 56px header。
- 内容区放 `a-table` 展示用户列表。
- 工具栏提供搜索与刷新。
- 操作通过 `a-popconfirm`、`a-modal`、`a-drawer` 实现。

---

## 4. 控制台布局

```text
┌─────────────────────────────────────────────────┐
│ 爱创作 · 管理控制台                  [管理员头像] [退出] │  ← Header (56px)
├──────────┬──────────────────────────────────────┤
│          │ 首页 / 用户管理                       │  ← 面包屑
│ 品牌区   ├──────────────────────────────────────┤
│          │                                      │
│ 用户管理 │     注册用户管理                       │
│          │     查看与管理平台注册用户              │
│          │                                      │
│          │     [搜索框] [查询] [重置] [刷新]     │
│          │     ┌────────────────────────────┐   │
│          │     │  ID │ 账号 │ 邮箱 │ ...  │   │
│          │     ├────────────────────────────┤   │
│          │     │  1  │ ... │ ... │ ...   │   │
│          │     └────────────────────────────┘   │
│          │              [分页器]                 │
└──────────┴──────────────────────────────────────┘
```

### 4.1 侧边栏

- 宽度 200px，背景白。
- 顶部品牌区：Logo + 「爱创作 管理控制台」品牌字。
- 菜单项：「用户管理」（默认激活），图标使用 `@ant-design/icons-vue` 中的 `UserOutlined`。
- 激活态：背景 `#fff0f2`，文字 `#ff2442`。

### 4.2 Header

- 高度 56px，背景白，下边框 `border-light`。
- 左侧：面包屑「首页 / 用户管理」。
- 右侧：管理员头像（首字母圆形）+ 名称 + 「退出登录」按钮。

### 4.3 内容区

- 背景 `#f8f9fa`。
- 内边距 24px。
- 内容卡片白底圆角，阴影 `shadow-sm`。

---

## 5. 用户列表页面

### 5.1 页面标题区

- 标题：「注册用户管理」（h3，字号 18px，字重 600）。
- 描述：「查看与管理平台注册用户」。

### 5.2 工具栏

| 元素 | 规范 |
|---|---|
| 搜索输入框 | `a-input` 占位符「账号或邮箱」，宽度 280px |
| 查询按钮 | `a-button type="primary"` 主色 |
| 重置按钮 | `a-button` 默认态 |
| 刷新按钮 | `a-button` 默认态，带 `ReloadOutlined` 图标 |

### 5.3 表格列

| 列名 | 字段 | 宽度 | 说明 |
|---|---|---|---|
| ID | id | 80px | 数字 |
| 账号 | account | 140px | 文本 |
| 邮箱 | email | 200px | 文本 |
| 昵称 | nickname | 120px | 文本 |
| 状态 | status | 100px | `a-tag`：启用=success 绿，禁用=error 红 |
| 注册时间 | createdAt | 170px | yyyy-MM-dd HH:mm |
| 最后登录 | lastLoginAt | 170px | yyyy-MM-dd HH:mm 或「—」 |
| 操作 | actions | 240px | 启用/禁用、重置密码、查看详情按钮 |

### 5.4 分页

- `a-pagination`，位于表格底部右侧。
- 每页条数：10 / 20 / 50。
- 显示总数与当前页。

---

## 6. 操作与交互

### 6.1 启用/禁用

- 当前状态为「启用」时，操作按钮显示「禁用」。
- 当前状态为「禁用」时，操作按钮显示「启用」。
- 点击后弹出 `a-popconfirm` 确认，确认后调用 `updateUserStatus(id, status)`。
- 成功：刷新列表 + `message.success` 提示。
- 失败：`message.error` 提示错误信息。

### 6.2 重置密码

- 操作按钮「重置密码」。
- 点击后弹出 `a-modal`：
  - 标题：重置用户密码
  - 内容：用户账号 + 提示「重置后密码将设为 `123456`，请通知用户及时修改。」
  - 底部：「取消」「确认重置」按钮。
- 确认后调用 `resetUserPassword(id)`。
- 成功：`message.success('密码已重置')`；失败提示错误。

### 6.3 查看详情

- 操作按钮「查看详情」。
- 点击后弹出右侧 `a-drawer`，宽度 480px。
- 抽屉内展示字段：
  - ID
  - 账号
  - 邮箱
  - 昵称
  - 状态（tag）
  - 邀请码
  - 注册时间
  - 最后登录时间
- 底部「关闭」按钮。

### 6.4 搜索与刷新

- 搜索：输入关键词后点击「查询」或按回车，调用 `listUsers({ keyword, page, pageSize })`，过滤规则：账号或邮箱包含关键词（不区分大小写）。
- 重置：清空搜索框并重新加载第一页。
- 刷新：重新加载当前查询条件下的数据。

---

## 7. 数据层

### 7.1 Mock 用户数据结构

```javascript
{
  id: 1,
  account: 'aichuang_001',
  email: 'aichuang_001@example.com',
  nickname: '夜雨微凉',
  status: 'enabled', // 'enabled' | 'disabled'
  inviteCode: 'ABC123',
  createdAt: '2026-06-01 10:23:45',
  lastLoginAt: '2026-07-02 08:11:02'
}
```

### 7.2 Mock 接口（src/api/user.js）

```javascript
export function listUsers(params) { /* return Promise */ }
export function getUser(id) { /* return Promise */ }
export function updateUserStatus(id, status) { /* return Promise */ }
export function resetUserPassword(id) { /* return Promise */ }
```

- 使用 `setTimeout` 模拟 300~600ms 延迟。
- 生成 50 条 mock 数据，含中文昵称、随机邮箱。

### 7.3 状态管理（src/composables/useUserManagement.js）

```javascript
export function useUserManagement() {
  return {
    users,        // 当前页数据
    loading,      // 加载态
    total,        // 总数
    page,         // 当前页
    pageSize,     // 每页条数
    keyword,      // 搜索关键词
    fetchUsers,   // 加载列表
    handleSearch, // 搜索
    handleReset,  // 重置
    handlePageChange,
    handleStatusChange,
    handleResetPassword
  }
}
```

---

## 8. 文件结构

```text
project/admin/web/src/
├── layouts/
│   └── AdminLayout.vue          # 控制台布局
├── views/
│   ├── LoginView.vue
│   ├── ConsoleView.vue          # 重定向到 /console/users
│   └── UserListView.vue         # 用户列表
├── api/
│   ├── auth.js
│   └── user.js                  # 新增
├── composables/
│   ├── useTheme.js
│   └── useUserManagement.js     # 新增
└── router/
    └── index.js                 # 调整路由
```

### 8.1 路由配置

```javascript
{
  path: '/console',
  component: () => import('@/layouts/AdminLayout.vue'),
  meta: { requiresAuth: true },
  children: [
    {
      path: '',
      redirect: 'users'
    },
    {
      path: 'users',
      name: 'AdminUserList',
      component: () => import('@/views/UserListView.vue')
    }
  ]
}
```

---

## 9. 数据流

```text
UserListView 挂载
    → useUserManagement.init()
        → 调用 listUsers({ page: 1, pageSize: 10 })
            → setTimeout 模拟延迟
            → 过滤/分页 mock 数据
                → 返回 { list, total }
                    → 更新 users / total

搜索：handleSearch(keyword) → fetchUsers()
状态切换：handleStatusChange → updateUserStatus → fetchUsers()
重置密码：handleResetPassword → resetUserPassword → message 提示
查看详情：点击 → a-drawer 显示用户详情（从当前 users 查找）
```

---

## 10. 验收标准

- [ ] 控制台布局正常渲染：侧边栏 + header + 内容区。
- [ ] 侧边栏「用户管理」高亮激活。
- [ ] 表格加载 50 条 mock 数据并显示分页器。
- [ ] 搜索按账号或邮箱过滤生效（不区分大小写）。
- [ ] 启用/禁用操作触发 popconfirm 确认并更新状态。
- [ ] 重置密码弹框可确认并显示成功提示。
- [ ] 查看详情打开右侧抽屉并展示完整字段。
- [ ] Playwright 脚本验证：页面渲染、搜索过滤、详情抽屉、状态切换。
- [ ] 项目命名与目录结构符合前端代码规范。

---

## 11. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-07-03 | v1.0 | 初稿：管理控制台用户管理功能设计 |