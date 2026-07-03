# 玩法指南（/guide）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 `/guide` 玩法指南公开页面，用左侧分类目录 + 右侧正文结构回答“平台能干嘛 / 怎么挣钱”，并嵌入时间节省计算器与收益排行榜预览两个互动模块。

**Architecture:** 将内容、目录、文章渲染、互动组件拆分为独立 Vue 单文件组件；内容数据集中维护在 `src/data/guide-content.js`；页面本身不负责具体渲染逻辑，只负责布局、主题、响应式和锚点同步；互动模块纯前端实现，排行榜预览复用现有 `useLeaderboard`。

**Tech Stack:** Vue 3 (Composition API + `script setup`), Vue Router 4, Ant Design Vue (已存在), Vite, Playwright (验证), localStorage (现有 mock 数据)。

## Global Constraints

- 保持纯前端，无后端接口。
- 不引入新依赖，使用项目已有 UI 库和样式约定。
- 主品牌色 `#FF2442`，暗色主题已存在，新增页面/组件必须兼容。
- 文案写死在本地数据文件中，方便运营后续修改。
- 页面公开访问，无需登录；涉及登录后才能操作的功能按钮引导去登录。
- 不弹强制 modal、不拦截主创建流程。
- 移动端目录折叠为抽屉，正文全宽。
- 所有新增组件放 `src/components/guide/`，数据放 `src/data/`。

---

## File Structure

| 文件 | 职责 |
|---|---|
| `src/data/guide-content.js` | 指南内容数据源：4 个分类、每类下的文章标题与正文、嵌入组件标识。 |
| `src/components/guide/TimeCalculator.vue` | 时间节省计算器：输入每周产量/原耗时/时薪，输出每月节省小时与人工成本。 |
| `src/components/guide/LeaderboardPreview.vue` | 排行榜预览卡片：展示本月创作币榜 TOP 5，点击查看完整榜单。 |
| `src/components/guide/GuideSidebar.vue` | 左侧目录组件：渲染分类与文章树，处理 active 高亮，emit 点击事件。 |
| `src/components/guide/GuideArticle.vue` | 右侧文章渲染组件：渲染标题、正文、嵌入的互动组件插槽。 |
| `src/views/GuideIndex.vue` | 页面容器：两栏布局、主题切换、响应式抽屉、锚点滚动、URL hash 同步。 |
| `src/router/index.js` | 新增 `/guide` 公开路由。 |
| `src/views/Home.vue` | 顶部导航加「玩法指南」，Hero 区加辅助链接。 |
| `src/views/console/ConsoleLayout.vue` | 新增“未生成文章”时的新手横幅。 |
| `src/views/console/EarningsIndex.vue` | 空收益状态加玩法指南链接。 |
| `src/views/console/LeaderboardIndex.vue` | 规则弹框底部加完整玩法指南链接。 |
| `tests/e2e/verify_guide.py` | Playwright 验证脚本：检查页面渲染、目录滚动、计算器、排行榜预览、入口链接。 |

---

## Task 1: 指南内容数据

**Files:**
- Create: `project/user/web/src/data/guide-content.js`

**Interfaces:**
- Produces: `guideSections` 数组，每个元素含 `id`, `title`, `articles`；`articles` 元素含 `id`, `title`, `content` 或 `component`。
- Consumes: 被 `GuideIndex.vue` 和 `GuideSidebar.vue` 导入。

- [ ] **Step 1: 创建数据文件**

创建 `project/user/web/src/data/guide-content.js`：

```js
export const guideSections = [
  {
    id: 'what',
    title: '平台能干嘛',
    articles: [
      {
        id: 'what-intro',
        title: '爱创作是做什么的',
        content: `
          <p>爱创作是一款 AI 自媒体写作助手。</p>
          <p>你只需要输入一个写作方向，AI 会在 3 分钟内生成一篇结构完整、语言通顺、适配多平台的自媒体文章。</p>
          <p>不管是公众号长文、小红书笔记、今日头条文章，还是抖音图文、百家号内容，都可以一次生成，复制到对应平台直接发布。</p>
        `
      },
      {
        id: 'what-output',
        title: '3 分钟能写出什么',
        content: `
          <p>一次生成可得：</p>
          <ul>
            <li>高打开率标题（3-5 个备选）</li>
            <li>带钩子开头的正文</li>
            <li>分段清晰的结构</li>
            <li>金句结尾与 CTA</li>
            <li>适配目标平台的排版与字数</li>
          </ul>
          <p>生成后可直接导出 Word，或复制正文到公众号、小红书、抖音、今日头条、百家号等平台发布。</p>
        `
      },
      {
        id: 'what-platforms',
        title: '支持哪些平台',
        content: `
          <p>目前已适配主流自媒体平台：</p>
          <ul>
            <li>微信公众号</li>
            <li>小红书</li>
            <li>今日头条</li>
            <li>百家号</li>
            <li>知乎</li>
            <li>抖音图文</li>
          </ul>
          <p>每个平台都有对应的模板和排版规则，生成时一键切换。</p>
        `
      }
    ]
  },
  {
    id: 'money',
    title: '能赚多少钱',
    articles: [
      {
        id: 'money-platform',
        title: '平台内收益：创作币、排行榜、邀请',
        content: `
          <p>在爱创作，你可以通过以下方式获得创作币：</p>
          <ul>
            <li><strong>风格市场</strong>：发布自己的写作风格，被其他用户使用时获得收益。</li>
            <li><strong>里程碑奖励</strong>：完成创作任务获得一次性奖励。</li>
            <li><strong>收益排行榜</strong>：每月创作币榜或自媒体收入榜进入前 10，获得 100 创作币奖励。</li>
            <li><strong>邀请返利</strong>：邀请好友注册并订阅，获得返利。</li>
          </ul>
          <p>1 创作币 = 1 元人民币，满 100 创作币可申请提现。</p>
        `
      },
      {
        id: 'money-external',
        title: '外部自媒体变现：流量主、商单、带货',
        content: `
          <p>爱创作帮你把内容生产时间从 3 小时压缩到 3 分钟，省下来的时间可以用来运营账号、接商单、做流量主。</p>
          <ul>
            <li><strong>公众号</strong>：流量主广告分成 + 商务合作。</li>
            <li><strong>小红书</strong>：笔记带货 + 品牌商单。</li>
            <li><strong>抖音</strong>：中视频计划 + 橱窗带货 + 星图商单。</li>
            <li><strong>今日头条/百家号</strong>：广告分成。</li>
          </ul>
          <p>多发、多发平台、持续优化标题，是提升外部收入的关键。</p>
        `
      },
      {
        id: 'money-calculator',
        title: '时间节省计算器',
        component: 'TimeCalculator'
      }
    ]
  },
  {
    id: 'how',
    title: '怎么赚',
    articles: [
      {
        id: 'how-step1',
        title: '第一步：生成第一篇内容',
        content: `
          <p>点击「开始创作」，输入你的写作方向。</p>
          <p>可以是热点观点、产品测评、经验分享、情感故事等。AI 会根据你的方向生成完整文章。</p>
        `
      },
      {
        id: 'how-step2',
        title: '第二步：选择发布平台',
        content: `
          <p>根据内容选择最适合的平台。</p>
          <p>小红书适合短图文和情绪化表达，公众号适合深度长文，今日头条和百家号适合资讯类内容。</p>
        `
      },
      {
        id: 'how-step3',
        title: '第三步：多平台分发',
        content: `
          <p>同一篇内容可以改写后分发到多个平台，最大化流量价值。</p>
          <p>爱创作支持一次生成多平台版本，也可以导出后微调标题和开头再发布。</p>
        `
      },
      {
        id: 'how-step4',
        title: '第四步：申报收入/冲击榜单',
        content: `
          <p>发布内容后，回到爱创作申报你的自媒体收入。</p>
          <p>申报审核通过后，金额会累加进「自媒体收入榜」，每月前 10 名可获得 100 创作币奖励。</p>
        `
      },
      {
        id: 'how-leaderboard',
        title: '本月创作币榜',
        component: 'LeaderboardPreview'
      }
    ]
  },
  {
    id: 'withdraw',
    title: '怎么提现',
    articles: [
      {
        id: 'withdraw-coin',
        title: '创作币是什么',
        content: `
          <p>创作币是爱创作平台的虚拟货币。</p>
          <p>你可以通过风格市场、排行榜、邀请返利等方式获得创作币，也可以在平台内消费（如订阅会员、购买生成额度）。</p>
          <p>1 创作币 = 1 元人民币。</p>
        `
      },
      {
        id: 'withdraw-settle',
        title: '如何结算收益',
        content: `
          <p>收益按自然周（周一至周日）统计，每周一可手动结算上周收益。</p>
          <p>点击「结算上周」后，未结算收益会立即转入账户余额。</p>
        `
      },
      {
        id: 'withdraw-rule',
        title: '提现门槛与到账说明',
        content: `
          <p>账户余额满 100 创作币可申请提现到支付宝。</p>
          <p>未结算收益不可提现，结算前请确认收益明细无误。</p>
          <p>提现申请提交后，通常在 1-3 个工作日内到账。</p>
        `
      }
    ]
  }
]
```

- [ ] **Step 2: 验证数据格式**

在 `project/user/web` 目录下执行：

```bash
node -e "const { guideSections } = require('./src/data/guide-content.js'); console.log(guideSections.map(s => s.title).join(', '))"
```

Expected output: `平台能干嘛, 能赚多少钱, 怎么赚, 怎么提现`

- [ ] **Step 3: 提交**

```bash
git add project/user/web/src/data/guide-content.js
git commit -m "$(cat <<'EOF'
data(guide): 新增玩法指南内容数据

- 4 个分类、12 篇文章正文
- 支持嵌入 TimeCalculator 与 LeaderboardPreview 组件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: 时间节省计算器组件

**Files:**
- Create: `project/user/web/src/components/guide/TimeCalculator.vue`

**Interfaces:**
- Consumes: 无外部依赖，纯本地状态。
- Produces: 无 props/emit，内部渲染表单与计算结果。

- [ ] **Step 1: 创建组件文件**

创建 `project/user/web/src/components/guide/TimeCalculator.vue`：

```vue
<template>
  <div class="time-calculator">
    <h4 class="tc-title">算算你能省多少</h4>
    <div class="tc-fields">
      <div class="tc-field">
        <label>每周写几篇</label>
        <a-input-number v-model:value="articlesPerWeek" :min="0" :precision="0" class="tc-input" />
      </div>
      <div class="tc-field">
        <label>原来每篇花几小时</label>
        <a-input-number v-model:value="originalHours" :min="0" :precision="1" class="tc-input" />
      </div>
      <div class="tc-field">
        <label>时薪估算（元）</label>
        <a-input-number v-model:value="hourlyRate" :min="0" :precision="0" class="tc-input" />
      </div>
    </div>
    <div class="tc-result">
      <template v-if="isValid">
        <div class="tc-result-line">
          每月可节省 <span class="tc-result-num">{{ savedHours }}</span> 小时
        </div>
        <div class="tc-result-line">
          相当于 <span class="tc-result-num">{{ savedMoney }}</span> 元人工成本
        </div>
        <p class="tc-result-tip">省下的时间可以用来做选题、运营账号，或者直接再生产更多内容。</p>
      </template>
      <template v-else>
        <div class="tc-result-placeholder">请输入有效数字</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const articlesPerWeek = ref(5)
const originalHours = ref(2)
const hourlyRate = ref(50)

const isValid = computed(() => {
  return Number.isFinite(articlesPerWeek.value) && articlesPerWeek.value >= 0
    && Number.isFinite(originalHours.value) && originalHours.value >= 0
    && Number.isFinite(hourlyRate.value) && hourlyRate.value >= 0
})

const savedHours = computed(() => {
  if (!isValid.value) return 0
  const perWeek = articlesPerWeek.value * (originalHours.value - 0.05)
  return Number((perWeek * 4).toFixed(1))
})

const savedMoney = computed(() => {
  if (!isValid.value) return 0
  return Number((savedHours.value * hourlyRate.value).toFixed(0))
})
</script>

<style scoped>
.time-calculator {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
}
.tc-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.tc-fields {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.tc-field label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 6px;
}
.tc-input {
  width: 100%;
}
.tc-result {
  background: #fff5f7;
  border-radius: 10px;
  padding: 16px;
}
.tc-result-line {
  font-size: 15px;
  color: #1a1a1a;
  margin-bottom: 6px;
}
.tc-result-num {
  color: #ff2442;
  font-weight: 700;
  font-size: 18px;
}
.tc-result-tip {
  font-size: 13px;
  color: #8c8c8c;
  margin: 10px 0 0;
}
.tc-result-placeholder {
  color: #8c8c8c;
  font-size: 14px;
}

@media (max-width: 768px) {
  .tc-fields {
    grid-template-columns: 1fr;
  }
}

body[data-theme="dark"] .time-calculator {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .tc-title,
body[data-theme="dark"] .tc-result-line {
  color: #e0e0e0;
}
body[data-theme="dark"] .tc-result {
  background: rgba(255, 36, 66, 0.08);
}
body[data-theme="dark"] .tc-result-tip,
body[data-theme="dark"] .tc-result-placeholder,
body[data-theme="dark"] .tc-field label {
  color: #a6a6a6;
}
</style>
```

- [ ] **Step 2: 启动开发服务器验证组件**

```bash
cd project/user/web
npm run dev
```

临时在任意页面（如 `Home.vue`）引入并展示 `TimeCalculator`，检查：

- 默认显示 5 篇/周、2 小时/篇、50 元/时。
- 结果约为 `5 × (2 - 0.05) × 4 = 39` 小时，`39 × 50 = 1950` 元。
- 输入负数时显示“请输入有效数字”。

验证后移除临时引入。

- [ ] **Step 3: 提交**

```bash
git add project/user/web/src/components/guide/TimeCalculator.vue
git commit -m "$(cat <<'EOF'
feat(guide): 新增时间节省计算器组件

- 输入每周产量、原耗时、时薪
- 实时计算每月节省小时与人工成本
- 支持暗色主题与移动端适配

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: 排行榜预览组件

**Files:**
- Create: `project/user/web/src/components/guide/LeaderboardPreview.vue`

**Interfaces:**
- Consumes: `useLeaderboard.js` 的 `getCoinLeaderboard(month)`，或固定 mock 数据兜底。
- Produces: 无 props/emit，内部处理登录状态与跳转。

- [ ] **Step 1: 创建组件文件**

创建 `project/user/web/src/components/guide/LeaderboardPreview.vue`：

```vue
<template>
  <div class="leaderboard-preview">
    <h4 class="lp-title">本月创作币榜 TOP 5</h4>
    <div class="lp-list">
      <div
        v-for="item in topList"
        :key="item.userId"
        :class="['lp-row', { me: item.isMe }]"
      >
        <span class="lp-rank">{{ item.rank }}</span>
        <span class="lp-name">{{ item.nickname }}</span>
        <span class="lp-amount">{{ item.amount.toLocaleString() }} 创作币</span>
      </div>
    </div>
    <button class="lp-btn" @click="handleViewFull">查看完整榜单</button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const MOCK_TOP = [
  { userId: 'm1', nickname: '创作者小王', amount: 12580, rank: 1 },
  { userId: 'm2', nickname: '文案阿杰', amount: 9200, rank: 2 },
  { userId: 'm3', nickname: '自媒体老李', amount: 7150, rank: 3 },
  { userId: 'm4', nickname: '写作喵', amount: 5400, rank: 4 },
  { userId: 'm5', nickname: '内容工匠', amount: 3880, rank: 5 }
]

const topList = ref([...MOCK_TOP])

function formatMonth(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

onMounted(async () => {
  try {
    const mod = await import('@/composables/useLeaderboard.js')
    if (mod.getCoinLeaderboard) {
      const list = mod.getCoinLeaderboard(formatMonth(new Date()))
      topList.value = list.slice(0, 5)
    }
  } catch (e) {
    // 保持 mock 数据
  }
})

const handleViewFull = () => {
  const isLoggedIn = localStorage.getItem('aichuangzuo_user_id')
  if (isLoggedIn) {
    router.push('/console/leaderboard')
  } else {
    router.push('/login?from=guide')
  }
}
</script>

<style scoped>
.leaderboard-preview {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
}
.lp-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.lp-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}
.lp-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
  font-size: 14px;
}
.lp-row.me {
  background: #fff5f7;
  font-weight: 600;
}
.lp-rank {
  width: 24px;
  color: #8c8c8c;
  font-weight: 600;
}
.lp-row:nth-child(1) .lp-rank { color: #faad14; }
.lp-row:nth-child(2) .lp-rank { color: #bfbfbf; }
.lp-row:nth-child(3) .lp-rank { color: #d48806; }
.lp-name {
  flex: 1;
  color: #1a1a1a;
}
.lp-amount {
  color: #ff2442;
  font-weight: 600;
}
.lp-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid #ff2442;
  background: #fff;
  color: #ff2442;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.lp-btn:hover {
  background: #fff5f7;
}

body[data-theme="dark"] .leaderboard-preview {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .lp-title,
body[data-theme="dark"] .lp-name {
  color: #e0e0e0;
}
body[data-theme="dark"] .lp-row {
  background: #141414;
}
body[data-theme="dark"] .lp-row.me {
  background: rgba(255, 36, 66, 0.12);
}
body[data-theme="dark"] .lp-btn {
  background: #1f1f1f;
  border-color: #ff4d6f;
  color: #ff4d6f;
}
body[data-theme="dark"] .lp-btn:hover {
  background: rgba(255, 36, 66, 0.12);
}
</style>
```

- [ ] **Step 2: 验证组件渲染**

在开发服务器中临时引入 `LeaderboardPreview` 到 `Home.vue`，检查：

- 显示 TOP 5 列表。
- 未登录时点击“查看完整榜单”跳转 `/login?from=guide`。
- 登录后（手动在控制台执行 `localStorage.setItem('aichuangzuo_user_id', 'u_test')`）点击跳转 `/console/leaderboard`。

验证后移除临时引入。

- [ ] **Step 3: 提交**

```bash
git add project/user/web/src/components/guide/LeaderboardPreview.vue
git commit -m "$(cat <<'EOF'
feat(guide): 新增排行榜预览组件

- 展示本月创作币榜 TOP 5
- 优先复用 useLeaderboard.getCoinLeaderboard，失败则使用 mock 数据
- 根据登录状态跳转登录页或控制台排行榜

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: 左侧目录组件

**Files:**
- Create: `project/user/web/src/components/guide/GuideSidebar.vue`

**Interfaces:**
- Consumes: `sections` prop (来自 `guide-content.js` 的 `guideSections`)，`activeId` prop。
- Produces: `select` emit，payload 为 `{ sectionId, articleId }`。

- [ ] **Step 1: 创建组件文件**

创建 `project/user/web/src/components/guide/GuideSidebar.vue`：

```vue
<template>
  <aside class="guide-sidebar">
    <div class="gs-mobile-toggle" @click="mobileOpen = !mobileOpen">
      <span>目录</span>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="3" y1="12" x2="21" y2="12" />
        <line x1="3" y1="6" x2="21" y2="6" />
        <line x1="3" y1="18" x2="21" y2="18" />
      </svg>
    </div>
    <div :class="['gs-nav', { open: mobileOpen }]">
      <div
        v-for="section in sections"
        :key="section.id"
        class="gs-section"
      >
        <div class="gs-section-title" @click="toggleSection(section.id)">
          {{ section.title }}
        </div>
        <div v-show="expanded[section.id] !== false" class="gs-articles">
          <div
            v-for="article in section.articles"
            :key="article.id"
            :class="['gs-article', { active: activeId === article.id }]"
            @click="handleClick(section.id, article.id)"
          >
            {{ article.title }}
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  sections: { type: Array, required: true },
  activeId: { type: String, default: '' }
})

const emit = defineEmits(['select'])

const mobileOpen = ref(false)
const expanded = ref({})

// 默认展开包含 activeId 的分类
watch(() => props.activeId, (id) => {
  if (!id) return
  const section = props.sections.find(s => s.articles.some(a => a.id === id))
  if (section) {
    expanded.value[section.id] = true
  }
}, { immediate: true })

const toggleSection = (id) => {
  expanded.value[id] = expanded.value[id] === false
}

const handleClick = (sectionId, articleId) => {
  mobileOpen.value = false
  emit('select', { sectionId, articleId })
}
</script>

<style scoped>
.guide-sidebar {
  width: 260px;
  flex-shrink: 0;
}
.gs-mobile-toggle {
  display: none;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}
.gs-mobile-toggle svg {
  width: 18px;
  height: 18px;
}
.gs-nav {
  position: sticky;
  top: 24px;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}
.gs-section {
  margin-bottom: 8px;
}
.gs-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
}
.gs-section-title:hover {
  background: #f5f5f5;
}
.gs-articles {
  padding-left: 8px;
}
.gs-article {
  font-size: 13px;
  color: #595959;
  padding: 7px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.5;
}
.gs-article:hover {
  color: #ff2442;
  background: #fff5f7;
}
.gs-article.active {
  color: #ff2442;
  background: #fff0f2;
  font-weight: 500;
}

@media (max-width: 768px) {
  .guide-sidebar {
    width: 100%;
  }
  .gs-mobile-toggle {
    display: flex;
  }
  .gs-nav {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 260px;
    background: #fff;
    z-index: 100;
    padding: 16px;
    transform: translateX(-100%);
    transition: transform 0.25s;
    box-shadow: 2px 0 12px rgba(0,0,0,0.1);
  }
  .gs-nav.open {
    transform: translateX(0);
  }
}

body[data-theme="dark"] .gs-mobile-toggle {
  color: #a6a6a6;
  border-bottom-color: #303030;
}
body[data-theme="dark"] .gs-section-title,
body[data-theme="dark"] .gs-article.active {
  color: #e0e0e0;
}
body[data-theme="dark"] .gs-article {
  color: #a6a6a6;
}
body[data-theme="dark"] .gs-section-title:hover,
body[data-theme="dark"] .gs-article:hover {
  background: #2a2a2a;
}
body[data-theme="dark"] .gs-article.active {
  background: rgba(255, 36, 66, 0.15);
}
body[data-theme="dark"] .gs-nav {
  background: #1f1f1f;
}
</style>
```

- [ ] **Step 2: 验证目录组件**

临时在 `Home.vue` 引入 `GuideSidebar` 并传入 `guideSections`，检查：

- 4 个分类渲染正确。
- 点击文章标题 emit 事件。
- 移动端窗口宽度下（DevTools 切到 iPhone SE）出现“目录”按钮，点击展开抽屉。

验证后移除临时引入。

- [ ] **Step 3: 提交**

```bash
git add project/user/web/src/components/guide/GuideSidebar.vue
git commit -m "$(cat <<'EOF'
feat(guide): 新增左侧目录组件

- 渲染分类与文章树
- 支持 active 高亮与移动端抽屉

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: 右侧文章渲染组件

**Files:**
- Create: `project/user/web/src/components/guide/GuideArticle.vue`

**Interfaces:**
- Consumes: `section` prop (来自 `guideSections` 的单个分类对象)。
- Produces: 无 emit，只负责渲染。

- [ ] **Step 1: 创建组件文件**

创建 `project/user/web/src/components/guide/GuideArticle.vue`：

```vue
<template>
  <article :id="section.id" class="guide-article">
    <h2 class="ga-section-title">{{ section.title }}</h2>
    <div
      v-for="article in section.articles"
      :id="article.id"
      :key="article.id"
      class="ga-article"
    >
      <h3 class="ga-title">{{ article.title }}</h3>
      <div v-if="article.content" class="ga-content" v-html="article.content" />
      <component
        :is="componentMap[article.component]"
        v-else-if="article.component"
      />
    </div>
  </article>
</template>

<script setup>
import TimeCalculator from './TimeCalculator.vue'
import LeaderboardPreview from './LeaderboardPreview.vue'

const componentMap = {
  TimeCalculator,
  LeaderboardPreview
}

defineProps({
  section: { type: Object, required: true }
})
</script>

<style scoped>
.guide-article {
  padding-bottom: 48px;
  margin-bottom: 48px;
  border-bottom: 1px solid #f0f0f0;
}
.guide-article:last-child {
  border-bottom: none;
  margin-bottom: 0;
}
.ga-section-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 24px;
}
.ga-article {
  margin-bottom: 32px;
}
.ga-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px;
}
.ga-content {
  font-size: 15px;
  color: #595959;
  line-height: 1.8;
}
.ga-content :deep(p) {
  margin: 0 0 12px;
}
.ga-content :deep(ul) {
  padding-left: 20px;
  margin: 0 0 12px;
}
.ga-content :deep(li) {
  margin-bottom: 6px;
}
.ga-content :deep(strong) {
  color: #1a1a1a;
}

body[data-theme="dark"] .guide-article {
  border-bottom-color: #303030;
}
body[data-theme="dark"] .ga-section-title,
body[data-theme="dark"] .ga-title,
body[data-theme="dark"] .ga-content :deep(strong) {
  color: #e0e0e0;
}
body[data-theme="dark"] .ga-content {
  color: #a6a6a6;
}
</style>
```

- [ ] **Step 2: 验证文章渲染**

临时在 `Home.vue` 引入 `GuideArticle` 并传入第一个分类，检查：

- 标题、正文列表、加粗文本渲染正常。
- 传入含 `component: 'TimeCalculator'` 的文章时，正确渲染计算器。

验证后移除临时引入。

- [ ] **Step 3: 提交**

```bash
git add project/user/web/src/components/guide/GuideArticle.vue
git commit -m "$(cat <<'EOF'
feat(guide): 新增右侧文章渲染组件

- 渲染分类标题与文章正文
- 支持 v-html 内容与动态嵌入互动组件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: 玩法指南页面与路由

**Files:**
- Create: `project/user/web/src/views/GuideIndex.vue`
- Modify: `project/user/web/src/router/index.js`

**Interfaces:**
- Consumes: `guideSections` from `src/data/guide-content.js`，`GuideSidebar`, `GuideArticle`。
- Produces: 公开路由 `/guide`。

- [ ] **Step 1: 创建页面文件**

创建 `project/user/web/src/views/GuideIndex.vue`：

```vue
<template>
  <div class="guide-page">
    <!-- 顶部导航 -->
    <header class="guide-nav">
      <div class="nav-brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="nav-logo"
        />
        <span class="nav-brand-name">爱创作</span>
      </div>
      <div class="nav-links">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/pricing" class="nav-link">会员</router-link>
        <router-link to="/guide" class="nav-link active">玩法指南</router-link>
        <button
          class="theme-toggle"
          :title="currentTheme === 'light' ? '切换深色主题' : '切换浅色主题'"
          @click="toggleTheme"
        >
          <svg v-if="currentTheme === 'light'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5" />
            <line x1="12" y1="1" x2="12" y2="3" />
            <line x1="12" y1="21" x2="12" y2="23" />
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
            <line x1="1" y1="12" x2="3" y2="12" />
            <line x1="21" y1="12" x2="23" y2="12" />
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
          </svg>
        </button>
        <router-link to="/login" class="nav-cta">开始创作</router-link>
      </div>
    </header>

    <!-- 主体 -->
    <div class="guide-body">
      <GuideSidebar
        :sections="guideSections"
        :active-id="activeArticleId"
        @select="handleSelect"
      />
      <div class="guide-main">
        <div class="guide-hero">
          <h1>玩法指南</h1>
          <p>3 分钟了解爱创作能做什么，以及如何把它变成收益。</p>
        </div>
        <div class="guide-articles-wrap">
          <GuideArticle
            v-for="section in guideSections"
            :key="section.id"
            :section="section"
          />
        </div>
        <div class="guide-footer-cta">
          <h3>准备好开始了吗？</h3>
          <p>每天 3 分钟，把内容变成账号流量和收入。</p>
          <router-link to="/login" class="guide-cta-btn">立即开始创作</router-link>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <footer class="guide-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { guideSections } from '@/data/guide-content.js'
import GuideSidebar from '@/components/guide/GuideSidebar.vue'
import GuideArticle from '@/components/guide/GuideArticle.vue'

const route = useRoute()
const router = useRouter()

const activeArticleId = ref('')

const THEME_KEY = 'aichuangzuo_theme'
const currentTheme = ref('light')

const toggleTheme = () => {
  const next = currentTheme.value === 'light' ? 'dark' : 'light'
  currentTheme.value = next
  document.body.setAttribute('data-theme', next)
  localStorage.setItem(THEME_KEY, next)
}

const loadTheme = () => {
  const saved = localStorage.getItem(THEME_KEY) || 'light'
  currentTheme.value = saved
  document.body.setAttribute('data-theme', saved)
}

const handleSelect = ({ articleId }) => {
  const el = document.getElementById(articleId)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    router.replace({ hash: `#${articleId}` })
    activeArticleId.value = articleId
  }
}

const observerArticles = () => {
  const ids = guideSections.flatMap(s => s.articles.map(a => a.id))
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          activeArticleId.value = entry.target.id
          if (route.hash !== `#${entry.target.id}`) {
            router.replace({ hash: `#${entry.target.id}` })
          }
        }
      })
    },
    { rootMargin: '-20% 0px -60% 0px', threshold: 0 }
  )
  ids.forEach((id) => {
    const el = document.getElementById(id)
    if (el) observer.observe(el)
  })
}

onMounted(() => {
  loadTheme()
  nextTick(() => {
    observerArticles()
    if (route.hash) {
      const id = route.hash.slice(1)
      const el = document.getElementById(id)
      if (el) {
        el.scrollIntoView({ behavior: 'auto', block: 'start' })
        activeArticleId.value = id
      }
    }
  })
})
</script>

<style scoped>
.guide-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.guide-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 48px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}
.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nav-logo {
  height: 32px;
  width: auto;
}
.nav-brand-name {
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 32px;
}
.nav-link {
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: color 0.2s;
}
.nav-link:hover,
.nav-link.active {
  color: #ff2442;
}
.nav-cta {
  padding: 8px 22px;
  background: #ff2442;
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.nav-cta:hover {
  background: #e61e3a;
}
.theme-toggle {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.theme-toggle:hover {
  background: #fff5f7;
  color: #ff2442;
}
.theme-toggle svg {
  width: 18px;
  height: 18px;
}
.guide-body {
  flex: 1;
  display: flex;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 24px;
  gap: 40px;
}
.guide-main {
  flex: 1;
  min-width: 0;
}
.guide-hero {
  margin-bottom: 40px;
}
.guide-hero h1 {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 10px;
}
.guide-hero p {
  font-size: 16px;
  color: #595959;
  margin: 0;
}
.guide-footer-cta {
  text-align: center;
  padding: 48px 24px;
  background: linear-gradient(135deg, #fff0f2 0%, #fff 100%);
  border-radius: 16px;
  margin-top: 24px;
}
.guide-footer-cta h3 {
  font-size: 22px;
  color: #1a1a1a;
  margin: 0 0 8px;
}
.guide-footer-cta p {
  font-size: 15px;
  color: #595959;
  margin: 0 0 20px;
}
.guide-cta-btn {
  display: inline-block;
  padding: 14px 36px;
  background: #ff2442;
  color: #fff;
  border-radius: 28px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.guide-cta-btn:hover {
  background: #e61e3a;
}
.guide-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}
.guide-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}

@media (max-width: 768px) {
  .guide-nav {
    padding: 14px 16px;
  }
  .guide-body {
    flex-direction: column;
    padding: 16px;
    gap: 16px;
  }
  .guide-hero h1 {
    font-size: 24px;
  }
  .nav-links {
    gap: 16px;
  }
}

body[data-theme="dark"] .guide-page {
  background: #141414;
}
body[data-theme="dark"] .guide-nav,
body[data-theme="dark"] .guide-footer {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .nav-brand-name,
body[data-theme="dark"] .guide-hero h1,
body[data-theme="dark"] .guide-footer-cta h3 {
  color: #e0e0e0;
}
body[data-theme="dark"] .nav-link {
  color: #a6a6a6;
}
body[data-theme="dark"] .nav-link:hover,
body[data-theme="dark"] .nav-link.active {
  color: #ff4d6f;
}
body[data-theme="dark"] .nav-cta {
  background: linear-gradient(135deg, #ff6b8a 0%, #ff2442 100%);
}
body[data-theme="dark"] .theme-toggle {
  color: #a6a6a6;
}
body[data-theme="dark"] .theme-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}
body[data-theme="dark"] .guide-hero p,
body[data-theme="dark"] .guide-footer-cta p,
body[data-theme="dark"] .guide-footer {
  color: #a6a6a6;
}
body[data-theme="dark"] .guide-footer-cta {
  background: linear-gradient(135deg, #331018 0%, #1f1f1f 100%);
}
body[data-theme="dark"] .guide-cta-btn {
  background: linear-gradient(135deg, #ff6b8a 0%, #ff2442 100%);
}
body[data-theme="dark"] .guide-footer span + span::before {
  color: #303030;
}
</style>
```

- [ ] **Step 2: 新增路由**

修改 `project/user/web/src/router/index.js`，在 `/pricing` 路由后添加：

```js
  {
    path: '/guide',
    name: 'Guide',
    component: () => import('@/views/GuideIndex.vue')
  },
```

- [ ] **Step 3: 验证页面**

```bash
cd project/user/web
npm run dev
```

打开 `http://localhost:5173/guide`（或 Vite 实际端口），检查：

- 页面渲染 4 个分类和对应文章。
- 左侧目录可点击，点击后右侧滚动，URL hash 变化。
- 滚动正文时左侧 active 项自动切换。
- 时间节省计算器和排行榜预览正常显示。
- 主题切换按钮工作。
- 底部 CTA 跳转登录页。

- [ ] **Step 4: 提交**

```bash
git add project/user/web/src/views/GuideIndex.vue project/user/web/src/router/index.js
git commit -m "$(cat <<'EOF'
feat(guide): 新增玩法指南页面与路由

- 新增 /guide 公开页面
- 集成左侧目录、右侧文章、主题切换、响应式布局
- 支持锚点滚动与 URL hash 同步

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: 首页入口

**Files:**
- Modify: `project/user/web/src/views/Home.vue`

**Interfaces:**
- Consumes: 现有导航与 Hero 结构。
- Produces: 新增「玩法指南」入口和 Hero 辅助链接。

- [ ] **Step 1: 顶部导航加入口**

在 `Home.vue` 的 `nav-links` 中，于「会员」后新增：

```vue
<router-link to="/guide" class="nav-link">玩法指南</router-link>
```

- [ ] **Step 2: Hero 区加辅助链接**

在 `Home.vue` 的 `hero-checkmarks` 后、或主 CTA 下方新增：

```vue
<div class="hero-guide-link">
  <router-link to="/guide">不知道怎么变现？看看玩法指南 →</router-link>
</div>
```

并添加样式（放在 `style scoped` 内）：

```css
.hero-guide-link {
  margin-top: 20px;
  font-size: 14px;
}
.hero-guide-link a {
  color: #ff2442;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.hero-guide-link a:hover {
  color: #e61e3a;
}
body[data-theme="dark"] .hero-guide-link a {
  color: #ff4d6f;
}
```

- [ ] **Step 3: 验证首页入口**

打开首页 `http://localhost:5173/`，检查：

- 顶部导航出现「玩法指南」且可点击。
- Hero 区出现辅助链接且可点击。
- 暗色主题下链接颜色正确。

- [ ] **Step 4: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(guide): 首页增加玩法指南入口

- 顶部导航新增「玩法指南」
- Hero 区新增辅助链接

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: 控制台引导与相关页面回流

**Files:**
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`
- Modify: `project/user/web/src/views/console/EarningsIndex.vue`
- Modify: `project/user/web/src/views/console/LeaderboardIndex.vue`

**Interfaces:**
- Consumes: `aichuangzuo_generation_queue` localStorage key（判断是否有作品）。
- Produces: 新手横幅、空状态链接、规则弹框链接。

- [ ] **Step 1: 控制台新手横幅**

在 `ConsoleLayout.vue` 的 `script setup` 中新增：

```js
import { useRouter } from 'vue-router'

const router = useRouter()

const WORKS_KEY = 'aichuangzuo_generation_queue'
const GUIDE_BANNER_DISMISSED_KEY = 'aichuangzuo_guide_banner_dismissed'

const hasWorks = () => {
  const raw = localStorage.getItem(WORKS_KEY)
  if (!raw) return false
  try {
    const list = JSON.parse(raw)
    return Array.isArray(list) && list.some(item => item.status === 'completed')
  } catch {
    return false
  }
}

const guideBannerVisible = ref(!localStorage.getItem(GUIDE_BANNER_DISMISSED_KEY) && !hasWorks())

const dismissGuideBanner = () => {
  guideBannerVisible.value = false
  localStorage.setItem(GUIDE_BANNER_DISMISSED_KEY, '1')
}

const goToGuide = () => {
  router.push('/guide')
}
```

在 `header` 与 `console-content` 之间插入横幅（约在 `line 733` 后）：

```vue
    <!-- 玩法指南横幅 -->
    <div v-if="guideBannerVisible" class="guide-banner">
      <span class="guide-banner-text" @click="goToGuide">
        新手？3 分钟了解怎么在爱创作变现 →
      </span>
      <span class="guide-banner-close" @click="dismissGuideBanner">✕</span>
    </div>
```

在 `style scoped` 中添加：

```css
.guide-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 24px;
  background: #fff5f7;
  border-bottom: 1px solid #ffd1d9;
  font-size: 14px;
}
.guide-banner-text {
  color: #ff2442;
  cursor: pointer;
  font-weight: 500;
}
.guide-banner-text:hover {
  text-decoration: underline;
}
.guide-banner-close {
  color: #8c8c8c;
  cursor: pointer;
  font-size: 12px;
}
.guide-banner-close:hover {
  color: #595959;
}
body[data-theme="dark"] .guide-banner {
  background: #331018;
  border-bottom-color: #52222b;
}
body[data-theme="dark"] .guide-banner-text {
  color: #ff4d6f;
}
body[data-theme="dark"] .guide-banner-close {
  color: #a6a6a6;
}
body[data-theme="dark"] .guide-banner-close:hover {
  color: #e0e0e0;
}
```

- [ ] **Step 2: 我的账户空状态加链接**

在 `EarningsIndex.vue` 中，找到空状态：

```vue
<div v-if="weeklyList.length === 0" class="account-empty">
  暂无收益结算记录
</div>
```

改为：

```vue
<div v-if="weeklyList.length === 0" class="account-empty">
  <div>还没有收益</div>
  <router-link to="/guide" class="guide-link">看看怎么赚创作币 →</router-link>
</div>
```

添加样式：

```css
.guide-link {
  display: inline-block;
  margin-top: 8px;
  color: #ff2442;
  font-size: 14px;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.guide-link:hover {
  color: #e61e3a;
}
body[data-theme="dark"] .guide-link {
  color: #ff4d6f;
}
```

- [ ] **Step 3: 排行榜规则弹框加链接**

在 `LeaderboardIndex.vue` 的规则 `a-modal` 内容末尾（`ol` 之后）新增：

```vue
      <div class="rules-guide-link">
        <router-link to="/guide">阅读完整玩法指南 →</router-link>
      </div>
```

并添加样式（若 `LeaderboardIndex.vue` 有 scoped style，直接追加；否则用全局 style）：

```css
.rules-guide-link {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
}
.rules-guide-link a {
  color: #ff2442;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.rules-guide-link a:hover {
  color: #e61e3a;
}
body[data-theme="dark"] .rules-guide-link {
  border-top-color: #303030;
}
body[data-theme="dark"] .rules-guide-link a {
  color: #ff4d6f;
}
```

- [ ] **Step 4: 验证引导与回流**

1. 清空 `localStorage` 中的 `aichuangzuo_generation_queue` 和 `aichuangzuo_guide_banner_dismissed`，进入控制台，确认横幅出现。
2. 点击横幅跳转 `/guide`。
3. 点击横幅关闭按钮，刷新后横幅不再出现。
4. 生成一篇文章后（或手动往 `aichuangzuo_generation_queue` 插入 completed 记录），刷新控制台，横幅不再出现。
5. 进入「我的账户」，清空收益记录，确认空状态出现玩法指南链接。
6. 进入「收益排行榜」，打开规则弹框，确认底部有完整玩法指南链接。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/console/ConsoleLayout.vue \
  project/user/web/src/views/console/EarningsIndex.vue \
  project/user/web/src/views/console/LeaderboardIndex.vue
git commit -m "$(cat <<'EOF'
feat(guide): 控制台与相关页面增加玩法指南引导

- 控制台新增可关闭的新手横幅
- 我的账户空收益状态加玩法指南链接
- 收益排行榜规则弹框加完整指南链接

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: Playwright 验证脚本

**Files:**
- Create: `tests/e2e/verify_guide.py`

**Interfaces:**
- Consumes: 运行中的 dev server（默认 `http://localhost:5173`）。
- Produces: 截图与断言结果。

- [ ] **Step 1: 创建验证脚本**

创建 `tests/e2e/verify_guide.py`：

```python
import os
import sys
from playwright.sync_api import sync_playwright, expect

BASE_URL = os.environ.get("BASE_URL", "http://localhost:5173")
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")


def ensure_dir(path):
    os.makedirs(path, exist_ok=True)


def main():
    ensure_dir(SCREENSHOT_DIR)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()

        # 1. 首页入口
        page.goto(f"{BASE_URL}/")
        page.locator("text=玩法指南").first.click()
        page.wait_for_url(f"{BASE_URL}/guide")
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "guide_page.png"))
        print("OK: 首页可进入玩法指南")

        # 2. 左侧目录存在
        expect(page.locator("text=平台能干嘛")).to_be_visible()
        expect(page.locator("text=能赚多少钱")).to_be_visible()
        expect(page.locator("text=怎么赚")).to_be_visible()
        expect(page.locator("text=怎么提现")).to_be_visible()
        print("OK: 左侧目录 4 个分类存在")

        # 3. 时间节省计算器
        expect(page.locator("text=算算你能省多少")).to_be_visible()
        page.locator("text=算算你能省多少").scroll_into_view_if_needed()
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "guide_calculator.png"))
        print("OK: 时间节省计算器存在")

        # 4. 排行榜预览
        expect(page.locator("text=本月创作币榜 TOP 5")).to_be_visible()
        page.locator("text=本月创作币榜 TOP 5").scroll_into_view_if_needed()
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "guide_leaderboard.png"))
        print("OK: 排行榜预览存在")

        # 5. 点击目录滚动
        page.locator("text=怎么提现").first.click()
        page.wait_for_timeout(600)
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "guide_scroll.png"))
        print("OK: 点击目录可滚动")

        # 6. 底部 CTA
        page.locator("text=立即开始创作").last.scroll_into_view_if_needed()
        expect(page.locator("text=立即开始创作").last).to_be_visible()
        print("OK: 底部 CTA 存在")

        browser.close()
        print("All guide page checks passed.")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 运行验证脚本**

确保 dev server 已启动：

```bash
cd project/user/web
npm run dev
```

另开终端执行：

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_guide.py
```

Expected output: 所有 `OK:` 行打印，最终 `All guide page checks passed.`。

若端口不是 5173，设置环境变量：

```bash
BASE_URL=http://localhost:8080 python3 tests/e2e/verify_guide.py
```

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/verify_guide.py
git commit -m "$(cat <<'EOF'
test(guide): 新增玩法指南页面验证脚本

- 使用 Playwright 验证 /guide 页面渲染、目录、计算器、排行榜预览、CTA

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review

### 1. Spec coverage

| Spec 要求 | 对应 Task |
|---|---|
| 独立 `/guide` 页面 | Task 6 |
| 左侧分类目录 + 右侧正文 | Task 4 + Task 5 + Task 6 |
| 按用户问题切 4 分类 | Task 1 |
| 时间节省计算器 | Task 2 |
| 排行榜预览 | Task 3 |
| 首页入口 | Task 7 |
| 控制台横幅 | Task 8 |
| 相关页面回流 | Task 8 |
| 响应式/暗色主题 | Task 4 + Task 6 |
| 验证测试 | Task 9 |

### 2. Placeholder scan

- 无 TBD/TODO。
- 无 “add appropriate error handling” 等模糊描述。
- 每个步骤都包含可执行代码或命令。

### 3. Type/接口一致性

- `guideSections` 结构在 Task 1 定义，Task 4/5/6 消费，字段一致。
- `getCoinLeaderboard(month)` 签名与现有 `useLeaderboard.js` 一致。
- `GuideSidebar` emit 的 payload 与 `GuideIndex.vue` 的 `handleSelect` 一致。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-03-guide-plan.md`.

Two execution options:

**1. Subagent-Driven (recommended)** - Dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** - Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.

Which approach?
