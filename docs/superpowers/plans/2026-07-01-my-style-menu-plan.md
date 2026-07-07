# “我的风格”独立菜单实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在控制台侧边栏新增“我的风格”独立页面，与创作页风格弹框共享同一份风格状态，实现提前设计、多入口管理。

**Architecture：** 新增 `useStyles.js` 组合式函数承载系统预设、我的风格、当前风格及 CRUD；新增 `StylesIndex.vue` 作为独立管理页；`ConsoleLayout.vue` 增加侧边栏入口；`router/index.js` 注册新路由；`CreateIndex.vue` 仅将风格弹框的数据源改为引用 `useStyles.js`，模板保持不动。

**Tech Stack：** Vue 3 Composition API、Vue Router 4、Ant Design Vue 4、@ant-design/icons-vue、Vite、Playwright。

## Global Constraints

- 数据仅保存在前端内存，不写入 localStorage，便于后续替换为后端 API。
- 创作页风格弹框模板与交互尽量不动，只调整数据源引用。
- 风格名称最多 20 字，提示词最多 1000 字，名称去重不区分大小写。
- 系统预设只读；我的风格支持新建、编辑、删除。
- 新增端到端 Playwright 测试验证关键路径。

---

## File Structure

| 文件 | 操作 | 职责 |
|---|---|---|
| `project/user/web/src/composables/useStyles.js` | 创建 | 共享风格状态、系统预设、CRUD、校验 |
| `project/user/web/src/views/console/StylesIndex.vue` | 创建 | 独立“我的风格”管理页面 |
| `project/user/web/src/router/index.js` | 修改 | 注册 `/console/styles` 路由 |
| `project/user/web/src/views/console/ConsoleLayout.vue` | 修改 | 侧边栏新增“我的风格”入口 |
| `project/user/web/src/views/console/CreateIndex.vue` | 修改 | 风格弹框改用 `useStyles.js` 数据 |
| `tests/e2e/verify_my_style_page.py` | 创建 | 端到端验证脚本 |

---

### Task 1: 创建共享状态 `useStyles.js`

**Files:**
- Create: `project/user/web/src/composables/useStyles.js`

**Interfaces:**
- Consumes: 无
- Produces:
  - `systemStyles`：系统预设风格数组（只读）。
  - `myStyles`：`ref([])`，自定义风格数组。
  - `currentStyle`：`ref(systemStyles[0])`，当前应用的风格。
  - `applyStyle(style)`：设置当前风格。
  - `addCustomStyle(style)`：新增自定义风格。
  - `updateCustomStyle(oldName, style)`：更新自定义风格。
  - `removeCustomStyle(name)`：删除自定义风格。
  - `isStyleNameExists(name, excludeName?)`：检查名称是否已存在。

- [ ] **Step 1: 创建目录并写入 `useStyles.js`**

```bash
mkdir -p project/user/web/src/composables
```

```javascript
// project/user/web/src/composables/useStyles.js
import { ref } from 'vue'

export const systemStyles = [
  {
    name: '年度总结',
    desc: '回顾、复盘、展望',
    promptSummary: '语气：回顾性、感恩 + 数据自省\n结构：成绩 + 反思 + 明年目标\n长度：1500-2500 字，带小标题分章',
    prompt: '你是一位擅长年度复盘与展望的写手。文章语气应回顾性、感恩且带数据自省。结构分为：成绩回顾、深度反思、明年目标。长度 1500-2500 字，使用小标题分章。'
  },
  {
    name: '产品评测',
    desc: '客观、数据驱动、多角度对比',
    promptSummary: '语气：客观中立、有理有据\n结构：外观 + 性能 + 体验 + 总结\n要素：必带参数对比表 + 优缺点',
    prompt: '你是一位客观中立的产品评测作者。文章需数据驱动、多角度对比，结构分为外观、性能、体验、总结，必须包含参数对比表和优缺点分析。'
  },
  {
    name: '情感散文',
    desc: '细腻、共情、个人化表达',
    promptSummary: '语气：细腻、温暖、第一人称\n修辞：善用比喻、意象、留白\n结构：场景 + 情绪 + 升华',
    prompt: '你擅长写情感散文。使用细腻温暖的第一人称，善用比喻、意象和留白。结构为：场景描写、情绪铺陈、主题升华。'
  },
  {
    name: '职场干货',
    desc: '实操性强、结构清晰',
    promptSummary: '语气：专业务实、老板视角\n结构：痛点 + 方案 + 步骤 + 案例\n要素：可执行的 checklist',
    prompt: '你是一位专业务实的职场作者。从老板视角出发，结构为痛点、方案、步骤、案例，必须提供可执行的 checklist。'
  },
  {
    name: '热点评论',
    desc: '观点鲜明、论据紧凑',
    promptSummary: '语气：犀利、有态度\n结构：事件概述 + 观点 + 论据 + 结论\n要素：引用数据或权威观点',
    prompt: '你是一位观点鲜明的热点评论员。语气犀利有态度，结构为事件概述、核心观点、论据支撑、结论，需引用数据或权威观点。'
  },
  {
    name: '知识科普',
    desc: '深入浅出、逻辑清晰',
    promptSummary: '语气：亲和、易懂\n结构：问题 + 原理 + 案例 + 总结\n要素：避免术语堆砌，善用类比',
    prompt: '你是一位知识科普作者。语气亲和易懂，结构为提出问题、解释原理、给出案例、总结要点。避免术语堆砌，善用类比。'
  },
  {
    name: '营销转化',
    desc: '引导行动、强说服',
    promptSummary: '语气：紧迫感 + 利益点突出\n结构：痛点共鸣 + 方案 + 案例 + CTA\n要素：必带限时/优惠/倒计时',
    prompt: '你是一位营销转化写手。语气紧迫、利益点突出，结构为痛点共鸣、解决方案、案例证明、行动号召（CTA），必须包含限时/优惠/倒计时要素。'
  },
  {
    name: '故事叙事',
    desc: '沉浸感、有冲突与转折',
    promptSummary: '语气：克制、文学化\n结构：起承转合 + 人物对话\n要素：场景细节 + 心理活动',
    prompt: '你是一位故事叙事作者。语气克制文学化，结构为起承转合，包含人物对话，注重场景细节和心理活动描写。'
  }
]

export const myStyles = ref([])

export const currentStyle = ref(systemStyles[0])

export const applyStyle = (style) => {
  currentStyle.value = style
}

export const addCustomStyle = (style) => {
  myStyles.value.push({
    name: style.name.trim(),
    desc: style.desc || '自定义风格',
    prompt: style.prompt.trim(),
    count: 0
  })
}

export const updateCustomStyle = (oldName, style) => {
  const idx = myStyles.value.findIndex(x => x.name === oldName)
  if (idx > -1) {
    myStyles.value[idx] = {
      ...myStyles.value[idx],
      name: style.name.trim(),
      desc: style.desc || '自定义风格',
      prompt: style.prompt.trim()
    }
  }
}

export const removeCustomStyle = (name) => {
  const idx = myStyles.value.findIndex(x => x.name === name)
  if (idx > -1) {
    myStyles.value.splice(idx, 1)
  }
  if (currentStyle.value && currentStyle.value.name === name) {
    currentStyle.value = systemStyles[0]
  }
}

export const isStyleNameExists = (name, excludeName = null) => {
  const target = name.trim().toLowerCase()
  if (!target) return false
  if (excludeName && target === excludeName.trim().toLowerCase()) return false
  const inSystem = systemStyles.some(s => s.name.trim().toLowerCase() === target)
  const inCustom = myStyles.value.some(s => s.name.trim().toLowerCase() === target)
  return inSystem || inCustom
}
```

- [ ] **Step 2: 提交**

```bash
git add project/user/web/src/composables/useStyles.js
git commit -m "feat(styles): 添加共享风格状态 useStyles.js

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 注册路由并添加侧边栏入口

**Files:**
- Modify: `project/user/web/src/router/index.js`
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`

**Interfaces:**
- Consumes: 无
- Produces: `/console/styles` 路由可用；侧边栏显示“我的风格”入口。

- [ ] **Step 1: 在 `router/index.js` 的 `/console` children 中新增路由**

在 `path: 'works'` 同级位置插入：

```javascript
{
  path: 'styles',
  name: 'ConsoleStyles',
  component: () => import('@/views/console/StylesIndex.vue')
}
```

完整 children 片段：

```javascript
children: [
  {
    path: '',
    redirect: '/console/create'
  },
  {
    path: 'create',
    name: 'ConsoleCreate',
    component: () => import('@/views/console/CreateIndex.vue')
  },
  {
    path: 'queue',
    redirect: '/console/works'
  },
  {
    path: 'works',
    name: 'ConsoleWorks',
    component: () => import('@/views/console/WorksIndex.vue')
  },
  {
    path: 'styles',
    name: 'ConsoleStyles',
    component: () => import('@/views/console/StylesIndex.vue')
  },
  {
    path: 'preview',
    name: 'ConsolePreview',
    component: () => import('@/views/console/PreviewIndex.vue')
  }
]
```

- [ ] **Step 2: 在 `ConsoleLayout.vue` 中导入图标并新增侧边栏项**

将导入块：

```javascript
import {
  EditOutlined,
  FolderOutlined
} from '@ant-design/icons-vue'
```

改为：

```javascript
import {
  EditOutlined,
  FolderOutlined,
  SmileOutlined
} from '@ant-design/icons-vue'
```

将 `navItems` 数组：

```javascript
const navItems = [
  { path: '/console/create', label: '创作', icon: EditOutlined },
  { path: '/console/works', label: '我的作品', icon: FolderOutlined }
]
```

改为：

```javascript
const navItems = [
  { path: '/console/create', label: '创作', icon: EditOutlined },
  { path: '/console/works', label: '我的作品', icon: FolderOutlined },
  { path: '/console/styles', label: '我的风格', icon: SmileOutlined }
]
```

- [ ] **Step 3: 启动开发服务器并验证侧边栏入口**

```bash
cd project/user/web && npm run dev -- --port 22345 --host
```

浏览器访问 `http://localhost:22345/console/styles`（若端口被占用则使用实际端口），确认：
- 侧边栏出现“我的风格”。
- 点击后 URL 变为 `/console/styles`。
- 页面显示 `StylesIndex.vue` 内容（当前可能为空或占位）。

- [ ] **Step 4: 提交**

```bash
git add project/user/web/src/router/index.js project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(styles): 注册 /console/styles 路由并添加侧边栏入口

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 创建 `StylesIndex.vue` 页面骨架

**Files:**
- Create: `project/user/web/src/views/console/StylesIndex.vue`

**Interfaces:**
- Consumes: `useStyles.js` 中的 `systemStyles`、`myStyles`、`currentStyle`、`applyStyle`。
- Produces: 独立页面可渲染标题、tab 切换、空状态、两个风格列表。

- [ ] **Step 1: 创建 `StylesIndex.vue` 骨架**

```vue
<template>
  <div class="styles-index">
    <div class="styles-header">
      <div>
        <h2 class="styles-title">我的风格</h2>
        <p class="styles-subtitle">提前设计你的专属写作风格，创作时一键选用</p>
      </div>
    </div>

    <div class="styles-tabs">
      <button
        :class="['styles-tab', { active: activeTab === 'my' }]"
        @click="activeTab = 'my'; editorMode = false"
      >
        我的风格
      </button>
      <button
        :class="['styles-tab', { active: activeTab === 'system' }]"
        @click="activeTab = 'system'; editorMode = false"
      >
        系统预设
      </button>
    </div>

    <!-- 我的风格 -->
    <div v-show="activeTab === 'my'" class="styles-content">
      <div v-if="myStyles.length === 0" class="styles-empty">
        <a-empty description="还没有自定义风格">
          <button class="empty-btn" @click="goToCreate">去创建一个</button>
        </a-empty>
      </div>
      <div v-else class="styles-grid">
        <div class="style-add-card" @click="goToCreate">
          <div class="style-add-icon">+</div>
          <div class="style-add-text">新建我的风格</div>
        </div>
        <div
          v-for="(s, idx) in myStyles"
          :key="s.name"
          class="style-card"
        >
          <div class="style-card-title">{{ s.name }}</div>
          <div class="style-card-desc">{{ s.desc }} · 已用 {{ s.count }} 次</div>
          <div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
          <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
          <div class="style-card-actions">
            <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
            <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
              {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
            </button>
            <button class="style-action-btn" @click.stop="goToEdit(s)">编辑</button>
            <button class="style-action-btn style-del-btn" @click.stop="deleteStyle(s.name)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 系统预设 -->
    <div v-show="activeTab === 'system'" class="styles-content">
      <div class="styles-grid">
        <div
          v-for="s in systemStyles"
          :key="s.name"
          class="style-card"
        >
          <div class="style-card-title">{{ s.name }}</div>
          <div class="style-card-desc">{{ s.desc }}</div>
          <div class="style-card-prompt">{{ s.promptSummary }}</div>
          <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
          <div class="style-card-actions">
            <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
            <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
              {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  systemStyles,
  myStyles,
  applyStyle,
  removeCustomStyle
} from '@/composables/useStyles.js'

const router = useRouter()
const activeTab = ref('my')
const editorMode = ref(false)
const expandedNames = ref(new Set())

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const togglePrompt = (name) => {
  const set = new Set(expandedNames.value)
  if (set.has(name)) {
    set.delete(name)
  } else {
    set.add(name)
  }
  expandedNames.value = set
}

const goToCreate = () => {
  // 将在 Task 5 中实现
}

const goToEdit = (style) => {
  // 将在 Task 5 中实现
}

const useStyle = (style) => {
  applyStyle(style)
  router.push('/console/create')
}

const deleteStyle = (name) => {
  removeCustomStyle(name)
}
</script>

<style scoped>
.styles-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.styles-header {
  margin-bottom: 20px;
}

.styles-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.styles-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.styles-tabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  margin-bottom: 20px;
  width: fit-content;
}

.styles-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.styles-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.styles-empty {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}

.empty-btn {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  margin-top: 12px;
}

.empty-btn:hover {
  background: #e61e3a;
}

.styles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.style-add-card {
  border: 1px dashed #d9d9d9;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 160px;
}

.style-add-card:hover {
  border-color: #07c160;
  background: #f6ffed;
}

.style-add-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5f5f5;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.style-add-text {
  font-size: 14px;
  color: #595959;
}

.style-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
}

.style-card:hover {
  border-color: #ffd1d9;
  box-shadow: 0 2px 12px rgba(255, 36, 66, 0.08);
}

.style-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.style-card-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 10px;
}

.style-card-prompt {
  font-size: 12px;
  color: #bfbfbf;
  line-height: 1.5;
  margin-bottom: 12px;
  flex: 1;
  white-space: pre-line;
}

.style-prompt-full {
  font-size: 12px;
  color: #595959;
  line-height: 1.6;
  background: #fafafa;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
  white-space: pre-line;
}

.style-card-actions {
  display: flex;
  gap: 8px;
}

.style-action-btn {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.style-action-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.style-action-btn.style-del-btn:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}
</style>
```

- [ ] **Step 2: 验证页面渲染**

开发服务器保持运行，访问 `http://localhost:22345/console/styles`：
- 确认“我的风格”和“系统预设”两个 tab。
- 切换 tab，系统预设应显示 8 张卡片。
- “我的风格”tab 应显示空状态（因为 `myStyles` 为空）。

- [ ] **Step 3: 提交**

```bash
git add project/user/web/src/views/console/StylesIndex.vue
git commit -m "feat(styles): 创建我的风格独立页面骨架

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: 在 `StylesIndex.vue` 中实现编辑器子视图

**Files:**
- Modify: `project/user/web/src/views/console/StylesIndex.vue`

**Interfaces:**
- Consumes: `systemStyles`、`myStyles`、`addCustomStyle`、`updateCustomStyle`、`isStyleNameExists`。
- Produces: 页面内可新建/编辑自定义风格并返回列表。

- [ ] **Step 1: 在 `<template>` 的 `styles-tabs` 后替换两个 tab 的内容为动态渲染**

将两个 `v-show` 的 `styles-content` 块替换为以下结构（保留原列表逻辑，新增 `editorMode` 判断）：

```vue
<!-- 我的风格 -->
<div v-show="activeTab === 'my'" class="styles-content">
  <div v-if="editorMode" class="style-editor">
    <div class="style-editor-header">
      <button class="style-editor-back" @click="goBack">← 返回</button>
      <div class="style-editor-title">{{ editingStyle.originalName ? '编辑提示词' : '新建我的风格' }}</div>
    </div>
    <div class="style-editor-form">
      <div class="style-editor-field">
        <label class="style-editor-label">风格名称 <span class="required">*</span></label>
        <input
          v-model="editingStyle.name"
          type="text"
          class="style-editor-input"
          placeholder="例如：我的小红书风"
          maxlength="20"
        />
        <div v-if="errors.name" class="style-editor-error">{{ errors.name }}</div>
      </div>
      <div class="style-editor-field">
        <label class="style-editor-label">风格提示词 <span class="required">*</span></label>
        <textarea
          v-model="editingStyle.prompt"
          class="style-editor-textarea"
          placeholder="描述你希望 AI 采用的语气、结构、用词习惯等..."
          rows="5"
        ></textarea>
        <div class="style-editor-counter" :class="{ over: editingStyle.prompt.length > 1000 }">
          {{ editingStyle.prompt.length }} / 1000
        </div>
        <div v-if="errors.prompt" class="style-editor-error">{{ errors.prompt }}</div>
      </div>
      <div class="style-editor-presets">
        <div class="style-editor-preset-label">快速填充模板：</div>
        <div class="style-editor-preset-list">
          <div
            v-for="preset in systemStyles"
            :key="preset.name"
            class="style-preset-card"
            @click="editingStyle.prompt = preset.prompt"
          >
            <div class="style-preset-title">{{ preset.name }}</div>
            <div class="style-preset-desc">{{ preset.desc }}</div>
          </div>
        </div>
      </div>
      <button
        class="save-style-btn"
        :disabled="!isFormValid"
        @click="saveStyle"
      >
        保存
      </button>
    </div>
  </div>

  <div v-else>
    <div v-if="myStyles.length === 0" class="styles-empty">
      <a-empty description="还没有自定义风格">
        <button class="empty-btn" @click="goToCreate">去创建一个</button>
      </a-empty>
    </div>
    <div v-else class="styles-grid">
      <div class="style-add-card" @click="goToCreate">
        <div class="style-add-icon">+</div>
        <div class="style-add-text">新建我的风格</div>
      </div>
      <div
        v-for="s in myStyles"
        :key="s.name"
        class="style-card"
      >
        <div class="style-card-title">{{ s.name }}</div>
        <div class="style-card-desc">{{ s.desc }} · 已用 {{ s.count }} 次</div>
        <div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
        <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
        <div class="style-card-actions">
          <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
          <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
            {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
          </button>
          <button class="style-action-btn" @click.stop="goToEdit(s)">编辑</button>
          <button class="style-action-btn style-del-btn" @click.stop="deleteStyle(s.name)">删除</button>
        </div>
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 2: 在 `<script setup>` 中新增编辑器状态与逻辑**

替换 `<script setup>` 块为：

```javascript
<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  systemStyles,
  myStyles,
  applyStyle,
  addCustomStyle,
  updateCustomStyle,
  removeCustomStyle,
  isStyleNameExists
} from '@/composables/useStyles.js'

const router = useRouter()
const activeTab = ref('my')
const editorMode = ref(false)
const expandedNames = ref(new Set())

const editingStyle = reactive({
  originalName: '',
  name: '',
  prompt: ''
})

const errors = reactive({
  name: '',
  prompt: ''
})

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const togglePrompt = (name) => {
  const set = new Set(expandedNames.value)
  if (set.has(name)) {
    set.delete(name)
  } else {
    set.add(name)
  }
  expandedNames.value = set
}

const validate = () => {
  errors.name = ''
  errors.prompt = ''

  const name = editingStyle.name.trim()
  const prompt = editingStyle.prompt.trim()
  let valid = true

  if (!name) {
    errors.name = '请输入风格名称'
    valid = false
  } else if (name.length > 20) {
    errors.name = '风格名称最多 20 字'
    valid = false
  } else if (isStyleNameExists(name, editingStyle.originalName)) {
    errors.name = '该风格名称已存在'
    valid = false
  }

  if (!prompt) {
    errors.prompt = '请输入风格提示词'
    valid = false
  } else if (prompt.length > 1000) {
    errors.prompt = '风格提示词最多 1000 字'
    valid = false
  }

  return valid
}

const isFormValid = computed(() => {
  const name = editingStyle.name.trim()
  const prompt = editingStyle.prompt.trim()
  return name && name.length <= 20 && prompt && prompt.length <= 1000 && !isStyleNameExists(name, editingStyle.originalName)
})

const goToCreate = () => {
  editingStyle.originalName = ''
  editingStyle.name = ''
  editingStyle.prompt = ''
  errors.name = ''
  errors.prompt = ''
  editorMode.value = true
}

const goToEdit = (style) => {
  editingStyle.originalName = style.name
  editingStyle.name = style.name
  editingStyle.prompt = style.prompt
  errors.name = ''
  errors.prompt = ''
  editorMode.value = true
}

const goBack = () => {
  editorMode.value = false
}

const saveStyle = () => {
  if (!validate()) return
  if (editingStyle.originalName) {
    updateCustomStyle(editingStyle.originalName, {
      name: editingStyle.name,
      prompt: editingStyle.prompt
    })
  } else {
    addCustomStyle({
      name: editingStyle.name,
      prompt: editingStyle.prompt
    })
  }
  editorMode.value = false
}

const useStyle = (style) => {
  applyStyle(style)
  router.push('/console/create')
}

const deleteStyle = (name) => {
  removeCustomStyle(name)
}
</script>
```

- [ ] **Step 3: 在 `<style scoped>` 末尾追加编辑器样式**

```css
.style-editor {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
  max-width: 720px;
}

.style-editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.style-editor-back {
  background: none;
  border: none;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px 4px 0;
}

.style-editor-back:hover {
  color: #07c160;
}

.style-editor-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-editor-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.style-editor-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.style-editor-label {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.style-editor-label .required {
  color: #ff4d4f;
  margin-left: 2px;
}

.style-editor-input,
.style-editor-textarea {
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.style-editor-input:focus,
.style-editor-textarea:focus {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.1);
}

.style-editor-error {
  color: #ff4d4f;
  font-size: 12px;
}

.style-editor-counter {
  text-align: right;
  font-size: 12px;
  color: #8c8c8c;
}

.style-editor-counter.over {
  color: #ff4d4f;
}

.style-editor-presets {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.style-editor-preset-label {
  font-size: 13px;
  color: #595959;
}

.style-editor-preset-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.style-preset-card {
  flex: 0 0 160px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.style-preset-card:hover {
  border-color: #07c160;
  background: #f6ffed;
}

.style-preset-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.style-preset-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.save-style-btn {
  padding: 10px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  width: fit-content;
}

.save-style-btn:hover {
  background: #e61e3a;
}

.save-style-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}
```

- [ ] **Step 4: 验证编辑器功能**

在页面中：
1. 点击“新建我的风格”。
2. 填写名称和提示词，点击保存，返回列表看到新卡片。
3. 点击“编辑”，修改提示词后保存。
4. 点击“删除”，卡片消失。
5. 名称重复、超长、空值时保存按钮禁用并显示错误。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/console/StylesIndex.vue
git commit -m "feat(styles): 实现我的风格页面编辑器与 CRUD

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: 重构创作页风格弹框使用 `useStyles.js`

**Files:**
- Modify: `project/user/web/src/views/console/CreateIndex.vue`

**Interfaces:**
- Consumes: `useStyles.js` 中的 `systemStyles`、`myStyles`、`currentStyle`、`addCustomStyle`、`updateCustomStyle`、`removeCustomStyle`、`applyStyle`。
- Produces: 创作页风格弹框与独立页面共享同一份数据和 CRUD。

- [ ] **Step 1: 在 `CreateIndex.vue` 顶部导入 `useStyles.js`**

在 `<script setup>` 的 import 区域新增：

```javascript
import {
  systemStyles,
  myStyles,
  currentStyle,
  applyStyle,
  addCustomStyle,
  updateCustomStyle,
  removeCustomStyle
} from '@/composables/useStyles.js'
```

- [ ] **Step 2: 删除 `CreateIndex.vue` 中本地定义的系统预设和状态**

删除以下代码（约第 876-887 行）：

```javascript
const systemStyles = [
  { name: '年度总结', desc: '回顾、复盘、展望', promptSummary: '语气：回顾性、感恩 + 数据自省\n结构：成绩 + 反思 + 明年目标\n长度：1500-2500 字，带小标题分章' },
  { name: '产品评测', desc: '客观、数据驱动、多角度对比', promptSummary: '语气：客观中立、有理有据\n结构：外观 + 性能 + 体验 + 总结\n要素：必带参数对比表 + 优缺点' },
  { name: '情感散文', desc: '细腻、共情、个人化表达', promptSummary: '语气：细腻、温暖、第一人称\n修辞：善用比喻、意象、留白\n结构：场景 + 情绪 + 升华' },
  { name: '职场干货', desc: '实操性强、结构清晰', promptSummary: '语气：专业务实、老板视角\n结构：痛点 + 方案 + 步骤 + 案例\n要素：可执行的 checklist' },
  { name: '热点评论', desc: '观点鲜明、论据紧凑', promptSummary: '语气：犀利、有态度\n结构：事件概述 + 观点 + 论据 + 结论\n要素：引用数据或权威观点' },
  { name: '知识科普', desc: '深入浅出、逻辑清晰', promptSummary: '语气：亲和、易懂\n结构：问题 + 原理 + 案例 + 总结\n要素：避免术语堆砌，善用类比' },
  { name: '营销转化', desc: '引导行动、强说服', promptSummary: '语气：紧迫感 + 利益点突出\n结构：痛点共鸣 + 方案 + 案例 + CTA\n要素：必带限时/优惠/倒计时' },
  { name: '故事叙事', desc: '沉浸感、有冲突与转折', promptSummary: '语气：克制、文学化\n结构：起承转合 + 人物对话\n要素：场景细节 + 心理活动' }
]
const myStyles = ref([])
const currentStyle = ref(systemStyles[0])
```

- [ ] **Step 3: 替换 `saveStyle`、`deleteStyle`、`applyStyle` 实现**

将原有：

```javascript
const saveStyle = () => {
  if (!editingStyle.name.trim() || !editingStyle.prompt.trim()) return
  myStyles.value.push({
    name: editingStyle.name,
    desc: '自定义风格',
    count: 0,
    prompt: editingStyle.prompt
  })
  createStyleMode.value = false
}

const deleteStyle = (name) => {
  const idx = myStyles.value.findIndex(x => x.name === name)
  if (idx > -1) myStyles.value.splice(idx, 1)
  if (selectedStyleName.value === name) selectedStyleName.value = null
}
```

改为：

```javascript
const saveStyle = () => {
  if (!editingStyle.name.trim() || !editingStyle.prompt.trim()) return
  if (editingStyle.name.trim().length > 20 || editingStyle.prompt.trim().length > 1000) return
  if (editingStyle.isEdit) {
    updateCustomStyle(editingStyle.originalName, {
      name: editingStyle.name,
      prompt: editingStyle.prompt
    })
  } else {
    addCustomStyle({
      name: editingStyle.name,
      prompt: editingStyle.prompt
    })
  }
  createStyleMode.value = false
}

const deleteStyle = (name) => {
  removeCustomStyle(name)
  if (selectedStyleName.value === name) selectedStyleName.value = null
}
```

- [ ] **Step 4: 修改 `applyStyle` 函数使用共享方法**

将原有：

```javascript
const applyStyle = () => {
  if (!selectedStyleName.value) return
  const s = systemStyles.find(x => x.name === selectedStyleName.value) ||
            myStyles.value.find(x => x.name === selectedStyleName.value)
  if (s) {
    currentStyle.value = s
    styleVisible.value = false
  }
}
```

改为：

```javascript
const applyStyle = () => {
  if (!selectedStyleName.value) return
  const s = systemStyles.find(x => x.name === selectedStyleName.value) ||
            myStyles.value.find(x => x.name === selectedStyleName.value)
  if (s) {
    applyStyleShared(s)
    styleVisible.value = false
  }
}
```

注意：由于函数名与导入的 `applyStyle` 冲突，需将导入重命名：

```javascript
import {
  systemStyles,
  myStyles,
  currentStyle,
  applyStyle as applyStyleShared,
  addCustomStyle,
  updateCustomStyle,
  removeCustomStyle
} from '@/composables/useStyles.js'
```

- [ ] **Step 5: 修改 `goToCreateStyle` 以支持编辑模式**

将原有：

```javascript
const editingStyle = reactive({ name: '', prompt: '' })
```

改为：

```javascript
const editingStyle = reactive({ originalName: '', name: '', prompt: '', isEdit: false })
```

将原有：

```javascript
const goToCreateStyle = () => {
  createStyleMode.value = true
  editingStyle.name = ''
  editingStyle.prompt = ''
}
```

改为：

```javascript
const goToCreateStyle = () => {
  createStyleMode.value = true
  editingStyle.isEdit = false
  editingStyle.originalName = ''
  editingStyle.name = ''
  editingStyle.prompt = ''
}
```

将模板中“编辑提示词”按钮：

```vue
<button class="style-action-btn" @click.stop="goToCreateStyle">编辑提示词</button>
```

改为：

```vue
<button class="style-action-btn" @click.stop="goToEditStyle(m)">编辑提示词</button>
```

并在 `<script setup>` 中新增：

```javascript
const goToEditStyle = (style) => {
  createStyleMode.value = true
  editingStyle.isEdit = true
  editingStyle.originalName = style.name
  editingStyle.name = style.name
  editingStyle.prompt = style.prompt
}
```

- [ ] **Step 6: 验证弹框与独立页面数据同步**

1. 在创作页点击风格 chip 打开弹框。
2. 在“我的风格”tab 新建一个风格并保存。
3. 关闭弹框，访问 `/console/styles`，确认新风格已同步出现。
4. 在独立页面编辑该风格，返回创作页弹框，确认内容已更新。

- [ ] **Step 7: 提交**

```bash
git add project/user/web/src/views/console/CreateIndex.vue
git commit -m "refactor(create): 风格弹框改用共享 useStyles 状态

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: 添加端到端测试

**Files:**
- Create: `tests/e2e/verify_my_style_page.py`

**Interfaces:**
- Consumes: 开发服务器在 `http://localhost:22345`（或实际端口）。
- Produces: 测试脚本验证独立页面的渲染、CRUD 和跳转。

- [ ] **Step 1: 创建测试脚本**

```python
# tests/e2e/verify_my_style_page.py
from playwright.sync_api import sync_playwright

URL = 'http://localhost:22347'  # 根据实际 dev server 端口调整

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        # 1. 访问我的风格页面
        page.goto(f'{URL}/console/styles')
        page.wait_for_timeout(500)

        assert page.locator('.styles-title').inner_text() == '我的风格'
        assert page.locator('.styles-tab').count() == 2

        # 2. 系统预设 tab 显示 8 张卡片
        page.locator('button:has-text("系统预设")').click()
        page.wait_for_timeout(300)
        assert page.locator('.styles-content:visible .style-card').count() == 8

        # 3. 我的风格 tab 为空
        page.locator('button:has-text("我的风格")').click()
        page.wait_for_timeout(300)
        assert page.locator('.styles-empty').count() == 1

        # 4. 新建风格
        page.locator('button:has-text("去创建一个")').click()
        page.wait_for_timeout(300)
        page.locator('.style-editor-input').fill('测试风格')
        page.locator('.style-editor-textarea').fill('这是一段测试风格提示词，语气轻松活泼。')
        page.locator('button:has-text("保存")').click()
        page.wait_for_timeout(300)

        cards = page.locator('.styles-content:visible .style-card')
        assert cards.count() == 1
        assert '测试风格' in cards.first.inner_text()

        # 5. 编辑风格
        page.locator('button:has-text("编辑")').click()
        page.wait_for_timeout(300)
        page.locator('.style-editor-textarea').fill('已更新的测试风格提示词。')
        page.locator('button:has-text("保存")').click()
        page.wait_for_timeout(300)
        assert '已更新' in cards.first.inner_text()

        # 6. 删除风格
        page.locator('button:has-text("删除")').click()
        page.wait_for_timeout(300)
        assert page.locator('.styles-empty').count() == 1

        # 7. 系统预设“使用”跳转创作页
        page.locator('button:has-text("系统预设")').click()
        page.wait_for_timeout(300)
        page.locator('.styles-content:visible .style-action-btn').first.click()
        page.wait_for_timeout(500)
        assert '/console/create' in page.url

        page.screenshot(path='tests/e2e/screenshots/my_style_page.png')
        print('我的风格独立页面验证通过')

        browser.close()

if __name__ == '__main__':
    main()
```

- [ ] **Step 2: 运行测试**

确保开发服务器仍在运行，然后：

```bash
python3 tests/e2e/verify_my_style_page.py
```

预期输出：`我的风格独立页面验证通过`

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/verify_my_style_page.py
git commit -m "test(styles): 添加我的风格独立页面端到端验证

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 7: 最终验证与清理

**Files:**
- 无新增文件

**Interfaces:**
- Consumes: 前述所有任务结果。
- Produces: 功能完整的“我的风格”独立页面。

- [ ] **Step 1: 完整走查**

1. 侧边栏“我的风格”入口可点击。
2. 两个 tab 切换正常。
3. 我的风格支持新建、编辑、删除、使用。
4. 系统预设可查看、使用。
5. 创作页风格弹框仍可正常使用，且与独立页面数据同步。
6.  Playwright 测试通过。

- [ ] **Step 2: 检查未提交改动**

```bash
git status
```

确认无遗漏的修改或新增文件。

- [ ] **Step 3: 最终提交（如需要）**

若仍有未提交改动：

```bash
git add -A
git commit -m "feat(styles): 完成我的风格独立菜单功能

- 新增 /console/styles 页面
- 共享 useStyles.js 状态
- 创作页弹框与独立页面数据互通
- 添加端到端测试

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Spec Coverage Check

| 规格要求 | 对应任务 |
|---|---|
| 侧边栏新增“我的风格”独立入口 | Task 2 |
| 新增 `/console/styles` 路由 | Task 2 |
| 页面分“我的风格”和“系统预设”两个 tab | Task 3 |
| 我的风格支持新建/编辑/删除 | Task 4 |
| 系统预设只读 | Task 3 |
| 点击“使用”跳转创作页并应用风格 | Task 4 |
| 卡片可展开查看完整提示词 | Task 3、Task 4 |
| 创作页弹框不动，仅切换数据源 | Task 5 |
| 前端内存状态，不写入 localStorage | Task 1 |
| 名称 20 字、提示词 1000 字、名称去重 | Task 4 |
| 新增 Playwright 测试 | Task 6 |

## Placeholder Scan

- 无 TBD、TODO、implement later。
- 所有步骤均包含实际代码或命令。
- 文件路径、函数名、变量名在任务间保持一致。

## Type Consistency

- `useStyles.js` 导出名称：`systemStyles`、`myStyles`、`currentStyle`、`applyStyle`、`addCustomStyle`、`updateCustomStyle`、`removeCustomStyle`、`isStyleNameExists`。
- `StylesIndex.vue` 和 `CreateIndex.vue` 中引用名称一致。
- `applyStyle` 在 `CreateIndex.vue` 中因局部函数重名，使用 `applyStyleShared` 别名导入。
