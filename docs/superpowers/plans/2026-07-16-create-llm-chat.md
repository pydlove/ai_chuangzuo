# 引导模式 LLM-Chat 化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把引导模式话题步骤改成"折叠 → 流式展开"的大模型对话感；把模板从平台耦合中拆出为独立的第 4 个聊天步骤并新增内联预览（"查看完整预览"按钮打开 TemplateModal）。

**Architecture:** 新增自包含组件 `TopicSuggestionBubble.vue`（三态切换：collapsed / expanding / done），由 `GuidedChat` 在 `kind: 'topic'` 步骤里挂载；`GuidedChat` 内部 chat flow 改为 4 步 `topic → 平台 → 风格 → 模板`，`onQuickConfirm` 新增 `optionsType: 'template'` 分支，`applyPlatformDefault` 不再设 `selectedTemplateKey`；`TemplateModal` 不动。整个改动纯前端，`/topics/random` 与 `/export-templates` 复用。

**Tech Stack:** Vue 3 `<script setup>`、ant-design-vue 4（a-tooltip）、Playwright E2E。

**Spec:** `docs/superpowers/specs/2026-07-16-create-llm-chat-design.md`

## Global Constraints

- 主色 `#FF2442`（`--color-primary`），所有颜色走 CSS 变量；**必须兼容深色主题**（`body[data-theme="dark"]` 选择器），禁写死 `#fff` 等浅色值。
- 接口信封：`{code:0,data:...}`；`fetchRandomTopics(count)` 返回 `[{id,title,summary}]`；`useExportTemplates().templates` 返回 `[{key,name,desc,platform,bgColor,textColor,visualStyle,...}]`。
- 开发服务器：`cd project/user/web && npm run dev`，E2E 默认端口 `http://localhost:22345`，可用 `BASE` 环境变量覆盖。
- E2E 脚本放 `tests/e2e/`，截图放 `tests/e2e/screenshots/`；命名沿用 `verify_*.py`。
- `applyPlatformDefault` 当前签名 `(platform) => void`，**只负责字数**；**不得在 `optionsType==='platform'` 分支里 set `selectedTemplateKey`**，否则破坏解耦。
- 不改 `useCreateForm.js`、`TopicCapsules.vue`、`TemplateModal.vue`、`modals/*`；改完确认无引用残留再删除任何旧代码。
- 不用的代码开发结束后必须删掉（CLAUDE.md 约定）。

---

### Task 1: TopicSuggestionBubble + GuidedChat 接入

**Files:**
- Create: `project/user/web/src/views/console/create/TopicSuggestionBubble.vue`
- Modify: `project/user/web/src/views/console/create/GuidedChat.vue:115`（import）、`GuidedChat.vue:26`（template）
- Test: `tests/e2e/verify_topic_streaming.py`

**Interfaces:**
- Consumes: `fetchRandomTopics(count)` from `@/api/topic.js`；`useCreateForm()` → `{ customTitle, customRequirement }`
- Produces: `defineEmits(['select'])`，触发时已经把 `customTitle`/`customRequirement` 写好；调用方接 `onTopicCapsule(topic)` 同现 `TopicCapsules`。

- [ ] **Step 1: 写 E2E（红）：`verify_topic_streaming.py`**

写一个不到 60 行的小脚本，断言：
- 进 `/console/create`，默认引导模式可见（`.guided-chat`）
- mock `/api/v1/user/topics/random` 返回 6 个标题
- 折叠按钮 `.inspire-btn` 可见且文字含"没灵感"
- 点击后出现 `.typing-cursor`；等 600ms 它消失
- 等到 `.refresh-suggestion` 可见（流式结束标志），断言 6 个 `.suggestion-title-card` 全部 `is_visible() = True`
- 点 `.refresh-suggestion` 后新 batch 加载（id 必须变化或 mock 的 counter 增加）
- 选某个标题 → 应出现 `.chat-msg.user`（用户泡泡）

完整脚本骨架：

```python
#!/usr/bin/env python3
"""引导模式话题步骤：折叠按钮 → 点击 → 600ms 打字 → 6 标题渐显 → 换一批 → 选标题。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

MOCK = [
    {"id": 1, "title": "测试主题 A", "summary": "概要 A"},
    {"id": 2, "title": "测试主题 B", "summary": "概要 B"},
    {"id": 3, "title": "测试主题 C", "summary": "概要 C"},
    {"id": 4, "title": "测试主题 D", "summary": "概要 D"},
    {"id": 5, "title": "测试主题 E", "summary": "概要 E"},
    {"id": 6, "title": "测试主题 F", "summary": "概要 F"},
]

batch_state = {"n": 0}


def on_topics(route):
    batch_state["n"] += 1
    # 第二次换一批时 id 偏移，方便断言触发了刷新
    payload = [{"id": i + batch_state["n"] * 100, "title": t["title"] + (" (新)" if batch_state["n"] > 1 else ""), "summary": t["summary"]} for i, t in enumerate(MOCK)]
    route.fulfill(json={"code": 0, "data": payload})


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        # 必要的通用 mock（免得之前步骤出错）
        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                                "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))
        page.route("**/api/v1/user/topics/random*", on_topics)

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        ok_collapsed = page.query_selector(".inspire-btn") is not None
        page.click(".inspire-btn")
        page.wait_for_timeout(100)
        ok_typing = page.query_selector(".typing-cursor") is not None
        # 等到换一批可见（=流式结束）
        page.wait_for_selector(".refresh-suggestion", timeout=8000, state="visible")
        page.wait_for_timeout(200)

        cards = page.query_selector_all(".suggestion-title-card")
        visible = [c for c in cards if c.is_visible()]
        ok_six = len(visible) == 6

        # 选第一个标题
        visible[0].query_selector(".suggestion-title").click()
        page.wait_for_timeout(400)
        ok_user = page.query_selector(".chat-msg.user") is not None

        page.screenshot(path=f"{SHOTS}/topic_streaming.png")

        # 换一批
        before_refresh_n = batch_state["n"]
        page.click(".refresh-suggestion")
        page.wait_for_selector(".refresh-suggestion", timeout=5000)
        ok_refresh = batch_state["n"] == before_refresh_n + 1

        for n, ok in [("collapsed", ok_collapsed), ("typing", ok_typing), ("six-titles", ok_six),
                       ("user-bubble", ok_user), ("refresh-fired", ok_refresh)]:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all([ok_collapsed, ok_typing, ok_six, ok_user, ok_refresh]):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()
```

- [ ] **Step 2: 跑 E2E 验证红**

Run: `python3 tests/e2e/verify_topic_streaming.py`
Expected: FAIL — `TimeoutError` because `.inspire-btn` does not exist.

- [ ] **Step 3: 新建 `TopicSuggestionBubble.vue`（折叠态 + 自加载）**

文件首版（只渲染折叠按钮 + 拉一次 `/topics/random`，不展开）。完整代码：

```vue
<template>
  <div class="topic-suggestion">
    <button v-if="collapsed" class="inspire-btn" @click="expand">
      💡 没灵感？试试点我
    </button>
    <div v-else>
      <div class="suggestion-status">已为你想到几个方向 👇</div>
      <div v-if="typing" class="typing-cursor"><span></span><span></span><span></span></div>
      <div v-else class="suggestion-titles">
        <div v-for="(topic, i) in topics" :key="topic.id" v-show="i < revealedCount" class="suggestion-title-card">
          <a-tooltip :title="topic.title" placement="top">
            <button class="suggestion-title" :disabled="topic.used" @click="select(topic)">
              {{ topic.title }}
            </button>
          </a-tooltip>
        </div>
        <button v-if="!expanding" class="refresh-suggestion" @click="refresh">换一批</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchRandomTopics, markTopicUsed } from '@/api/topic.js'
import { useCreateForm } from './useCreateForm.js'

const emit = defineEmits(['select'])
const { customTitle, customRequirement } = useCreateForm()

const collapsed = ref(true)
const expanding = ref(false)
const revealedCount = ref(0)
const typing = ref(false)
const topics = ref([])
let expandTimer = null
let revealTimer = null

const loadTopics = async () => {
  try {
    const list = await fetchRandomTopics(6)
    topics.value = (list || []).map(t => ({ id: t.id, title: t.title, summary: t.summary, used: false }))
  } catch {
    topics.value = []
  }
}

const startStream = () => {
  expanding.value = true
  typing.value = true
  revealedCount.value = 0
  if (revealTimer) clearInterval(revealTimer)
  if (expandTimer) clearTimeout(expandTimer)
  expandTimer = setTimeout(() => {
    typing.value = false
    revealTimer = setInterval(() => {
      revealedCount.value++
      if (revealedCount.value >= topics.value.length) {
        clearInterval(revealTimer)
        expanding.value = false
      }
    }, 150)
  }, 600)
}

const expand = () => {
  if (!collapsed.value) return
  collapsed.value = false
  startStream()
}

const refresh = async () => {
  await loadTopics()
  startStream()
}

const select = (topic) => {
  if (topic.used) return
  customTitle.value = topic.title
  customRequirement.value = topic.summary || ''
  topic.used = true
  markTopicUsed(topic.id).catch(() => {})
  emit('select', topic)
}

onMounted(loadTopics)
defineExpose({ loadTopics })
</script>

<style scoped>
.topic-suggestion { margin-top: 4px; }

.inspire-btn {
  background: var(--color-bg-card);
  border: 1px dashed var(--color-border-default);
  border-radius: 18px;
  padding: 8px 18px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.inspire-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.suggestion-status {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 10px;
}

.typing-cursor {
  display: inline-flex;
  gap: 4px;
  padding: 10px 14px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
}

.typing-cursor span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: cursor-bounce 1.2s infinite ease-in-out;
}

.typing-cursor span:nth-child(2) { animation-delay: 0.15s; }
.typing-cursor span:nth-child(3) { animation-delay: 0.3s; }

@keyframes cursor-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.suggestion-titles {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggestion-title-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  padding: 10px 14px;
  transition: all 0.2s;
}

.suggestion-title-card:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.suggestion-title {
  width: 100%;
  background: none;
  border: none;
  padding: 0;
  text-align: left;
  font-size: 14px;
  color: var(--color-text-primary);
  cursor: pointer;
  font-weight: 500;
}

.suggestion-title:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-suggestion {
  align-self: flex-start;
  margin-top: 4px;
  padding: 6px 16px;
  background: none;
  border: 1px solid var(--color-border-default);
  border-radius: 16px;
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.refresh-suggestion:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .inspire-btn {
  background: #1f1f1f;
  border-color: #434343;
  color: #a6a6a6;
}
</style>
```

- [ ] **Step 4: GuidedChat.vue 接入（替换 TopicCapsules）**

在 `GuidedChat.vue:113-115`（import 段）：

```js
import TopicSuggestionBubble from './TopicSuggestionBubble.vue'
// 删掉：import TopicCapsules from './TopicCapsules.vue'
```

`GuidedChat.vue:26`（template 段，`kind === 'topic'` 分支里）：

```html
      <!-- 步骤 1：主题输入 + 流式灵感气泡 -->
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
        <TopicSuggestionBubble @select="onTopicCapsule" />
      </template>
```

`onTopicCapsule` 已有，不需要改签名：select 事件带的 topic 对象有 `title` / `summary` 字段，与 TopicCapsules 的 emit 完全一致。

- [ ] **Step 5: 跑 E2E 验证绿**

Run: `python3 tests/e2e/verify_topic_streaming.py`
Expected: 5 个断言全部 PASS；`ALL PASS` 终止。

如果某项 FAIL，常见原因：
- `.is_visible()` 计数不对：检查 `v-show="i < revealedCount"` 是否每次自增；用 `console.log` 在 E2E 临时插值看 `revealedCount`。
- 打字光标没出现：`setTimeout` 没触发；检查 `collapsed.value = false; startStream()` 时序。

- [ ] **Step 6: 提交**

```bash
git add project/user/web/src/views/console/create/TopicSuggestionBubble.vue project/user/web/src/views/console/create/GuidedChat.vue tests/e2e/verify_topic_streaming.py
git commit -m "feat(create): 话题步骤大模型流式 — 折叠态+600ms 打字+6 标题渐显+换一批"
```

---

### Task 2: 4 步聊天 + 模板 effect-card + 平台-模板解耦 + 确认卡

**Files:**
- Modify: `project/user/web/src/views/console/create/GuidedChat.vue:202-243`（新增 `askTemplate`，`onQuickConfirm` 加分支，`applyPlatformDefault` 瘦身）、`GuidedChat.vue:52-65`（确认卡 4 行 + 4 编辑按钮 + 加 `preview-full-btn` 插槽）、`GuidedChat.vue:30-49`（新增 template effect-card 分支）
- Test: `tests/e2e/verify_template_step.py`、`tests/e2e/verify_template_decouple.py`

**Interfaces:**
- Consumes: `useExportTemplates()` 返回 `apiTemplates`，每项有 `key`、`name`、`desc`、`platform`、`bgColor`、`textColor`。
- Produces: `askTemplate()` 内部 push `kind:'quick', optionsType:'template'` 的消息；`onQuickConfirm` 在 `optionsType==='template'` 时 `selectedTemplateKey.value = opt.raw.key` 后 `push({kind:'confirm'})`。`applyPlatformDefault(p)` 改后**不再写 `selectedTemplateKey.value`**。`onQuickConfirm` 顶部不变，但接受 `m.editingMode` 字段（为 true 时不论答案都 `push({kind:'confirm'})`，用于"改 X"入口的回流）。

- [ ] **Step 1: 写两个 E2E（红）**

`verify_template_step.py` 完整骨架（断言 4 步走完 + 模板 effect-card 预览 + "查看完整预览" 能开 TemplateModal + 确认卡 4 行 + 4 编辑按钮）：

```python
#!/usr/bin/env python3
"""引导模式 4 步流程 + 模板 effect-card + 完整预览入口 + 确认卡 4 行。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                                "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
        page.route("**/api/v1/user/topics/random*", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化", "prompt": "x", "scope": "通用"}]}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"templateKey": "xiaohongshu-note", "name": "种草清单", "description": "小红书种草结构",
                 "platform": "xiaohongshu", "bgColor": "#fff0f2", "textColor": "#1a1a1a", "visualStyle": {"bg": "#fff0f2"}},
                {"templateKey": "wechat-article", "name": "公众号深度文", "description": "公众号长文",
                 "platform": "wechat", "bgColor": "#fff", "textColor": "#262626", "visualStyle": {"bg": "#fff"}},
                {"templateKey": "xiaohongshu-food", "name": "深夜食堂", "description": "美食探店",
                 "platform": "xiaohongshu", "bgColor": "#fff8e1", "textColor": "#5d4037", "visualStyle": {"bg": "#fff8e1"}}
            ]}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        # topic
        page.fill(".topic-input", "测试话题")
        page.keyboard.press("Enter")
        page.wait_for_timeout(400)
        # platform
        page.click(".quick-option:has-text('小红书')")
        page.wait_for_timeout(300)
        ok_template_preview_btn_on_platform = page.query_selector(".template-preview-btn") is None
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        # style
        page.click(".quick-option:has-text('轻松口语')")
        page.wait_for_timeout(300)
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        # --- 模板步骤 ---
        ok_template_question = page.query_selector("text=想用哪种模板渲染？") is not None
        page.click(".quick-option:has-text('种草清单')")
        page.wait_for_timeout(400)
        ok_effect_card = page.query_selector(".effect-card") is not None
        ok_color_swatch = page.query_selector(".color-swatch") is not None
        preview_btn = page.query_selector(".template-preview-btn")
        ok_preview_btn = preview_btn is not None
        page.screenshot(path=f"{SHOTS}/template_step_preview.png")

        ok_modal = False
        if preview_btn:
            preview_btn.click()
            page.wait_for_timeout(800)
            ok_modal = page.query_selector(".template-modal .ant-modal-content") is not None
            page.keyboard.press("Escape")
            page.wait_for_timeout(400)

        # 确认
        page.click(".quick-confirm")
        page.wait_for_timeout(500)
        confirm = page.query_selector(".confirm-card")
        ok_confirm = confirm is not None
        meta_text = confirm.inner_text() if confirm else ""
        ok_platform_in = "小红书" in meta_text
        ok_style_in = "轻松口语" in meta_text
        ok_template_in = "种草清单" in meta_text
        ok_edit_btns = all(confirm.query_selector(f"button:has-text('{t}')") for t in ["改主题", "改平台", "改风格", "改模板"])
        page.screenshot(path=f"{SHOTS}/template_step_confirm.png")

        results = [("template-question", ok_template_question),
                   ("template-effect-card", ok_effect_card),
                   ("color-swatch", ok_color_swatch),
                   ("preview-btn", ok_preview_btn),
                   ("no-preview-btn-on-platform", ok_template_preview_btn_on_platform),
                   ("full-preview-opens-modal", ok_modal),
                   ("confirm-card", ok_confirm),
                   ("platform-in-meta", ok_platform_in),
                   ("style-in-meta", ok_style_in),
                   ("template-in-meta", ok_template_in),
                   ("edit-buttons", ok_edit_btns)]
        for n, ok in results:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all(ok for _, ok in results):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()
```

`verify_template_decouple.py` 完整骨架：

```python
#!/usr/bin/env python3
"""平台-模板解耦：选完平台不应自动设 selectedTemplateKey。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                                "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
        page.route("**/api/v1/user/topics/random*", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"bizNo": "S1", "name": "轻松口语", "description": "x", "promptSummary": "x", "prompt": "x", "scope": "通用"}]}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat",
                 "bgColor": "#fff", "textColor": "#262626", "visualStyle": {}, "description": "微信长文"},
                {"templateKey": "xiaohongshu-note", "name": "种草清单", "platform": "xiaohongshu",
                 "bgColor": "#fff0f2", "textColor": "#1a1a1a", "visualStyle": {}, "description": "小红书种草"},
                {"templateKey": "general-story", "name": "故事卡片", "platform": "general",
                 "bgColor": "#fafafa", "textColor": "#1a1a1a", "visualStyle": {}, "description": "通用"}
            ]}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        page.fill(".topic-input", "测试")
        page.keyboard.press("Enter")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('小红书')")
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('轻松口语')")
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        # 模板步骤 — 故意选一个非平台前缀
        page.click(".quick-option:has-text('故事卡片')")
        page.wait_for_timeout(200)
        page.click(".quick-confirm")
        page.wait_for_timeout(400)

        confirm_text = page.query_selector(".confirm-card").inner_text()
        ok_decouple = "故事卡片" in confirm_text
        ok_no_autopick = "公众号深度文" not in confirm_text

        page.screenshot(path=f"{SHOTS}/template_decouple.png")
        for n, ok in [("decouple", ok_decouple), ("no-autopick", ok_no_autopick)]:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not (ok_decouple and ok_no_autopick):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()
```

- [ ] **Step 2: 跑两个 E2E 验证红**

Run:
```
python3 tests/e2e/verify_template_step.py
python3 tests/e2e/verify_template_decouple.py
```
Expected: 两个都 FAIL（缺 `askTemplate`、模板 effect-card、`改模板` 按钮）。

- [ ] **Step 3: GuidedChat.vue — 流程函数：`askTemplate` + `onQuickConfirm` 新分支 + `applyPlatformDefault` 瘦身 + `onQuickConfirm` 支持 `editingMode`**

在 `GuidedChat.vue` script 段紧邻 `askStyle` 之后新增 `askTemplate`：

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
```

把 `applyPlatformDefault` 改成只设置字数（**删掉原 selectedTemplateKey 那两行**）：

```js
// 平台确认后只带默认字数（模板由用户独立选择）
const applyPlatformDefault = (p) => {
  const presets = wordCountPresets.platform[p.key] || wordCountPresets.platform.general
  const wc = presets.find(x => x.count === p.recommendWords) || presets[0]
  currentWordCount.value = { count: wc.count, label: wc.label, desc: wc.desc || '' }
}
```

`onQuickConfirm` 整体替换为（新增 `template` 分支 + 把 `style → confirm` 改为 `style → askTemplate` + 支持 `editingMode` 短路回 confirm）：

```js
const onQuickConfirm = (m, opt) => {
  m.done = true
  if (m.optionsType === 'platform') {
    currentPlatform.value = opt.raw
    applyPlatformDefault(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    if (m.editingMode) { push({ role: 'ai', kind: 'confirm' }); return }
    askStyle()
  } else if (m.optionsType === 'style') {
    applyStyle(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    if (m.editingMode) { push({ role: 'ai', kind: 'confirm' }); return }
    askTemplate()
  } else if (m.optionsType === 'template') {
    selectedTemplateKey.value = opt.raw.key
    push({ role: 'user', kind: 'text', text: opt.label })
    push({ role: 'ai', kind: 'confirm' })
  }
}
```

- [ ] **Step 4: GuidedChat.vue — template effect-card：HTML + CSS + helper 函数**

模板（`kind === 'quick'` 的 `#preview` 插槽）替换为（含 `template` 分支 + 预览按钮），位置 `GuidedChat.vue:30-49`：

```html
          <QuickReplies v-if="!m.done" :options="m.options" @confirm="(opt) => onQuickConfirm(m, opt)">
            <template #preview="{ option }">
              <!-- 平台效果卡 -->
              <div v-if="m.optionsType === 'platform'" class="effect-card">
                <div class="effect-title">{{ option.label }}</div>
                <div class="effect-line">· 推荐 {{ option.raw.recommendWords }} 字，{{ platformTraitWordLabel(option.raw) }}</div>
                <div class="effect-line">· 平台特性：{{ option.raw.trait }}</div>
              </div>
              <!-- 风格效果卡 -->
              <div v-else-if="m.optionsType === 'style'" class="effect-card">
                <div class="effect-title">{{ option.label }}</div>
                <div class="effect-line">· {{ option.raw.desc }}</div>
                <div class="effect-line effect-prompt">· {{ option.raw.promptSummary }}</div>
              </div>
              <!-- 模板效果卡 -->
              <div v-else-if="m.optionsType === 'template'" class="effect-card">
                <div class="effect-title">{{ option.label }}</div>
                <div class="effect-line">· 平台：{{ platformLabel(option.raw.platform) }}</div>
                <div class="effect-line">· 适用：{{ option.raw.desc || '通用场景' }}</div>
                <div class="effect-line">· 主色：
                  <span class="color-swatch" :style="{ background: option.raw.bgColor || '#fff' }"></span>
                  <span class="color-swatch" :style="{ background: option.raw.textColor || '#1a1a1a' }"></span>
                </div>
                <button class="template-preview-btn" @click="openFullPreview(option.raw)">查看完整预览 →</button>
              </div>
            </template>
          </QuickReplies>
```

script 段（紧邻 `platformTraitWordLabel` 之后）追加：

```js
const platformLabel = (key) => {
  const p = platforms.find(x => x.key === key)
  return p ? p.name : '通用'
}
const openFullPreview = (tplRaw) => {
  selectedTemplateKey.value = tplRaw.key
  templateVisible.value = true
}
```

CSS 段（紧邻 `.effect-prompt` 之后）追加：

```css
.color-swatch {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  margin: 0 4px;
  vertical-align: middle;
  border: 1px solid var(--color-border-light);
}

.template-preview-btn {
  margin-top: 10px;
  padding: 6px 14px;
  background: var(--color-primary-light);
  border: 1px solid var(--color-primary);
  border-radius: 14px;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.template-preview-btn:hover {
  background: var(--color-primary);
  color: #fff;
}
```

- [ ] **Step 5: GuidedChat.vue — 确认卡 4 行 + 4 编辑按钮 + editPlatform/editStyle/editTemplate**

模板段（`kind === 'confirm'` 块）整体替换：

```html
        <!-- 步骤 4：确认卡片 -->
        <template v-else-if="m.kind === 'confirm'">
          <div class="confirm-card">
            <div class="confirm-title">📄 {{ customTitle }}</div>
            <div class="confirm-meta">
              {{ currentPlatform?.name || '未选' }} · {{ currentStyle?.name || '默认风格' }} · {{ currentTemplate?.name || '默认模板' }}
            </div>
            <div class="confirm-meta">字数：{{ currentWordCount?.count || 800 }} 字</div>
            <div class="confirm-quota">本次消耗 1 次 · 剩余 {{ quotaRemaining }} 次</div>
            <div class="confirm-actions">
              <button class="confirm-generate" @click="handleConfirmGenerate(m)">⚡ 开始生成</button>
              <button class="confirm-edit" @click="editTopic">改主题</button>
              <button class="confirm-edit" @click="editPlatform">改平台</button>
              <button class="confirm-edit" @click="editStyle">改风格</button>
              <button class="confirm-edit" @click="editTemplate">改模板</button>
            </div>
          </div>
        </template>
```

script 段（`editTopic` 函数之后）追加：

```js
// 编辑入口：push 的 quick 消息带 editingMode: true，让 onQuickConfirm 答完不再继续下一问，直接 push confirm
const editPlatform = () => push({
  role: 'ai',
  kind: 'quick',
  text: '准备发哪个平台？',
  optionsType: 'platform',
  options: platforms.map(p => ({ key: p.key, label: p.name, raw: p })),
  editingMode: true
})
const editStyle = () => push({
  role: 'ai',
  kind: 'quick',
  text: '想要什么风格？',
  optionsType: 'style',
  options: systemStyles.value.slice(0, 6).map(s => ({ key: s.name, label: s.name, raw: s })),
  editingMode: true
})
const editTemplate = () => push({
  role: 'ai',
  kind: 'quick',
  text: '想用哪种模板渲染？',
  optionsType: 'template',
  options: apiTemplates.value.map(t => ({ key: t.key, label: t.name, raw: t })),
  editingMode: true
})
```

注意：`editTopic` 现有行为不变（仍调 `push({role:'ai', kind:'topic'})`）；`editingMode` 是新增的可选字段，**首次 chat 流的所有 `push({kind:'quick'})` 调用都不设置它**，自然走"答完继续下一问"路径。

- [ ] **Step 6: 跑两个 E2E 验证绿**

Run:
```
python3 tests/e2e/verify_template_step.py
python3 tests/e2e/verify_template_decouple.py
```
Expected:
- `verify_template_step.py`：11 项断言全 PASS。
- `verify_template_decouple.py`：`decouple / no-autopick` 全 PASS。

如果某项 FAIL：
- `template-question` 失败 → 检查 `askTemplate` 文本与 effect card 是否齐全。
- `preview-btn` 失败 → 检查 HTML 中 `.template-preview-btn` 与 `openFullPreview` 是否在 effect-card 模板里。
- `full-preview-opens-modal` 失败 → 确认 `openFullPreview` 同时写了 `selectedTemplateKey` 和 `templateVisible`（缺一 TemplateModal 不会打开）。
- `confirm-card / edit-buttons` 失败 → 检查 `kind === 'confirm'` 模板里 5 个按钮齐全（生成 + 改主题 + 改平台 + 改风格 + 改模板）。
- `decouple / no-autopick` 失败 → 检查 `applyPlatformDefault` 是否还有 `selectedTemplateKey.value = …` 残留行。

- [ ] **Step 7: 提交**

```bash
git add project/user/web/src/views/console/create/GuidedChat.vue tests/e2e/verify_template_step.py tests/e2e/verify_template_decouple.py
git commit -m "feat(create): 引导模式 4 步 + 模板内联预览 + 平台-模板解耦"
```

---

### Task 3: 旧 E2E 适配 + 全量回归

**Files:**
- Modify: `tests/e2e/verify_guided_skeleton.py`、`tests/e2e/verify_guided_flow.py`
- 测试保留：`tests/e2e/verify_guided_generate.py`、`verify_create_mobile.py`、`verify_minimal_panel.py`、`verify_queue_drawer.py`、`verify_create_modals_after_extract.py`

**Interfaces:** 无；只动 E2E 脚本。

- [ ] **Step 1: 改 `verify_guided_flow.py` — 模板步骤补一个选项选择**

原来 `verify_guided_flow.py`（T6 commit 5421690）走完 platform+style 后期待的是 confirm 卡。改成走完 platform+style+template 之后期待 confirm。

定位原脚本中类似 `page.click(".quick-confirm")` 序列的最后一次（应该是 style 之后），新增两行（在原有 `page.wait_for_timeout(500)` 后）：

```python
# 新增：模板步骤（4 步流程）
page.wait_for_timeout(600)
ok_template_q = page.query_selector("text=想用哪种模板渲染？") is not None
page.click(".quick-option:has-text('公众号深度文')")
page.wait_for_timeout(300)
page.click(".quick-confirm")
page.wait_for_timeout(500)
```

末尾断言集合追加 `("template-question", ok_template_q)`。完整文件结构不变——只插入新步骤+加断言。

- [ ] **Step 2: 跑 `verify_guided_flow.py`**

Run: `python3 tests/e2e/verify_guided_flow.py`
Expected: `effect-card / confirm-card / confirm-meta / edit-config-modal / back-to-confirm / template-question` 全部 PASS。

如果 FAIL（最常见）：
- 模板 mock 没返回 → 给 `**/api/v1/user/export-templates**` 加 route（同模板预设）
- "template-question" 文本对不上 → 检查 `askTemplate` 里 `text: '想用哪种模板渲染？'`

- [ ] **Step 3: 改 `verify_guided_skeleton.py` — 加 export-templates mock**

T9 已加 benefits/topics/styles mock。骨架里还缺 `**/api/v1/user/export-templates**` mock（否则后来模板步骤会因 useExportTemplates 抛 promise rejection，无视觉但不影响 skeleton 断言），且 platform/step 顺序变了之后第二条 platform 选 `小红书` 行为没变。**实际上 skeleton 只测到 platform-options/preview-card 触发即可，不进 confirm**——脚本不需要改 platform 后的步骤。

但脚本里 `text=小红书` 是 platform options，最新版 mock `topic-options` 数 ≥5 即可（小红书、公众号、头条、知乎、百家号、抖音、通用 = 7）。仍然 PASS。

新增 `export-templates` mock（即便不被 skeleton 用到，避免 console 红色错误）：

```python
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": []}))
```

- [ ] **Step 4: 跑 `verify_guided_skeleton.py`**

Run: `python3 tests/e2e/verify_guided_skeleton.py`
Expected: 全部 PASS。

- [ ] **Step 5: 全量 E2E 回归**

Run:
```
cd project/user/web && npx vite build 2>&1 | tail -3
python3 tests/e2e/verify_create_modals_after_extract.py
python3 tests/e2e/verify_queue_drawer.py
python3 tests/e2e/verify_minimal_panel.py
python3 tests/e2e/verify_guided_skeleton.py
python3 tests/e2e/verify_topic_streaming.py
python3 tests/e2e/verify_template_step.py
python3 tests/e2e/verify_template_decouple.py
python3 tests/e2e/verify_guided_flow.py
python3 tests/e2e/verify_guided_generate.py
python3 tests/e2e/verify_create_mobile.py
```

Expected: 9 个脚本（去掉 verify_guided_flow.py 在 Step 2 跑过，但全量再确认一次）全部 `ALL PASS`；vite build 通过。

如果某项 FAIL：
- `verify_guided_generate.py` 失败：失败原因不在重设计范围（生成链路没改），但可能被 mock 变更影响——优先怀疑 export-templates mock 缺/字段名变更。
- `verify_create_mobile.py` 失败：极简/抽屉/引导模式断言，全新功能对移动端不影响。

- [ ] **Step 6: 提交**

```bash
git add tests/e2e/verify_guided_skeleton.py tests/e2e/verify_guided_flow.py
git commit -m "test(create): 适配旧 E2E 到 4 步流程 + 9 脚本全量回归通过"
```

---

## Self-Review 结论

- **Spec 覆盖**：
  - 流式展开（折叠 / 600ms 打字 / 渐显 / 换一批）✓ T1 Step 3 (`startStream`)
  - 引导 4 步 topic → 平台 → 风格 → 模板 ✓ T2 Step 3（askTemplate 在 style 后）
  - 模板 effect-card（名称 / 平台 / 适用 / 色块）✓ T2 Step 4
  - "查看完整预览" 按钮调 TemplateModal ✓ T2 Step 4 (`openFullPreview`)
  - `applyPlatformDefault` 只管字数 ✓ T2 Step 3（删 selectedTemplateKey 行）
  - 确认卡 4 行 + 4 编辑按钮 ✓ T2 Step 5
  - 编辑模式编辑完回 confirm 而非继续下一问 ✓ T2 Step 3 (`editingMode` flag) + T2 Step 5（edit 函数附 `editingMode:true`）
  - 极简/TemplateModal/后端 不动 ✓ T3 Step 5 全量回归做对照
  - 3 个新 E2E + 改造 2 个 ✓ T1/T2/T3
- **Placeholder 扫描**：无 TBD/TODO；常量（600ms、150ms、6 个标题）来自 spec 决定。
- **类型一致性**：
  - `applyPlatformDefault(p)` 签名不变，内部不再写 `selectedTemplateKey` ✓
  - `onQuickConfirm(m, opt)` 顶部依旧 `m.done = true`，新增 `m.editingMode` 可选字段不影响其他调用方（`editTopic` 没设 editingMode，旧流不变）✓
  - `TopicSuggestionBubble` `select` 事件参数为原 `topic` 对象（`{id,title,summary,used}`），与 `TopicCapsules.applyTopic` emit 内容一致 ✓
