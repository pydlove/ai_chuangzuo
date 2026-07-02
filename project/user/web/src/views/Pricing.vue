<template>
  <div class="pricing-page">
    <!-- 导航栏 -->
    <header class="pricing-nav">
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
        <router-link to="/pricing" class="nav-link active">价格</router-link>
        <button
          class="theme-toggle"
          :title="currentTheme === 'light' ? '切换深色主题' : '切换浅色主题'"
          @click="toggleTheme"
        >
          <svg
            v-if="currentTheme === 'light'"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
          <svg
            v-else
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
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

    <!-- 主内容 -->
    <div class="pricing-body">
      <div class="pricing-content">
        <h1 class="pricing-title">每天 3 分钟，AI 帮你写完一篇文章</h1>
        <p class="pricing-subtitle">告别熬夜憋稿，轻松开启内容变现之旅</p>

        <!-- 周期切换 -->
        <div class="billing-toggle">
          <button
            v-for="cycle in cycles"
            :key="cycle.key"
            :class="['toggle-btn', { active: activeCycle === cycle.key }]"
            @click="activeCycle = cycle.key"
          >
            {{ cycle.label }}
            <span v-if="cycle.key === 'year'" class="toggle-badge">最高立省 ¥359</span>
          </button>
        </div>

        <!-- 查看对比 -->
        <div class="compare-link">
          <span @click="scrollToCompare">
            查看完整权益对比 <span class="arrow">↓</span>
          </span>
        </div>

        <!-- 定价卡片 -->
        <div class="pricing-cards">
          <div
            v-for="plan in plans"
            :key="plan.key"
            :class="['pricing-card', { recommended: plan.recommended }]"
          >
            <div v-if="plan.recommended" class="recommended-badge">最受欢迎</div>
            <div class="plan-name">{{ plan.name }}</div>
            <div v-if="getPrice(plan).original" class="plan-original">
              ¥{{ getPrice(plan).original }}
            </div>
            <div class="plan-price">
              ¥{{ getPrice(plan).current }}<span class="plan-period">/{{ getPeriodLabel() }}</span>
            </div>
            <div class="plan-articles">{{ getArticles(plan) }}</div>
            <div v-if="getSavings(plan)" class="plan-savings">年付立省 ¥{{ getSavings(plan) }}</div>
            <button class="plan-btn" :class="{ primary: plan.recommended }">
              立即订阅
            </button>
            <ul class="plan-features">
              <li v-for="feature in plan.features" :key="feature.text" :class="{ disabled: !feature.included }">
                <span class="feature-icon">{{ feature.included ? '✓' : '✗' }}</span>
                {{ feature.text }}
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 权益对比表 -->
      <div id="pricing-compare" class="compare-section">
        <div class="compare-header">
          <h2>功能权益对比</h2>
          <span class="compare-hint">✓ 包含 · ✗ 不包含</span>
        </div>
        <table class="compare-table">
          <thead>
            <tr>
              <th style="width: 32%;">权益</th>
              <th>基础版</th>
              <th class="recommended-col">专业版<span>最受欢迎</span></th>
              <th>旗舰版</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in compareRows" :key="row.label">
              <td>{{ row.label }}</td>
              <td v-html="getCell(row, 'basic')"></td>
              <td class="recommended-col" v-html="getCell(row, 'pro')"></td>
              <td v-html="getCell(row, 'flagship')"></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 底部 -->
    <footer class="pricing-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// ---------- 主题切换 ----------
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

onMounted(() => {
  loadTheme()
})

const activeCycle = ref('month')
const cycles = [
  { key: 'month', label: '月度' },
  { key: 'quarter', label: '季度' },
  { key: 'year', label: '年度' }
]

const getPeriodLabel = () => {
  return activeCycle.value === 'month' ? '月'
    : activeCycle.value === 'quarter' ? '季' : '年'
}

const plans = [
  {
    key: 'basic',
    name: '基础版',
    recommended: false,
    monthly: { original: null, current: 29.9, articles: '30 篇 AI 文章/月' },
    quarter: { original: 89.7, current: 80.7, articles: '90 篇 AI 文章/季' },
    year: { original: 358.8, current: 251.2, articles: '360 篇 AI 文章/年', savings: 107.6 },
    features: [
      { text: '30 篇/月 AI 文章生成', included: true },
      { text: '导出 Word', included: true },
      { text: '复制正文', included: true },
      { text: 'AI 选题灵感', included: true },
      { text: 'AI 标题优化', included: false },
      { text: '在线编辑', included: false },
      { text: '写作风格定制', included: false },
      { text: 'SEO 关键词建议', included: false },
      { text: '8 款基础模板', included: true },
      { text: '5 张贴图/月', included: true },
      { text: '批量生成/改写', included: false },
      { text: '批量导出', included: false },
      { text: '30 天历史记录', included: true },
      { text: '标准生成队列', included: true },
    ]
  },
  {
    key: 'pro',
    name: '专业版',
    recommended: true,
    monthly: { original: null, current: 59.9, articles: '100 篇 AI 文章/月' },
    quarter: { original: 179.7, current: 161.7, articles: '300 篇 AI 文章/季' },
    year: { original: 718.8, current: 503.2, articles: '1200 篇 AI 文章/年', savings: 215.6 },
    features: [
      { text: '100 篇/月 AI 文章生成', included: true },
      { text: '导出 Word', included: true },
      { text: '复制正文', included: true },
      { text: 'AI 选题灵感', included: true },
      { text: 'AI 标题优化', included: true },
      { text: '在线编辑', included: true },
      { text: '3 种预置写作风格', included: true },
      { text: 'SEO 关键词建议', included: false },
      { text: '全部 20+ 模板', included: true },
      { text: '30 张贴图/月', included: true },
      { text: '批量生成/改写', included: false },
      { text: '批量导出', included: false },
      { text: '永久历史记录', included: true },
      { text: '优先生成队列', included: true },
    ]
  },
  {
    key: 'flagship',
    name: '旗舰版',
    recommended: false,
    monthly: { original: null, current: 99.9, articles: '300 篇 AI 文章/月' },
    quarter: { original: 299.7, current: 269.7, articles: '900 篇 AI 文章/季' },
    year: { original: 1198.8, current: 839.2, articles: '3600 篇 AI 文章/年', savings: 359.6 },
    features: [
      { text: '300 篇/月 AI 文章生成', included: true },
      { text: '导出 Word', included: true },
      { text: '复制正文', included: true },
      { text: 'AI 选题灵感', included: true },
      { text: 'AI 标题优化', included: true },
      { text: '在线编辑', included: true },
      { text: '自定义风格 + 记忆偏好', included: true },
      { text: 'SEO 关键词建议', included: true },
      { text: '全部模板 + 自定义模板', included: true },
      { text: '100 张贴图/月', included: true },
      { text: '批量生成/改写', included: true },
      { text: '批量导出', included: true },
      { text: '永久历史记录', included: true },
      { text: '极速生成通道', included: true },
    ]
  }
]

const getPrice = (plan) => {
  const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
  const cycle = plan[keyMap[activeCycle.value]]
  return { original: cycle.original, current: cycle.current }
}

const getArticles = (plan) => {
  const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
  return plan[keyMap[activeCycle.value]].articles
}

const getSavings = (plan) => {
  const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
  return plan[keyMap[activeCycle.value]].savings || null
}

const compareRows = [
  { label: 'AI 文章生成', basic: '30 篇/月', pro: '100 篇/月', flagship: '300 篇/月' },
  { label: '导出 Word', basic: true, pro: true, flagship: true },
  { label: '复制正文', basic: true, pro: true, flagship: true },
  { label: 'AI 选题灵感', basic: true, pro: true, flagship: true },
  { label: 'AI 标题优化', basic: false, pro: true, flagship: true },
  { label: '在线编辑', basic: false, pro: true, flagship: true },
  { label: '写作风格定制', basic: false, pro: '3 种预置', flagship: '自定义 + 记忆' },
  { label: 'SEO 关键词建议', basic: false, pro: false, flagship: true },
  { label: '文章模板', basic: '8 款基础', pro: '全部 20+', flagship: '全部 + 自定义' },
  { label: '贴图生成', basic: '5 张/月', pro: '30 张/月', flagship: '100 张/月' },
  { label: '批量生成/改写', basic: false, pro: false, flagship: true },
  { label: '批量导出', basic: false, pro: false, flagship: true },
  { label: '历史记录', basic: '30 天', pro: '永久', flagship: '永久' },
  { label: '生成队列优先级', basic: '标准', pro: '优先', flagship: '极速' },
]

const getCell = (row, col) => {
  const val = row[col]
  if (val === true) return '<span style="color:#FF2442;font-weight:600;">✓</span>'
  if (val === false) return '<span style="color:#FF2442;font-weight:600;">✗</span>'
  return `<span style="font-weight:500;">${val}</span>`
}

const scrollToCompare = () => {
  document.getElementById('pricing-compare')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.pricing-page {
  min-height: 100vh;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
}

/* 导航栏 */
.pricing-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-logo {
  width: auto;
  height: 32px;
  object-fit: cover;
}

.nav-brand-name {
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-link {
  font-size: 14px;
  color: #262626;
  cursor: pointer;
  transition: color 0.2s;
}

.nav-link:hover,
.nav-link.active {
  color: #FF2442;
}

.nav-cta {
  padding: 8px 22px;
  background: #FF2442;
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.nav-cta:hover {
  background: #E61E3A;
}

/* 主内容 */
.pricing-body {
  flex: 1;
  padding: 40px 24px;
}

.pricing-content {
  max-width: 960px;
  margin: 0 auto;
  text-align: center;
}

.pricing-title {
  font-size: 28px;
  margin-bottom: 8px;
  color: #1a1a1a;
}

.pricing-subtitle {
  color: #595959;
  margin-bottom: 28px;
}

/* 周期切换 */
.billing-toggle {
  display: inline-flex;
  background: #f5f5f5;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 12px;
  gap: 0;
}

.toggle-btn {
  padding: 8px 24px;
  background: transparent;
  color: #595959;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.toggle-btn.active {
  background: #fff;
  color: #FF2442;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.toggle-badge {
  color: #FF2442;
  font-size: 12px;
  font-weight: 500;
}

/* 查看对比 */
.compare-link {
  margin-bottom: 28px;
}

.compare-link span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #FF2442;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 1px solid #FF2442;
  padding-bottom: 1px;
}

.arrow {
  font-size: 12px;
}

/* 定价卡片 */
.pricing-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 40px;
  text-align: left;
}

.pricing-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  position: relative;
  border: 2px solid transparent;
  transition: box-shadow 0.25s, border-color 0.25s, transform 0.25s;
  cursor: pointer;
}

.pricing-card:hover {
  box-shadow: 0 8px 32px rgba(255,36,66,0.18);
  border-color: #FFCBD4;
  transform: translateY(-4px);
}

.pricing-card.recommended {
  border-color: #FF2442;
  box-shadow: 0 4px 24px rgba(255,36,66,0.15);
}

.pricing-card.recommended:hover {
  box-shadow: 0 12px 40px rgba(255,36,66,0.25);
  transform: translateY(-6px);
}

.recommended-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: #FF2442;
  color: #fff;
  padding: 4px 16px;
  border-radius: 12px;
  font-size: 12px;
  white-space: nowrap;
}

.plan-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1a1a1a;
}

.plan-original {
  font-size: 14px;
  color: #8c8c8c;
  text-decoration: line-through;
  margin-bottom: 4px;
}

.plan-price {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.plan-period {
  font-size: 14px;
  color: #8c8c8c;
  font-weight: 400;
}

.plan-articles {
  color: #FF2442;
  font-size: 13px;
  margin-bottom: 16px;
}

.plan-savings {
  color: #FF2442;
  font-size: 13px;
  margin-bottom: 16px;
  text-align: left;
}

.plan-features {
  list-style: none;
  padding: 0;
  margin: 0 0 12px 0;
  color: #595959;
  font-size: 14px;
  line-height: 2;
}

.plan-features li.disabled {
  color: #8c8c8c;
}

.feature-icon {
  margin-right: 6px;
  font-weight: 600;
}

.plan-btn {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  background: #fff;
  color: #FF2442;
  border: 1px solid #FF2442;
  transition: all 0.2s;
  margin-bottom: 20px;
}

.plan-btn:hover {
  background: #FFF5F7;
}

.plan-btn.primary {
  background: #FF2442;
  color: #fff;
  border: none;
}

.plan-btn.primary:hover {
  background: #E61E3A;
}

/* 权益对比表 */
.compare-section {
  max-width: 960px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  text-align: left;
}

.compare-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.compare-header h2 {
  font-size: 20px;
  color: #1a1a1a;
  margin: 0;
}

.compare-hint {
  font-size: 13px;
  color: #8c8c8c;
}

.compare-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.compare-table th,
.compare-table td {
  text-align: center;
  padding: 14px 12px;
}

.compare-table th {
  font-weight: 600;
  color: #1a1a1a;
  border-bottom: 2px solid #f0f0f0;
}

.compare-table th.recommended-col {
  background: #FFE5EB;
  color: #FF2442;
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
}

.compare-table th span {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #FF2442;
}

.compare-table tr:hover td {
  background: #FFF5F7;
}

.compare-table td {
  border-bottom: 1px solid #f5f5f5;
  color: #595959;
}

.compare-table td.recommended-col {
  background: #FFE5EB;
  font-weight: 500;
}

/* 底部 */
.pricing-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}

.pricing-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}

/* 主题切换按钮 */
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
  background: #FFF5F7;
  color: #FF2442;
}

.theme-toggle svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .pricing-page {
  background: #141414;
}

body[data-theme="dark"] .pricing-nav {
  background: #1f1f1f;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .nav-brand-name {
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
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .nav-cta:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .theme-toggle {
  color: #a6a6a6;
}

body[data-theme="dark"] .theme-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme="dark"] .pricing-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .pricing-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .billing-toggle {
  background: #262626;
}

body[data-theme="dark"] .toggle-btn {
  color: #a6a6a6;
}

body[data-theme="dark"] .toggle-btn.active {
  background: #1f1f1f;
  color: #ff4d6f;
}

body[data-theme="dark"] .compare-link span {
  color: #ff4d6f;
  border-bottom-color: #ff4d6f;
}

body[data-theme="dark"] .pricing-card {
  background: #1f1f1f;
}

body[data-theme="dark"] .pricing-card:hover {
  border-color: #ff4d6f;
}

body[data-theme="dark"] .pricing-card.recommended {
  border-color: #ff4d6f;
}

body[data-theme="dark"] .plan-name,
body[data-theme="dark"] .plan-price {
  color: #e0e0e0;
}

body[data-theme="dark"] .plan-original,
body[data-theme="dark"] .plan-period {
  color: #8c8c8c;
}

body[data-theme="dark"] .plan-articles,
body[data-theme="dark"] .plan-savings {
  color: #ff4d6f;
}

body[data-theme="dark"] .plan-features {
  color: #a6a6a6;
}

body[data-theme="dark"] .plan-features li.disabled {
  color: #666;
}

body[data-theme="dark"] .plan-btn {
  background: #1f1f1f;
  color: #ff4d6f;
  border-color: #ff4d6f;
}

body[data-theme="dark"] .plan-btn:hover {
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .plan-btn.primary {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
  border: none;
}

body[data-theme="dark"] .plan-btn.primary:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .compare-section {
  background: #1f1f1f;
}

body[data-theme="dark"] .compare-header h2 {
  color: #e0e0e0;
}

body[data-theme="dark"] .compare-hint {
  color: #8c8c8c;
}

body[data-theme="dark"] .compare-table th {
  color: #e0e0e0;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .compare-table th.recommended-col {
  background: rgba(255, 36, 66, 0.18);
  color: #ff4d6f;
}

body[data-theme="dark"] .compare-table th span {
  color: #ff4d6f;
}

body[data-theme="dark"] .compare-table tr:hover td {
  background: rgba(255, 36, 66, 0.06);
}

body[data-theme="dark"] .compare-table td {
  border-bottom-color: #262626;
  color: #a6a6a6;
}

body[data-theme="dark"] .compare-table td.recommended-col {
  background: rgba(255, 36, 66, 0.1);
  color: #e0e0e0;
}

body[data-theme="dark"] .pricing-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .pricing-footer span + span::before {
  color: #303030;
}
</style>
