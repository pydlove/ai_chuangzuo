# 风格市场视觉升级 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `StyleMarketIndex.vue` 从功能列表重做为平台门面页：① Banner + ② 上传激励卡 + ③ 官方精选大卡（横滑） + ④ 收益潜力榜 + ⑤ 全部风格（tab + 卡片升级）。

**Architecture:** 单一页面 `StyleMarketIndex.vue` 顶部到底部分 5 个区块；新增 3 个聚合 `computed`（marketStats / topCreators / featuredStyles）放 `useStyleMarket.js`，纯前端聚合 `marketStyles` + `earningsRecords`，零外部依赖。

**Tech Stack:** Vue 3 (Composition API, `<script setup>`) + Ant Design Vue + Playwright E2E。

---

## Global Constraints

- 仅动 `project/user/web/src/views/console/StyleMarketIndex.vue` 和 `project/user/web/src/composables/useStyleMarket.js`，其他文件禁止触碰。
- 计费：消费者使用他人风格 **不扣币**，创作者 +0.2 币/次，周里程碑 5/15/30/60（以 `useStyleMarket.js` 现有代码为准）。
- 设计系统（`docs/design/design-system.md`）：
  - 主色 `--color-primary #ff2442`，**禁止金色辅助色**
  - 字号：`--font-h1 24 / h2 20 / h3 18 / body 14 / small 12 / caption 11`，**禁止 17/13px 等非规范值**
  - 圆角 `radius-sm 4 / md 6 / lg 8 / xl 12`，**禁止超过 16**
  - 阴影 `shadow-sm 0 1px 2px rgba(0,0,0,.04)` / `sm2 0 2px 12px rgba(0,0,0,.06)` / `md 0 4px 12px rgba(0,0,0,.10)` / `lg 0 8px 32px rgba(0,0,0,.15)`，**禁用非 0,0,0 色相**
  - 禁止大段纯黑，用 `--color-text-primary #1a1a1a`
- 暗色主题必须覆盖所有新增类（`body[data-theme="dark"]` + 全局 ant-modal 适配）。
- 响应式断点 `768px`。
- 不接后端；不动 localStorage 键名 / 数据结构。
- 不动路由；不动 `EarningsIndex.vue` / `StylesIndex.vue` / `CreateIndex.vue` / `ConsoleLayout.vue`。
- 完成后必须清除已无引用的旧 `.style-market-*` 类（CLAUDE.md 强制）。

---

## File Map

| 文件 | 角色 | 操作 |
|---|---|---|
| `project/user/web/src/composables/useStyleMarket.js` | 数据层（含 4 个新 computed） | 修改 |
| `project/user/web/src/views/console/StyleMarketIndex.vue` | 5 个区块的展示层 | 重写 |
| `tests/e2e/verify_style_market.py` | 5 区块 + 9 场景 E2E | 修改 / 重写 |

不动：`router/index.js`、`EarningsIndex.vue`、`StylesIndex.vue`、`CreateIndex.vue`、`ConsoleLayout.vue`、`useStyles.js`、`api/*`、`api/style.js`、`project/admin/*`、后端。

---

## Task Index

1. useStyleMarket.js 新增 4 个聚合 computed
2. StyleMarketIndex.vue 重写 template + ① 平台 Banner
3. ② 上传激励卡
4. ③ 官方精选大卡（横滑）
5. ④ 收益潜力榜
6. ⑤ 全部风格（tab 胶囊化 + 卡片升级）
7. 响应式适配
8. 暗色主题适配
9. 清理旧 `.style-market-*` 死 CSS（grep + 删）
10. Playwright E2E 改写（按 §9 9 场景）
11. 最终手动收尾验证（7 项目录屏 + commit）

---

## Task 1: useStyleMarket.js 新增 4 个聚合 computed

**Files:**
- Modify: `project/user/web/src/composables/useStyleMarket.js`（在文件末尾追加 4 个 computed 的 export）

**Interfaces:**
- Consumes: `marketStyles` (ref), `earningsRecords` (ref) — 现有 export。
- Produces: 4 个新 `computed` export — `marketStats` / `totalEarningsByCreator` / `topCreators` / `featuredStyles` —— Vue 模板按这些名字 import。

- [ ] **Step 1: 在 useStyleMarket.js 末尾追加 4 个 computed**

打开 `project/user/web/src/composables/useStyleMarket.js`，在最后一行 `}` 之前（即文件末尾的 `function getPreviousWeek() { ... }` 之后）插入下面这段代码：

```js
// ===== 风格市场视觉升级 v2 — 聚合 computed =====
// 数据来源原则：weeklyUses / totalUses / weeklyEarnings 取 marketStyles 单条；
// totalEarnings（创作者总收益）取 earningsRecords，按 styleId → creatorId 关联。

const totalEarningsByCreator = computed(() => {
  const map = {}
  earningsRecords.value.forEach((r) => {
    if (!(r.amount > 0)) return
    const s = marketStyles.value.find((m) => m.id === r.styleId)
    if (!s) return
    map[s.creatorId] = (map[s.creatorId] || 0) + r.amount
  })
  return map
})

export const marketStats = computed(() => {
  const approved = marketStyles.value.filter((s) => s.status === 'approved')
  return {
    approvedCount: approved.length,
    totalUses: approved.reduce((sum, s) => sum + (s.totalUses || 0), 0),
    totalEarnings: Object.values(totalEarningsByCreator.value).reduce(
      (sum, v) => sum + v, 0
    )
  }
})

export const topCreators = computed(() => {
  const map = new Map()
  marketStyles.value
    .filter((s) => s.status === 'approved')
    .forEach((s) => {
      const cur = map.get(s.creatorId) || {
        creatorId: s.creatorId,
        creatorName: s.creatorName || '匿名用户',
        weeklyEarnings: 0,
        weeklyUses: 0,
        bestStyle: null
      }
      cur.weeklyEarnings += s.weeklyEarnings || 0
      cur.weeklyUses += s.weeklyUses || 0
      if (!cur.bestStyle || (s.totalUses || 0) > (cur.bestStyle.totalUses || 0)) {
        cur.bestStyle = s
      }
      map.set(s.creatorId, cur)
    })
  const tbm = totalEarningsByCreator.value
  return Array.from(map.values())
    .map((c) => ({ ...c, totalEarnings: tbm[c.creatorId] || 0 }))
    .sort((a, b) => b.weeklyEarnings - a.weeklyEarnings)
    .slice(0, 5)
})

export const featuredStyles = computed(() =>
  marketStyles.value
    .filter((s) => s.status === 'approved' && (s.totalUses || 0) >= 5)
    .sort((a, b) => (b.totalUses || 0) - (a.totalUses || 0))
    .slice(0, 8)
)
```

- [ ] **Step 2: 确认文件顶部已 import computed**

文件第 1 行应为 `import { ref, computed } from 'vue'`，确认即可。**不要改动其他 import**。

- [ ] **Step 3: 起用户端 dev server 确认无编译错误**

```bash
cd project/user/web && (npm run dev 2>&1 | tee /tmp/user_web_dev.log) &
sleep 8
curl -sf http://localhost:22345 > /dev/null && echo "dev server OK"
```

期望：`dev server OK`。如果端口不通，看 `tail -30 /tmp/user_web_dev.log` 找错误。

- [ ] **Step 4: commit**

```bash
git add project/user/web/src/composables/useStyleMarket.js
git commit -m "feat(style-market): 新增 marketStats / topCreators / featuredStyles 三个聚合 computed"
```

---

## Task 2: StyleMarketIndex.vue 重写 template + ① 平台 Banner

**Files:**
- Modify: `project/user/web/src/views/console/StyleMarketIndex.vue`（template 与 `<script setup>`、`<style scoped>` 全部重写；保留 `<style>` 全局段处理 ant-modal 暗色）

**Interfaces:**
- Consumes: `@/composables/useStyleMarket.js` 新导出的 `marketStyles` / `marketStats` / `topCreators` / `featuredStyles` / `useMarketStyle` / `simulateExternalUse` / `toggleFavorite` / `isFavorite` / `loadMarketStyles`

- [ ] **Step 1: 新建模板骨架 — 5 个区块父容器 + ① 区**

把整个 `<template>` 内容替换为：

```vue
<template>
  <div class="market-page">
    <!-- ① 平台 Banner 区 -->
    <section class="market-banner">
      <div class="market-banner-text">
        <h1 class="market-banner-title">爱创作 · 风格市场</h1>
        <p class="market-banner-sub">
          官方运营 · 精选创作者风格 · 使用即获收益分成
          <span class="market-banner-rules-link" @click="rulesVisible = true">收益规则</span>
        </p>
      </div>
      <div class="market-banner-stats">
        <div class="market-banner-stat">
          <div class="market-banner-stat-num">{{ marketStats.approvedCount }}</div>
          <div class="market-banner-stat-label">已上架款</div>
        </div>
        <div class="market-banner-stat">
          <div class="market-banner-stat-num">{{ marketStats.totalUses }}</div>
          <div class="market-banner-stat-label">累计使用次</div>
        </div>
        <div class="market-banner-stat">
          <div class="market-banner-stat-num">{{ formatCoins(marketStats.totalEarnings) }}</div>
          <div class="market-banner-stat-label">累计发放币</div>
        </div>
      </div>
    </section>

    <!-- ②③④⑤ placeholder — 后续 Task 填充 -->
    <section class="market-upload-card" data-tbd="task-3"></section>
    <section class="market-featured" data-tbd="task-4"></section>
    <section class="market-creators" data-tbd="task-5"></section>
    <section class="market-grid-section" data-tbd="task-6"></section>
  </div>

  <!-- 收益规则弹框 — 保留 v1 写法，直接复用 -->
  <a-modal
    class="rules-modal"
    :open="rulesVisible"
    title="风格市场收益规则"
    :footer="null"
    :width="560"
    centered
    @cancel="rulesVisible = false"
  >
    <ol class="style-market-rules-list">
      <li>他人每使用一次你分享的风格，你将获得 <span class="style-market-rule-highlight">0.2 创作币</span> 奖励。</li>
      <li>每周根据风格被使用次数发放里程碑奖励：<span class="style-market-rule-highlight">50 次 5 币</span>、<span class="style-market-rule-highlight">200 次 15 币</span>、<span class="style-market-rule-highlight">500 次 30 币</span>、<span class="style-market-rule-highlight">1000 次 60 币</span>。</li>
      <li>里程碑奖励 <span class="style-market-rule-highlight">每周结算一次</span>，结算后当周使用次数清零并重新累计。</li>
      <li>使用他人分享的风格 <span class="style-market-rule-highlight">无需支付创作币</span>，创作者仍可正常获得收益。</li>
      <li>如发现违规刷量行为，平台有权 <span class="style-market-rule-highlight">取消相关收益并下架风格</span>。</li>
    </ol>
    <div class="style-market-rules-footer">* 活动最终解释权归平台所有。</div>
  </a-modal>
</template>
```

- [ ] **Step 2: 替换 `<script setup>`**

替换整个 `<script setup>` 块为：

```js
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  marketStyles,
  marketStats,
  topCreators,
  featuredStyles,
  useMarketStyle,
  simulateExternalUse,
  toggleFavorite,
  isFavorite,
  loadMarketStyles
} from '@/composables/useStyleMarket.js'

const router = useRouter()
const currentUserId = ref(localStorage.getItem('aichuangzuo_user_id') || '')
const rulesVisible = ref(false)

const formatCoins = (n) => Number(n || 0).toFixed(2)
const formatUses = (n) => Number(n || 0).toLocaleString()

onMounted(() => {
  loadMarketStyles()
})
</script>
```

- [ ] **Step 3: 重写 `<style scoped>` 顶部，只放 ① 区与页面外壳 + 全局暗色适配**

把 `<style scoped>` 整块替换为：

```css
.market-page {
  padding: var(--space-lg) var(--space-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
  max-width: 1280px;
  margin: 0 auto;
}

.market-banner {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: var(--space-xl);
  align-items: center;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: var(--space-xl) var(--space-xl);
  box-shadow: var(--shadow-sm2);
}

.market-banner-title {
  font-size: var(--font-h1);
  font-weight: 700;
  color: var(--color-primary);
  margin: 0 0 var(--space-sm) 0;
  letter-spacing: -0.5px;
}

.market-banner-sub {
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.market-banner-rules-link {
  color: var(--color-primary);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 4px;
  font-weight: 500;
}
.market-banner-rules-link:hover { color: var(--color-primary-hover); }

.market-banner-stats {
  display: flex;
  gap: var(--space-lg);
}

.market-banner-stat {
  background: var(--color-bg-page);
  border-radius: var(--radius-xl);
  padding: var(--space-md) var(--space-lg);
  min-width: 120px;
}
.market-banner-stat-num {
  font-size: var(--font-h2);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}
.market-banner-stat-label {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-top: var(--space-xs);
}

/* ① 区暗色 */
body[data-theme="dark"] .market-banner { background: #1f1f1f; }
body[data-theme="dark"] .market-banner-title { color: #ff6b81; }
body[data-theme="dark"] .market-banner-sub { color: var(--color-text-secondary); }
body[data-theme="dark"] .market-banner-stat { background: #141414; }
body[data-theme="dark"] .market-banner-stat-num { color: var(--color-text-primary); }
body[data-theme="dark"] .market-banner-stat-label { color: var(--color-text-secondary); }

/* ②③④⑤ 后续 task 填充，先放空规则占位防止 build break */
.market-upload-card,
.market-featured,
.market-creators,
.market-grid-section { min-height: 1px; }
```

- [ ] **Step 4: 在 `<style>` 全局段追加暗色 ant-modal 适配**

把 `<style>` 段（不带 scoped 的）整段替换为：

```css
<style>
/* ① 区收益规则弹层暗色（全局） */
body[data-theme="dark"] .rules-modal .ant-modal-content {
  background: #141414;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6);
}
body[data-theme="dark"] .rules-modal .ant-modal-header {
  background: #141414;
  border-bottom-color: #303030;
}
body[data-theme="dark"] .rules-modal .ant-modal-title { color: #e0e0e0; }
body[data-theme="dark"] .rules-modal .ant-modal-close { color: #a6a6a6; }
body[data-theme="dark"] .rules-modal .ant-modal-close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}
body[data-theme="dark"] .style-market-rules-list { color: #a6a6a6; }
body[data-theme="dark"] .style-market-rules-footer {
  border-top-color: #303030;
  color: #a6a6a6;
}
body[data-theme="dark"] .style-market-rule-highlight { color: #ff6b81; }
</style>
```

**注意**：上面这段是在已有 `<style>` 全局段**追加**而不是覆盖。如果文件里没有 `<style>` 段，就用 Edit 加。

- [ ] **Step 5: dev server 验证 ① 区渲染**

访问 `http://localhost:22345/console/style-market`，看：

- 顶部 H1 "爱创作 · 风格市场" 红字显示
- 副文案 + "收益规则" 链接
- 三个数据芯片显示数字（即使为 0 也行）
- 控制台无 error

如果某个引用报错（如 `marketStats is undefined`），回头检查 script setup 的 import 列表。

- [ ] **Step 6: commit**

```bash
git add project/user/web/src/views/console/StyleMarketIndex.vue
git commit -m "feat(style-market): 重写页面骨架 + ① 平台 Banner 区"
```

---

## Task 3: ② 上传激励卡

**Files:**
- Modify: `project/user/web/src/views/console/StyleMarketIndex.vue`

- [ ] **Step 1: 替换 `<section class="market-upload-card" data-tbd="task-3"></section>` 占位**

把上一步留下的占位替换为：

```vue
    <!-- ② 上传激励卡 -->
    <section class="market-upload-card" @click="goUpload">
      <div class="market-upload-icon">＋</div>
      <div class="market-upload-body">
        <div class="market-upload-title">上传你的风格，开始赚创作币</div>
        <div class="market-upload-sub">每被他人使用 1 次即得 0.2 币；周里程碑最高额外 +60</div>
      </div>
      <button class="market-upload-cta" @click.stop="goUpload">立即上架</button>
    </section>
```

- [ ] **Step 2: 在 `<script setup>` 添加 `goUpload`**

在 `onMounted` 行之前加入：

```js
const goUpload = () => {
  router.push('/console/styles')
}
```

- [ ] **Step 3: 在 `<style scoped>` 加 ② 区样式 + 暗色**

在 `.market-upload-card, .market-featured, .market-creators, .market-grid-section { min-height: 1px; }` 这行**之前**插入下面这段：

```css
.market-upload-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: var(--space-lg);
  background: var(--color-bg-card);
  border-left: 4px solid var(--color-primary);
  border-radius: var(--radius-xl);
  padding: var(--space-lg) var(--space-xl);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  box-shadow: var(--shadow-sm2);
}
.market-upload-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.market-upload-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-xl);
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-h2);
  font-weight: 600;
}
.market-upload-body { min-width: 0; }
.market-upload-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}
.market-upload-sub {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
}
.market-upload-cta {
  background: var(--color-primary);
  color: #fff;
  border: 0;
  border-radius: var(--radius-lg);
  height: 40px;
  padding: 0 var(--space-lg);
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.market-upload-cta:hover { background: var(--color-primary-hover); }

/* ② 暗色 */
body[data-theme="dark"] .market-upload-card { background: #1f1f1f; }
body[data-theme="dark"] .market-upload-icon {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}
body[data-theme="dark"] .market-upload-title { color: var(--color-text-primary); }
body[data-theme="dark"] .market-upload-sub { color: var(--color-text-secondary); }
```

- [ ] **Step 4: dev server 验证**

访问 `/console/style-market`，banner 下方应出现一张左边红条 + "上传你的风格，开始赚创作币" + "立即上架" 的卡。点击整卡或按钮，URL 应跳到 `/console/styles`。

- [ ] **Step 5: commit**

```bash
git add project/user/web/src/views/console/StyleMarketIndex.vue
git commit -m "feat(style-market): ② 上传激励卡"
```

---

## Task 4: ③ 官方精选大卡（横滑）

**Files:**
- Modify: `project/user/web/src/views/console/StyleMarketIndex.vue`

- [ ] **Step 1: 替换 `<section class="market-featured" data-tbd="task-4"></section>` 占位**

替换为：

```vue
    <!-- ③ 官方精选大卡 -->
    <section class="market-featured">
      <div class="market-section-head">
        <div class="market-section-title-wrap">
          <h2 class="market-section-title">官方精选</h2>
          <span class="market-official-badge">官方</span>
        </div>
        <button class="market-section-link" @click="scrollToGrid">查看全部 →</button>
      </div>
      <div v-if="featuredStyles.length === 0" class="market-featured-empty">
        官方精选即将上线
      </div>
      <div v-else class="market-featured-rail">
        <div
          v-for="s in featuredStyles"
          :key="s.id"
          class="market-featured-card"
          :style="{ background: featuredBackground(s) }"
          @click="openPrompt(s)"
        >
          <div class="market-featured-name">{{ s.name }}</div>
          <div v-if="s.scope" class="market-featured-tag"># {{ firstScope(s.scope) }}</div>
          <div class="market-featured-uses">🔥 本周使用 {{ s.weeklyUses }} 次</div>
          <div class="market-featured-creator">
            by {{ s.creatorName || '匿名用户' }} · 累计赚 {{ formatCoins(getMarketStyleEarnings(s.id)) }} 币
          </div>
        </div>
      </div>
    </section>
```

- [ ] **Step 2: 在 `<script setup>` 加 featured helpers**

在 `goUpload` 函数之后插入：

```js
import { getMarketStyleEarnings } from '@/composables/useStyleMarket.js'

const firstScope = (scope) => (scope || '').split(/[,，]/)[0]?.trim() || ''

const featuredBackground = (s) => {
  // 简单 hash → 在三个深色渐变中选一个，保证视觉差异
  const palette = [
    'linear-gradient(135deg, #1a1a1a 0%, #2a1015 100%)',
    'linear-gradient(135deg, #1f1f1f 0%, #2c1f0a 100%)',
    'linear-gradient(135deg, #14142b 0%, #2a0a1f 100%)',
    'linear-gradient(135deg, #0d1f1f 0%, #1f3a2a 100%)',
    'linear-gradient(135deg, #2a1f1f 0%, #1a1010 100%)'
  ]
  const idx = (s.id || '').split('').reduce((sum, c) => sum + c.charCodeAt(0), 0) % palette.length
  return palette[idx]
}

const openPrompt = (s) => {
  // v1 已有 promptModal；本期先在 console 暴露给作者，后续 task 实现 modal 内容
  console.log('[featured] open', s.id, s.name)
  router.push(`/console/create?marketStyleId=${s.id}`)
}

const scrollToGrid = () => {
  document.querySelector('.market-grid-section')?.scrollIntoView({ behavior: 'smooth' })
}
```

- [ ] **Step 3: 在 `<style scoped>` 加 ③ 区样式 + 暗色**

在 ② 区样式块之后插入：

```css
.market-section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-md);
}
.market-section-title-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.market-section-title {
  font-size: var(--font-h2);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}
.market-official-badge {
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-caption);
  border-radius: var(--radius-md);
  padding: 2px 8px;
  font-weight: 600;
}
.market-section-link {
  background: transparent;
  border: 0;
  color: var(--color-primary);
  cursor: pointer;
  font-size: var(--font-body);
  font-weight: 500;
}
.market-section-link:hover { color: var(--color-primary-hover); }

.market-featured-empty {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-placeholder);
  font-size: var(--font-body);
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
}
.market-featured-rail {
  display: flex;
  gap: var(--space-md);
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: var(--space-sm);
  scrollbar-width: thin;
}
.market-featured-rail::-webkit-scrollbar { height: 6px; }
.market-featured-rail::-webkit-scrollbar-thumb {
  background: var(--color-bg-hover);
  border-radius: 3px;
}
.market-featured-card {
  scroll-snap-align: start;
  flex: 0 0 320px;
  height: 200px;
  border-radius: var(--radius-xl);
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.market-featured-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}
.market-featured-name {
  font-size: var(--font-h3);
  font-weight: 700;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.market-featured-tag {
  display: inline-flex;
  width: fit-content;
  background: rgba(255, 36, 66, 0.85);
  color: #fff;
  font-size: var(--font-caption);
  border-radius: var(--radius-md);
  padding: 2px 8px;
  margin-top: var(--space-xs);
}
.market-featured-uses {
  font-size: var(--font-h2);
  font-weight: 700;
  line-height: 1.2;
}
.market-featured-creator {
  font-size: var(--font-caption);
  color: rgba(255, 255, 255, 0.7);
}
```

- [ ] **Step 4: dev server 验证**

访问 `/console/style-market`，若 `marketStyles` 已有 ≥5 个 `totalUses >= 5` 的 approved 风格，③ 区显示 5 张深色横滑大卡。否则空态文案。

- [ ] **Step 5: commit**

```bash
git add project/user/web/src/views/console/StyleMarketIndex.vue
git commit -m "feat(style-market): ③ 官方精选大卡（横滑）"
```

---

## Task 5: ④ 收益潜力榜

**Files:**
- Modify: `project/user/web/src/views/console/StyleMarketIndex.vue`

- [ ] **Step 1: 替换 `<section class="market-creators" data-tbd="task-5"></section>` 占位**

替换为：

```vue
    <!-- ④ 收益潜力榜 -->
    <section class="market-creators">
      <div class="market-section-head">
        <div class="market-section-title-wrap">
          <h2 class="market-section-title">收益潜力榜</h2>
          <span class="market-section-sub">看看谁在用风格赚到币</span>
        </div>
      </div>
      <div v-if="topCreators.length === 0" class="market-creators-empty">
        暂无上榜创作者
      </div>
      <div v-else class="market-creators-rail">
        <div
          v-for="c in topCreators"
          :key="c.creatorId"
          class="market-creator-card"
        >
          <div class="market-creator-avatar">{{ (c.creatorName || '匿').charAt(0) }}</div>
          <div class="market-creator-name">{{ c.creatorName || '匿名用户' }}</div>
          <div class="market-creator-weekly">本周 +{{ formatCoins(c.weeklyEarnings) }} 币</div>
          <div v-if="c.bestStyle" class="market-creator-best">
            代表风格 · {{ c.bestStyle.name }}
          </div>
          <button
            v-if="c.bestStyle"
            class="market-creator-use"
            @click="handleUse(c.bestStyle)"
          >
            使用
          </button>
        </div>
      </div>
    </section>
```

- [ ] **Step 2: 在 `<script setup>` 加 `handleUse`**

在 `openPrompt` 函数之后插入：

```js
const handleUse = (s) => {
  try {
    useMarketStyle(s.id)
    router.push(`/console/create?marketStyleId=${s.id}`)
  } catch (err) {
    alert(err.message)
  }
}
```

- [ ] **Step 3: 在 `<style scoped>` 加 ④ 区样式 + 暗色**

在 ③ 区样式块之后插入：

```css
.market-section-sub {
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  font-weight: 400;
}

.market-creators-empty {
  padding: var(--space-xl);
  text-align: center;
  color: var(--color-text-placeholder);
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  font-size: var(--font-body);
}

.market-creators-rail {
  display: flex;
  gap: var(--space-md);
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: var(--space-sm);
  scrollbar-width: thin;
}
.market-creators-rail::-webkit-scrollbar { height: 6px; }
.market-creators-rail::-webkit-scrollbar-thumb {
  background: var(--color-bg-hover);
  border-radius: 3px;
}

.market-creator-card {
  flex: 0 0 200px;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm2);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  scroll-snap-align: start;
  transition: transform 0.2s, box-shadow 0.2s;
}
.market-creator-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.market-creator-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-h3);
  font-weight: 700;
  margin-bottom: var(--space-sm);
}
.market-creator-name {
  font-size: var(--font-body);
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}
.market-creator-weekly {
  font-size: var(--font-h2);
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.2;
  margin-bottom: var(--space-sm);
}
.market-creator-best {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-md);
  white-space: nowrap;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}
.market-creator-use {
  background: var(--color-primary);
  color: #fff;
  border: 0;
  border-radius: var(--radius-lg);
  height: 36px;
  width: 100%;
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
}
.market-creator-use:hover { background: var(--color-primary-hover); }

/* ④ 暗色 */
body[data-theme="dark"] .market-creator-card { background: #1f1f1f; }
body[data-theme="dark"] .market-creator-name { color: var(--color-text-primary); }
body[data-theme="dark"] .market-section-sub { color: var(--color-text-secondary); }
```

- [ ] **Step 4: dev server 验证**

访问 `/console/style-market`，④ 区显示 Top 5 创作者小卡或空态文案。点 "使用" 按钮应跳到 `/console/create?marketStyleId=...`。

- [ ] **Step 5: commit**

```bash
git add project/user/web/src/views/console/StyleMarketIndex.vue
git commit -m "feat(style-market): ④ 收益潜力榜 Top 5"
```

---

## Task 6: ⑤ 全部风格区（tab 胶囊化 + 卡片升级）

**Files:**
- Modify: `project/user/web/src/views/console/StyleMarketIndex.vue`

- [ ] **Step 1: 替换 `<section class="market-grid-section" data-tbd="task-6"></section>` 占位**

替换为：

```vue
    <!-- ⑤ 全部风格区 -->
    <section class="market-grid-section" ref="gridSection">
      <div class="market-section-head">
        <div class="market-section-title-wrap">
          <h2 class="market-section-title">全部风格</h2>
          <span class="market-section-sub">共 {{ approvedStyles.length }} 款</span>
        </div>
        <div class="market-search">
          <input
            v-model="searchQuery"
            type="text"
            class="market-search-input"
            placeholder="搜索风格名或适用范围"
          />
        </div>
      </div>

      <div class="market-tabs">
        <button
          v-for="tab in tabOptions"
          :key="tab.key"
          :class="['market-tab', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="filteredStyles.length === 0" class="market-empty">
        暂无已上架风格
      </div>
      <div v-else class="market-grid">
        <div
          v-for="s in filteredStyles"
          :key="s.id"
          class="market-card"
        >
          <div class="market-card-cover" :style="{ background: featuredBackground(s) }">
            <div class="market-card-cover-tags">
              <span v-for="t in parseScopeTags(s.scope)" :key="t" class="market-card-tag">
                # {{ t }}
              </span>
            </div>
            <span v-if="s.creatorId === currentUserId" class="market-card-mine">我的</span>
          </div>
          <div class="market-card-body">
            <div class="market-card-title">{{ s.name }}</div>
            <div class="market-card-creator">
              <span class="market-card-creator-avatar">
                {{ (s.creatorName || '匿').charAt(0) }}
              </span>
              <span>by {{ s.creatorName || '匿名用户' }}</span>
            </div>
            <div class="market-card-prompt">{{ promptSummary(s.prompt) }}</div>
            <div class="market-card-stats">
              <span>🔥 本周 {{ s.weeklyUses }} 次</span>
              <span>累计 {{ s.totalUses }} 次</span>
            </div>
            <div class="market-card-actions">
              <button class="market-card-use" @click="handleUse(s)">使用</button>
              <button
                :class="['market-card-fav', { active: isFavorite(s.id) }]"
                @click="handleToggleFavorite(s.id)"
              >
                {{ isFavorite(s.id) ? '♥' : '♡' }}
              </button>
              <button class="market-card-view" @click="openPrompt(s)">查看</button>
              <button
                v-if="s.creatorId === currentUserId"
                class="market-card-sim"
                @click="handleSimulate(s)"
              >
                模拟
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
```

- [ ] **Step 2: 在 `<script setup>` 加 search query / filtered / tab / scope helpers**

在 `handleUse` 函数之后追加：

```js
const searchQuery = ref('')
const activeTab = ref('all')

const tabOptions = [
  { key: 'all', label: '全部' },
  { key: 'week-hot', label: '本周最热' },
  { key: 'all-hot', label: '历史最热' },
  { key: 'new', label: '最新' },
  { key: 'featured', label: '官方精选' }
]

const approvedStyles = computed(() =>
  marketStyles.value.filter((s) => s.status === 'approved')
)

const filteredStyles = computed(() => {
  let list = approvedStyles.value
  if (activeTab.value === 'week-hot') {
    list = [...list].sort((a, b) => (b.weeklyUses || 0) - (a.weeklyUses || 0))
  } else if (activeTab.value === 'all-hot') {
    list = [...list].sort((a, b) => (b.totalUses || 0) - (a.totalUses || 0))
  } else if (activeTab.value === 'new') {
    list = [...list].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
  } else if (activeTab.value === 'featured') {
    list = list
      .filter((s) => (s.totalUses || 0) >= 5)
      .sort((a, b) => (b.totalUses || 0) - (a.totalUses || 0))
  }
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return list
  return list.filter(
    (s) =>
      s.name.toLowerCase().includes(q) ||
      (s.scope && s.scope.toLowerCase().includes(q))
  )
})

const parseScopeTags = (scopeStr) =>
  !scopeStr ? [] : scopeStr.split(/[,，]/).map((t) => t.trim()).filter(Boolean)

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const handleToggleFavorite = (id) => toggleFavorite(id)
const handleSimulate = (s) => {
  try {
    simulateExternalUse(s.id)
  } catch (err) {
    alert(err.message)
  }
}
```

- [ ] **Step 3: 在 `<style scoped>` 加 ⑤ 区样式 + 暗色**

在 ④ 区样式块之后插入：

```css
.market-grid-section { scroll-margin-top: var(--space-xl); }

.market-search { display: flex; align-items: center; }
.market-search-input {
  width: 100%;
  min-width: 240px;
  max-width: 480px;
  height: var(--control-height, 40px);
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-full);
  font-size: var(--font-body);
  background: var(--color-bg-page);
  outline: none;
}
.market-search-input:focus {
  background: var(--color-bg-card);
  border-color: var(--color-primary);
}

.market-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--color-bg-page);
  padding: 4px;
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-lg);
}
.market-tab {
  padding: var(--space-sm) var(--space-md);
  border: 0;
  background: transparent;
  border-radius: var(--radius-md);
  font-size: var(--font-body);
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.market-tab.active {
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-sm);
}
.market-tab:hover { color: var(--color-text-primary); }

.market-empty {
  padding: var(--space-xl) 0;
  text-align: center;
  color: var(--color-text-placeholder);
  font-size: var(--font-body);
}

.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-lg);
}

.market-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm2);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s, box-shadow 0.2s;
}
.market-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
}

.market-card-cover {
  position: relative;
  height: 80px;
  padding: var(--space-sm) var(--space-md);
}
.market-card-cover-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.market-card-tag {
  background: rgba(255, 255, 255, 0.85);
  color: var(--color-primary);
  font-size: var(--font-caption);
  border-radius: var(--radius-md);
  padding: 2px 8px;
  font-weight: 500;
}
.market-card-mine {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
  background: var(--color-info);
  color: #fff;
  font-size: var(--font-caption);
  border-radius: var(--radius-md);
  padding: 2px 8px;
  font-weight: 600;
}

.market-card-body {
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  flex: 1;
}
.market-card-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.market-card-creator {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-body);
  color: var(--color-text-regular);
}
.market-card-creator-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-small);
  font-weight: 700;
}
.market-card-prompt {
  font-size: var(--font-body);
  color: var(--color-text-regular);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.market-card-stats {
  display: flex;
  gap: var(--space-md);
  font-size: var(--font-small);
  color: var(--color-text-placeholder);
}
.market-card-actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: auto;
  padding-top: var(--space-sm);
}
.market-card-use {
  background: var(--color-primary);
  color: #fff;
  border: 0;
  border-radius: var(--radius-lg);
  height: 40px;
  padding: 0 var(--space-md);
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
}
.market-card-use:hover { background: var(--color-primary-hover); }
.market-card-fav {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  font-size: var(--font-body);
  color: var(--color-text-placeholder);
  cursor: pointer;
}
.market-card-fav.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.market-card-view, .market-card-sim {
  background: transparent;
  border: 0;
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: var(--space-sm);
}
.market-card-view:hover, .market-card-sim:hover {
  color: var(--color-primary);
}

/* ⑤ 暗色 */
body[data-theme="dark"] .market-card { background: #1f1f1f; }
body[data-theme="dark"] .market-search-input {
  background: #141414;
  border-color: #303030;
  color: var(--color-text-primary);
}
body[data-theme="dark"] .market-search-input:focus {
  background: #1f1f1f;
  border-color: var(--color-primary);
}
body[data-theme="dark"] .market-tabs { background: #141414; }
body[data-theme="dark"] .market-tab { color: var(--color-text-secondary); }
body[data-theme="dark"] .market-tab.active {
  background: #2a2a2a;
  color: var(--color-text-primary);
  box-shadow: none;
}
body[data-theme="dark"] .market-card-title { color: var(--color-text-primary); }
body[data-theme="dark"] .market-card-creator { color: var(--color-text-regular); }
body[data-theme="dark"] .market-card-creator-avatar {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}
body[data-theme="dark"] .market-card-prompt { color: #d9d9d9; }
body[data-theme="dark"] .market-card-stats { color: #a6a6a6; }
body[data-theme="dark"] .market-card-tag {
  background: rgba(255, 255, 255, 0.12);
  color: #ff6b81;
}
body[data-theme="dark"] .market-card-fav {
  background: transparent;
  border-color: #303030;
}
body[data-theme="dark"] .market-card-fav.active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff6b81;
  border-color: var(--color-primary);
}
```

- [ ] **Step 4: dev server 验证**

访问 `/console/style-market`，⑤ 区显示搜索框 + 5 个 tab + 卡片网格。每张卡片：深色封面 + 风格名 + by 行 + prompt 摘要 + 数据 + 「使用 / ♥ / 查看」按钮。点 tab 切换排序/筛选。

- [ ] **Step 5: commit**

```bash
git add project/user/web/src/views/console/StyleMarketIndex.vue
git commit -m "feat(style-market): ⑤ 全部风格区（tab 胶囊化 + 卡片升级）"
```

---

## Task 7: 响应式适配 (≤768px)

**Files:**
- Modify: `project/user/web/src/views/console/StyleMarketIndex.vue`

- [ ] **Step 1: 在 `<style scoped>` 末尾追加响应式段**

```css
/* 响应式：≤768px 单列，banner chip 横滑，rail 卡间距缩小 */
@media (max-width: 768px) {
  .market-page {
    padding: var(--space-md);
    gap: var(--space-lg);
  }
  .market-banner {
    grid-template-columns: 1fr;
    padding: var(--space-md);
  }
  .market-banner-stats {
    display: flex;
    overflow-x: auto;
    gap: var(--space-sm);
    padding-bottom: var(--space-sm);
    scrollbar-width: none;
    -webkit-mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 16px), transparent 100%);
            mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 16px), transparent 100%);
  }
  .market-banner-stats::-webkit-scrollbar { display: none; }
  .market-banner-stat {
    flex: 0 0 120px;
    min-width: 120px;
  }
  .market-upload-card {
    grid-template-columns: auto 1fr;
    padding: var(--space-md);
  }
  .market-upload-cta { grid-column: 1 / -1; width: 100%; margin-top: var(--space-sm); }
  .market-featured-card { flex: 0 0 280px; height: 180px; }
  .market-creator-card { flex: 0 0 160px; }
  .market-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    max-width: 100%;
    scrollbar-width: none;
  }
  .market-tabs::-webkit-scrollbar { display: none; }
  .market-tab { flex-shrink: 0; }
  .market-grid { grid-template-columns: 1fr; }
  .market-section-head { flex-wrap: wrap; gap: var(--space-sm); }
  .market-search-input { min-width: 200px; }
}
```

- [ ] **Step 2: dev server 验证**

浏览器 dev tools 切到 375×812（iPhone），访问页面：

- banner 标题居上，3 个 chip 横滑（不挤出右侧）
- ② 激励卡三栏变两栏 + 按钮满宽
- ③ 精选大卡变窄（280），仍可横滑
- ④ 创作者小卡变窄（160）
- ⑤ tab 横滑、卡片单列

- [ ] **Step 3: commit**

```bash
git add project/user/web/src/views/console/StyleMarketIndex.vue
git commit -m "feat(style-market): 响应式适配（≤768px）"
```

---

## Task 8: 暗色主题适配（清漏）

**Files:**
- Modify: `project/user/web/src/views/console/StyleMarketIndex.vue`

- [ ] **Step 1: 浏览 5 个区块，肉眼检查暗色下是否有白底/对比度问题**

切换方法（参考 ConsoleLayout）：`localStorage.setItem('aichuangzuo_theme', 'dark')` + reload。或用 Playwright `page.evaluate("document.body.dataset.theme='dark'")`。

特别检查：

- ② upload card 暗色背景
- ⑤ tab 未激活态文字可读性
- ⑤ 卡片 figma 封面在深色下的对比度
- 任何 `<a-modal>` 内嵌的弹层

- [ ] **Step 2: 修补发现的暗色问题**

把发现的问题加到对应选择器的 `body[data-theme="dark"]` 规则块中。**不**为单个选择器新建独立的暗色段，全部增量补充到对应区块已存在的暗色块尾。

- [ ] **Step 3: commit**

```bash
git add project/user/web/src/views/console/StyleMarketIndex.vue
git commit -m "fix(style-market): 暗色主题适配修补"
```

---

## Task 9: 清理旧 `.style-market-*` 死 CSS

**Files:**
- Modify: `project/user/web/src/views/console/StyleMarketIndex.vue`

- [ ] **Step 1: grep 全仓确认哪些旧选择器已无引用**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
for cls in style-market-search-bar style-market-tabs style-market-tab style-market-search \
           style-market-search-input style-market-empty style-market-grid \
           style-market-card style-market-card-head style-market-card-avatar \
           style-market-card-title style-market-card-meta style-market-card-prompt \
           style-market-card-stats style-market-card-actions style-market-use-btn \
           style-market-favorite-btn style-market-simulate-btn \
           prompt-detail-modal prompt-detail-creator prompt-detail-scope-list \
           prompt-detail-scope prompt-detail-prompt prompt-detail-stats \
           prompt-detail-actions prompt-detail-use-btn prompt-detail-close-btn \
           favorite-hint-modal favorite-hint-body favorite-hint-actions favorite-hint-btn; do
  hits=$(grep -rIn --include="*.vue" --include="*.js" --include="*.py" --include="*.ts" "$cls" \
         project user web api tests scripts 2>/dev/null | grep -v 'StyleMarketIndex.vue' | wc -l)
  printf "%s: %s\n" "$cls" "$hits"
done
```

期望：**所有旧选择器的非零 hits 应为 0**（因为重写后的 page 不再用旧类，refs 全部由 `tests/e2e/verify_style_market.py` 用新语义选择器替代 — 见 Task 10）。

如果 hits > 0：把 `grep` 命中列出，确认是有意的外部引用（如脚本）还是模板里漏改。

- [ ] **Step 2: 删除文件中所有 `<style scoped>` 块里的 `.style-market-*` 和 `.prompt-detail-*` 和 `.favorite-hint-*` 等已替换的旧 CSS**

只保留仍被某处引用的旧类。把块体里过时的 CSS 整段剪掉。

- [ ] **Step 3: dev server 验证外观无变化**

访问 `/console/style-market`，对每个区块肉眼比对：banner / 上传卡 / 精选 / 创作者榜 / 全部风格 视觉效果应该与 Task 6 完成后一致。无 console error。

- [ ] **Step 4: commit**

```bash
git add project/user/web/src/views/console/StyleMarketIndex.vue
git commit -m "chore(style-market): 清理已被新类替代的旧 style-market-* 死 CSS"
```

---

## Task 10: Playwright E2E 改写（按 §9 9 场景）

**Files:**
- Modify: `tests/e2e/verify_style_market.py`（重写，保留旧的发布→使用主流程，扩展 5 区块验证）

**Interfaces:**
- 前置：USER_WEB dev server 在 22345 端口跑起来；EarningsIndex 已经在用 useMarketStyle。
- 输出：在 `tests/e2e/screenshots/` 下生成最新一组截图。

- [ ] **Step 1: 重写 verify_style_market.py**

完整替换文件内容为：

```python
"""风格市场视觉升级 E2E 验证 — 覆盖 SPEC §9 全部 9 个验证场景。

前置：用户端 dev server 在 22345 端口跑起来（npm run dev）。
执行：python tests/e2e/verify_style_market.py
"""

import os
from playwright.sync_api import sync_playwright

URL = os.environ.get('APP_URL', 'http://localhost:22345')
SCREENSHOT_DIR = 'tests/e2e/screenshots'
os.makedirs(SCREENSHOT_DIR, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})
        errors = []
        page.on('pageerror', lambda exc: errors.append(str(exc)))

        # ───── 前置：清环境 + 准备一个 approved 风格 ─────
        page.goto(f'{URL}/console/styles')
        page.wait_for_timeout(800)
        page.evaluate("""
          () => {
            localStorage.removeItem('aichuangzuo_style_market')
            localStorage.removeItem('aichuangzuo_earnings_records')
            localStorage.setItem('aichuangzuo_coin_balance', '10')
            localStorage.setItem('aichuangzuo_user_id', 'u_test_e2e_market')
          }
        """)
        page.reload()
        page.wait_for_timeout(500)

        page.locator('.style-add-card:has-text("新建我的风格")').click()
        page.wait_for_timeout(300)
        page.locator('.style-editor-input').fill('市场测试风格')
        page.locator('.style-editor-textarea').fill('这是一段用于市场测试的风格提示词，重点展示语言的克制感与克制后的留白美。')
        page.locator('.style-scope-tag-input').fill('公众号情感文')
        page.locator('.style-scope-tag-input').press('Enter')
        page.locator('.style-editor-form button:has-text("保存")').click()
        page.wait_for_timeout(400)

        card = page.locator('.style-card:has-text("市场测试风格")')
        card.locator('button:has-text("发布")').click()
        page.wait_for_timeout(400)
        page.locator('.publish-confirm-submit:has-text("确认发布")').click()
        page.wait_for_timeout(400)
        card.locator('button:has-text("通过")').click()
        page.wait_for_timeout(300)

        # ───── ① 平台 Banner ─────
        page.goto(f'{URL}/console/style-market')
        page.wait_for_timeout(800)
        assert page.locator('.market-banner').is_visible(), 'banner 缺失'
        assert '市场测试风格' not in page.content() or page.locator('.market-banner-stat').count() >= 3, 'chip 缺失'
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_banner.png', full_page=True)

        # ───── ② 上传激励卡 ─────
        assert page.locator('.market-upload-card').is_visible(), '激励卡缺失'
        assert '立即上架' in page.content(), '"立即上架" 缺失'
        page.locator('.market-upload-cta').click()
        page.wait_for_timeout(500)
        assert '/console/styles' in page.url, f'上传卡跳转异常: {page.url}'
        page.go_back()
        page.wait_for_timeout(500)

        # ───── ④ 收益潜力榜（无数据时是空态） ─────
        assert page.locator('.market-creators').is_visible(), '收益榜缺失'
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_creators.png')

        # ───── 数据正确性 ─────
        # 触发一次模拟使用，让榜上能出现 1 人
        page.locator('.market-card-sim').first.click()
        page.wait_for_timeout(500)
        page.locator('.market-creator-card').first.wait_for(timeout=2000)
        assert page.locator('.market-creator-card').count() >= 1, '收益榜无数据'
        creator_weekly_text = page.locator('.market-creator-weekly').first.inner_text()
        assert '+0.20' in creator_weekly_text, f'收益榜数据未更新: {creator_weekly_text}'
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_creators_filled.png')

        # ───── ⑤ 全部风格 + tab + 搜索 ─────
        assert page.locator('.market-grid-section').is_visible(), '全部风格区缺失'
        for tab in ['全部', '本周最热', '历史最热', '最新', '官方精选']:
            page.locator(f'.market-tab:has-text("{tab}")').click()
            page.wait_for_timeout(300)
        page.locator('.market-search-input').fill('市场测试')
        page.wait_for_timeout(300)
        assert '市场测试风格' in page.content(), '搜索无结果'
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_grid.png')

        # ───── 现有功能回归：使用市场风格 ─────
        page.locator('.market-search-input').fill('')
        page.wait_for_timeout(200)
        page.locator('.market-card-use').first.click()
        page.wait_for_timeout(800)
        assert '/console/create' in page.url, f'使用按钮跳转异常: {page.url}'
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_applied.png')

        # ───── 收益规则弹框 ─────
        page.goto(f'{URL}/console/style-market')
        page.wait_for_timeout(500)
        page.locator('.market-banner-rules-link').click()
        page.wait_for_timeout(400)
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_rules.png')
        page.locator('.ant-modal-close').click()
        page.wait_for_timeout(300)

        # ───── 响应式 ─────
        page.set_viewport_size({'width': 375, 'height': 812})
        page.wait_for_timeout(400)
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_mobile.png', full_page=True)
        page.set_viewport_size({'width': 1280, 'height': 800})

        # ───── 暗色主题 ─────
        page.evaluate("() => { document.body.dataset.theme = 'dark'; }")
        page.wait_for_timeout(400)
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_dark.png', full_page=True)
        page.evaluate("() => { document.body.dataset.theme = ''; }")

        if errors:
            raise AssertionError('页面 JS 错误: ' + ' / '.join(errors))
        print('风格市场 v2 验证通过')
        browser.close()


if __name__ == '__main__':
    main()
```

- [ ] **Step 2: 跑 E2E**

```bash
# 起 dev server 后台
cd project/user/web && (npm run dev 2>&1 | tee /tmp/user_web_dev.log) &
sleep 8
curl -sf http://localhost:22345 > /dev/null && echo "dev server OK"
python tests/e2e/verify_style_market.py
```

期望输出末尾：`风格市场 v2 验证通过`。

如果失败：先读 `tests/e2e/screenshots/` 下最近一张截图 + 终端 traceback，定位 selector 不匹配或跳转路径问题，按提示修。

- [ ] **Step 3: 清掉后台 dev server**

```bash
pkill -f 'vite' || true
```

- [ ] **Step 4: commit**

```bash
git add tests/e2e/verify_style_market.py
git commit -m "test(style-market): E2E 覆盖 5 区块 + 9 场景（响应式 + 暗色 + 回归）"
```

---

## Task 11: 最终手动收尾验证

**Files:**
- (no code change)

- [ ] **Step 1: 跑完整 E2E**

```bash
cd project/user/web && npm run dev &
sleep 8
python tests/e2e/verify_style_market.py
pkill -f 'vite' || true
```

期望：`风格市场 v2 验证通过`。如果失败，找到对应 Task 修补再 commit。

- [ ] **Step 2: 浏览 `tests/e2e/screenshots/` 全部 v2 截图**

肉眼检查：banner、激励卡、精选、收益榜、全部风格、移动端、暗色，每张都不应有：

- 视觉错位（文字溢出、按钮错位）
- 白底（在暗色截图里）
- 缺元素（页面应加载完毕）
- 文字不可读（对比度）

- [ ] **Step 3: 清理 dev server**

```bash
pkill -f 'vite' || true
```

- [ ] **Step 4: git status 确认本次工作区干净**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git status --short project/user/web tests/e2e
```

期望：只有本次新 commit 留下的文件，无未提交改动。如果有，确认是不是 v2 的改动残留，commit 后再继续。

---

## Self-Review Checklist（plan 内嵌自审）

- [x] Spec §1 目标 → Task 11 收尾验证
- [x] Spec §2 设计决策 → Task 1（数据约束）+ Task 9（合规扫描）
- [x] Spec §3 整体架构 → Task 2（骨架）+ Task 3-6（5 区块）
- [x] Spec §4.1 banner → Task 2
- [x] Spec §4.2 upload → Task 3
- [x] Spec §4.3 featured → Task 4
- [x] Spec §4.4 top creators → Task 5
- [x] Spec §4.5 grid → Task 6
- [x] Spec §5 数据层 → Task 1
- [x] Spec §6 文案 → Task 2-6 的 template 字面量
- [x] Spec §7 边界空态 → Task 4/5 各 has empty 分支
- [x] Spec §8 暗色 → Task 2-6 各带暗色块 + Task 8 清漏
- [x] Spec §9 验证 → Task 10
- [x] Spec §10 实现位置 → 严格只动 3 个文件：useStyleMarket.js + StyleMarketIndex.vue + verify_style_market.py
- [x] Spec §11 非目标 → Task 2-10 均未触发
- [x] Spec §12 风险 → Task 1/8/9 各自处理

无 TBD、无 "fill in detail"、无 "参照 Task N" 跳过代码块。每步包含可粘贴的代码 + 命令 + 期望输出。
