# 创作页双模式重设计 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把用户端「开始创作」页（CreateIndex.vue，3857 行）重做为双模式：默认对话引导模式（固定问题流）+ 极简模式（居中一体化输入卡片），队列改右侧抽屉，移动端适配。

**Architecture:** 共享状态收敛到两个 composable 单例（`useCreateForm` 创作配置 + 模式、`useGenerationQueue` 队列 + 轮询），CreateIndex 瘦身为模式容器；引导/极简两个模式组件 + 4 个配置弹框 + 队列抽屉 + 灵感胶囊各自独立成件，全部放在 `src/views/console/create/` 目录。

**Tech Stack:** Vue 3 `<script setup>`、ant-design-vue 4（a-modal / a-drawer / a-tooltip）、Playwright E2E（项目无单元测试设施，按 CLAUDE.md 约定用 Playwright 脚本验证）。

**Spec:** `docs/superpowers/specs/2026-07-16-create-page-redesign-design.md`

## Global Constraints

- 主色 `#FF2442`（`--color-primary`），成功色 `#07c160`，全部颜色走 `src/styles/index.css` 的 CSS 变量（`--color-bg-card` 等），**必须兼容深色主题**（`body[data-theme="dark"]` 变量已就绪，禁写死 `#fff`/`#fafafa` 等浅色值，已存在的写死值随迁移一并改变量）。
- 模式持久化 key：`aichuangzuo_create_mode`，值 `guided` | `minimal`，默认 `guided`。
- 接口信封：`{code: 0, data: ...}`；`submitGeneration` 返回完整 `GenerationTaskVO`（含 `id`、`progressPct`、`status`：0 排队 / 1 生成中 / 2 完成 / 3 失败）。
- 单任务轮询用 `getGenerationTask(id)`（`GET /generation-tasks/{id}`），3s 一次；队列列表轮询 5s 一次。
- 配置弹框 4 个（平台/字数/风格/模板）为**纯迁移**，交互视觉不变。
- 开发服务器：`cd project/user/web && npm run dev`（端口被占时自动 +1，E2E 脚本用 `BASE` 环境变量覆盖，默认 `http://localhost:22346`）。
- E2E 脚本放 `tests/e2e/`，截图放 `tests/e2e/screenshots/`，Playwright `page.route` mock API（信封 `{code:0,data:...}`）。
- 不用的代码开发结束后必须删掉（CLAUDE.md 约定）：迁移后旧样式/旧逻辑 grep 确认无引用再删。

---

### Task 1: 共享状态 composable — useCreateForm + useGenerationQueue

**Files:**
- Create: `project/user/web/src/views/console/create/useCreateForm.js`
- Create: `project/user/web/src/views/console/create/useGenerationQueue.js`

**Interfaces:**
- Produces（后续所有任务依赖）:
  - `useCreateForm()` → `{ createMode, setCreateMode, customTitle, customRequirement, currentPlatform, currentWordCount, customWordCount, selectedTemplateKey, platformVisible, wordCountVisible, styleVisible, templateVisible, clearForm }`
  - 模块级导出 `platforms`（带 `recommendWords`/`trait`）、`wordCountPresets`
  - `useGenerationQueue()` → `{ queueList, activeCount, loadQueue, startPolling, stopPolling }`
  - 模块级导出 `mapStatus(code)`、`statusText(status)`

- [ ] **Step 1: 创建 `create/useCreateForm.js`**

`platforms` 从现 CreateIndex.vue:685-693 迁移并扩展 `recommendWords`/`trait`（引导模式效果预览卡用）；`wordCountPresets` 从 707-764 原样迁移：

```js
import { ref } from 'vue'

// 模块级单例：引导模式与极简模式共享同一份创作配置（同 useStyles 模式）
const MODE_KEY = 'aichuangzuo_create_mode'

export const platforms = [
  { key: 'wechat', name: '公众号', desc: '深度长文，适合专业内容输出', recommendWords: 1500, trait: '长文深度阅读，段落完整，适合观点输出' },
  { key: 'xiaohongshu', name: '小红书', desc: '轻松图文，种草安利效果好', recommendWords: 800, trait: '短段落多 emoji，自动带话题标签' },
  { key: 'toutiao', name: '今日头条', desc: '算法分发，热点资讯类内容', recommendWords: 800, trait: '算法友好，热点资讯风格' },
  { key: 'baijiahao', name: '百家号', desc: '多平台分发，SEO友好', recommendWords: 1500, trait: 'SEO 友好，知识科普调性' },
  { key: 'douyin', name: '抖音图文', desc: '短视频+图文，流量大', recommendWords: 300, trait: '图配文短文案，金句为主' },
  { key: 'zhihu', name: '知乎', desc: '深度问答，专业知识分享', recommendWords: 1500, trait: '专业问答体，逻辑严谨' },
  { key: 'bilibili', name: 'B站', desc: '专栏图文，年轻兴趣社区', recommendWords: 1500, trait: '专栏图文，年轻社区语气' }
]

export const wordCountPresets = {
  platform: {
    wechat: [
      { count: 800, label: '早报 / 简评' },
      { count: 1500, label: '标准深度文' },
      { count: 2500, label: '专题报道' },
      { count: 3000, label: '行业研究（上限）' }
    ],
    xiaohongshu: [
      { count: 300, label: '标题种草' },
      { count: 500, label: '图文分享' },
      { count: 800, label: '详细测评' },
      { count: 1200, label: '步骤拆解教程' }
    ],
    toutiao: [
      { count: 400, label: '热点快讯' },
      { count: 800, label: '事件报道' },
      { count: 1500, label: '专题分析' },
      { count: 2000, label: '观点长文' }
    ],
    baijiahao: [
      { count: 1000, label: '知识科普' },
      { count: 1500, label: '生活攻略' },
      { count: 2000, label: '人文叙事' },
      { count: 2500, label: '行业洞察' }
    ],
    douyin: [
      { count: 150, label: '封面金句' },
      { count: 300, label: '图配文' },
      { count: 600, label: '情感短篇' }
    ],
    bilibili: [
      { count: 800, label: '动态短文' },
      { count: 1500, label: '科普专栏' },
      { count: 2500, label: '深度评测' },
      { count: 3000, label: '连载长文' }
    ],
    general: [
      { count: 500, label: '短文' },
      { count: 1000, label: '中等' },
      { count: 1500, label: '标准' },
      { count: 2500, label: '长文' }
    ]
  },
  scenario: [
    { count: 1200, label: '教程 / 步骤', desc: '操作步骤详细说明，适合图文对照' },
    { count: 1000, label: '测评 / 对比', desc: '优缺点详细对比，附评分' },
    { count: 500, label: '清单 / 种草', desc: '快速清单 + 标签，重点突出' },
    { count: 1800, label: '故事 / 叙事', desc: '沉浸式叙事，节奏完整' }
  ],
  tier: [
    { count: 500, label: '短文', desc: '速读，3 分钟读完' },
    { count: 1000, label: '中等', desc: '快速浏览，5 分钟读完' },
    { count: 1500, label: '标准', desc: '深度阅读，8 分钟读完' },
    { count: 2500, label: '长文', desc: '完整报告，12 分钟读完' }
  ]
}

const createMode = ref(localStorage.getItem(MODE_KEY) === 'minimal' ? 'minimal' : 'guided')
const customTitle = ref('')
const customRequirement = ref('')
const currentPlatform = ref(platforms[0])
const currentWordCount = ref({ count: 1500, label: '标准', desc: '深度阅读，8 分钟读完' })
const customWordCount = ref(1500)
const selectedTemplateKey = ref('wechat')
const platformVisible = ref(false)
const wordCountVisible = ref(false)
const styleVisible = ref(false)
const templateVisible = ref(false)

export function useCreateForm() {
  function setCreateMode(mode) {
    createMode.value = mode
    try { localStorage.setItem(MODE_KEY, mode) } catch { /* 隐私模式忽略 */ }
  }
  function clearForm() {
    customTitle.value = ''
    customRequirement.value = ''
  }
  return {
    createMode, setCreateMode,
    customTitle, customRequirement,
    currentPlatform, currentWordCount, customWordCount, selectedTemplateKey,
    platformVisible, wordCountVisible, styleVisible, templateVisible,
    clearForm
  }
}
```

- [ ] **Step 2: 创建 `create/useGenerationQueue.js`**

```js
import { ref, computed } from 'vue'
import { listGenerationTasks } from '@/api/generation.js'

// 模块级单例：队列数据 + 轮询，两模式共用（引导模式进度卡、极简模式抽屉、徽章）
const queueList = ref([])
let timer = null

export const mapStatus = (code) =>
  code === 0 ? 'queued' : code === 1 ? 'generating' : code === 2 ? 'completed' : code === 3 ? 'failed' : 'queued'

export const statusText = (status) =>
  ({ generating: '生成中', queued: '排队中', completed: '已完成', failed: '失败' }[status] || status)

export function useGenerationQueue() {
  const activeCount = computed(
    () => queueList.value.filter(t => t.status === 'queued' || t.status === 'generating').length
  )

  async function loadQueue() {
    try {
      const data = await listGenerationTasks({ page: 1, pageSize: 20 })
      queueList.value = (data.list || []).map(t => ({
        id: t.id,
        title: t.title || t.inputParam?.title || '未命名',
        platform: t.inputParam?.platform || '未选择',
        wordCount: t.wordLimitTarget || 0,
        status: mapStatus(t.status),
        progress: t.progressPct || 0,
        createdAt: t.createdAt,
        completedAt: t.completedAt
      }))
    } catch {
      queueList.value = []
    }
  }

  function startPolling() {
    if (timer) return
    loadQueue()
    timer = setInterval(loadQueue, 5000)
  }

  function stopPolling() {
    clearInterval(timer)
    timer = null
  }

  return { queueList, activeCount, loadQueue, startPolling, stopPolling }
}
```

- [ ] **Step 3: 验证编译**

Run: `cd project/user/web && npx vite build 2>&1 | tail -5`
Expected: 构建成功（新文件暂无人引用，tree-shake 不报错即可）

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/console/create/
git commit -m "refactor(create): 创作配置与队列状态抽为 composable 单例"
```

---

### Task 2: 提取 4 个配置弹框 + 灵感胶囊组件（CreateIndex 减重，视觉不变）

**Files:**
- Create: `project/user/web/src/views/console/create/modals/PlatformModal.vue`
- Create: `project/user/web/src/views/console/create/modals/WordCountModal.vue`
- Create: `project/user/web/src/views/console/create/modals/StyleModal.vue`
- Create: `project/user/web/src/views/console/create/modals/TemplateModal.vue`
- Create: `project/user/web/src/views/console/create/TopicCapsules.vue`
- Modify: `project/user/web/src/views/console/CreateIndex.vue`

**Interfaces:**
- Consumes: Task 1 的 `useCreateForm`（弹框可见性、选中值读写）、`@/composables/useStyles.js`（`systemStyles`/`myStyles`/`currentStyle` 等，风格弹框内部逻辑保持原样）
- Produces: 4 个弹框组件（无 props，读写 composable 单例）；`TopicCapsules`（emits `apply(topic)`）

迁移映射（行号基于当前 CreateIndex.vue）：

| 组件 | template 行 | script 行 | style 行（关键词起止） |
|---|---|---|---|
| PlatformModal | 154-180 | 697-704（open/select） | `.platform-grid`(1735) → `.platform-desc`(1769)，加 `.modal-title-wrap` 系列(1775-1790) |
| WordCountModal | 182-275 | 707-788（tab/presets/select，presets 改用 composable 导入） | `.wc-tabs`(1792) → `.wc-*` 全部（至风格选择注释前） |
| StyleModal | 277-风格弹框结束 | 790-902 全部 | `.style-*` 全部 |
| TemplateModal | 模板弹框段 | 904-971 + `filteredTemplates`/`buildLargePreview` 相关 | `.template-*`/`.tpl-*` 全部 |
| TopicCapsules | 77-98 | 656-679（topics/loadTopics/refreshTopics/applyTopic 的 fetch+mark 部分） | `.topic-capsules` 系列 |

注意：

- 弹框可见性 v-model 用 composable 的 `platformVisible` 等，如 `<a-modal v-model:open="platformVisible">`（script setup 中从 `useCreateForm()` 解构）。
- `.modal-title-wrap`/`.modal-title`/`.modal-subtitle` 是 4 个弹框共用样式 → 移到 `src/styles/index.css` 末尾（全局），深色覆盖一并移动。
- TopicCapsules 内部 `applyTopic` 逻辑保持：填入 `customTitle`/`customRequirement`（从 useCreateForm 取）+ `markTopicUsed`；`@click="applyTopic"` 后 `emit('apply', topic)`（极简模式将来不再需要额外动作，emit 预留给引导模式）。
- **顺带删除已死样式**（当前 template 已不用）：`.quota-card` 系列、`.mode-tabs` 系列、`.topic-section`/`.topic-grid`/`.topic-card` 系列、`.input-section`/`.create-mode`/`.form-*`、`.settings-toolbar`/`.settings-sep`、`.action-bar`/`.action-primary`（action-link 保留）、`.queue-export-btn`/`.queue-item-footer`、`.refresh-btn`、`.topic-title`/`.topic-meta`/`.topic-tag`/`.topic-heat`/`.topic-footer`/`.topic-hint`。删法：grep 类名确认 template 无引用后整块删。

- [ ] **Step 1: 建 `modals/PlatformModal.vue`**

按上表剪切 template 154-180 + script + style；可见性/选中值改用 `useCreateForm()`：

```vue
<template>
  <a-modal
    v-model:open="platformVisible"
    :footer="null"
    :width="560"
    centered
    :closable="true"
    class="platform-modal"
  >
    <template #title>
      <div class="modal-title-wrap">
        <div class="modal-title">选择发布平台</div>
        <div class="modal-subtitle">选择目标平台，AI 将按平台规则推荐模板、字数和标签</div>
      </div>
    </template>
    <div class="platform-grid">
      <div
        v-for="p in platforms"
        :key="p.key"
        :class="['platform-item', { selected: currentPlatform.key === p.key }]"
        @click="selectPlatform(p)"
      >
        <div class="platform-name">{{ p.name }}</div>
        <div class="platform-desc">{{ p.desc }}</div>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { platforms, useCreateForm } from '../useCreateForm.js'

const { platformVisible, currentPlatform } = useCreateForm()

const selectPlatform = (p) => {
  currentPlatform.value = p
  platformVisible.value = false
}
</script>

<style scoped>
/* 从 CreateIndex.vue 剪切 .platform-grid → .platform-desc 全部规则，原样保留 */
</style>
```

- [ ] **Step 2: 建 `modals/WordCountModal.vue`、`StyleModal.vue`、`TemplateModal.vue`**

同 Step 1 模式剪切。WordCountModal 中 `wordCountPresets` 改为 `import { wordCountPresets, useCreateForm } from '../useCreateForm.js'`；StyleModal 继续用 `@/composables/useStyles.js` 的全部导出（`systemStyles`/`myStyles`/`currentStyle`/`loadMyStyles`/`applyStyle`/`addCustomStyle`/`updateCustomStyle`/`removeCustomStyle`/`stylePresets` 如在该 composable）；`currentStyle` 读写保持不变。TemplateModal 继续用 `useExportTemplates()` + `buildLargePreview`。

- [ ] **Step 3: 建 `TopicCapsules.vue`**

```vue
<template>
  <div v-if="topics.length > 0" class="topic-capsules">
    <span class="topic-capsules-label">没灵感？点一个快速开始：</span>
    <div class="topic-capsules-grid">
      <a-tooltip v-for="topic in topics" :key="topic.id" :title="topic.title" placement="top">
        <button
          :class="['topic-capsule', { used: topic.used }]"
          :disabled="topic.used"
          @click="topic.used ? null : applyTopic(topic)"
        >
          {{ topic.title }}
        </button>
      </a-tooltip>
    </div>
    <button class="refresh-capsule" @click="refreshTopics">换一批</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { fetchRandomTopics, markTopicUsed } from '@/api/topic.js'
import { useCreateForm } from './useCreateForm.js'

const emit = defineEmits(['apply'])
const { customTitle, customRequirement } = useCreateForm()

const topics = ref([])

const loadTopics = async () => {
  try {
    const list = await fetchRandomTopics(6)
    topics.value = (list || []).map(t => ({ id: t.id, title: t.title, summary: t.summary, used: false }))
  } catch {
    topics.value = []
  }
}

const applyTopic = (topic) => {
  customTitle.value = topic.title
  customRequirement.value = topic.summary || ''
  topic.used = true
  markTopicUsed(topic.id).catch(() => {})
  emit('apply', topic)
}

const refreshTopics = () => { loadTopics() }

defineExpose({ loadTopics })
</script>

<style scoped>
/* 从 CreateIndex.vue 剪切 .topic-capsules 全部规则 */
</style>
```

- [ ] **Step 4: CreateIndex.vue 改用新组件**

template 中 4 个弹框 + 灵感胶囊段替换为：

```vue
<PlatformModal />
<WordCountModal />
<StyleModal />
<TemplateModal />
...
<TopicCapsules ref="topicCapsulesRef" />
```

script 顶部 import 组件；onMounted 中 `loadTopics()` 改为 `topicCapsulesRef.value?.loadTopics()`；删除已迁移的 template/script/style 段 + 死样式清单。

- [ ] **Step 5: E2E 验证弹框仍可打开**

写 `tests/e2e/verify_create_modals_after_extract.py`：

```python
#!/usr/bin/env python3
"""弹框提取后回归：4 个弹框均可打开且视觉正常（浅色+深色截图）。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22346")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)

        checks = [
            ("button.settings-chip:nth-of-type(1)", "modal_platform", ".platform-modal .ant-modal-content"),
            ("button.settings-chip:nth-of-type(2)", "modal_wc", ".word-count-modal .ant-modal-content"),
            ("button.settings-chip:nth-of-type(3)", "modal_style", ".style-modal .ant-modal-content"),
            ("button.settings-chip:nth-of-type(4)", "modal_template", ".template-modal .ant-modal-content"),
        ]
        failures = []
        for chip_sel, name, modal_sel in checks:
            page.click(chip_sel, timeout=3000)
            page.wait_for_timeout(600)
            ok = page.query_selector(modal_sel) is not None
            page.screenshot(path=f"{SHOTS}/{name}.png")
            print(("PASS" if ok else "FAIL"), name)
            if not ok:
                failures.append(name)
            page.keyboard.press("Escape")
            page.wait_for_timeout(400)

        browser.close()
        if failures:
            raise SystemExit(f"FAILED: {failures}")
        print("ALL PASS")

main()
```

Run: `python3 tests/e2e/verify_create_modals_after_extract.py`（先确认 dev server 端口，`BASE=http://localhost:PORT python3 ...`）
Expected: ALL PASS；肉眼检查 4 张截图与改动前一致。

- [ ] **Step 6: Commit**

```bash
git add project/user/web/src/views/console/ tests/e2e/verify_create_modals_after_extract.py
git commit -m "refactor(create): 4 个配置弹框与灵感胶囊拆为独立组件，清除死样式"
```

---

### Task 3: QueueDrawer — 右侧栏改抽屉

**Files:**
- Create: `project/user/web/src/views/console/create/QueueDrawer.vue`
- Modify: `project/user/web/src/views/console/CreateIndex.vue`

**Interfaces:**
- Consumes: `useGenerationQueue()`（`queueList`/`activeCount`/`startPolling`/`stopPolling`）
- Produces: `QueueDrawer` 组件 `v-model:open`；`useGenerationQueue` 新增 `queueOpen` ref（见下）

- [ ] **Step 1: useGenerationQueue 增加 `queueOpen`**

```js
const queueOpen = ref(false)
// return 中导出 queueOpen
```

- [ ] **Step 2: 建 `QueueDrawer.vue`**

队列列表 UI 从 CreateIndex template 101-151 迁移，外层 `.queue-panel` 改为 a-drawer；样式 `.queue-panel-*`/`.queue-item-*` 剪切过来（`.queue-panel` 背景/圆角/阴影规则删除，drawer 自带容器）：

```vue
<template>
  <a-drawer
    v-model:open="open"
    title="生成队列"
    placement="right"
    :width="isMobile ? '100%' : 360"
    class="queue-drawer"
  >
    <template #extra>
      <button class="queue-more-btn" @click="goWorks">查看更多 →</button>
    </template>
    <div v-if="queueList.length === 0" class="queue-panel-empty">
      <InboxOutlined class="empty-icon" />
      <div class="empty-text">暂无生成任务</div>
      <div class="empty-hint">点击「生成文章」开始创作</div>
    </div>
    <div v-else class="queue-panel-list">
      <div
        v-for="item in queueList"
        :key="item.id"
        :class="['queue-panel-item', item.status]"
        :style="item.status === 'completed' ? 'cursor: pointer' : ''"
        @click="item.status === 'completed' && goWorks()"
      >
        <div class="queue-item-top">
          <div class="queue-item-icon">
            <LoadingOutlined v-if="item.status === 'generating'" :spin="true" />
            <CheckCircleOutlined v-else-if="item.status === 'completed'" />
            <ClockCircleOutlined v-else-if="item.status === 'queued'" />
            <CloseCircleOutlined v-else />
          </div>
          <div class="queue-item-info">
            <a-tooltip :title="item.title" placement="top">
              <span class="queue-item-title">{{ item.title }}</span>
            </a-tooltip>
            <div class="queue-item-meta">
              <span class="queue-item-status-badge" :class="item.status">
                {{ item.status === 'generating' ? `生成中 ${Math.min(100, Math.round(item.progress))}%` : statusText(item.status) }}
              </span>
            </div>
          </div>
        </div>
        <div v-if="item.status === 'generating'" class="queue-item-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: Math.min(100, Math.round(item.progress)) + '%' }"></div>
          </div>
          <div class="progress-hint">已完成 {{ Math.min(100, Math.round(item.progress)) }}%</div>
        </div>
      </div>
    </div>
  </a-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { InboxOutlined, LoadingOutlined, CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import { useGenerationQueue, statusText } from './useGenerationQueue.js'
import { useIsMobile } from '@/composables/useMobile.js'

const props = defineProps({ open: Boolean })
const emit = defineEmits(['update:open'])
const open = computed({
  get: () => props.open,
  set: (v) => emit('update:open', v)
})

const router = useRouter()
const isMobile = useIsMobile()
const { queueList } = useGenerationQueue()

const goWorks = () => {
  open.value = false
  router.push('/console/works')
}
</script>

<style scoped>
/* 从 CreateIndex.vue 剪切：.queue-panel-header/.queue-panel-title/.queue-more-btn/.queue-panel-more*/
/* .queue-panel-empty/.queue-panel-list/.queue-panel-item 全部 .queue-item-* 规则 */
/* 删除：.queue-panel 自身背景/圆角/阴影；.queue-panel-more「还有 N 个任务」（列表已全量 20 条）*/
</style>
```

- [ ] **Step 3: CreateIndex 接入**

template：右侧 `<div class="queue-panel">...</div>` 整块删除，替换为 `<QueueDrawer v-model:open="queueOpen" />`；顶部操作行（原 hero-action-row 左侧）加队列触发按钮：

```vue
<button class="action-link" @click="queueOpen = true">
  📋 队列<template v-if="activeCount > 0">（{{ activeCount }}）</template>
</button>
```

script：`const { queueList, activeCount, queueOpen, startPolling, stopPolling } = useGenerationQueue()`；onMounted 中 `loadMiniQueue(); setInterval(...)` 两行改为 `startPolling()`；新增 `onUnmounted(stopPolling)`；`handleGenerate` 成功后 `loadMiniQueue()` 改为 `loadQueue()` 且 **`queueOpen.value = true`**（spec：生成后抽屉自动滑出）；删除 `miniQueueList`/`miniStatusText`/`mapStatus`/`loadMiniQueue`。

- [ ] **Step 4: E2E 验证**

写 `tests/e2e/verify_queue_drawer.py`：mock 队列接口返回 1 条生成中任务 → 点「队列」→ drawer 出现且含进度条 → 截图：

```python
#!/usr/bin/env python3
"""队列抽屉：触发按钮打开、进行中任务徽章、列表渲染。"""
import os, json
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22346")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

TASK = {
    "id": 101, "bizNo": "T101", "status": 1, "progressPct": 45,
    "title": "35岁被裁后，我靠副业翻身", "wordLimitTarget": 1500,
    "inputParam": {"title": "35岁被裁后，我靠副业翻身", "platform": "wechat"},
    "createdAt": "2026-07-16T10:00:00"
}

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.route("**/api/v1/user/generation-tasks?**",
                   lambda r: r.fulfill(json={"code": 0, "data": {"list": [TASK], "total": 1}}))
        page.route("**/api/v1/user/generation-tasks",
                   lambda r: r.fulfill(json={"code": 0, "data": {"list": [TASK], "total": 1}}))
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)

        page.click("text=队列", timeout=3000)
        page.wait_for_timeout(800)
        drawer = page.query_selector(".ant-drawer-open .queue-panel-list")
        ok_drawer = drawer is not None
        ok_progress = page.query_selector(".ant-drawer-open .queue-item-progress") is not None
        page.screenshot(path=f"{SHOTS}/queue_drawer.png")
        print("PASS drawer" if ok_drawer else "FAIL drawer")
        print("PASS progress" if ok_progress else "FAIL progress")
        browser.close()
        if not (ok_drawer and ok_progress):
            raise SystemExit("FAILED")
        print("ALL PASS")

main()
```

Run: `python3 tests/e2e/verify_queue_drawer.py`
Expected: ALL PASS；截图中 drawer 从右侧滑出、进度条 45%。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/ tests/e2e/verify_queue_drawer.py
git commit -m "feat(create): 生成队列从常驻右侧栏改为右侧抽屉，生成后自动滑出"
```

---

### Task 4: MinimalPanel — 一体化输入卡片 + 居中极简布局

**Files:**
- Create: `project/user/web/src/views/console/create/MinimalPanel.vue`
- Modify: `project/user/web/src/views/console/CreateIndex.vue`

**Interfaces:**
- Consumes: `useCreateForm`（全部表单项 + 弹框可见性）、`useGenerationQueue`（`queueOpen`/`activeCount`/`loadQueue`）、`useBenefits`（额度）、`currentStyle`（useStyles）、`useExportTemplates`
- Produces: `MinimalPanel` 组件（无 props）；CreateIndex 本任务后仅剩极简壳（引导下个任务接入）

- [ ] **Step 1: 建 `MinimalPanel.vue`**

```vue
<template>
  <div class="minimal-panel">
    <!-- 顶部行 -->
    <div class="minimal-topbar">
      <h2 class="create-title">开始创作</h2>
      <div class="minimal-topbar-right">
        <span class="quota-text">本月剩余 <strong>{{ quotaRemaining }}</strong> / {{ quotaTotal }} 次</span>
        <button class="topbar-btn" @click="queueOpen = true">
          📋 队列<template v-if="activeCount > 0">（{{ activeCount }}）</template>
        </button>
        <button class="topbar-btn" @click="setCreateMode('guided')">💬 引导模式</button>
      </div>
    </div>

    <!-- 一体化输入卡片 -->
    <div class="hero-card" :class="{ focused: heroFocused }">
      <input
        v-model="customTitle"
        type="text"
        class="hero-title-input"
        placeholder="输入标题或想法，例如：职场新人快速提升效率的 5 个方法"
        @focus="heroFocused = true"
        @blur="heroFocused = false"
      />
      <textarea
        ref="requirementEl"
        v-model="customRequirement"
        class="hero-textarea"
        rows="4"
        placeholder="补充要求：语气、案例、重点…"
        @input="autoGrow"
        @focus="heroFocused = true"
        @blur="heroFocused = false"
      ></textarea>

      <div class="hero-divider"></div>

      <div class="hero-chips">
        <button class="settings-chip" @click="platformVisible = true">
          <span class="chip-icon">📱</span><span>{{ currentPlatform.name }}</span><span class="chip-caret">▾</span>
        </button>
        <button class="settings-chip" @click="wordCountVisible = true">
          <span class="chip-icon">📝</span><span>{{ currentWordCount.count }} 字 · {{ currentWordCount.label }}</span><span class="chip-caret">▾</span>
        </button>
        <button class="settings-chip" @click="styleVisible = true">
          <span class="chip-icon">🎨</span><span>{{ currentStyle?.name || '选择风格' }}</span><span class="chip-caret">▾</span>
        </button>
        <button class="settings-chip" @click="templateVisible = true">
          <span class="chip-icon">🖼</span><span>{{ currentTemplate?.name }}</span><span class="chip-caret">▾</span>
        </button>
      </div>

      <div class="hero-action-row">
        <div class="hero-action-left">
          <button class="action-link" @click="handleSaveDraft">保存草稿</button>
          <button class="action-link" @click="router.push('/console/works?tab=drafts')">草稿箱</button>
        </div>
        <button class="hero-generate-btn" @click="handleGenerate">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
          </svg>
          生成文章
        </button>
      </div>
    </div>

    <!-- 灵感胶囊 -->
    <TopicCapsules />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import TopicCapsules from './TopicCapsules.vue'
import { useCreateForm } from './useCreateForm.js'
import { useGenerationQueue } from './useGenerationQueue.js'
import { currentStyle } from '@/composables/useStyles.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { submitGeneration } from '@/api/generation.js'
import { saveDraft } from '@/api/draft.js'

const router = useRouter()
const {
  setCreateMode, customTitle, customRequirement,
  currentPlatform, currentWordCount, selectedTemplateKey,
  platformVisible, wordCountVisible, styleVisible, templateVisible,
  clearForm
} = useCreateForm()
const { queueOpen, activeCount, loadQueue } = useGenerationQueue()
const { templates: apiTemplates } = useExportTemplates()
const { benefits } = useBenefits()

const currentTemplate = computed(() => apiTemplates.value.find(t => t.key === selectedTemplateKey.value) || apiTemplates.value[0])
const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)

const heroFocused = ref(false)
const requirementEl = ref(null)
const autoGrow = () => {
  const el = requirementEl.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 300) + 'px'
}

const handleSaveDraft = async () => {
  try {
    await saveDraft({
      customTitle: customTitle.value,
      customRequirement: customRequirement.value,
      platform: currentPlatform.value?.name,
      wordCount: currentWordCount.value?.count,
      style: currentStyle.value?.name,
      template: currentTemplate.value?.name
    })
    message.success('草稿已保存')
  } catch (e) {
    console.warn('保存草稿失败', e)
  }
}

const handleGenerate = async () => {
  if (!customTitle.value.trim()) {
    message.warning('请输入文章标题')
    return
  }
  if (!customRequirement.value.trim()) {
    message.warning('请补充你的核心观点和要求')
    return
  }
  if (quotaTotal.value <= 0) {
    message.warning('开通会员后才能使用 AI 生成文章')
    router.push('/pricing')
    return
  }
  if (quotaRemaining.value <= 0) {
    message.warning('本月额度已用完，升级会员可获得更多额度')
    router.push('/pricing')
    return
  }
  try {
    await submitGeneration({
      title: customTitle.value,
      description: customRequirement.value,
      platform: currentPlatform.value?.key || '',
      styleRef: currentStyle.value?.id || currentStyle.value?.name || '',
      wordCount: currentWordCount.value?.count || 800,
      template: currentTemplate.value?.key || 'wechat'
    })
    message.success('已加入生成队列')
    clearForm()
    requirementEl.value && (requirementEl.value.style.height = '')
    loadQueue()
    queueOpen.value = true   // spec：生成后抽屉自动滑出
  } catch (e) {
    message.error(e?.message || '提交失败，请稍后重试')
  }
}
</script>

<style scoped>
.minimal-panel {
  max-width: 760px;
  margin: 0 auto;
  width: 100%;
}

.minimal-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.create-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.minimal-topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quota-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-right: 4px;
}

.quota-text strong {
  color: var(--color-primary);
}

.topbar-btn {
  border: 1px solid var(--color-border-light);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.topbar-btn:hover {
  color: var(--color-primary);
  border-color: var(--color-primary-light);
}

/* 一体化输入卡片 */
.hero-card {
  background: var(--color-bg-card);
  border-radius: 16px;
  padding: 20px 20px 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1.5px solid transparent;
  transition: border-color 0.25s, box-shadow 0.25s;
}

.hero-card.focused {
  border-color: var(--color-primary-light);
  box-shadow: 0 4px 24px rgba(255, 36, 66, 0.10);
}

.hero-title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  background: transparent;
  padding: 4px 0 10px;
}

.hero-title-input::placeholder {
  color: var(--color-text-placeholder);
  font-weight: 400;
}

.hero-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-regular);
  background: transparent;
  min-height: 112px;
  max-height: 300px;
  overflow-y: auto;
  font-family: inherit;
}

.hero-textarea::placeholder {
  color: var(--color-text-placeholder);
}

.hero-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 12px 0;
}

.hero-chips {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

/* .settings-chip/.chip-caret 样式从 CreateIndex 剪切（1645-1670），追加： */
.settings-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 8px 14px;
}

.chip-icon {
  font-size: 13px;
}

.hero-action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-action-left {
  display: flex;
  gap: 16px;
}

/* .action-link 样式从 CreateIndex 剪切保留 */
/* .hero-generate-btn 样式从 CreateIndex 剪切保留 */

@media (max-width: 768px) {
  .minimal-panel { max-width: 100%; }
  .hero-chips { flex-wrap: nowrap; overflow-x: auto; padding-bottom: 4px; }
  .hero-chips .settings-chip { flex-shrink: 0; }
  .quota-text { display: none; }  /* 移动端额度并入抽屉顶部（Task 8 处理），先隐藏 */
}
</style>
```

- [ ] **Step 2: CreateIndex 瘦身为极简壳**

template 主体替换为：

```vue
<template>
  <div class="create-index">
    <MinimalPanel />
    <QueueDrawer v-model:open="queueOpen" />
    <PlatformModal />
    <WordCountModal />
    <StyleModal />
    <TemplateModal />
  </div>
</template>
```

script 只留：组件 import、`useGenerationQueue()`（`queueOpen`/`startPolling`/`stopPolling` + onMounted/onUnmounted）、onMounted 中的草稿恢复逻辑（`restoreDraft` 恢复 `customTitle` 等 → 字段改从 `useCreateForm()` 取）、`route.query.marketStyleId` 处理、`loadBenefits()`、`loadExportTemplates()`、系统风格加载（`loadSystemStyles`，若原本在 ConsoleLayout 已调则不调）。删除：quota-card/topic/mode-tabs 等剩余死样式、`.create-layout`/`.create-card` 旧布局样式、已迁移组件的样式。

- [ ] **Step 3: 页面背景光晕（全局浅色+深色）**

`src/styles/index.css` 的 console 内容区或 CreateIndex 根节点加（放 CreateIndex scoped 样式即可）：

```css
.create-index {
  min-height: 100%;
  padding: 24px 24px 40px;
  background:
    radial-gradient(600px 300px at 50% -80px, rgba(255, 36, 66, 0.05), transparent 70%);
}

body[data-theme="dark"] .create-index {
  background:
    radial-gradient(600px 300px at 50% -80px, rgba(255, 36, 66, 0.08), transparent 70%);
}
```

- [ ] **Step 4: E2E 验证 + 截图走查**

写 `tests/e2e/verify_minimal_panel.py`：localStorage 设 `aichuangzuo_create_mode=minimal` → 打开页面 → 断言一体化卡片存在（`.hero-card`）、4 个胶囊带图标、textarea 默认高度 ≤ 130px、输入长文本后自动撑高 → 浅色/深色各截一张图：

```python
#!/usr/bin/env python3
"""极简模式：一体化卡片、配置胶囊、textarea 自动撑高、深色兼容。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22346")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.goto(f"{BASE}/console/create", wait_until="domcontentloaded")
        page.evaluate("localStorage.setItem('aichuangzuo_create_mode', 'minimal')")
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        ok_card = page.query_selector(".hero-card") is not None
        chips = page.query_selector_all(".hero-chips .settings-chip")
        ok_chips = len(chips) == 4 and all("chip-icon" in (c.inner_html() or "") for c in chips)

        h0 = page.eval_on_selector(".hero-textarea", "e => e.offsetHeight")
        page.fill(".hero-textarea", "测试内容\n" * 30)
        page.wait_for_timeout(300)
        h1 = page.eval_on_selector(".hero-textarea", "e => e.offsetHeight")
        ok_grow = h0 <= 130 and h1 > h0

        page.screenshot(path=f"{SHOTS}/minimal_light.png")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark'); document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)
        page.screenshot(path=f"{SHOTS}/minimal_dark.png")

        print("PASS card" if ok_card else "FAIL card")
        print(f"PASS chips({len(chips)})" if ok_chips else f"FAIL chips({len(chips)})")
        print(f"PASS autogrow({h0}->{h1})" if ok_grow else f"FAIL autogrow({h0}->{h1})")
        browser.close()
        if not (ok_card and ok_chips and ok_grow):
            raise SystemExit("FAILED")
        print("ALL PASS")

main()
```

Run: `python3 tests/e2e/verify_minimal_panel.py`
Expected: ALL PASS；肉眼走查两张截图（聚焦态可用 DevTools 补验）。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/ tests/e2e/verify_minimal_panel.py
git commit -m "feat(create): 极简模式 — 一体化输入卡片 + 居中布局 + 品牌光晕"
```

---

### Task 5: GuidedChat 骨架 + ChatMessage + QuickReplies + 模式切换

**Files:**
- Create: `project/user/web/src/views/console/create/ChatMessage.vue`
- Create: `project/user/web/src/views/console/create/QuickReplies.vue`
- Create: `project/user/web/src/views/console/create/GuidedChat.vue`
- Modify: `project/user/web/src/views/console/CreateIndex.vue`

**Interfaces:**
- Consumes: `useCreateForm`、`useGenerationQueue`、`useBenefits`、`TopicCapsules`（步骤 1 快捷回复）
- Produces:
  - `ChatMessage`：props `{ role: 'ai' | 'user' }`，默认 slot 为气泡内容
  - `QuickReplies`：props `{ options: Array }`；emits `confirm(option)`；scoped slot `preview`（`{ option }`）渲染效果卡
  - `GuidedChat` 内部消息模型（Task 6/7 依赖）：
    ```js
    // messages.value 元素：
    // { id, role: 'ai'|'user', kind: 'text'|'topic'|'quick'|'confirm'|'progress'|'result',
    //   text?, optionsType?: 'platform'|'style', taskId?, status?, progress? }
    ```

- [ ] **Step 1: 建 `ChatMessage.vue`**

```vue
<template>
  <div class="chat-msg" :class="role">
    <div v-if="role === 'ai'" class="chat-avatar">AI</div>
    <div class="chat-bubble">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({ role: { type: String, default: 'ai' } })
</script>

<style scoped>
.chat-msg {
  display: flex;
  margin-bottom: 16px;
  gap: 10px;
}

.chat-msg.user {
  justify-content: flex-end;
}

.chat-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chat-bubble {
  max-width: 85%;
  background: var(--color-bg-card);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-regular);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.chat-msg.user .chat-bubble {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-msg.ai .chat-bubble {
  border-bottom-left-radius: 4px;
}
</style>
```

- [ ] **Step 2: 建 `QuickReplies.vue`（两段式：点选→效果卡→确认）**

```vue
<template>
  <div class="quick-replies">
    <div class="quick-options">
      <button
        v-for="opt in options"
        :key="opt.key"
        :class="['quick-option', { selected: selected?.key === opt.key }]"
        @click="selected = opt"
      >
        {{ opt.label }}
      </button>
    </div>
    <div v-if="selected" class="quick-preview">
      <slot name="preview" :option="selected" />
      <div class="quick-preview-actions">
        <button class="quick-confirm" @click="onConfirm">确认 ✓</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({ options: { type: Array, default: () => [] } })
const emit = defineEmits(['confirm'])
const selected = ref(null)

const onConfirm = () => {
  if (!selected.value) return
  emit('confirm', selected.value)
}
</script>

<style scoped>
.quick-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-option {
  border: 1px solid var(--color-border-default);
  background: var(--color-bg-card);
  color: var(--color-text-regular);
  font-size: 13px;
  padding: 7px 14px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-option:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.quick-option.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 500;
}

.quick-preview {
  margin-top: 10px;
  border: 1px solid var(--color-primary-light);
  background: var(--color-primary-bg);
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--color-text-regular);
}

.quick-preview-actions {
  text-align: right;
  margin-top: 8px;
}

.quick-confirm {
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 18px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.quick-confirm:hover {
  background: var(--color-primary-hover);
}

@media (max-width: 768px) {
  .quick-options { flex-wrap: nowrap; overflow-x: auto; padding-bottom: 4px; }
  .quick-option { flex-shrink: 0; }
}
</style>
```

- [ ] **Step 3: 建 `GuidedChat.vue` 骨架（仅步骤 1 主题跑通）**

```vue
<template>
  <div class="guided-chat">
    <div class="guided-topbar">
      <h2 class="create-title">开始创作</h2>
      <button class="topbar-btn" @click="setCreateMode('minimal')">熟手模式 →</button>
    </div>

    <div ref="msgListEl" class="chat-list">
      <ChatMessage v-for="m in messages" :key="m.id" :role="m.role">
        <!-- 纯文本 -->
        <template v-if="m.kind === 'text'">{{ m.text }}</template>

        <!-- 步骤 1：主题输入 + 灵感胶囊 -->
        <template v-else-if="m.kind === 'topic'">
          <div class="chat-question">想写一篇什么主题的文章？</div>
          <div class="topic-input-row">
            <input
              v-model="topicInput"
              type="text"
              class="topic-input"
              placeholder="输入主题，回车发送…"
              @keyup.enter="submitTopic(topicInput)"
            />
            <button class="topic-send" @click="submitTopic(topicInput)">发送</button>
          </div>
          <TopicCapsules @apply="onTopicCapsule" />
        </template>

        <!-- 平台/风格快捷回复（Task 6 填充 preview 内容） -->
        <template v-else-if="m.kind === 'quick'">
          <div class="chat-question">{{ m.text }}</div>
          <QuickReplies :options="m.options" @confirm="(opt) => onQuickConfirm(m, opt)" />
        </template>
      </ChatMessage>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import ChatMessage from './ChatMessage.vue'
import QuickReplies from './QuickReplies.vue'
import TopicCapsules from './TopicCapsules.vue'
import { platforms, useCreateForm } from './useCreateForm.js'
import { systemStyles, applyStyle } from '@/composables/useStyles.js'

const { setCreateMode, customTitle, customRequirement, currentPlatform } = useCreateForm()

let seq = 0
const messages = ref([])
const topicInput = ref('')
const msgListEl = ref(null)

const scrollToBottom = async () => {
  await nextTick()
  const el = msgListEl.value
  if (el) el.scrollTop = el.scrollHeight
}

const push = (msg) => {
  messages.value.push({ id: ++seq, ...msg })
  scrollToBottom()
}

// 初始化：第一条 AI 消息（额度拦截在 Task 7 加）
push({ role: 'ai', kind: 'topic' })

const submitTopic = (text) => {
  const title = (text || '').trim()
  if (!title) return
  customTitle.value = title
  push({ role: 'user', kind: 'text', text: title })
  topicInput.value = ''
  askPlatform()
}

const onTopicCapsule = (topic) => {
  // TopicCapsules 已把标题/概要写入 customTitle/customRequirement
  push({ role: 'user', kind: 'text', text: topic.title })
  askPlatform()
}

const askPlatform = () => {
  push({
    role: 'ai',
    kind: 'quick',
    text: '准备发哪个平台？',
    optionsType: 'platform',
    options: platforms.map(p => ({ key: p.key, label: p.name, raw: p }))
  })
}

const onQuickConfirm = (m, opt) => {
  if (m.optionsType === 'platform') {
    currentPlatform.value = opt.raw
    push({ role: 'user', kind: 'text', text: opt.label })
    // Task 6：askStyle()
  }
}
</script>

<style scoped>
.guided-chat {
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.guided-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.create-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
}

/* .topbar-btn 与 MinimalPanel 同款 → 抽到 src/styles/index.css 全局（Task 4 若未抽则本任务抽） */

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 2px;
}

.chat-question {
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 10px;
}

.topic-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.topic-input {
  flex: 1;
  border: 1px solid var(--color-border-default);
  border-radius: 18px;
  padding: 8px 16px;
  font-size: 14px;
  outline: none;
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  transition: border-color 0.2s;
}

.topic-input:focus {
  border-color: var(--color-primary);
}

.topic-send {
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  padding: 8px 20px;
  border-radius: 18px;
  cursor: pointer;
  transition: background 0.2s;
}

.topic-send:hover {
  background: var(--color-primary-hover);
}

/* 聊天气泡内的灵感胶囊：去掉外层 label 换行差异，复用 TopicCapsule 样式即可 */
@media (max-width: 768px) {
  .topic-input-row {
    position: sticky;
    bottom: 0;
    background: var(--color-bg-page);
    padding: 8px 0;
  }
}
</style>
```

- [ ] **Step 4: CreateIndex 改为模式容器**

```vue
<template>
  <div class="create-index">
    <GuidedChat v-if="createMode === 'guided'" />
    <MinimalPanel v-else />
    <QueueDrawer v-model:open="queueOpen" />
    <PlatformModal />
    <WordCountModal />
    <StyleModal />
    <TemplateModal />
  </div>
</template>
```

script 增加 `const { createMode } = useCreateForm()`，import GuidedChat。

- [ ] **Step 5: E2E 验证**

写 `tests/e2e/verify_guided_skeleton.py`：默认进引导模式 → 主题输入「测试主题」回车 → 出现用户气泡 + 平台快捷胶囊 → 点「小红书」出现效果卡占位 → 点「熟手模式」切极简 → 刷新后仍是极简：

```python
#!/usr/bin/env python3
"""引导模式骨架：默认 guided、主题步骤、平台胶囊两段式、模式切换记忆。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22346")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        ok_default = page.query_selector(".guided-chat") is not None
        page.fill(".topic-input", "测试主题：AI 写作工具横评")
        page.keyboard.press("Enter")
        page.wait_for_timeout(600)
        ok_user_bubble = page.query_selector(".chat-msg.user") is not None
        ok_options = len(page.query_selector_all(".quick-option")) >= 5

        page.click("text=小红书", timeout=3000)
        page.wait_for_timeout(400)
        ok_preview = page.query_selector(".quick-preview") is not None
        page.screenshot(path=f"{SHOTS}/guided_skeleton.png")

        page.click("text=熟手模式", timeout=3000)
        page.wait_for_timeout(600)
        ok_minimal = page.query_selector(".minimal-panel") is not None
        page.reload(wait_until="networkidle")
        page.wait_for_timeout(1000)
        ok_remember = page.query_selector(".minimal-panel") is not None

        for name, ok in [("default-guided", ok_default), ("user-bubble", ok_user_bubble),
                         ("platform-options", ok_options), ("preview-card", ok_preview),
                         ("switch-minimal", ok_minimal), ("remember", ok_remember)]:
            print(("PASS " if ok else "FAIL ") + name)
        browser.close()
        if not all([ok_default, ok_user_bubble, ok_options, ok_preview, ok_minimal, ok_remember]):
            raise SystemExit("FAILED")
        print("ALL PASS")

main()
```

Run: `python3 tests/e2e/verify_guided_skeleton.py`
Expected: ALL PASS。

- [ ] **Step 6: Commit**

```bash
git add project/user/web/src/ tests/e2e/verify_guided_skeleton.py
git commit -m "feat(create): 引导模式骨架 — 聊天流 + 主题步骤 + 两段式快捷回复 + 模式切换记忆"
```

---

### Task 6: GuidedChat 步骤 2-4 — 平台/风格效果卡 + 确认卡片

**Files:**
- Modify: `project/user/web/src/views/console/create/GuidedChat.vue`

**Interfaces:**
- Consumes: Task 5 消息模型；`useExportTemplates`（`templates`）；`systemStyles`/`applyStyle`（useStyles）；`useCreateForm`（`currentWordCount`/`selectedTemplateKey`/`customRequirement`）
- Produces: `applyPlatformDefault(platform)`（引导模式专用，确认平台后自动带推荐字数 + 默认模板）；确认卡片消息 `kind: 'confirm'`

- [ ] **Step 1: 平台确认后自动带默认配置**

`onQuickConfirm` 中 platform 分支补全，并新增 `askStyle`：

```js
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { wordCountPresets } from './useCreateForm.js'

const { templates: apiTemplates } = useExportTemplates()
// useCreateForm 解构补充：currentWordCount, selectedTemplateKey

const applyPlatformDefault = (p) => {
  // 推荐字数：该平台预设中匹配 recommendWords，找不到取第一个
  const presets = wordCountPresets.platform[p.key] || wordCountPresets.platform.general
  const wc = presets.find(x => x.count === p.recommendWords) || presets[0]
  currentWordCount.value = { count: wc.count, label: wc.label, desc: wc.desc || '' }
  // 默认模板：key 以平台前缀开头的第一个模板，兜底第一个
  const t = apiTemplates.value.find(x => x.key.startsWith(p.key)) || apiTemplates.value[0]
  if (t) selectedTemplateKey.value = t.key
}

const onQuickConfirm = (m, opt) => {
  if (m.optionsType === 'platform') {
    currentPlatform.value = opt.raw
    applyPlatformDefault(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    askStyle()
  } else if (m.optionsType === 'style') {
    applyStyle(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    push({ role: 'ai', kind: 'confirm' })
  }
}

const askStyle = () => {
  push({
    role: 'ai',
    kind: 'quick',
    text: '想要什么风格？',
    optionsType: 'style',
    options: systemStyles.value.slice(0, 6).map(s => ({ key: s.name, label: s.name, raw: s }))
  })
}
```

注：若进入页面时 `systemStyles` 为空（异步未完成），`askStyle` 时取 `systemStyles.value` 已有值即可（`loadSystemStyles` 在 ConsoleLayout/CreateIndex 挂载链路上已调用）。

- [ ] **Step 2: 效果预览卡内容（QuickReplies 的 preview slot）**

template 中 `m.kind === 'quick'` 分支改为：

```vue
<template v-else-if="m.kind === 'quick'">
  <div class="chat-question">{{ m.text }}</div>
  <QuickReplies :options="m.options" @confirm="(opt) => onQuickConfirm(m, opt)">
    <template #preview="{ option }">
      <!-- 平台效果卡 -->
      <div v-if="m.optionsType === 'platform'" class="effect-card">
        <div class="effect-title">{{ option.label }}</div>
        <div class="effect-line">· 推荐 {{ option.raw.recommendWords }} 字，{{ platformTraitWordLabel(option.raw) }}</div>
        <div class="effect-line">· 默认模板：{{ defaultTemplateName(option.raw) }}</div>
        <div class="effect-line">· {{ option.raw.trait }}</div>
      </div>
      <!-- 风格效果卡 -->
      <div v-else class="effect-card">
        <div class="effect-title">{{ option.label }}</div>
        <div class="effect-line">· {{ option.raw.desc }}</div>
        <div class="effect-line effect-prompt">· {{ option.raw.promptSummary }}</div>
      </div>
    </template>
  </QuickReplies>
</template>
```

配套 script：

```js
const platformTraitWordLabel = (p) => {
  const presets = wordCountPresets.platform[p.key] || []
  return presets.find(x => x.count === p.recommendWords)?.label || '标准'
}

const defaultTemplateName = (p) => {
  const t = apiTemplates.value.find(x => x.key.startsWith(p.key)) || apiTemplates.value[0]
  return t?.name || '默认'
}
```

样式：

```css
.effect-title {
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.effect-line {
  color: var(--color-text-secondary);
}

.effect-prompt {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
```

- [ ] **Step 3: 确认卡片（步骤 4）**

template 新增分支：

```vue
<template v-else-if="m.kind === 'confirm'">
  <div class="confirm-card">
    <div class="confirm-title">📄 {{ customTitle }}</div>
    <div class="confirm-meta">
      {{ currentPlatform.name }} · {{ currentWordCount.count }} 字 · {{ currentStyle?.name || '默认风格' }}
    </div>
    <div class="confirm-meta">模板：{{ currentTemplate?.name }}</div>
    <div class="confirm-quota">本次消耗 1 次 · 剩余 {{ quotaRemaining }} 次</div>
    <div class="confirm-actions">
      <button class="confirm-generate" @click="handleConfirmGenerate(m)">⚡ 开始生成</button>
      <button class="confirm-edit" @click="editTopic">改主题</button>
      <button class="confirm-edit" @click="editConfig">改配置</button>
    </div>
  </div>
</template>
```

script：

```js
import { currentStyle } from '@/composables/useStyles.js'
import { useBenefits } from '@/composables/useBenefits.js'

const { benefits } = useBenefits()
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)
const currentTemplate = computed(() => apiTemplates.value.find(t => t.key === selectedTemplateKey.value) || apiTemplates.value[0])

// 改主题：重发主题问题；答完直接回确认卡（平台/风格已答保留）
const editingTopic = ref(false)
const editTopic = () => {
  editingTopic.value = true
  push({ role: 'ai', kind: 'topic' })
}

// submitTopic/onTopicCapsule 中分流：
// if (editingTopic.value) { editingTopic.value = false; push({ role:'ai', kind:'confirm' }) } else { askPlatform() }

const editConfig = () => {
  platformVisible.value = true   // 弹现有配置弹框；确认卡数据来自 composable，改完自动反映
}
```

样式：

```css
.confirm-card {
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  padding: 16px;
  background: var(--color-bg-card);
}

.confirm-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.confirm-meta {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.8;
}

.confirm-quota {
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin: 8px 0 12px;
}

.confirm-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.confirm-generate {
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 9px 22px;
  border-radius: 18px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: background 0.2s;
}

.confirm-generate:hover { background: var(--color-primary-hover); }

.confirm-edit {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  padding: 6px 8px;
}

.confirm-edit:hover { color: var(--color-primary); }
```

注：`platformVisible`/`wordCountVisible` 等需从 `useCreateForm()` 解构导入；`useCreateForm` 解构补充 `customRequirement`。

- [ ] **Step 4: E2E 验证**

扩展 `verify_guided_skeleton.py` 或新写 `tests/e2e/verify_guided_flow.py`：走完 主题→平台（确认小红书）→风格（确认第一个）→确认卡片出现且含「小红书 · 800 字」→ 点「改配置」弹平台弹框 → 关闭后确认卡仍在。mock `benefits/me` 返回额度 12 次。

```python
#!/usr/bin/env python3
"""引导模式全流程：四步走完出确认卡片，平台默认配置正确带出。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22346")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

def mock_all(page):
    page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
        "code": 0, "data": {"planKey": "pro", "planName": "专业版", "expiredAt": "",
                            "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
    page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))
    page.route("**/api/v1/user/topic-titles/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
    page.route("**/api/v1/user/styles/system**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化、短句", "prompt": "...", "scope": "通用"},
        {"bizNo": "S2", "name": "专业严谨", "description": "正式专业", "promptSummary": "术语准确", "prompt": "...", "scope": "通用"}]}))
    page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"key": "xiaohongshu-note", "name": "种草清单", "platform": "xiaohongshu"},
        {"key": "wechat-article", "name": "公众号深度文", "platform": "wechat"}]}))

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_all(page)
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        page.fill(".topic-input", "35岁被裁后，我靠副业翻身")
        page.keyboard.press("Enter")
        page.wait_for_timeout(500)

        page.click(".quick-option:has-text('小红书')")
        page.wait_for_timeout(400)
        ok_effect = page.query_selector(".quick-preview .effect-card") is not None
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        page.click(".quick-option:has-text('轻松口语')")
        page.wait_for_timeout(400)
        page.click(".quick-confirm")
        page.wait_for_timeout(600)

        ok_confirm = page.query_selector(".confirm-card") is not None
        meta = page.inner_text(".confirm-card") if ok_confirm else ""
        ok_meta = ("小红书" in meta) and ("800" in meta) and ("轻松口语" in meta)
        page.screenshot(path=f"{SHOTS}/guided_confirm.png")

        page.click("text=改配置")
        page.wait_for_timeout(600)
        ok_modal = page.query_selector(".platform-modal .ant-modal-content") is not None
        page.keyboard.press("Escape")
        page.wait_for_timeout(400)
        ok_back = page.query_selector(".confirm-card") is not None

        for name, ok in [("effect-card", ok_effect), ("confirm-card", ok_confirm),
                         ("confirm-meta", ok_meta), ("edit-config-modal", ok_modal), ("back-to-confirm", ok_back)]:
            print(("PASS " if ok else "FAIL ") + name)
        browser.close()
        if not all([ok_effect, ok_confirm, ok_meta, ok_modal, ok_back]):
            raise SystemExit("FAILED")
        print("ALL PASS")

main()
```

注：mock 路径（`styles/system`、`export-templates`）以实现文件里实际 API 路径为准微调； envelope 均为 `{code:0,data:...}`。

Run: `python3 tests/e2e/verify_guided_flow.py`
Expected: ALL PASS。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/create/ tests/e2e/verify_guided_flow.py
git commit -m "feat(create): 引导模式步骤 2-4 — 平台/风格效果卡 + 确认卡片 + 改主题/改配置"
```

---

### Task 7: GuidedChat 生成进度 / 结果卡片 / 额度拦截 / 重试

**Files:**
- Modify: `project/user/web/src/views/console/create/GuidedChat.vue`

**Interfaces:**
- Consumes: `submitGeneration`/`getGenerationTask`/`retryGenerationTask`（`@/api/generation.js`）；`useGenerationQueue().loadQueue`
- Produces: `stageText(pct)` 阶段文案映射；`kind: 'progress'` 与 `kind: 'result'` 消息

- [ ] **Step 1: 额度拦截（初始化时）**

script 中初始化段改为：

```js
import { useRouter } from 'vue-router'
import { loadBenefits } from '@/composables/useBenefits.js'

const router = useRouter()
const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)

// 初始化：先确保权益已加载，再决定第一条消息
onMounted(async () => {
  await loadBenefits()
  if (quotaTotal.value <= 0) {
    push({ role: 'ai', kind: 'quota', text: '开通会员后才能使用 AI 生成文章', actionText: '去开通会员', action: () => router.push('/pricing') })
  } else if (quotaRemaining.value <= 0) {
    push({ role: 'ai', kind: 'quota', text: '本月额度已用完，升级会员可获得更多额度', actionText: '去升级', action: () => router.push('/pricing') })
  } else {
    push({ role: 'ai', kind: 'topic' })
  }
})
```

（删除 Task 5 中顶层的 `push({ role: 'ai', kind: 'topic' })`，改到 onMounted；`benefits`/`quotaRemaining` 前已定义。）

template 新增分支：

```vue
<template v-else-if="m.kind === 'quota'">
  <div class="chat-question">{{ m.text }}</div>
  <button class="confirm-generate" @click="m.action">{{ m.actionText }}</button>
</template>
```

- [ ] **Step 2: 生成 + 进度卡片**

script：

```js
import { submitGeneration, getGenerationTask, retryGenerationTask } from '@/api/generation.js'
import { useGenerationQueue } from './useGenerationQueue.js'
import { message } from 'ant-design-vue'

const { loadQueue } = useGenerationQueue()

export const stageText = (pct) =>
  pct < 30 ? '正在生成大纲…' : pct < 70 ? '正在撰写正文…' : pct < 95 ? '正在排版润色…' : '即将完成…'

let pollTimer = null
const stopTaskPoll = () => { clearInterval(pollTimer); pollTimer = null }
onUnmounted(stopTaskPoll)

const handleConfirmGenerate = async (confirmMsg) => {
  if (!customTitle.value.trim()) return
  try {
    const task = await submitGeneration({
      title: customTitle.value,
      description: customRequirement.value,
      platform: currentPlatform.value?.key || '',
      styleRef: currentStyle.value?.id || currentStyle.value?.name || '',
      wordCount: currentWordCount.value?.count || 800,
      template: currentTemplate.value?.key || 'wechat'
    })
    // 确认卡原地替换为进度卡
    Object.assign(confirmMsg, { kind: 'progress', taskId: task.id, progress: 0, status: 'generating' })
    loadBenefits()   // 刷新剩余额度
    loadQueue()      // 队列徽章同步
    pollTask(confirmMsg, task.id)
  } catch (e) {
    message.error(e?.message || '提交失败，请稍后重试')
  }
}

const pollTask = (msg, taskId) => {
  stopTaskPoll()
  pollTimer = setInterval(async () => {
    try {
      const t = await getGenerationTask(taskId)
      msg.progress = t.progressPct || 0
      if (t.status === 2) {
        stopTaskPoll()
        Object.assign(msg, { kind: 'result', status: 'completed' })
        loadQueue()
      } else if (t.status === 3) {
        stopTaskPoll()
        Object.assign(msg, { status: 'failed', failedReason: t.failedReason || '生成失败' })
        loadQueue()
      }
    } catch { /* 单次轮询失败忽略，下轮继续 */ }
  }, 3000)
}

const retryTask = async (msg) => {
  try {
    await retryGenerationTask(msg.taskId)
    Object.assign(msg, { status: 'generating', progress: 0 })
    pollTask(msg, msg.taskId)
  } catch (e) {
    message.error(e?.message || '重试失败，请稍后再试')
  }
}

const restart = () => {
  stopTaskPoll()
  messages.value = []
  push({ role: 'ai', kind: 'topic' })
}
```

template 新增分支：

```vue
<!-- 进度卡片 -->
<template v-else-if="m.kind === 'progress'">
  <div class="confirm-card">
    <div class="confirm-title">📄 {{ customTitle }}</div>
    <template v-if="m.status === 'generating'">
      <div class="progress-bar chat-progress">
        <div class="progress-fill" :style="{ width: Math.min(100, Math.round(m.progress)) + '%' }"></div>
      </div>
      <div class="progress-stage">{{ stageText(m.progress) }} {{ Math.min(100, Math.round(m.progress)) }}%</div>
    </template>
    <template v-else>
      <div class="failed-text">❌ {{ m.failedReason }}</div>
      <div class="confirm-actions">
        <button class="confirm-generate" @click="retryTask(m)">重试</button>
      </div>
    </template>
  </div>
</template>

<!-- 结果卡片 -->
<template v-else-if="m.kind === 'result'">
  <div class="confirm-card">
    <div class="confirm-title">✅ {{ customTitle }}</div>
    <div class="confirm-meta">已生成完成</div>
    <div class="confirm-actions">
      <button class="confirm-generate" @click="router.push('/console/works')">查看文章</button>
      <button class="confirm-edit" @click="restart">再写一篇</button>
    </div>
  </div>
</template>
```

样式（复用 `.progress-bar`/`.progress-fill` 全局规则，若仍在 QueueDrawer scoped 中则把这两条移到 `src/styles/index.css` 全局）：

```css
.chat-progress {
  height: 6px;
  background: rgba(255, 36, 66, 0.15);
  border-radius: 3px;
  overflow: hidden;
  margin: 12px 0 8px;
}

.chat-progress .progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff2442, #ff6b81);
  border-radius: 3px;
  transition: width 0.3s;
}

.progress-stage {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.failed-text {
  color: var(--color-error);
  font-size: 13px;
  margin: 8px 0 12px;
}
```

- [ ] **Step 3: E2E 验证**

写 `tests/e2e/verify_guided_generate.py`：mock 提交返回 task id、单任务轮询依次返回 45% → 100%(status 2)；断言进度卡 → 结果卡；另跑一组 status 3 → 失败 + 重试按钮出现：

```python
#!/usr/bin/env python3
"""引导模式生成链路：确认→进度卡→结果卡；失败态→重试按钮。额度 0 拦截。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22346")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

def mock_common(page, quota_remaining=12, quota_value="50"):
    page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
        "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                            "benefits": [{"code": "ai_article_quota", "value": quota_value, "remaining": quota_remaining}]}}))
    page.route("**/api/v1/user/topic-titles/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
    page.route("**/api/v1/user/styles/system**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化", "prompt": "x", "scope": "通用"}]}))
    page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"key": "wechat-article", "name": "公众号深度文", "platform": "wechat"}]}))

def run_flow_to_confirm(page):
    page.goto(f"{BASE}/console/create", wait_until="networkidle")
    page.wait_for_timeout(1200)
    page.fill(".topic-input", "测试主题")
    page.keyboard.press("Enter")
    page.wait_for_timeout(400)
    page.click(".quick-option:has-text('公众号')")
    page.click(".quick-confirm")
    page.wait_for_timeout(400)
    page.click(".quick-option:has-text('轻松口语')")
    page.click(".quick-confirm")
    page.wait_for_timeout(500)

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # --- 场景 1：成功链路 ---
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_common(page)
        page.route("**/api/v1/user/generation-tasks", lambda r: (
            r.fulfill(json={"code": 0, "data": {"id": 900, "status": 1, "progressPct": 0}})
            if r.request.method == "POST" else
            r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}})))
        poll_state = {"n": 0}
        def on_poll(route):
            poll_state["n"] += 1
            done = poll_state["n"] >= 2
            route.fulfill(json={"code": 0, "data": {
                "id": 900, "status": 2 if done else 1, "progressPct": 100 if done else 45,
                "title": "测试主题", "inputParam": {}}})
        page.route("**/api/v1/user/generation-tasks/900", on_poll)
        run_flow_to_confirm(page)
        page.click(".confirm-generate")
        page.wait_for_timeout(1000)
        ok_progress = page.query_selector(".chat-progress") is not None
        page.wait_for_timeout(4500)  # 等第二轮轮询
        ok_result = page.query_selector("text=已生成完成") is not None
        page.screenshot(path=f"{SHOTS}/guided_result.png")
        print("PASS progress" if ok_progress else "FAIL progress")
        print("PASS result" if ok_result else "FAIL result")
        page.close()

        # --- 场景 2：额度 0 拦截 ---
        page2 = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_common(page2, quota_remaining=0)
        page2.goto(f"{BASE}/console/create", wait_until="networkidle")
        page2.wait_for_timeout(1200)
        ok_quota = page2.query_selector("text=本月额度已用完") is not None
        ok_no_input = page2.query_selector(".topic-input") is None
        print("PASS quota-block" if ok_quota else "FAIL quota-block")
        print("PASS no-input" if ok_no_input else "FAIL no-input")
        page2.screenshot(path=f"{SHOTS}/guided_quota_block.png")
        page2.close()

        browser.close()
        if not all([ok_progress, ok_result, ok_quota, ok_no_input]):
            raise SystemExit("FAILED")
        print("ALL PASS")

main()
```

Run: `python3 tests/e2e/verify_guided_generate.py`
Expected: ALL PASS。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/console/create/ tests/e2e/verify_guided_generate.py
git commit -m "feat(create): 引导模式生成链路 — 进度卡/结果卡/额度拦截/失败重试"
```

---

### Task 8: 移动端适配 + 深色走查（两模式）

**Files:**
- Modify: `project/user/web/src/views/console/create/GuidedChat.vue`
- Modify: `project/user/web/src/views/console/create/MinimalPanel.vue`
- Modify: `project/user/web/src/views/console/create/QueueDrawer.vue`
- Modify: `project/user/web/src/views/console/create/QuickReplies.vue`

**Interfaces:**
- Consumes: `useIsMobile`（`@/composables/useMobile.js`）
- Produces: 无新接口

- [ ] **Step 1: 移动端细则补齐**

对照 spec 逐项检查并补齐（部分规则已随组件任务内置，这里做走查补漏）：

- GuidedChat：`.chat-list` padding 16px；AI 气泡 max-width 85%（ChatMessage 已内置）；主题输入行 sticky 底部（已内置，验证 iOS 键盘顶起表现）
- MinimalPanel：`.minimal-topbar-right` 中 `.quota-text` 移动端隐藏（已内置）→ 改为在 QueueDrawer 顶部显示一行额度：QueueDrawer template 的 `title` slot 改为：
  ```vue
  <template #title>
    <div class="drawer-title-row">
      <span>生成队列</span>
      <span v-if="isMobile" class="quota-text">本月剩余 {{ quotaRemaining }} / {{ quotaTotal }} 次</span>
    </div>
  </template>
  ```
  （QueueDrawer 引入 `useBenefits` + 两个 computed）
- QueueDrawer：`isMobile` 时 `width="100%"`（已内置）
- 极简模式顶部行移动端：标题 20px，按钮字号 12px、padding 收窄（加 media 规则）

- [ ] **Step 2: E2E 移动视口验证**

写 `tests/e2e/verify_create_mobile.py`：viewport 390×844（iPhone 14），引导模式走两步 + 极简模式截图 + 抽屉全屏断言：

```python
#!/usr/bin/env python3
"""移动端：引导模式聊天流、极简模式卡片、队列抽屉全屏。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22346")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"
VIEW = {"width": 390, "height": 844}

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport=VIEW, is_mobile=True, has_touch=True)
        page.route("**/api/v1/user/**", lambda r: r.fulfill(json={"code": 0, "data": {}}))
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        ok_chat = page.query_selector(".guided-chat") is not None
        page.screenshot(path=f"{SHOTS}/mobile_guided.png")

        page.click("text=熟手模式")
        page.wait_for_timeout(600)
        ok_minimal = page.query_selector(".minimal-panel") is not None
        page.screenshot(path=f"{SHOTS}/mobile_minimal.png")

        page.click("text=队列")
        page.wait_for_timeout(800)
        drawer = page.query_selector(".ant-drawer-content-wrapper")
        w = page.evaluate("e => e.offsetWidth", drawer) if drawer else 0
        ok_full = w >= 390
        page.screenshot(path=f"{SHOTS}/mobile_drawer.png")

        for name, ok in [("guided", ok_chat), ("minimal", ok_minimal), ("drawer-fullscreen", ok_full)]:
            print(("PASS " if ok else "FAIL ") + name)
        browser.close()
        if not all([ok_chat, ok_minimal, ok_full]):
            raise SystemExit("FAILED")
        print("ALL PASS")

main()
```

Run: `python3 tests/e2e/verify_create_mobile.py`
Expected: ALL PASS + 肉眼走查三张截图。

- [ ] **Step 3: 深色走查**

写 `tests/e2e/verify_create_dark.py`：浅色/深色各截 引导模式、极简模式、队列抽屉、确认卡片 4 组，人工走查有无写死白色残留（重点：`.chat-bubble`、`.quick-option`、`.hero-card`、`.confirm-card` 应全部走变量）。发现写死值 → 改 CSS 变量。

Run: `python3 tests/e2e/verify_create_dark.py`
Expected: 截图无明显白块/黑字不清。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/console/create/ tests/e2e/verify_create_mobile.py tests/e2e/verify_create_dark.py
git commit -m "feat(create): 双模式移动端适配 + 深色主题走查修正"
```

---

### Task 9: 死代码清理 + 最终回归

**Files:**
- Modify: `project/user/web/src/views/console/CreateIndex.vue`（最终形态应 < 150 行）
- Modify: `project/user/web/src/views/console/create/*.vue`
- Delete: 无独立文件删除（CreateIndex.vue 保留为容器）

**Interfaces:**
- Consumes: 全部前序任务
- Produces: 无

- [ ] **Step 1: 死代码扫描**

逐项 grep 确认并删除：

```bash
cd project/user/web/src
# 旧样式/旧逻辑残留（应全部无引用后删除）
grep -rn "queue-panel\b" views/console/CreateIndex.vue          # 壳文件不应再有
grep -rn "miniQueueList\|loadMiniQueue\|miniStatusText" views/  # 应为 0
grep -rn "hero-input\|smart-defaults" views/                    # 旧类名应为 0
grep -rn "\.quota-card\|\.mode-tabs\|\.topic-section\|\.input-section\|\.settings-toolbar\|\.action-bar\|\.queue-export-btn" views/  # 应为 0
```

同时检查：

- CreateIndex.vue 中不再用到的 import（如 `InboxOutlined` 等队列图标已进 QueueDrawer）删除
- 4 个弹框相关旧函数（`openPlatformModal` 等直接写 `xxxVisible.value = true` 的内联逻辑）无残留
- `stylePresets`（风格编辑器快速填充模板）若在 StyleModal 提取中重复定义 → 保留 StyleModal 内一份

- [ ] **Step 2: 构建 + 全量 E2E 回归**

Run: `cd project/user/web && npx vite build 2>&1 | tail -3`
Expected: 构建成功，无警告新增

依次跑：

```bash
python3 tests/e2e/verify_create_modals_after_extract.py
python3 tests/e2e/verify_queue_drawer.py
python3 tests/e2e/verify_minimal_panel.py
python3 tests/e2e/verify_guided_skeleton.py
python3 tests/e2e/verify_guided_flow.py
python3 tests/e2e/verify_guided_generate.py
python3 tests/e2e/verify_create_mobile.py
```

Expected: 全部 ALL PASS

- [ ] **Step 3: 人工走查清单**

浏览器实际操作一遍（登录态）：

1. 默认进引导模式，走完四步真实生成一篇 → 进度卡 → 结果卡 → 查看文章跳转
2. 切熟手模式，刷新保持；一体化卡片聚焦微光；生成后抽屉自动滑出
3. 灵感胶囊：引导/极简两模式点击行为
4. 草稿保存/恢复（极简模式）
5. 移动端（DevTools 设备模拟）走一遍

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/
git commit -m "chore(create): 创作页重设计收尾 — 死代码清理"
```

---

## Self-Review 结论

- **Spec 覆盖**：双模式/切换记忆 ✓(T1/T5)、固定问题流 ✓(T5/T6)、两段式效果卡 ✓(T5/T6)、确认卡 ✓(T6)、进度/结果/重试 ✓(T7)、额度拦截 ✓(T7)、队列抽屉+自动滑出 ✓(T3/T4)、一体化卡片 ✓(T4)、灵感胶囊两模式 ✓(T2/T5)、光晕/聚焦微光 ✓(T4)、移动端 ✓(T5/T8)、深色 ✓(T8)、E2E 四脚本 ✓(分散各任务)、YAGNI（无真 AI 多轮/无对话草稿）✓。
- **Placeholder 扫描**：无 TBD/TODO；mock 路径一处注明「以实际 API 路径微调」（E2E 迭代性质，可接受）。
- **类型一致性**：`useCreateForm`/`useGenerationQueue` 接口在 T1 定义，T2-T8 引用一致；`queueOpen` 在 T3 Step1 补入 composable；消息模型 `kind` 取值 `text|topic|quick|quota|confirm|progress|result` 前后一致。
