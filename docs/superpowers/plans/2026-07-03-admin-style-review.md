# 管理控制台风格审核实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `project/admin/web/` 管理控制台实现「风格审核」功能，包含侧边栏菜单、风格审核列表、搜索分页、打回操作（填写原因）与状态持久化。

**Architecture:** 采用与「用户管理」一致的 Ant Design Vue 后台布局 + Table + Modal 模式。Mock 数据层放在 `api/style.js`，列表状态用 composable 管理。路由在 `/console` 下新增 `/console/styles` 子路由。

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
├── api/
│   ├── auth.js
│   ├── user.js
│   └── style.js                  # 新增
├── composables/
│   ├── useTheme.js
│   ├── useUserManagement.js
│   └── useStyleReview.js         # 新增
├── layouts/
│   └── AdminLayout.vue           # 修改：新增「风格审核」菜单
├── views/
│   ├── LoginView.vue
│   ├── ConsoleView.vue
│   ├── UserListView.vue
│   └── StyleReviewView.vue       # 新增
└── router/
    └── index.js                  # 修改：新增 /console/styles 子路由
```

---

### Task 1: Mock 风格接口 src/api/style.js

**Files:**
- Create: `project/admin/web/src/api/style.js`

**Interfaces:**
- Produces: `listStyles(params)`, `rejectStyle(id, reason)`.

- [ ] **Step 1: 创建 style.js**

```javascript
import storage from '@/utils/storage.js'

const STYLE_KEY = 'aichuangzuo_admin_style_review'

const SOURCE_TYPES = ['my', 'learned']
const STATUS_LIST = ['pending', 'pending', 'pending', 'pending', 'rejected']

const NAMES = ['娱乐至死', '温柔治愈', '犀利吐槽', '文艺清新', '专业严谨', '轻松俏皮', '热血激昂', '冷静客观', '幽默风趣', '唯美浪漫']
const SCOPES = ['公众号情感文', '小红书种草', '知乎回答', '今日头条', '百家号', '抖音脚本', '通用文案']
const PROMPTS = [
  '轻松幽默、网络热梗、短句为主，适合年轻读者',
  '温柔细腻、情感共鸣、用词优美，适合情感类内容',
  '犀利直接、观点鲜明、节奏快，适合评论类文章',
  '文艺清新、段落优美、引用诗句，适合生活方式',
  '专业严谨、数据支撑、逻辑清晰，适合行业分析',
  '轻松俏皮、表情丰富、互动性强，适合社交媒体',
  '热血激昂、排比有力、情绪饱满，适合励志内容',
  '冷静客观、事实陈述、不偏不倚，适合新闻报道',
  '幽默风趣、包袱不断、反转巧妙，适合娱乐内容',
  '唯美浪漫、画面感强、修辞丰富，适合故事散文'
]

function randomDate(daysAgo) {
  const date = new Date(Date.now() - Math.floor(Math.random() * daysAgo * 86400000))
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function loadStyles() {
  try {
    const raw = storage.get(STYLE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function saveStyles(styles) {
  storage.set(STYLE_KEY, JSON.stringify(styles))
}

function generateMockStyles() {
  return Array.from({ length: 30 }, (_, i) => ({
    id: `market-${String(i + 1).padStart(4, '0')}`,
    name: NAMES[i % NAMES.length],
    sourceType: SOURCE_TYPES[i % SOURCE_TYPES.length],
    creatorName: `用户${String(i + 1).padStart(3, '0')}`,
    prompt: PROMPTS[i % PROMPTS.length],
    scope: SCOPES[i % SCOPES.length],
    status: STATUS_LIST[i % STATUS_LIST.length],
    rejectReason: i % 5 === 4 ? '示例：风格描述过于宽泛，请补充具体写作要求' : '',
    createdAt: randomDate(30)
  }))
}

const MOCK_STYLES = loadStyles() || generateMockStyles()
if (!loadStyles()) {
  saveStyles(MOCK_STYLES)
}

function delay(ms = 300 + Math.floor(Math.random() * 300)) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function listStyles(params = {}) {
  const { keyword = '', page = 1, pageSize = 10 } = params
  return delay().then(() => {
    const styles = loadStyles() || MOCK_STYLES
    const filtered = styles.filter((s) => {
      if (!keyword) return true
      const kw = keyword.toLowerCase()
      return s.name.toLowerCase().includes(kw) || s.creatorName.toLowerCase().includes(kw)
    })
    const start = (page - 1) * pageSize
    return {
      list: filtered.slice(start, start + pageSize),
      total: filtered.length
    }
  })
}

export function rejectStyle(id, reason) {
  return delay().then(() => {
    const styles = loadStyles() || MOCK_STYLES
    const style = styles.find((s) => s.id === id)
    if (!style) throw new Error('风格不存在')
    if (style.status === 'rejected') throw new Error('该风格已被打回')
    if (!reason || !reason.trim()) throw new Error('请输入打回原因')
    style.status = 'rejected'
    style.rejectReason = reason.trim()
    saveStyles(styles)
    return style
  })
}
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/api/style.js
git commit -m "feat(admin): 添加风格审核 mock 接口与 30 条测试数据"
```

---

### Task 2: 列表状态 composable src/composables/useStyleReview.js

**Files:**
- Create: `project/admin/web/src/composables/useStyleReview.js`

**Interfaces:**
- Produces: `useStyleReview()` returns reactive state and handlers.
- Consumes: `listStyles`, `rejectStyle` from `src/api/style.js`.

- [ ] **Step 1: 创建 useStyleReview.js**

```javascript
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { listStyles, rejectStyle } from '@/api/style.js'

export function useStyleReview() {
  const styles = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(10)
  const keyword = ref('')

  const fetchStyles = async () => {
    loading.value = true
    try {
      const res = await listStyles({
        keyword: keyword.value,
        page: page.value,
        pageSize: pageSize.value
      })
      styles.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载风格列表失败')
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    page.value = 1
    fetchStyles()
  }

  const handleReset = () => {
    keyword.value = ''
    page.value = 1
    fetchStyles()
  }

  const handlePageChange = (newPage, newPageSize) => {
    page.value = newPage
    pageSize.value = newPageSize
    fetchStyles()
  }

  const handleReject = async (style, reason) => {
    try {
      await rejectStyle(style.id, reason)
      message.success('风格已打回')
      fetchStyles()
      return true
    } catch (error) {
      message.error(error.message || '打回失败')
      return false
    }
  }

  return {
    styles,
    total,
    loading,
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

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/composables/useStyleReview.js
git commit -m "feat(admin): 添加风格审核列表 composable"
```

---

### Task 3: 更新控制台布局 AdminLayout.vue

**Files:**
- Modify: `project/admin/web/src/layouts/AdminLayout.vue`

**Interfaces:**
- Consumes: `AuditOutlined` from `@ant-design/icons-vue`
- Renders: 侧边栏新增「风格审核」菜单项

- [ ] **Step 1: 修改 AdminLayout.vue**

替换 `script setup` 中的 import 与 `currentMenuName`：

```vue
<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserOutlined, AuditOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userName = computed(() => userStore.userInfo?.name || '管理员')
const userInitial = computed(() => userName.value.charAt(0))
const currentMenuName = computed(() => {
  if (route.path === '/console/users') return '用户管理'
  if (route.path === '/console/styles') return '风格审核'
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
```

替换模板中的 `a-menu`：

```vue
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
        <a-menu-item key="/console/styles">
          <template #icon>
            <AuditOutlined />
          </template>
          风格审核
        </a-menu-item>
      </a-menu>
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/layouts/AdminLayout.vue
git commit -m "feat(admin): 控制台侧边栏新增风格审核菜单"
```

---

### Task 4: 风格审核列表页面 src/views/StyleReviewView.vue

**Files:**
- Create: `project/admin/web/src/views/StyleReviewView.vue`

**Interfaces:**
- Consumes: `useStyleReview` from `@/composables/useStyleReview.js`

- [ ] **Step 1: 创建 StyleReviewView.vue**

```vue
<template>
  <div class="style-review">
    <a-card :bordered="false" class="style-review-card">
      <div class="style-review-header">
        <h3 class="style-review-title">风格审核</h3>
        <p class="style-review-desc">审核用户提交到风格市场的风格</p>
      </div>

      <!-- 工具栏 -->
      <div class="style-review-toolbar">
        <a-input
          v-model:value="keyword"
          placeholder="风格名称或创作者"
          allow-clear
          style="width: 280px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button @click="fetchStyles">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="styles"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sourceType'">
            {{ record.sourceType === 'my' ? '我的风格' : '学习的风格' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'pending' ? '#ff4d6f' : 'error'">
              {{ record.status === 'pending' ? '待审核' : '已打回' }}
            </a-tag>
            <a-button
              v-if="record.status === 'rejected'"
              type="link"
              size="small"
              class="reason-link"
              @click="openReasonModal(record)"
            >
              查看原因
            </a-button>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button
              type="link"
              size="small"
              danger
              :disabled="record.status === 'rejected'"
              @click="openRejectModal(record)"
            >
              打回
            </a-button>
          </template>
        </template>
      </a-table>

      <!-- 分页 -->
      <div class="style-review-pagination">
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

    <!-- 打回弹框 -->
    <a-modal
      v-model:open="rejectVisible"
      title="打回风格"
      ok-text="确认打回"
      cancel-text="取消"
      :confirm-loading="rejectSubmitting"
      @ok="confirmReject"
    >
      <p v-if="rejectTarget">风格名称：<strong>{{ rejectTarget.name }}</strong></p>
      <p v-if="rejectTarget" style="margin-top: 8px">创作者：<strong>{{ rejectTarget.creatorName }}</strong></p>
      <div style="margin-top: 16px">
        <label style="display: block; margin-bottom: 6px; font-weight: 500">打回原因 <span style="color: #ff4d4f">*</span></label>
        <a-textarea
          v-model:value="rejectReason"
          placeholder="请输入打回原因，用户将看到此说明"
          :maxlength="200"
          :rows="4"
          show-count
        />
      </div>
    </a-modal>

    <!-- 查看原因弹框 -->
    <a-modal
      v-model:open="reasonVisible"
      title="打回原因"
      :footer="null"
    >
      <p v-if="reasonTarget">风格名称：<strong>{{ reasonTarget.name }}</strong></p>
      <a-divider style="margin: 12px 0" />
      <p>{{ reasonTarget?.rejectReason || '—' }}</p>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { useStyleReview } from '@/composables/useStyleReview.js'

const {
  styles,
  total,
  loading,
  page,
  pageSize,
  keyword,
  fetchStyles,
  handleSearch,
  handleReset,
  handlePageChange,
  handleReject
} = useStyleReview()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 120 },
  { title: '风格名称', dataIndex: 'name', key: 'name', width: 160 },
  { title: '来源类型', dataIndex: 'sourceType', key: 'sourceType', width: 100 },
  { title: '创作者', dataIndex: 'creatorName', key: 'creatorName', width: 120 },
  { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 160 },
  { title: '操作', key: 'actions', width: 120 }
]

const rejectVisible = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const rejectSubmitting = ref(false)

const reasonVisible = ref(false)
const reasonTarget = ref(null)

const openRejectModal = (style) => {
  rejectTarget.value = style
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectTarget.value) return
  if (!rejectReason.value.trim()) {
    return
  }
  rejectSubmitting.value = true
  const ok = await handleReject(rejectTarget.value, rejectReason.value)
  rejectSubmitting.value = false
  if (ok) {
    rejectVisible.value = false
  }
}

const openReasonModal = (style) => {
  reasonTarget.value = style
  reasonVisible.value = true
}

onMounted(() => {
  fetchStyles()
})
</script>

<style scoped>
.style-review-card {
  border-radius: 8px;
}

.style-review-header {
  margin-bottom: 16px;
}

.style-review-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.style-review-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.style-review-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.style-review-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.reason-link {
  padding: 0 0 0 8px;
  font-size: 12px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/views/StyleReviewView.vue
git commit -m "feat(admin): 实现风格审核列表页面与打回弹框"
```

---

### Task 5: 更新路由 src/router/index.js

**Files:**
- Modify: `project/admin/web/src/router/index.js`

**Interfaces:**
- Produces: `/console/styles` route renders StyleReviewView as child of AdminLayout.

- [ ] **Step 1: 修改 router/index.js**

在 `/console` 的 `children` 数组中新增：

```javascript
      {
        path: 'styles',
        name: 'AdminStyleReview',
        component: () => import('@/views/StyleReviewView.vue')
      }
```

当前 `src/router/index.js` 内容应为：

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

启动 dev server 后访问 `http://localhost:22346/console/styles`，应显示风格审核列表。

```bash
cd project/admin/web && npm run dev
```

Expected: 服务器在 22346 启动，无报错。

- [ ] **Step 3: Commit**

```bash
git add project/admin/web/src/router/index.js
git commit -m "feat(admin): 控制台路由新增 /console/styles 风格审核"
```

---

### Task 6: Playwright 验证脚本

**Files:**
- Create: `tests/e2e/verify_admin_style_review.py`

**Interfaces:**
- Produces: Playwright script that verifies layout, table, search, reject modal, status change.

- [ ] **Step 1: 创建 verify_admin_style_review.py**

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


def test_admin_style_review():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False, slow_mo=100)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        login_as_admin(page)
        page.goto(f'{BASE_URL}/console/styles')
        page.wait_for_selector('.ant-table-row', timeout=10000)

        # 验证侧边栏和菜单
        assert '风格审核' in page.inner_text('.admin-menu')

        # 验证表格加载
        rows = page.query_selector_all('.ant-table-row')
        assert len(rows) >= 5, f'表格至少应有 5 行，实际 {len(rows)}'

        # 验证搜索过滤
        page.fill('input[placeholder="风格名称或创作者"]', '用户001')
        page.click('button:has-text("查 询")')
        page.wait_for_load_state('networkidle')
        rows_after_search = page.query_selector_all('.ant-table-row')
        assert len(rows_after_search) >= 1, '搜索后应至少有 1 条结果'

        # 重置搜索
        page.click('button:has-text("重 置")')
        page.wait_for_load_state('networkidle')

        # 验证打回弹框
        first_pending = page.query_selector('.ant-table-row:first-child button:has-text("打 回")')
        assert first_pending, '列表中应至少有一条待审核数据'
        first_pending.click()
        page.wait_for_selector('.ant-modal-content', timeout=5000)
        assert '打回风格' in page.inner_text('.ant-modal-title')

        # 未填原因时确认打回
        page.click('.ant-modal-content button:has-text("确 认 打 回")')
        page.wait_for_timeout(500)
        assert page.is_visible('.ant-modal-content'), '未填原因时应保持弹框'

        # 填写原因并确认
        page.fill('.ant-modal-content textarea', '风格描述过于宽泛，请补充具体写作要求')
        page.click('.ant-modal-content button:has-text("确 认 打 回")')
        page.wait_for_selector('.ant-modal-content', state='hidden', timeout=5000)

        # 验证状态变为已打回
        page.wait_for_timeout(500)
        assert '已打回' in page.inner_text('.ant-table-row:first-child')

        # 验证查看原因
        page.click('.ant-table-row:first-child button:has-text("查看原因")')
        page.wait_for_selector('.ant-modal-content', timeout=5000)
        assert '风格描述过于宽泛' in page.inner_text('.ant-modal-content')
        page.click('.ant-modal-content button:has-text("关 闭")')
        page.wait_for_selector('.ant-modal-content', state='hidden', timeout=5000)

        page.screenshot(path='tests/e2e/screenshots/admin_style_review.png')
        browser.close()


if __name__ == '__main__':
    test_admin_style_review()
```

- [ ] **Step 2: 运行验证脚本**

确保 dev server 已启动。

Run:

```bash
python3 tests/e2e/verify_admin_style_review.py
```

Expected: 脚本自动打开浏览器，验证布局、表格、搜索、打回弹框、状态变更，截图保存到 `tests/e2e/screenshots/admin_style_review.png`。

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/verify_admin_style_review.py
git commit -m "test(admin): 添加风格审核页面 Playwright 验证脚本"
```

---

## Self-Review

### Spec Coverage

| 规格章节 | 对应任务 |
|---|---|
| 侧边栏菜单 | Task 3 |
| 风格审核列表页 | Task 4 |
| 打回操作与原因 | Task 4 |
| 数据层 | Task 1, 2 |
| 文件结构与路由 | Task 3, 5 |
| 验收标准 | Task 6 |

### Placeholder Scan

- 无 TBD/TODO。
- 所有代码步骤包含完整代码。
- 所有命令包含预期输出。

### Type Consistency

- `useStyleReview()` 返回值：styles, total, loading, page, pageSize, keyword, fetchStyles, handleSearch, handleReset, handlePageChange, handleReject。
- StyleReviewView 中解构这些字段，名称一致。
- API 函数签名：listStyles(params), rejectStyle(id, reason)。
- mock 风格对象字段：id, name, sourceType, creatorName, prompt, scope, status, rejectReason, createdAt。
- 表格列字段引用一致。

---

## 执行方式选择

Plan complete and saved to `docs/superpowers/plans/2026-07-03-admin-style-review.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
