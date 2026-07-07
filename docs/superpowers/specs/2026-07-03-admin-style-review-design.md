# 管理控制台风格审核设计规格

> 日期：2026-07-03  
> 范围：`project/admin/web/` 管理控制台前端 - 用户提交风格审核功能  
> 状态：已确认，待实现

---

## 1. 概述

在爱创作管理控制台新增「风格审核」功能。管理员可查看用户从「我的风格」「学习的风格」提交到风格市场的待审核记录，对不合规的风格执行「打回」操作并填写原因。打回后风格状态变为 `rejected`，原因持久化展示。

本期仅实现管理控制台前端 mock，数据来自 admin web 自身生成的 mock，不依赖用户端 localStorage。

---

## 2. 目标与约束

### 2.1 目标

- 提供标准后台审核列表界面，符合「传统管理控制台」预期。
- 与已实现的「用户管理」保持一致的 Ant Design Vue 表格 + Modal 交互模式。
- mock 数据持久化，便于演示审核流程。

### 2.2 约束

- 技术栈：Vue 3 + Vite + Ant Design Vue 4.2.0 + Pinia + Vue Router 4。
- 主色：`#ff2442`，遵循 `docs/design/design-system.md`。
- 文件目录与命名遵循 `docs/architecture/frontend-code-conventions.md`。
- 禁止直接调用 Axios，统一通过 `src/api/`。
- 禁止直接操作 `localStorage`，统一通过 `src/utils/storage.js`。
- 单引号、无分号、2 空格缩进。

---

## 3. 设计方向

采用「Ant Design Vue 经典侧边栏布局 + Table + Modal」方案：

- 左侧固定 200px 侧边栏新增「风格审核」菜单项（与用户管理并列）。
- 内容区放 `a-table` 展示待审核风格列表。
- 操作列「打回」按钮触发 `a-modal`，modal 内填写原因并确认。
- 已打回的行保持可见，状态 tag 变为红色，操作按钮 disabled。

---

## 4. 控制台布局调整

### 4.1 侧边栏菜单

在现有菜单基础上新增一项：

- 「用户管理」：key `/console/users`，图标 `UserOutlined`
- 「风格审核」：key `/console/styles`，图标 `AuditOutlined`

当前激活项高亮规则不变：背景 `#fff0f2`，文字 `#ff2442`。

---

## 5. 风格审核列表页面

### 5.1 页面标题区

- 标题：「风格审核」（h3，字号 18px，字重 600）
- 描述：「审核用户提交到风格市场的风格」

### 5.2 表格列

| 列名 | 字段 | 宽度 | 说明 |
|---|---|---|---|
| ID | id | 120px | 市场风格 ID |
| 风格名称 | name | 160px | 文本 |
| 来源类型 | sourceType | 100px | 我的风格 / 学习的风格 |
| 创作者 | creatorName | 120px | 文本 |
| 提交时间 | createdAt | 170px | yyyy-MM-dd HH:mm |
| 状态 | status | 100px | `a-tag`：待审核=pink/red，已打回=error 红 |
| 操作 | actions | 120px | 打回按钮 |

### 5.3 状态展示

- `pending`：tag 颜色 `#ff4d6f` / `processing`，文字「待审核」
- `rejected`：tag 颜色 `error` 红，文字「已打回」

### 5.4 分页

- `a-pagination`，位于表格底部右侧。
- 每页条数：10 / 20 / 50。

---

## 6. 操作与交互

### 6.1 打回风格

- 当前状态为「待审核」时，操作列显示「打回」按钮。
- 当前状态为「已打回」时，「打回」按钮 disabled。
- 点击「打回」弹出 `a-modal`：
  - 标题：打回风格
  - 内容：展示「风格名称：xxx」「创作者：xxx」
  - 原因输入：`a-textarea`，占位符「请输入打回原因，用户将看到此说明」，必填，最多 200 字
  - 底部：「取消」「确认打回」按钮
- 确认后调用 `rejectStyle(id, reason)`。
- 成功：`message.success('已打回')`；刷新列表。
- 失败：`message.error` 提示错误信息。

### 6.2 查看原因

- 已打回行的状态 tag 旁显示「查看原因」文字链。
- 鼠标悬停显示 `a-tooltip` 展示 `rejectReason`。
- 或点击后弹出 `a-modal` 只读展示原因（二选一，推荐 tooltip 更轻量）。

### 6.3 搜索与刷新

- 搜索框占位符「风格名称或创作者」，宽度 280px。
- 支持按名称或创作者包含关键词过滤（不区分大小写）。
- 刷新按钮重新加载当前列表。

---

## 7. 数据层

### 7.1 Mock 风格数据结构

```javascript
{
  id: 'market-abc123',
  name: '娱乐至死',
  sourceType: 'my', // 'my' | 'learned'
  creatorName: '夜雨微凉',
  prompt: '轻松幽默、网络热梗、短句为主...',
  scope: '公众号情感文',
  status: 'pending', // 'pending' | 'rejected'
  rejectReason: '',
  createdAt: '2026-07-03 10:23:45'
}
```

### 7.2 Mock 接口（src/api/style.js）

```javascript
export function listStyles(params) { /* return Promise */ }
export function rejectStyle(id, reason) { /* return Promise */ }
```

- 使用 `setTimeout` 模拟 300~600ms 延迟。
- 生成 30 条 mock 数据，含不同状态、来源类型、中文昵称。
- 通过 `storage.js` 持久化，避免刷新后状态丢失。

### 7.3 状态管理（src/composables/useStyleReview.js）

```javascript
export function useStyleReview() {
  return {
    styles,
    loading,
    total,
    page,
    pageSize,
    keyword,
    fetchStyles,
    handleSearch,
    handleReset,
    handlePageChange,
    handleReject
  }
}
```

---

## 8. 文件结构

```text
project/admin/web/src/
├── api/
│   ├── auth.js
│   ├── user.js
│   └── style.js                  # 新增
├── composables/
│   ├── useTheme.js
│   ├── useUserManagement.js
│   └── useStyleReview.js         # 新增
├── layouts/
│   └── AdminLayout.vue           # 修改：新增菜单
├── views/
│   ├── LoginView.vue
│   ├── ConsoleView.vue
│   ├── UserListView.vue
│   └── StyleReviewView.vue       # 新增
└── router/
    └── index.js                  # 修改：新增 /console/styles 路由
```

### 8.1 路由配置

```javascript
{
  path: '/console',
  component: () => import('@/layouts/AdminLayout.vue'),
  meta: { requiresAuth: true },
  redirect: '/console/users',
  children: [
    {
      path: 'users',
      name: 'AdminUserList',
      component: () => import('@/views/UserListView.vue')
    },
    {
      path: 'styles',
      name: 'AdminStyleReview',
      component: () => import('@/views/StyleReviewView.vue')
    }
  ]
}
```

---

## 9. 数据流

```text
StyleReviewView 挂载
    → useStyleReview.init()
        → 调用 listStyles({ page: 1, pageSize: 10 })
            → 首次从 MOCK_STYLES 初始化并写入 storage
            → 后续从 storage 读取
            → setTimeout 模拟延迟
            → 过滤/分页
                → 返回 { list, total }
                    → 更新 styles / total

搜索：handleSearch(keyword) → fetchStyles()
打回：handleReject → rejectStyle(id, reason) → 更新 storage → fetchStyles()
```

---

## 10. 验收标准

- [ ] 侧边栏「风格审核」菜单正常渲染并与「用户管理」并列。
- [ ] 点击菜单进入 `/console/styles`，列表加载 30 条 mock 数据。
- [ ] 表格列与规格一致，状态 tag 正确显示。
- [ ] 搜索按风格名称或创作者过滤生效。
- [ ] 点击「打回」弹出 modal，未填原因时确认按钮不可点或提交报错。
- [ ] 填写原因后确认打回，该行状态变为「已打回」，打回按钮 disabled。
- [ ] 刷新页面后打回状态与原因保留。
- [ ] Playwright 脚本验证：列表渲染、打回 modal、状态变更。

---

## 11. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-07-03 | v1.0 | 初稿：管理控制台风格审核功能设计 |
