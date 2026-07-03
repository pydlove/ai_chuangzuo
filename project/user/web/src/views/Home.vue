<template>
  <div class="home-page">
    <!-- 导航栏 -->
    <header class="home-nav">
      <div class="nav-brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="nav-logo"
        />
        <span class="nav-brand-name">爱创作</span>
      </div>
      <div class="nav-links">
        <router-link to="/" class="nav-link nav-link-desktop active">首页</router-link>
        <router-link to="/pricing" class="nav-link nav-link-desktop">会员</router-link>
        <router-link to="/guide" class="nav-link nav-link-desktop">玩法指南</router-link>
        <button
          class="theme-toggle theme-toggle-desktop"
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
        <button
          class="mobile-menu-toggle"
          aria-label="打开菜单"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
      </div>
    </header>

    <!-- 移动端抽屉 -->
    <div
      v-if="mobileMenuOpen"
      class="mobile-drawer-backdrop"
      @click="mobileMenuOpen = false"
    />
    <div :class="['mobile-drawer', { open: mobileMenuOpen }]">
      <div class="mobile-drawer-header">
        <span class="mobile-drawer-title">菜单</span>
        <button
          class="mobile-drawer-close"
          aria-label="关闭菜单"
          @click="mobileMenuOpen = false"
        >
          ×
        </button>
      </div>
      <nav class="mobile-drawer-nav">
        <router-link to="/" class="mobile-drawer-link" @click="mobileMenuOpen = false">首页</router-link>
        <router-link to="/pricing" class="mobile-drawer-link" @click="mobileMenuOpen = false">会员</router-link>
        <router-link to="/guide" class="mobile-drawer-link" @click="mobileMenuOpen = false">玩法指南</router-link>
      </nav>
      <div class="mobile-drawer-footer">
        <button class="mobile-drawer-theme" @click="toggleTheme">
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
          <span>{{ currentTheme === 'light' ? '切换深色主题' : '切换浅色主题' }}</span>
        </button>
      </div>
    </div>

    <!-- Hero 区 -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-badge">AI 写作助手 · 多平台变现 · 账号长期增值</div>
        <h1 class="hero-title">会增值的自媒体账号，从第一篇文章开始</h1>
        <p class="hero-desc">
          3 分钟产出一篇能直接发的文章，平台内多重赚钱机制，
          让你的自媒体账号像滚雪球一样，越做越大、越来越值钱。
        </p>
        <div class="hero-actions">
          <router-link to="/login" class="hero-btn">立即开始创作</router-link>
          <router-link to="/guide" class="hero-btn-secondary">看看能赚多少钱</router-link>
        </div>
        <div class="hero-checkmarks">
          <span class="check-item"><span class="check-icon">✓</span>单篇 3 分钟成稿</span>
          <span class="check-item"><span class="check-icon">✓</span>多平台一稿多发变现</span>
          <span class="check-item"><span class="check-icon">✓</span>账号越久越值钱</span>
        </div>
        <div class="hero-guide-link">
          <router-link to="/guide">不知道怎么变现？先看看玩法指南 →</router-link>
        </div>
      </div>
    </section>

    <!-- 数据区 -->
    <section class="stats">
      <div class="stats-inner">
        <div class="stat-item">
          <div class="stat-num">¥ 360 万 +</div>
          <div class="stat-label">累计为创作者带来收益</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">12 万 +</div>
          <div class="stat-label">累计注册账号</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">6 大主流</div>
          <div class="stat-label">已覆盖变现平台</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">3 分钟</div>
          <div class="stat-label">平均成稿时间</div>
        </div>
      </div>
    </section>

    <!-- 特色功能 -->
    <section class="features">
      <div class="features-inner">
        <div class="features-header">
          <div class="features-tag">为什么选择爱创作</div>
          <h2 class="features-title">把时间变成账号资产</h2>
          <p class="features-subtitle">不教你写文案，只帮你把内容变成账号流量、持续收益和长期复利</p>
        </div>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <div class="feature-name">3 分钟成稿</div>
            <div class="feature-desc">输入写作方向，AI 自动完成标题、结构、正文。告别 3 小时憋一篇文。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
                <rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
              </svg>
            </div>
            <div class="feature-name">一稿多发跨平台</div>
            <div class="feature-desc">一次创作，公众号、小红书、抖音、百家号、头条、知乎全部适配，一份内容赚 N 份收益。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            </div>
            <div class="feature-name">爆款结构</div>
            <div class="feature-desc">内置高打开率标题、钩子开头、金句结尾，不用懂写作也能产出爆款。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
                <polyline points="10 9 9 9 8 9"/>
              </svg>
            </div>
            <div class="feature-name">导出即发布</div>
            <div class="feature-desc">生成后预览、微调、导出 Word，复制到任何平台直接发布，快速变现。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="1" x2="12" y2="23"/>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
              </svg>
            </div>
            <div class="feature-name">持续变现</div>
            <div class="feature-desc">创作币奖励、邀请好友返利、月榜奖金、外部自媒体收入申报……不是写一篇赚一篇，是越写越能赚。</div>
          </div>
          <div class="feature-card feature-card-asset">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                <polyline points="17 6 23 6 23 12"/>
              </svg>
            </div>
            <div class="feature-name">账号资产复利</div>
            <div class="feature-desc">自媒体账号像滚雪球 —— 粉丝、内容沉淀、平台权重，会随时间持续累加。早一天起号，早一天开始滚雪球。</div>
            <svg class="asset-chart" viewBox="0 0 240 60" preserveAspectRatio="none">
              <defs>
                <linearGradient id="asset-grad" x1="0" x2="1" y1="0" y2="0">
                  <stop offset="0%" stop-color="#FF2442" stop-opacity="0.2"/>
                  <stop offset="100%" stop-color="#FF2442" stop-opacity="1"/>
                </linearGradient>
              </defs>
              <polyline points="0,52 30,46 60,42 90,34 120,28 150,20 180,14 210,8 240,4" fill="none" stroke="url(#asset-grad)" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>
              <circle cx="240" cy="4" r="3.5" fill="#FF2442"/>
              <text x="234" y="14" fill="#FF2442" font-size="9" font-weight="700">↑</text>
            </svg>
          </div>
        </div>
      </div>
    </section>

    <!-- 收益玩法矩阵 -->
    <section class="earnings-section">
      <div class="earnings-inner">
        <div class="earnings-header">
          <div class="earnings-tag">4 种变现路径</div>
          <h2 class="earnings-title">边写边赚</h2>
          <p class="earnings-subtitle">平台内赚创作币 + 返利 + 奖金，平台外赚自媒体收入</p>
        </div>
        <div class="earnings-grid">
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="6" x2="12" y2="12"/>
                <line x1="12" y1="12" x2="16" y2="14"/>
              </svg>
            </div>
            <div class="feature-name">创作币奖励</div>
            <div class="feature-desc">完成任务、活动、上榜，1 元 = 1 创作币。抵扣会员购买、满 100 可提现到支付宝。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>
            <div class="feature-name">邀请好友返利</div>
            <div class="feature-desc">邀请 3 人 → 3 天会员；好友首单 10% 返利。老带新，你赚会员天数和创作币。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            </div>
            <div class="feature-name">排行榜奖金</div>
            <div class="feature-desc">创作币榜、自媒体收入榜，月榜 TOP10 各奖 100 创作币 —— 写得好就上榜。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <line x1="3" y1="9" x2="21" y2="9"/>
                <line x1="9" y1="21" x2="9" y2="9"/>
              </svg>
            </div>
            <div class="feature-name">自媒体收入申报</div>
            <div class="feature-desc">公众号、小红书、抖音、百家号、头条、知乎 收益申报，记录你的自媒体收入轨迹。</div>
          </div>
        </div>
        <router-link to="/guide" class="section-cta">查看完整玩法 · 看看别人赚了多少 →</router-link>
      </div>
    </section>

    <!-- 使用步骤 -->
    <section class="steps">
      <div class="steps-inner">
        <h2 class="steps-title">3 步起一个会增值的账号</h2>
        <p class="steps-subtitle">1 分钟注册，3 分钟第一篇，写不动也能保持账号在涨</p>
        <div class="steps-list">
          <div class="step-item">
            <div class="step-num">1</div>
            <div class="step-name">注册账号</div>
            <div class="step-desc">1 分钟（免费）</div>
          </div>
          <div class="step-item">
            <div class="step-num">2</div>
            <div class="step-name">输入主题</div>
            <div class="step-desc">1 句话（零门槛）</div>
          </div>
          <div class="step-item">
            <div class="step-num">3</div>
            <div class="step-name">AI 产出</div>
            <div class="step-desc">3 分钟可发（一篇成品）</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 最终 CTA -->
    <section class="cta-section">
      <h2 class="cta-title">现在起号，3 个月后看复利</h2>
      <p class="cta-desc">
        内容慢慢写，账号先到位 ——<br />
        等你准备好赚钱时，雪球已经在滚。
      </p>
      <div class="cta-actions">
        <router-link to="/login" class="hero-btn">立即开始创作</router-link>
        <router-link to="/guide" class="hero-btn-secondary">查看玩法指南</router-link>
      </div>
    </section>

    <!-- 底部 -->
    <footer class="home-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

// ---------- 主题切换 ----------
const THEME_KEY = 'aichuangzuo_theme'
const currentTheme = ref('light')
const mobileMenuOpen = ref(false)

watch(mobileMenuOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

onUnmounted(() => {
  document.body.style.overflow = ''
})

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
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #fff;
}

/* 导航栏 */
.home-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 48px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  user-select: none;
  cursor: default;
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
  transition: all 0.2s;
}

.nav-cta:hover {
  background: #E61E3A;
}

/* Hero */
.hero {
  background: linear-gradient(180deg, #FFE5EB 0%, #fff 100%);
  padding: 90px 48px 70px;
  text-align: center;
}

.hero-inner {
  max-width: 660px;
  margin: 0 auto;
}

.hero-badge {
  display: inline-block;
  background: #fff;
  border: 1px solid #FFCBD4;
  color: #FF2442;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 46px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
  line-height: 1.15;
}

.hero-desc {
  font-size: 18px;
  color: #595959;
  margin-bottom: 32px;
  line-height: 1.7;
}

.hero-btn {
  display: inline-block;
  padding: 16px 44px;
  background: #FF2442;
  color: #fff;
  border-radius: 30px;
  font-size: 18px;
  font-weight: 600;
  box-shadow: 0 10px 30px rgba(255,36,66,0.35);
  cursor: pointer;
  transition: all 0.2s;
}

.hero-btn:hover {
  background: #E61E3A;
}

.hero-checkmarks {
  display: flex;
  gap: 24px;
  justify-content: center;
  margin-top: 28px;
  flex-wrap: wrap;
}

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

.hero-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-btn-secondary {
  display: inline-block;
  padding: 14px 28px;
  background: transparent;
  color: #FF2442;
  border: 2px solid #FF2442;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.hero-btn-secondary:hover {
  background: #FF2442;
  color: #fff;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #595959;
  font-size: 14px;
}

.check-icon {
  width: 18px;
  height: 18px;
  background: #FF2442;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 数据区 */
.stats {
  background: #f8f9fa;
  padding: 50px 48px;
}

.stats-inner {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #FF2442;
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}

.stat-label {
  color: #595959;
  font-size: 14px;
}

/* 特色功能 */
.features {
  padding: 70px 48px;
}

.features-inner {
  max-width: 1100px;
  margin: 0 auto;
}

.features-header {
  text-align: center;
  margin-bottom: 48px;
}

.features-tag {
  color: #FF2442;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.features-title {
  font-size: 30px;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.features-subtitle {
  color: #595959;
  font-size: 15px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  text-align: left;
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.feature-icon svg {
  width: 22px;
  height: 22px;
}

.feature-name {
  font-weight: 600;
  font-size: 16px;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.feature-desc {
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
}

.feature-card-asset {
  background: linear-gradient(135deg, #1f1f1f 0%, #2a2a2a 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
}

.feature-card-asset .feature-icon {
  background: rgba(255, 36, 66, 0.15);
}

.feature-card-asset .feature-name {
  color: #fff;
}

.feature-card-asset .feature-desc {
  color: rgba(255, 255, 255, 0.75);
}

.asset-chart {
  width: 100%;
  height: 60px;
  margin-top: 14px;
  display: block;
}

/* 收益玩法矩阵 */
.earnings-section {
  background: #f8f9fa;
  padding: 70px 48px;
}

.earnings-inner {
  max-width: 1100px;
  margin: 0 auto;
}

.earnings-header {
  text-align: center;
  margin-bottom: 48px;
}

.earnings-tag {
  display: inline-block;
  color: #FF2442;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 0.05em;
}

.earnings-title {
  font-size: 30px;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-weight: 700;
}

.earnings-subtitle {
  color: #595959;
  font-size: 15px;
}

.earnings-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 40px;
}

.section-cta {
  display: block;
  width: max-content;
  margin: 0 auto;
  padding: 12px 28px;
  background: transparent;
  color: #FF2442;
  border: 2px solid #FF2442;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.section-cta:hover {
  background: #FF2442;
  color: #fff;
}

/* 使用步骤 */
.steps {
  background: linear-gradient(135deg, #FF2442 0%, #CC1730 100%);
  padding: 70px 48px;
  text-align: center;
}

.steps-inner {
  max-width: 900px;
  margin: 0 auto;
}

.steps-title {
  font-size: 30px;
  color: #fff;
  margin-bottom: 12px;
}

.steps-subtitle {
  color: rgba(255,255,255,0.9);
  font-size: 16px;
  margin-bottom: 48px;
}

.steps-list {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.step-item {
  flex: 1;
  background: rgba(255,255,255,0.12);
  border-radius: 16px;
  padding: 28px;
  text-align: center;
}

.step-num {
  width: 40px;
  height: 40px;
  background: #fff;
  color: #FF2442;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  margin: 0 auto 16px;
}

.step-name {
  font-weight: 600;
  color: #fff;
  font-size: 16px;
  margin-bottom: 8px;
}

.step-desc {
  color: rgba(255,255,255,0.85);
  font-size: 14px;
}

/* 最终 CTA */
.cta-section {
  padding: 70px 48px;
  text-align: center;
}

.cta-title {
  font-size: 28px;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.cta-desc {
  color: #595959;
  margin-bottom: 32px;
  font-size: 16px;
}

.cta-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

/* 底部 */
.home-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}

.home-footer span + span::before {
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
body[data-theme="dark"] .home-page {
  background: #141414;
}

body[data-theme="dark"] .home-nav {
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

body[data-theme="dark"] .hero {
  background: linear-gradient(180deg, rgba(255, 36, 66, 0.12) 0%, #141414 100%);
}

body[data-theme="dark"] .hero-badge {
  background: #1f1f1f;
  border-color: rgba(255, 77, 111, 0.5);
  color: #ff4d6f;
}

body[data-theme="dark"] .hero-title,
body[data-theme="dark"] .features-title,
body[data-theme="dark"] .feature-name,
body[data-theme="dark"] .cta-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .hero-desc,
body[data-theme="dark"] .check-item,
body[data-theme="dark"] .features-subtitle,
body[data-theme="dark"] .feature-desc,
body[data-theme="dark"] .cta-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .hero-btn {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
  box-shadow: 0 10px 30px rgba(255, 36, 66, 0.25);
}

body[data-theme="dark"] .hero-btn:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .check-icon {
  background: #ff4d6f;
}

body[data-theme="dark"] .hero-guide-link a {
  color: #ff4d6f;
}

body[data-theme="dark"] .stats {
  background: #1f1f1f;
}

body[data-theme="dark"] .stat-num {
  color: #ff4d6f;
}

body[data-theme="dark"] .stat-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .features-tag {
  color: #ff4d6f;
}

body[data-theme="dark"] .feature-card {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .earnings-section {
  background: #1a1a1a;
}

body[data-theme="dark"] .earnings-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .earnings-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .section-cta {
  color: #ff4d6f;
  border-color: #ff4d6f;
}

body[data-theme="dark"] .section-cta:hover {
  background: #ff4d6f;
  color: #fff;
}

body[data-theme="dark"] .feature-card-asset {
  background: linear-gradient(135deg, #1f1f1f 0%, #2a2a2a 100%);
}

body[data-theme="dark"] .feature-icon {
  background: rgba(255, 36, 66, 0.12) !important;
}

body[data-theme="dark"] .feature-icon svg {
  stroke: #ff4d6f;
}

body[data-theme="dark"] .steps {
  background: linear-gradient(135deg, #b01030 0%, #8a0f25 100%);
}

body[data-theme="dark"] .step-num {
  background: #e0e0e0;
  color: #FF2442;
}

body[data-theme="dark"] .cta-section {
  background: #141414;
}

body[data-theme="dark"] .home-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .home-footer span + span::before {
  color: #303030;
}

/* ========== 手机端导航与抽屉 ========== */

.nav-link-desktop,
.theme-toggle-desktop {
  display: inline-flex;
}

/* 汉堡按钮 */
.mobile-menu-toggle {
  display: none;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.mobile-menu-toggle:hover {
  background: #FFF5F7;
  color: #FF2442;
}

.mobile-menu-toggle svg {
  width: 20px;
  height: 20px;
}

/* 抽屉遮罩 */
.mobile-drawer-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;
}

/* 抽屉面板 */
.mobile-drawer {
  display: none;
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 260px;
  background: #fff;
  z-index: 100;
  transform: translateX(100%);
  transition: transform 0.25s ease;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.1);
  flex-direction: column;
}

.mobile-drawer.open {
  transform: translateX(0);
}

.mobile-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.mobile-drawer-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.mobile-drawer-close {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.mobile-drawer-close:hover {
  background: #f5f5f5;
  color: #FF2442;
}

.mobile-drawer-nav {
  flex: 1;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-drawer-link {
  padding: 12px;
  border-radius: 8px;
  font-size: 15px;
  color: #1a1a1a;
  transition: all 0.2s;
}

.mobile-drawer-link:hover,
.mobile-drawer-link.active {
  background: #FFF5F7;
  color: #FF2442;
}

.mobile-drawer-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

.mobile-drawer-theme {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #eee;
  background: #fff;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.mobile-drawer-theme:hover {
  border-color: #FFCBD4;
  color: #FF2442;
  background: #FFF5F7;
}

.mobile-drawer-theme svg {
  width: 18px;
  height: 18px;
}

/* ========== 媒体查询：手机端 ≤768px ========== */

@media (max-width: 768px) {
  .home-nav {
    padding: 12px 16px;
  }

  .nav-logo {
    height: 28px;
  }

  .nav-brand-name {
    font-size: 16px;
  }

  .nav-links {
    gap: 12px;
  }

  .nav-link-desktop,
  .theme-toggle-desktop {
    display: none;
  }

  .nav-cta {
    padding: 10px 20px;
    font-size: 13px;
    border-radius: 20px;
  }

  .mobile-menu-toggle {
    display: flex;
  }

  .mobile-drawer-backdrop {
    display: block;
  }

  .mobile-drawer {
    display: flex;
  }

  /* Hero */
  .hero {
    padding: 48px 20px 40px;
  }

  .hero-badge {
    font-size: 12px;
    padding: 5px 12px;
    margin-bottom: 16px;
  }

  .hero-title {
    font-size: 28px;
    line-height: 1.25;
    margin-bottom: 16px;
  }

  .hero-desc {
    font-size: 16px;
    margin-bottom: 24px;
  }

  .hero-btn {
    padding: 14px 32px;
    border-radius: 24px;
    font-size: 16px;
  }

  .hero-checkmarks {
    flex-direction: column;
    gap: 10px;
    margin-top: 24px;
  }

  .check-item {
    justify-content: center;
  }

  .hero-guide-link {
    margin-top: 24px;
  }

  /* 数据区 */
  .stats {
    padding: 32px 20px;
  }

  .stats-inner {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px 12px;
  }

  .stat-num {
    font-size: 24px;
  }

  .stat-label {
    font-size: 13px;
  }

  /* 特色功能 */
  .features {
    padding: 40px 20px;
  }

  .features-header {
    margin-bottom: 32px;
  }

  .features-title {
    font-size: 24px;
  }

  .features-subtitle {
    font-size: 14px;
  }

  .features-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .feature-card {
    padding: 20px;
    border-radius: 12px;
  }

  .feature-icon {
    width: 40px;
    height: 40px;
    margin-bottom: 14px;
  }

  .feature-icon svg {
    width: 20px;
    height: 20px;
  }

  .feature-name {
    font-size: 15px;
  }

  .feature-desc {
    font-size: 13px;
  }

  /* 账号资产复利卡 — 移动端调整 */
  .feature-card-asset {
    padding: 22px;
  }

  .asset-chart {
    height: 50px;
    margin-top: 12px;
  }

  /* 收益玩法矩阵 — 移动端切换单列 */
  .earnings-section {
    padding: 40px 20px;
  }

  .earnings-header {
    margin-bottom: 32px;
  }

  .earnings-title {
    font-size: 24px;
  }

  .earnings-subtitle {
    font-size: 14px;
  }

  .earnings-grid {
    grid-template-columns: 1fr;
    gap: 16px;
    margin-bottom: 32px;
  }

  .section-cta {
    padding: 10px 24px;
    font-size: 14px;
  }

  /* Hero 主+次双按钮 — 移动端改纵向 */
  .hero-actions {
    flex-direction: column;
    gap: 12px;
  }

  .hero-btn-secondary {
    padding: 12px 28px;
    font-size: 15px;
    border-radius: 24px;
  }

  /* 使用步骤 */
  .steps {
    padding: 40px 20px;
  }

  .steps-title {
    font-size: 24px;
  }

  .steps-subtitle {
    font-size: 14px;
    margin-bottom: 32px;
  }

  .steps-list {
    flex-direction: column;
    gap: 16px;
  }

  .step-item {
    padding: 20px;
  }

  /* 最终 CTA */
  .cta-section {
    padding: 40px 20px;
  }

  .cta-title {
    font-size: 22px;
  }

  .cta-desc {
    font-size: 14px;
    margin-bottom: 24px;
  }

  .cta-actions {
    flex-direction: column;
    gap: 12px;
  }

  /* 底部 */
  .home-footer {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 12px;
    padding: 16px 20px;
  }

  .home-footer span + span::before {
    display: none;
  }
}

/* ========== 手机端抽屉黑暗主题 ========== */

body[data-theme="dark"] .mobile-menu-toggle {
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-menu-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme="dark"] .mobile-drawer {
  background: #1f1f1f;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .mobile-drawer-header,
body[data-theme="dark"] .mobile-drawer-footer {
  border-color: #303030;
}

body[data-theme="dark"] .mobile-drawer-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .mobile-drawer-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-drawer-close:hover {
  background: #2a2a2a;
  color: #ff4d6f;
}

body[data-theme="dark"] .mobile-drawer-link {
  color: #e0e0e0;
}

body[data-theme="dark"] .mobile-drawer-link:hover,
body[data-theme="dark"] .mobile-drawer-link.active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme="dark"] .mobile-drawer-theme {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-drawer-theme:hover {
  border-color: rgba(255, 77, 111, 0.5);
  color: #ff4d6f;
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .hero-btn-secondary {
  color: #ff4d6f;
  border-color: #ff4d6f;
}

body[data-theme="dark"] .hero-btn-secondary:hover {
  background: #ff4d6f;
  color: #fff;
}
</style>
