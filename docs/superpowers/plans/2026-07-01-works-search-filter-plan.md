# 作品页搜索 + 筛选实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `WorksIndex.vue` 中为「已生成」和「草稿箱」两个 tab 增加标题关键词搜索、平台多选、风格多选、时间范围筛选功能，并让筛选条件在 tab 间共享。

**Architecture:** 纯前端实现。从 `localStorage` 读取生成队列和草稿数据，在渲染前统一为标准项结构，再用一组共享的筛选条件进行过滤。UI 使用 Ant Design Vue 的 `a-input-search`、`a-select` 和 `a-radio-group`。

**Tech Stack:** Vue 3 + Ant Design Vue + Vite + Playwright（测试）

## Global Constraints

- 不得引入新的依赖；使用项目中已有的 Ant Design Vue 组件。
- 平台/风格选项必须与 `CreateIndex.vue` 中的定义保持一致，禁止硬编码。
- 「已生成」tab 必须接入 `aichuangzuo_generation_queue` 的真实数据，不再使用空模拟数组。
- 所有筛选条件在 tab 切换时保留。
- 筛选为空时需提供「清空筛选」入口。
- 每次任务完成后提交一次 commit。

---

## File Structure

| File | Responsibility |
|------|---------------|
| `project/user/web/src/views/console/WorksIndex.vue` | 作品页主文件：UI、数据加载、标准化、过滤、空状态 |
| `project/user/web/src/views/console/CreateIndex.vue` | 平台列表与风格列表的源定义（只读参考，不修改） |
| `project/user/web/src/views/console/QueueIndex.vue` | 已生成数据加载逻辑的参考（只读参考，不修改） |
| `tests/e2e/verify_works_search_filter.py` | 新增 Playwright 端到端测试脚本 |

---

## Task 1: 接入「已生成」tab 的真实数据并统一数据模型

**Files:**
- Modify: `project/user/web/src/views/console/WorksIndex.vue`

**Interfaces:**
- Consumes: `localStorage.getItem('aichuangzuo_generation_queue')`（已生成）和 `localStorage.getItem('aichuangzuo_drafts')`（草稿）
- Produces: `worksList` ref、`draftsList` ref、统一后的 `normalizedWorks` / `normalizedDrafts` computed

- [ ] **Step 1: 在 `WorksIndex.vue` 的 `<script setup>` 中新增数据加载逻辑**

读取生成队列并过滤出 `status === 'completed'` 的项，字段映射与 `QueueIndex.vue` 的 `loadWorks` 保持一致：

```js
const WORKS_KEY = 'aichuangzuo_generation_queue'
const DRAFTS_KEY = 'aichuangzuo_drafts'

const worksList = ref([])

const loadWorks = () => {
  const saved = localStorage.getItem(WORKS_KEY)
  if (!saved) {
    worksList.value = []
    return
  }
  try {
    const queue = JSON.parse(saved)
    worksList.value = queue
      .filter(item => item.status === 'completed')
      .map(item => ({
        id: item.id,
        title: item.title,
        platform: item.platform,
        wordCount: item.wordCount,
        style: item.style,
        template: item.template || '未选择',
        completedAt: item.completedAt,
        content: item.content
      }))
      .sort((a, b) => new Date(b.completedAt || 0) - new Date(a.completedAt || 0))
  } catch {
    worksList.value = []
  }
}
```

- [ ] **Step 2: 修改草稿读取逻辑，使用统一的 `DRAFTS_KEY`**

将现有的 `draftsList` computed 改为：

```js
const draftsList = computed(() => {
  const raw = localStorage.getItem(DRAFTS_KEY) || '[]'
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
})
```

- [ ] **Step 3: 在 `onMounted` 中调用 `loadWorks()`**

```js
import { onMounted } from 'vue'

onMounted(() => {
  loadWorks()
})
```

- [ ] **Step 4: 启动 dev server 并手动验证「已生成」tab 能显示数据**

Run: `cd project/user/web && npm run dev -- --port 28586`

手动步骤：
1. 在浏览器打开 `http://localhost:28586/console/create`
2. 输入标题和需求，点击「生成文章」
3. 等待生成完成
4. 打开 `http://localhost:28586/console/works`
5. 切换到「已生成」tab，确认有作品出现

Expected: 已生成 tab 显示刚生成的作品。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/WorksIndex.vue
git commit -m "feat(works): 已生成 tab 接入真实生成队列数据"
```

---

## Task 2: 添加搜索和筛选 UI

**Files:**
- Modify: `project/user/web/src/views/console/WorksIndex.vue`

**Interfaces:**
- Consumes: Ant Design Vue 的 `a-input-search`、`a-select`、`a-radio-group`
- Produces: `searchKeyword`、`selectedPlatforms`、`selectedStyles`、`timeRange` 四个响应式筛选状态

- [ ] **Step 1: 引入平台与风格选项**

在 `<script setup>` 顶部定义（与 `CreateIndex.vue` 保持一致）：

```js
const platformOptions = [
  { key: 'wechat', label: '微信公众号' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'toutiao', label: '今日头条' },
  { key: 'baijiahao', label: '百家号' },
  { key: 'douyin', label: '抖音图文' },
  { key: 'zhihu', label: '知乎' }
]

const styleOptions = [
  { key: '产品评测', label: '产品评测' },
  { key: '情感散文', label: '情感散文' },
  { key: '职场干货', label: '职场干货' },
  { key: '营销文案', label: '营销文案' },
  { key: '年度总结', label: '年度总结' },
  { key: '知识科普', label: '知识科普' },
  { key: '热点评论', label: '热点评论' },
  { key: '故事叙事', label: '故事叙事' }
]

const timeRangeOptions = [
  { key: 'all', label: '全部' },
  { key: '7', label: '近7天' },
  { key: '30', label: '近30天' },
  { key: '90', label: '近90天' }
]
```

- [ ] **Step 2: 新增筛选状态**

```js
const searchKeyword = ref('')
const selectedPlatforms = ref([])
const selectedStyles = ref([])
const timeRange = ref('all')
```

- [ ] **Step 3: 改造页面头部结构**

将 `<div class="works-header">` 替换为：

```vue
<div class="works-header">
  <h2 class="works-title">我的作品</h2>

  <div class="works-filter-bar">
    <a-input-search
      v-model:value="searchKeyword"
      placeholder="搜索标题关键词"
      class="works-search"
      allow-clear
    />
    <a-select
      v-model:value="selectedPlatforms"
      mode="multiple"
      class="works-filter-select"
      placeholder="平台"
      :max-tag-count="1"
      :options="platformOptions.map(p => ({ value: p.key, label: p.label }))"
      allow-clear
    />
    <a-select
      v-model:value="selectedStyles"
      mode="multiple"
      class="works-filter-select"
      placeholder="风格"
      :max-tag-count="1"
      :options="styleOptions.map(s => ({ value: s.key, label: s.label }))"
      allow-clear
    />
    <a-radio-group v-model:value="timeRange" class="works-filter-time">
      <a-radio-button v-for="opt in timeRangeOptions" :key="opt.key" :value="opt.key">
        {{ opt.label }}
      </a-radio-button>
    </a-radio-group>
  </div>

  <div class="works-tabs">
    <button :class="['works-tab', { active: activeTab === 'works' }]" @click="activeTab = 'works'">已生成</button>
    <button :class="['works-tab', { active: activeTab === 'drafts' }]" @click="activeTab = 'drafts'">草稿箱</button>
  </div>
</div>
```

- [ ] **Step 4: 添加筛选区域样式**

在 `<style scoped>` 底部追加：

```css
.works-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  margin: 0 24px;
}

.works-search {
  width: 220px;
}

.works-filter-select {
  min-width: 120px;
}

.works-filter-time {
  display: flex;
  flex-shrink: 0;
}
```

- [ ] **Step 5: 启动 dev server 验证 UI 渲染正常**

Run: `cd project/user/web && npm run dev -- --port 28586`

手动步骤：
1. 打开 `http://localhost:28586/console/works`
2. 确认标题行出现搜索框、平台、风格、时间筛选和 tabs
3. 检查下拉和单选按钮能正常展开

Expected: 筛选 UI 正常显示，无报错。

- [ ] **Step 6: Commit**

```bash
git add project/user/web/src/views/console/WorksIndex.vue
git commit -m "feat(works): 添加搜索和筛选 UI"
```

---

## Task 3: 实现数据标准化与筛选逻辑

**Files:**
- Modify: `project/user/web/src/views/console/WorksIndex.vue`

**Interfaces:**
- Consumes: `worksList`、`draftsList`、四个筛选状态
- Produces: `filteredWorks`、`filteredDrafts` computed；统一的 `normalizeItems` 辅助函数

- [ ] **Step 1: 添加数据标准化辅助函数**

在 `<script setup>` 中添加：

```js
const normalizeItems = (items, type) => {
  return items.map(item => {
    if (type === 'draft') {
      return {
        id: item.id,
        title: item.customTitle || '未命名草稿',
        platformName: item.platform?.name || '未选择平台',
        styleName: item.style?.name || '未选择',
        date: item.savedAt ? new Date(item.savedAt) : null,
        raw: item
      }
    }
    return {
      id: item.id,
      title: item.title,
      platformName: item.platform || '未选择',
      styleName: item.style || '未选择',
      date: item.completedAt ? new Date(item.completedAt) : null,
      raw: item
    }
  })
}
```

- [ ] **Step 2: 添加过滤辅助函数**

```js
const isWithinDays = (date, days) => {
  if (!date) return false
  const now = new Date()
  const diff = (now - date) / (1000 * 60 * 60 * 24)
  return diff <= days
}

const matchesFilters = (item) => {
  // 标题关键词
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    if (!item.title.toLowerCase().includes(kw)) {
      return false
    }
  }

  // 平台多选
  if (selectedPlatforms.value.length > 0) {
    const platformMap = {
      wechat: '微信公众号',
      xiaohongshu: '小红书',
      toutiao: '今日头条',
      baijiahao: '百家号',
      douyin: '抖音图文',
      zhihu: '知乎'
    }
    const selectedLabels = selectedPlatforms.value.map(k => platformMap[k])
    if (!selectedLabels.includes(item.platformName)) {
      return false
    }
  }

  // 风格多选
  if (selectedStyles.value.length > 0) {
    if (!selectedStyles.value.includes(item.styleName)) {
      return false
    }
  }

  // 时间范围
  if (timeRange.value !== 'all') {
    const days = parseInt(timeRange.value, 10)
    if (!isWithinDays(item.date, days)) {
      return false
    }
  }

  return true
}
```

- [ ] **Step 3: 用过滤后的列表替换模板中的原始列表**

在 `<script setup>` 中添加：

```js
const filteredWorks = computed(() => {
  return normalizeItems(worksList.value, 'work').filter(matchesFilters)
})

const filteredDrafts = computed(() => {
  return normalizeItems(draftsList.value, 'draft').filter(matchesFilters)
})
```

- [ ] **Step 4: 修改模板使用 `filteredWorks` 和 `filteredDrafts`**

将 `v-for="work in worksList"` 改为 `v-for="work in filteredWorks"`，并将 `v-for="draft in draftsList"` 改为 `v-for="draft in filteredDrafts"`。

同时更新空状态判断：
- `v-if="worksList.length === 0"` 改为 `v-if="worksList.length === 0"`（保持原逻辑，表示无数据）
- 在 `v-else` 内新增 `v-if="filteredWorks.length === 0"` 的筛选空状态（见 Task 4）

- [ ] **Step 5: 修复模板中依赖原始字段的地方**

由于 `filteredWorks` / `filteredDrafts` 的项已经是标准化结构，模板中引用字段需要调整：

对于「已生成」卡片：
```vue
<div class="work-title">{{ work.title }}</div>
<div class="work-meta">
  <span>{{ work.platformName }}</span>
  <span>·</span>
  <span>{{ work.raw.wordCount }} 字</span>
  <span>·</span>
  <span>{{ formatDate(work.raw.completedAt) }}</span>
</div>
```

对于「草稿箱」卡片：
```vue
<div class="work-title">{{ draft.title }}</div>
<div class="work-meta">
  <span>{{ draft.platformName }}</span>
  <span>·</span>
  <span>{{ draft.raw.wordCount?.count || 0 }} 字</span>
  <span>·</span>
  <span>保存于 {{ formatDate(draft.raw.savedAt) }}</span>
</div>
```

- [ ] **Step 6: 启动 dev server 验证筛选生效**

Run: `cd project/user/web && npm run dev -- --port 28586`

手动步骤：
1. 准备多条生成记录和草稿（不同平台、风格、时间）
2. 访问 `/console/works`
3. 输入标题关键词，确认列表实时过滤
4. 选择平台/风格，确认显示对应结果
5. 切换时间范围，确认日期过滤生效
6. 切换 tab，确认筛选条件保留

Expected: 搜索和筛选均生效，tab 切换后条件不丢失。

- [ ] **Step 7: Commit**

```bash
git add project/user/web/src/views/console/WorksIndex.vue
git commit -m "feat(works): 实现数据标准化与筛选逻辑"
```

---

## Task 4: 添加筛选后空状态与清空筛选按钮

**Files:**
- Modify: `project/user/web/src/views/console/WorksIndex.vue`

**Interfaces:**
- Consumes: 四个筛选状态 ref
- Produces: `clearFilters` 函数

- [ ] **Step 1: 添加清空筛选函数**

```js
const clearFilters = () => {
  searchKeyword.value = ''
  selectedPlatforms.value = []
  selectedStyles.value = []
  timeRange.value = 'all'
}
```

- [ ] **Step 2: 在「已生成」区域添加筛选后空状态**

在「已生成」列表 `v-else` 内部，卡片循环前插入：

```vue
<div v-if="filteredWorks.length === 0" class="works-empty">
  <div class="empty-icon">🔍</div>
  <div class="empty-text">未找到匹配的作品</div>
  <button class="empty-btn" @click="clearFilters">清空筛选</button>
</div>
```

- [ ] **Step 3: 在「草稿箱」区域添加筛选后空状态**

在「草稿箱」列表 `v-else` 内部，卡片循环前插入：

```vue
<div v-if="filteredDrafts.length === 0" class="works-empty">
  <div class="empty-icon">🔍</div>
  <div class="empty-text">未找到匹配的草稿</div>
  <button class="empty-btn" @click="clearFilters">清空筛选</button>
</div>
```

- [ ] **Step 4: 启动 dev server 验证清空筛选功能**

手动步骤：
1. 访问 `/console/works`
2. 输入一个不存在的关键词，使列表为空
3. 点击「清空筛选」按钮
4. 确认搜索框清空、筛选重置、列表恢复

Expected: 清空筛选后显示全部结果。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/WorksIndex.vue
git commit -m "feat(works): 添加筛选空状态与清空筛选"
```

---

## Task 5: 编写并运行 Playwright 端到端测试

**Files:**
- Create: `tests/e2e/verify_works_search_filter.py`

**Interfaces:**
- Consumes: 运行中的 dev server on `http://localhost:28586`
- Produces: 测试截图 `tests/e2e/screenshots/works_search_filter.png`

- [ ] **Step 1: 创建测试脚本**

```python
from playwright.sync_api import sync_playwright

URL = 'http://localhost:28586'

def seed_data(page):
    page.evaluate('''
        () => {
            const queue = [
                {
                    id: 1,
                    title: '职场新人快速提升效率',
                    platform: '微信公众号',
                    wordCount: 1200,
                    style: '职场干货',
                    template: '默认模板',
                    status: 'completed',
                    completedAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
                    content: { title: '职场新人快速提升效率', body: '正文内容' }
                },
                {
                    id: 2,
                    title: '小红书爆款文案写作技巧',
                    platform: '小红书',
                    wordCount: 800,
                    style: '营销文案',
                    template: '小红书卡片',
                    status: 'completed',
                    completedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 10).toISOString(),
                    content: { title: '小红书爆款文案写作技巧', body: '正文内容' }
                }
            ];
            const drafts = [
                {
                    id: 3,
                    customTitle: '知乎深度回答：如何高效学习',
                    platform: { key: 'zhihu', name: '知乎' },
                    wordCount: { count: 1500 },
                    style: { name: '知识科普' },
                    savedAt: new Date().toISOString()
                }
            ];
            localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify(queue));
            localStorage.setItem('aichuangzuo_drafts', JSON.stringify(drafts));
        }
    ''')

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        page.goto(f'{URL}/console/works')
        seed_data(page)
        page.reload()

        # 验证已生成 tab 有两条数据
        page.wait_for_selector('.work-card', timeout=10000)
        cards = page.locator('.work-card')
        assert cards.count() == 2, 'Expected 2 works'

        # 标题搜索
        page.locator('.works-search input').fill('职场')
        page.wait_for_timeout(300)
        assert cards.count() == 1, 'Expected 1 work after title search'
        assert '职场新人快速提升效率' in cards.first.inner_text()

        page.locator('.works-search input').fill('')
        page.wait_for_timeout(300)

        # 平台筛选：选择小红书
        page.locator('.works-filter-select').nth(0).click()
        page.locator('.ant-select-item[title="小红书"]').click()
        page.wait_for_timeout(300)
        assert cards.count() == 1, 'Expected 1 work after platform filter'
        assert '小红书爆款文案写作技巧' in cards.first.inner_text()

        # 清空筛选
        page.locator('button:has-text("清空筛选")').click()
        page.wait_for_timeout(300)
        assert cards.count() == 2, 'Expected 2 works after clearing filters'

        # 切换到草稿箱
        page.locator('button:has-text("草稿箱")').click()
        page.wait_for_timeout(300)
        draft_cards = page.locator('.draft-card')
        assert draft_cards.count() == 1, 'Expected 1 draft'

        # 草稿箱标题搜索
        page.locator('.works-search input').fill('知乎')
        page.wait_for_timeout(300)
        assert draft_cards.count() == 1, 'Expected 1 draft after search'
        assert '知乎深度回答' in draft_cards.first.inner_text()

        page.screenshot(path='tests/e2e/screenshots/works_search_filter.png')
        print('作品页搜索筛选验证通过')

        browser.close()

if __name__ == '__main__':
    main()
```

- [ ] **Step 2: 启动 dev server**

Run: `cd project/user/web && npm run dev -- --port 28586`

- [ ] **Step 3: 运行测试脚本**

Run: `python3 tests/e2e/verify_works_search_filter.py`

Expected output:
```
作品页搜索筛选验证通过
```

- [ ] **Step 4: Commit**

```bash
git add tests/e2e/verify_works_search_filter.py tests/e2e/screenshots/works_search_filter.png
git commit -m "test(works): 添加作品页搜索筛选端到端测试"
```

---

## Self-Review

**1. Spec coverage:**
- 搜索框放在标题右侧同一行 → Task 2
- 标题关键词实时过滤 → Task 3
- 平台多选、风格多选 → Task 2 + Task 3
- 时间范围（近7/30/90天/全部）→ Task 2 + Task 3
- 两个 tab 都支持且条件共享 → Task 1 + Task 3
- 已生成 tab 接入真实数据 → Task 1
- 筛选为空时清空筛选 → Task 4
- Playwright 测试 → Task 5

**2. Placeholder scan:** 无 TBD/TODO/"实现 later"/"类似的"等占位符。

**3. Type consistency:**
- 已生成项使用 `platform` 字符串和 `style` 字符串
- 草稿项使用 `platform.name` 和 `style.name`
- 标准化后统一使用 `platformName` 和 `styleName`
- 时间字段统一为 `Date` 对象

所有需求均已覆盖，计划可直接执行。
