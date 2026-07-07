# 管理控制台用户管理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `project/admin/web/` 管理控制台实现注册用户管理页面，包含侧边栏布局、用户列表表格、搜索分页、启用/禁用、重置密码、查看详情操作。

**Architecture:** 采用 Ant Design Vue 经典后台布局（左 200px 侧边栏 + 顶 56px header + 内容区）。Mock 数据层放在 `api/user.js`，列表状态用 composable 管理。路由从 `/console` 改为带子路由的 AdminLayout，默认跳转到 `/console/users`。

**Tech Stack:** Vue 3, Vite 5, Ant Design Vue 4, Pinia 2, Vue Router 4, @ant-design/icons-vue 7

## Global Constraints

- 主色：`#ff2442`，遵循 `docs/design/design-system.md`。
- 命名规范遵循 `docs/architecture/frontend-code-conventions.md`。
- 禁止直接调用 Axios，统一通过 `src/api/`。
- 禁止直接操作 `localStorage`，统一通过 `src/utils/storage.js`。
- 单引号、无分号、2 空格缩进。
- 现有 `/login` 路由保留不变。

---

## File Structure

```text
project/admin/web/src/
├── layouts/
│   └── AdminLayout.vue        # 控制台布局（新增）
├── views/
│   ├── LoginView.vue
│   ├── ConsoleView.vue        # 保留文件但路由不再使用
│   └── UserListView.vue       # 用户列表（新增）
├── api/
│   ├── auth.js
│   └── user.js                # 新增
├── composables/
│   ├── useTheme.js
│   └── useUserManagement.js   # 新增
└── router/
    └── index.js               # 修改
```

---

### Task 1: Mock 用户接口 src/api/user.js

**Files:**
- Create: `project/admin/web/src/api/user.js`

**Interfaces:**
- Produces: `listUsers(params)`, `getUser(id)`, `updateUserStatus(id, status)`, `resetUserPassword(id)`.

- [ ] **Step 1: 创建 user.js**

```javascript
// 内置 50 条 mock 数据，含中文昵称、随机邮箱、状态、邀请码
// 使用 setTimeout 模拟 300~600ms 延迟

const NICKNAMES = ['夜雨微凉', '山间清风', '云端之上', '月下独酌', '林深时见鹿', '海蓝时见鲸', '梦里花落', '半夏微凉', '浅笑安然', '一纸荒年']
const STATUS_LIST = ['enabled', 'enabled', 'enabled', 'enabled', 'disabled']

function randomDate(daysAgo) {
  const date = new Date(Date.now() - Math.floor(Math.random() * daysAgo * 86400000))
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function generateInviteCode() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let result = ''
  for (let i = 0; i < 6; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

const MOCK_USERS = Array.from({ length: 50 }, (_, i) => ({
  id: i + 1,
  account: `aichuang_${String(i + 1).padStart(3, '0')}`,
  email: `user${i + 1}@example.com`,
  nickname: NICKNAMES[i % NICKNAMES.length],
  status: STATUS_LIST[i % STATUS_LIST.length],
  inviteCode: generateInviteCode(),
  createdAt: randomDate(180),
  lastLoginAt: Math.random() > 0.2 ? randomDate(30) : null
}))

function delay(ms = 300 + Math.floor(Math.random() * 300)) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function listUsers(params = {}) {
  const { keyword = '', page = 1, pageSize = 10 } = params
  return delay().then(() => {
    const filtered = MOCK_USERS.filter((u) => {
      if (!keyword) return true
      const kw = keyword.toLowerCase()
      return u.account.toLowerCase().includes(kw) || u.email.toLowerCase().includes(kw)
    })
    const start = (page - 1) * pageSize
    return {
      list: filtered.slice(start, start + pageSize),
      total: filtered.length
    }
  })
}

export function getUser(id) {
  return delay().then(() => {
    const user = MOCK_USERS.find((u) => u.id === id)
    if (!user) throw new Error('用户不存在')
    return user
  })
}

export function updateUserStatus(id, status) {
  return delay().then(() => {
    const user = MOCK_USERS.find((u) => u.id === id)
    if (!user) throw new Error('用户不存在')
    user.status = status
    return user
  })
}

export function resetUserPassword(id) {
  return delay().then(() => {
    const user = MOCK_USERS.find((u) => u.id === id)
    if (!user) throw new Error('用户不存在')
    return { id, newPassword: '123456' }
  })
}
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/api/user.js
git commit -m "feat(admin): 添加用户管理 mock 接口与 50 条测试数据"
```

---

### Task 2: 列表状态 composable src/composables/useUserManagement.js

**Files:**
- Create: `project/admin/web/src/composables/useUserManagement.js`

**Interfaces:**
- Produces: `useUserManagement()` returns reactive state and handlers.
- Consumes: `listUsers`, `getUser`, `updateUserStatus`, `resetUserPassword` from `src/api/user.js`.

- [ ] **Step 1: 创建 useUserManagement.js**

```javascript
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listUsers, updateUserStatus, resetUserPassword } from '@/api/user.js'

export function useUserManagement() {
  const users = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(10)
  const keyword = ref('')

  const fetchUsers = async () => {
    loading.value = true
    try {
      const res = await listUsers({
        keyword: keyword.value,
        page: page.value,
        pageSize: pageSize.value
      })
      users.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载用户列表失败')
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    page.value = 1
    fetchUsers()
  }

  const handleReset = () => {
    keyword.value = ''
    page.value = 1
    fetchUsers()
  }

  const handlePageChange = (newPage, newPageSize) => {
    page.value = newPage
    pageSize.value = newPageSize
    fetchUsers()
  }

  const handleStatusChange = async (user) => {
    const nextStatus = user.status === 'enabled' ? 'disabled' : 'enabled'
    try {
      await updateUserStatus(user.id, nextStatus)
      message.success(`用户已${nextStatus === 'enabled' ? '启用' : '禁用'}`)
      fetchUsers()
    } catch (error) {
      message.error(error.message || '更新状态失败')
    }
  }

  const handleResetPassword = async (user) => {
    try {
      const res = await resetUserPassword(user.id)
      message.success(`密码已重置为 ${res.newPassword}`)
    } catch (error) {
      message.error(error.message || '重置密码失败')
    }
  }

  return {
    users,
    total,
    loading,
    page,
    pageSize,
    keyword,
    fetchUsers,
    handleSearch,
    handleReset,
    handlePageChange,
    handleStatusChange,
    handleResetPassword
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/composables/useUserManagement.js
git commit -m "feat(admin): 添加用户管理列表 composable"
```

---

### Task 3: 控制台布局 src/layouts/AdminLayout.vue

**Files:**
- Create: `project/admin/web/src/layouts/AdminLayout.vue`

**Interfaces:**
- Consumes: `useUserStore` from `@/stores/user.js`
- Consumes: `useRouter` for navigation
- Renders: sidebar + header + router-view

- [ ] **Step 1: 创建 AdminLayout.vue**

```vue
<template>
  <a-layout class="admin-layout">
    <!-- 侧边栏 -->
    <a-layout-sider width="200" class="admin-sider">
      <div class="sider-brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="sider-logo"
        />
        <div class="sider-brand-text">
          <div class="sider-brand-name">爱创作</div>
          <div class="sider-brand-tag">管理控制台</div>
        </div>
      </div>
      <a-menu
        mode="inline"
        :selected-keys="[$route.path]"
        class="admin-menu"
        @click="handleMenuClick"
      >
        <a-menu-item key="/console/users">
          <template #icon>
            <UserOutlined />
          </template>
          用户管理
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <!-- 顶部 header -->
      <a-layout-header class="admin-header">
        <a-breadcrumb class="admin-breadcrumb">
          <a-breadcrumb-item>首页</a-breadcrumb-item>
          <a-breadcrumb-item>{{ currentMenuName }}</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="admin-user">
          <a-avatar class="admin-avatar">{{ userInitial }}</a-avatar>
          <span class="admin-user-name">{{ userName }}</span>
          <a-button type="link" size="small" @click="handleLogout">退出登录</a-button>
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userName = computed(() => userStore.userInfo?.name || '管理员')
const userInitial = computed(() => userName.value.charAt(0))
const currentMenuName = computed(() => {
  if (route.path === '/console/users') return '用户管理'
  return ''
})

const handleMenuClick = ({ key }) => {
  router.push(key)
}

const handleLogout = () => {
  userStore.clearToken()
  message.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-sider {
  background: #ffffff;
  border-right: 1px solid #eeeeee;
}

.sider-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #eeeeee;
}

.sider-logo {
  height: 32px;
  width: auto;
}

.sider-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.sider-brand-name {
  font-weight: 700;
  font-size: 16px;
  color: #1a1a1a;
}

.sider-brand-tag {
  font-size: 11px;
  color: #8c8c8c;
}

.admin-menu {
  border-inline-end: none;
  padding: 8px 0;
}

.admin-menu :deep(.ant-menu-item) {
  margin: 4px 8px;
  border-radius: 8px;
  height: 40px;
  line-height: 40px;
}

.admin-menu :deep(.ant-menu-item-selected) {
  background: #fff0f2;
  color: #ff2442;
}

.admin-header {
  background: #ffffff;
  border-bottom: 1px solid #eeeeee;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  line-height: 56px;
}

.admin-breadcrumb {
  font-size: 14px;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-avatar {
  background: #ff2442;
}

.admin-user-name {
  font-size: 14px;
  color: #262626;
}

.admin-content {
  background: #f8f9fa;
  padding: 24px;
  min-height: calc(100vh - 56px);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/layouts/AdminLayout.vue
git commit -m "feat(admin): 添加控制台布局（侧边栏 + 顶部 header + 内容区）"
```

---

### Task 4: 用户列表页面 src/views/UserListView.vue

**Files:**
- Create: `project/admin/web/src/views/UserListView.vue`

**Interfaces:**
- Consumes: `useUserManagement` from `@/composables/useUserManagement.js`
- Consumes: `getUser` from `@/api/user.js`

- [ ] **Step 1: 创建 UserListView.vue**

```vue
<template>
  <div class="user-list">
    <a-card :bordered="false" class="user-list-card">
      <div class="user-list-header">
        <h3 class="user-list-title">注册用户管理</h3>
        <p class="user-list-desc">查看与管理平台注册用户</p>
      </div>

      <!-- 工具栏 -->
      <div class="user-list-toolbar">
        <a-input
          v-model:value="keyword"
          placeholder="账号或邮箱"
          allow-clear
          style="width: 280px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button @click="fetchUsers">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'enabled' ? 'green' : 'red'">
              {{ record.status === 'enabled' ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'lastLoginAt'">
            {{ record.lastLoginAt || '—' }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-popconfirm
                :title="record.status === 'enabled' ? '确定禁用该用户？' : '确定启用该用户？'"
                ok-text="确认"
                cancel-text="取消"
                @confirm="handleStatusChange(record)"
              >
                <a-button type="link" size="small">
                  {{ record.status === 'enabled' ? '禁用' : '启用' }}
                </a-button>
              </a-popconfirm>
              <a-button type="link" size="small" @click="openResetPasswordModal(record)">
                重置密码
              </a-button>
              <a-button type="link" size="small" @click="openDetailDrawer(record)">
                查看详情
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- 分页 -->
      <div class="user-list-pagination">
        <a-pagination
          :current="page"
          :page-size="pageSize"
          :total="total"
          :page-size-options="['10', '20', '50']"
          show-size-changer
          show-total
          @change="handlePageChange"
          @show-size-change="handlePageChange"
        />
      </div>
    </a-card>

    <!-- 重置密码弹框 -->
    <a-modal
      v-model:open="resetPasswordVisible"
      title="重置用户密码"
      ok-text="确认重置"
      cancel-text="取消"
      @ok="confirmResetPassword"
    >
      <p>账号：<strong>{{ resetPasswordTarget?.account }}</strong></p>
      <p style="color: #8c8c8c; margin-top: 12px">
        重置后密码将设为 <code>123456</code>，请通知用户及时修改。
      </p>
    </a-modal>

    <!-- 查看详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      title="用户详情"
      :width="480"
      placement="right"
    >
      <a-descriptions v-if="detailUser" :column="1" bordered>
        <a-descriptions-item label="ID">{{ detailUser.id }}</a-descriptions-item>
        <a-descriptions-item label="账号">{{ detailUser.account }}</a-descriptions-item>
        <a-descriptions-item label="邮箱">{{ detailUser.email }}</a-descriptions-item>
        <a-descriptions-item label="昵称">{{ detailUser.nickname }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="detailUser.status === 'enabled' ? 'green' : 'red'">
            {{ detailUser.status === 'enabled' ? '启用' : '禁用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="邀请码">{{ detailUser.inviteCode }}</a-descriptions-item>
        <a-descriptions-item label="注册时间">{{ detailUser.createdAt }}</a-descriptions-item>
        <a-descriptions-item label="最后登录">{{ detailUser.lastLoginAt || '—' }}</a-descriptions-item>
      </a-descriptions>
      <template #footer>
        <a-button @click="detailVisible = false">关闭</a-button>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { useUserManagement } from '@/composables/useUserManagement.js'
import { getUser } from '@/api/user.js'

const {
  users,
  total,
  loading,
  page,
  pageSize,
  keyword,
  fetchUsers,
  handleSearch,
  handleReset,
  handlePageChange,
  handleStatusChange,
  handleResetPassword
} = useUserManagement()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '账号', dataIndex: 'account', key: 'account', width: 140 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '注册时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '最后登录', key: 'lastLoginAt', width: 170 },
  { title: '操作', key: 'actions', width: 280 }
]

const resetPasswordVisible = ref(false)
const resetPasswordTarget = ref(null)
const detailVisible = ref(false)
const detailUser = ref(null)

const openResetPasswordModal = (user) => {
  resetPasswordTarget.value = user
  resetPasswordVisible.value = true
}

const confirmResetPassword = async () => {
  if (!resetPasswordTarget.value) return
  await handleResetPassword(resetPasswordTarget.value)
  resetPasswordVisible.value = false
}

const openDetailDrawer = async (user) => {
  try {
    detailUser.value = await getUser(user.id)
    detailVisible.value = true
  } catch (error) {
    // 失败时使用列表中的数据兜底
    detailUser.value = user
    detailVisible.value = true
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-list-card {
  border-radius: 8px;
}

.user-list-header {
  margin-bottom: 16px;
}

.user-list-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.user-list-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.user-list-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.user-list-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/views/UserListView.vue
git commit -m "feat(admin): 实现注册用户管理列表页面"
```

---

### Task 5: 更新路由 src/router/index.js

**Files:**
- Modify: `project/admin/web/src/router/index.js`

**Interfaces:**
- Produces: `/console` route renders AdminLayout with children.
- `/console` (empty) redirects to `/console/users`.
- `/console/users` renders UserListView.

- [ ] **Step 1: 替换 router/index.js**

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user.js'

const routes = [
  {
    path: '/login',
    name: 'AdminLogin',
    component: () => import('@/views/LoginView.vue')
  },
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
      }
    ]
  },
  {
    path: '/',
    redirect: '/console'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && userStore.isLoggedIn) {
    return '/console/users'
  }
})

export default router
```

- [ ] **Step 2: 验证 dev server**

启动 dev server 后访问 `http://localhost:22346/` 应重定向到 `/login`（未登录）或 `/console/users`（已登录）。

```bash
cd project/admin/web && npm run dev
```

Expected: 服务器在 22346 启动，无报错。

- [ ] **Step 3: Commit**

```bash
git add project/admin/web/src/router/index.js
git commit -m "refactor(admin): 控制台路由改为带 AdminLayout 的嵌套结构"
```

---

### Task 6: Playwright 验证脚本

**Files:**
- Create: `tests/e2e/verify_admin_user_management.py`

**Interfaces:**
- Produces: Playwright script that verifies layout, table, search, detail drawer, status change.

- [ ] **Step 1: 创建 verify_admin_user_management.py**

```python
from playwright.sync_api import sync_playwright
import time

BASE_URL = 'http://localhost:22346'


def login_as_admin(page):
    """在登录页注入 token 后跳转控制台"""
    page.goto(f'{BASE_URL}/login')
    page.wait_for_selector('.login-card', timeout=10000)
    page.evaluate("""
      window.localStorage.setItem('admin_access_token', JSON.stringify('mock-token'))
    """)


def test_admin_user_management():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False, slow_mo=100)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        # 通过登录页写入 token
        login_as_admin(page)
        page.goto(f'{BASE_URL}/console/users')
        page.wait_for_selector('.ant-table-row', timeout=10000)

        # 验证侧边栏和菜单
        assert '用户管理' in page.inner_text('.sider-brand-text')
        assert '用户管理' in page.inner_text('.admin-menu')

        # 验证表格加载
        rows = page.query_selector_all('.ant-table-row')
        assert len(rows) >= 5, f'表格至少应有 5 行，实际 {len(rows)}'

        # 验证搜索过滤
        page.fill('input[placeholder="账号或邮箱"]', 'aichuang_001')
        page.click('button:has-text("查询")')
        time.sleep(1)
        rows_after_search = page.query_selector_all('.ant-table-row')
        assert len(rows_after_search) >= 1, '搜索后应至少有 1 条结果'
        first_account = page.inner_text('.ant-table-row:first-child td:nth-child(2)')
        assert 'aichuang_001' in first_account.lower() or 'user1' in first_account.lower()

        # 重置搜索
        page.click('button:has-text("重置")')
        time.sleep(1)

        # 验证查看详情抽屉
        page.click('.ant-table-row:first-child button:has-text("查看详情")')
        page.wait_for_selector('.ant-drawer-content', timeout=5000)
        assert '用户详情' in page.inner_text('.ant-drawer-title')
        assert '账号' in page.inner_text('.ant-drawer-content')
        page.click('.ant-drawer-footer button:has-text("关闭")')
        time.sleep(0.5)

        # 验证重置密码弹框
        page.click('.ant-table-row:first-child button:has-text("重置密码")')
        page.wait_for_selector('.ant-modal-content', timeout=5000)
        assert '重置用户密码' in page.inner_text('.ant-modal-title')
        page.click('.ant-modal-content button:has-text("取消")')
        time.sleep(0.5)

        # 验证启用/禁用 popconfirm
        page.click('.ant-table-row:first-child button:has-text("禁用")')
        page.wait_for_selector('.ant-popover-content', timeout=5000)
        page.click('.ant-popover-content button:has-text("取消")')
        time.sleep(0.5)

        page.screenshot(path='tests/e2e/screenshots/admin_user_list.png')
        browser.close()


if __name__ == '__main__':
    test_admin_user_management()
```

- [ ] **Step 2: 运行验证脚本**

确保 dev server 已启动。

Run:

```bash
python3 tests/e2e/verify_admin_user_management.py
```

Expected: 脚本自动打开浏览器，验证布局、表格、搜索、详情抽屉、重置密码弹框、popconfirm，截图保存到 `tests/e2e/screenshots/admin_user_list.png`。

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/verify_admin_user_management.py
git commit -m "test(admin): 添加用户管理页面 Playwright 验证脚本"
```

---

## Self-Review

### Spec Coverage

| 规格章节 | 对应任务 |
|---|---|
| 控制台布局 | Task 3 |
| 用户列表页结构 | Task 4 |
| 操作与交互 | Task 4 |
| 数据层 | Task 1, 2 |
| 文件结构与路由 | Task 3, 5 |
| 验收标准 | Task 6 |

### Placeholder Scan

- 无 TBD/TODO。
- 所有代码步骤包含完整代码。
- 所有命令包含预期输出。

### Type Consistency

- `useUserManagement()` 返回值：users, total, loading, page, pageSize, keyword, fetchUsers, handleSearch, handleReset, handlePageChange, handleStatusChange, handleResetPassword。
- UserListView 中解构这些字段，名称一致。
- API 函数签名：listUsers(params), getUser(id), updateUserStatus(id, status), resetUserPassword(id)。
- mock 用户对象字段：id, account, email, nickname, status, inviteCode, createdAt, lastLoginAt。
- 表格列字段引用一致。

---

## 执行方式选择

Plan complete and saved to `docs/superpowers/plans/2026-07-03-admin-user-management.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?