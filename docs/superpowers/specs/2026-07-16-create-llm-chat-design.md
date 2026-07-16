# 创作页引导模式 LLM-Chat 化

> 引导模式贴近大模型对话体验：话题步骤标题默认折叠（点击后渐进展开），模板从平台耦合中拆出为独立聊天步骤并新增内联预览。极简模式不受影响。

## 背景与目标

现状问题（基于 2026-07-16 重设计已落地的引导模式诊断）：

- 话题步骤直接平铺 6 个标题胶囊（默认全展开），缺乏"先问后答"的对话感
- 用户希望话题步骤像 ChatGPT：输入框 + 一个"没灵感？试试点我"按钮 → 点击后模拟流式回答，标题逐个出现
- 模板从平台确认后自动套用，`applyPlatformDefault` 把字数 + 模板一锅端，**平台和模板是两个独立维度**（发公众号也能用小红书种草清单模板），必须拆开
- 引导模式无任何模板预览入口，TemplateModal 才看得见预览，但引导模式默认不弹它
- 新增"模板"步骤需要带预览，否则用户盲选 5~30 个模板

目标：

- 话题步骤有"折叠 → 触发 → 流式展开 → 选一个"的对话感
- 引导模式聊天流变 4 步：topic → 平台 → 风格 → 模板
- 模板步骤支持内联效果预览卡 + "查看完整预览"按钮（开 TemplateModal）
- `applyPlatformDefault` 只管字数，不再设置模板

## 已确认的关键决策

| 决策点 | 结论 |
|---|---|
| 展开方式 | 渐进展开：AI 答话 → 600ms 打字光标 → 6 个标题每 150ms 渐显 → "换一批" |
| 步骤顺序 | topic → 平台 → 风格 → 模板 |
| 模板预览 | 内联效果卡（名称 / 平台 / 适用场景 / 调色色块）+ "查看完整预览" 按钮调 TemplateModal |
| 后端 | 不动：复用 `/topics/random` 和 `/export-templates` |

## 组件变更

```
project/user/web/src/views/console/create/
├── GuidedChat.vue         # 改：4 步流程 / 模板 effect-card 内联预览 / applyPlatformDefault 解耦模板
├── TopicSuggestionBubble.vue  # 新：折叠态 + 流式展开态 + "换一批" 三态切换
├── TopicCapsules.vue      # 不变：MinimalPanel 仍用它（极简模式保持直白胶囊）
├── modals/TemplateModal.vue  # 不变：作为"完整预览"入口被 chat 调用
└── useCreateForm.js       # 不变：state 共享机制已够
```

**新增一个组件，改一个组件。其它不变。**

## `TopicSuggestionBubble.vue`

自包含单元，由 GuidedChat 在 `kind: 'topic'` 步骤模板里取代原 `<TopicCapsules>`。三个状态：

| 状态 | UI |
|---|---|
| **collapsed** | 一个按钮 `💡 没灵感？试试点我` |
| **expanding** | 按钮变 disabled 提示 `已为你想到几个方向 👇` + 600ms 打字光标 + 6 个标题按 `revealedCount` 渐显 |
| **done** | 6 个标题全部显示，底部出现 `换一批` |

内部状态：

```js
const collapsed = ref(true)
const expanding = ref(false)
const revealedCount = ref(0)   // 0..topics.length
const typing = ref(false)
const topics = ref([])         // 当前 batch，6 个

const expand = () => {
  collapsed.value = false
  expanding.value = true
  typing.value = true
  setTimeout(() => {
    typing.value = false
    expandInterval = setInterval(() => {
      if (++revealedCount.value >= topics.value.length) {
        expanding.value = false
        clearInterval(expandInterval)
      }
    }, 150)
  }, 600)
}

const refresh = async () => {
  // 调 fetchRandomTopics(6) 重置 revealedCount + typing + expanding，重走 600ms→渐显
}
```

`onMounted(loadTopics)` 自加载（沿用现有 TopicCapsules 懒加载模式，6 个）。`select(topic)` 事件等价原 `onTopicCapsule`。

## `GuidedChat.vue` 变更

### 流程顺序

```js
submitTopic / onTopicCapsule  // user → topic
  → askPlatform()
onQuickConfirm('platform')    // user → platform；只 applyPlatformDefault 设置字数
  → askStyle()
onQuickConfirm('style')       // user → style
  → askTemplate()              // 新增：第 4 步
onQuickConfirm('template')    // user → template
  → push({ kind: 'confirm' })
```

### `applyPlatformDefault` 瘦身

```js
// 旧（同时设字数 + 模板）
const applyPlatformDefault = (p) => {
  const presets = wordCountPresets.platform[p.key] || wordCountPresets.platform.general
  const wc = presets.find(x => x.count === p.recommendWords) || presets[0]
  currentWordCount.value = { count: wc.count, label: wc.label, desc: wc.desc || '' }
  const t = apiTemplates.value.find(x => x.key.startsWith(p.key)) || apiTemplates.value[0]
  if (t) selectedTemplateKey.value = t.key  // ← 删
}

// 新
const applyPlatformDefault = (p) => {
  const presets = wordCountPresets.platform[p.key] || wordCountPresets.platform.general
  const wc = presets.find(x => x.count === p.recommendWords) || presets[0]
  currentWordCount.value = { count: wc.count, label: wc.label, desc: wc.desc || '' }
}
```

### 新增 `askTemplate` + `optionsType: 'template'`

```js
const askTemplate = () => {
  push({
    role: 'ai',
    kind: 'quick',
    text: '想用哪种模板渲染？',
    optionsType: 'template',
    options: apiTemplates.value.map(t => ({
      key: t.key, label: t.name, raw: t
    }))
  })
}

const onQuickConfirm = (m, opt) => {
  m.done = true
  if (m.optionsType === 'platform') {
    currentPlatform.value = opt.raw
    applyPlatformDefault(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    askStyle()
  } else if (m.optionsType === 'style') {
    applyStyle(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    askTemplate()                                  // ← 接 askTemplate 而不是 confirm
  } else if (m.optionsType === 'template') {       // ← 新分支
    selectedTemplateKey.value = opt.raw.key
    push({ role: 'user', kind: 'text', text: opt.label })
    push({ role: 'ai', kind: 'confirm' })
  }
}
```

### 模板 effect-card（新增）

在 `kind: 'quick'` 模板分支的 `#preview` 插槽里新增模板渲染（复用现有 `platform` / `style` 的 effect-card 视觉范式）：

```html
<template v-if="m.optionsType === 'template'" class="effect-card">
  <div class="effect-title">{{ option.label }}</div>
  <div class="effect-line">· 平台：{{ platformLabel(option.raw.platform) }}</div>
  <div class="effect-line">· 适用：{{ option.raw.desc || '通用场景' }}</div>
  <div class="effect-line">· 主色：
    <span class="color-swatch" :style="{background: option.raw.bgColor || '#fff'}"></span>
    <span class="color-swatch" :style="{background: option.raw.textColor || '#1a1a1a'}"></span>
  </div>
  <button class="preview-full-btn" @click="openFullPreview">查看完整预览 →</button>
</template>
```

`openFullPreview()` = `templateVisible.value = true`（TemplateModal 自动选中当前项，`watch(templateVisible)` 已处理）。

### 确认卡 4 行

```html
<div class="confirm-meta">{{ currentPlatform.name }} · {{ currentStyle?.name || '默认风格' }} · {{ currentTemplate?.name }}</div>
<div class="confirm-meta">字数：{{ currentWordCount.count }} 字</div>
<div class="confirm-actions">
  <button class="confirm-generate" @click="handleConfirmGenerate(m)">⚡ 开始生成</button>
  <button class="confirm-edit" @click="editTopic">改主题</button>
  <button class="confirm-edit" @click="editPlatform">改平台</button>
  <button class="confirm-edit" @click="editStyle">改风格</button>
  <button class="confirm-edit" @click="editTemplate">改模板</button>
</div>
```

`editPlatform`/`editStyle`/`editTemplate` 分别 push 回对应步骤的 AI 气泡（kind: 'quick', optionsType 锁定），已答内容保留。

## `applyPlatformDefault` 解耦影响

- MinimalPanel 完全不受影响（4 个 chip 独立，本来就没调 `applyPlatformDefault`）
- TemplateModal 行为不变，但用户在 chat 走了 4 步选了非平台前缀模板，到 TemplateModal 还是会被 `watch(templateVisible)` 选中 — 没问题
- 已答步骤的 `m.done` 仍折叠确保不重复触发（沿用既有 fix）

## 测试方案

3 个新 E2E 脚本 + 改造现有 2 个：

| 脚本 | 断言 |
|---|---|
| `verify_topic_streaming.py` | 默认进 chat，点 `没灵感？试试点我`，600ms 后看 `.typing-cursor`，6 个标题逐渐出现，最后 `.refresh-btn`（"换一批"）可见；点换一批触发 reload |
| `verify_template_step.py` | 跑通 4 步（topic+平台+风格+模板），确认卡 4 行 + 模板 effect-card 内的 `.preview-full-btn` 可点开 TemplateModal |
| `verify_template_decouple.py` | 走完 topic + 平台两步（不选模板），看确认卡的 `currentTemplate` 仍是默认（不是自动套用平台前缀模板）。具体断言：confirm-meta 行的 `模板：xxx`，xxx 不以用户选择的平台 key 开头 |
| **改造** `verify_guided_skeleton.py` | step 4 期望从"风格"变"模板"，小红书选项不可见（变成风格步骤） |
| **改造** `verify_guided_flow.py` | 同上 + 模板步骤断言模板名出现在 confirm |

`verify_guided_generate.py` 不动：成功后进度卡渲染本来就与模板步骤无关。

## 不做（YAGNI）

- 不加真的流式 API（SSE），前端 600ms + 渐显够用
- 不修改 TemplateModal（已有大预览 + 应用逻辑，重新设计它超出本次范围）
- 不做"上一题/下一题"分步进度条（对话天然有"已答内容折叠"的状态）
- 不在 MinimalPanel 改任何东西
- 不动后端 / SQL / Flyway

## 风险

- 600ms + 150ms × 6 = 总 ~1.5s 流式时间，可接受但要快测；如觉得慢，单条间隔可降到 100ms
- 标题渐显在深色模式下需要确认胶囊颜色对比度（继承现有 `.topic-capsule` 深色样式即可）
- `applyPlatformDefault` 瘦身是个语义破坏性变更：原行为"选平台=选模板"被取消，存量体验变化要明确告知（用户已答过本 spec，决定了拆开，故接受）
