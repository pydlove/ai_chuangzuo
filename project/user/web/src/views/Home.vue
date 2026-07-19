<template>
  <div class="home-page">
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

    <!-- Hero 区(banner 嵌入右侧) -->
    <section class="hero">
      <div class="hero-deco hero-deco-1" :style="{ transform: `translateY(${scrollY * 0.15}px)` }"></div>
      <div class="hero-deco hero-deco-2" :style="{ transform: `translateY(${scrollY * 0.08}px)` }"></div>
      <div class="hero-deco hero-deco-3" :style="{ transform: `translateY(${scrollY * 0.12}px)` }"></div>
      <div class="hero-inner">
        <div class="hero-text">
          <div class="hero-badge">
            <span class="hero-badge-dot"></span>
            AI 写作助手 · 多平台变现 · 账号长期增值
          </div>
          <h1 class="hero-title">会增值的自媒体账号<br />从第一篇文章开始</h1>
          <p class="hero-desc">
            3 分钟产出一篇能直接发的文章，平台内多重赚钱机制，<br />
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
        <component
          v-if="banners.length"
          :is="banners[0].linkUrl ? 'a' : 'div'"
          v-bind="banners[0].linkUrl ? { href: banners[0].linkUrl, target: '_blank', rel: 'noopener' } : {}"
          class="hero-banner-card"
        >
          <img :src="banners[0].imageUrl" :alt="'banner-' + banners[0].id" class="hero-banner-card__img" />
          <div class="hero-banner-card__cta">
            <span>查看详情</span>
            <span class="hero-banner-card__arrow">→</span>
          </div>
        </component>
      </div>
    </section>

    <!-- 数据区 -->
    <section class="stats">
      <div class="stats-inner">
        <div class="stat-item reveal" data-reveal-delay="0">
          <div class="stat-num">5000 +</div>
          <div class="stat-label">累计注册账号</div>
        </div>
        <div class="stat-item reveal" data-reveal-delay="120">
          <div class="stat-num">6 大主流</div>
          <div class="stat-label">已覆盖变现平台</div>
        </div>
        <div class="stat-item reveal" data-reveal-delay="240">
          <div class="stat-num">3 分钟</div>
          <div class="stat-label">平均成稿时间</div>
        </div>
      </div>
    </section>

    <!-- 特色功能 -->
    <section class="features">
      <div class="features-inner">
        <div class="features-header reveal" data-reveal-delay="0">
          <div class="section-tag">为什么选择爱创作</div>
          <h2 class="features-title">把时间变成账号资产</h2>
          <p class="features-subtitle">不教你写文案，只帮你把内容变成账号流量、持续收益和长期复利</p>
        </div>
        <div class="features-grid">
          <div class="feature-card reveal" data-reveal-delay="100">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <div class="feature-name">3 分钟成稿</div>
            <div class="feature-desc">输入写作方向，AI 自动完成标题、结构、正文。告别 3 小时憋一篇文。</div>
          </div>
          <div class="feature-card reveal" data-reveal-delay="200">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
                <rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
              </svg>
            </div>
            <div class="feature-name">一稿多发跨平台</div>
            <div class="feature-desc">一次创作，公众号、小红书、抖音、百家号、头条、知乎全部适配，一份内容赚 N 份收益。</div>
          </div>
          <div class="feature-card reveal" data-reveal-delay="300">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            </div>
            <div class="feature-name">爆款结构</div>
            <div class="feature-desc">内置高打开率标题、钩子开头、金句结尾，不用懂写作也能产出爆款。</div>
          </div>
          <div class="feature-card reveal" data-reveal-delay="400">
            <div class="feature-icon">
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
          <div class="feature-card reveal" data-reveal-delay="500">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="1" x2="12" y2="23"/>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
              </svg>
            </div>
            <div class="feature-name">持续变现</div>
            <div class="feature-desc">创作币奖励、邀请好友返利、月榜奖金、外部自媒体收入申报……不是写一篇赚一篇，是越写越能赚。</div>
          </div>
          <div class="feature-card feature-card-asset reveal" data-reveal-delay="600">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                <polyline points="17 6 23 6 23 12"/>
              </svg>
            </div>
            <div class="feature-name">账号资产复利</div>
            <div class="feature-desc">自媒体账号像滚雪球 —— 粉丝、内容沉淀、平台权重，会随时间持续累加。早一天起号，早一天开始滚雪球。</div>
            <svg class="asset-chart" viewBox="0 0 240 60" preserveAspectRatio="none">
              <defs>
                <linearGradient id="asset-grad" x1="0" x2="1" y1="0" y2="0">
                  <stop offset="0%" stop-color="#fff" stop-opacity="0.15"/>
                  <stop offset="100%" stop-color="#fff" stop-opacity="0.9"/>
                </linearGradient>
              </defs>
              <polyline points="0,52 30,46 60,42 90,34 120,28 150,20 180,14 210,8 240,4" fill="none" stroke="url(#asset-grad)" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>
              <circle cx="240" cy="4" r="3.5" fill="#fff"/>
              <text x="234" y="14" fill="#fff" font-size="9" font-weight="700">↑</text>
            </svg>
          </div>
        </div>
      </div>
    </section>

    <!-- 收益玩法矩阵 -->
    <section class="earnings-section">
      <div class="earnings-inner">
        <div class="earnings-header reveal" data-reveal-delay="0">
          <div class="section-tag">4 种变现路径</div>
          <h2 class="earnings-title">边写边赚</h2>
          <p class="earnings-subtitle">平台内赚创作币 + 返利 + 奖金，平台外赚自媒体收入</p>
        </div>
        <div class="earnings-grid">
          <div class="feature-card reveal" data-reveal-delay="100">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="6" x2="12" y2="12"/>
                <line x1="12" y1="12" x2="16" y2="14"/>
              </svg>
            </div>
            <div class="feature-name">创作币奖励</div>
            <div class="feature-desc">完成任务、活动、上榜，1 元 = 1 创作币。抵扣会员购买、满 100 可提现到支付宝。</div>
          </div>
          <div class="feature-card reveal" data-reveal-delay="200">
            <div class="feature-icon">
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
          <div class="feature-card reveal" data-reveal-delay="300">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            </div>
            <div class="feature-name">排行榜奖金</div>
            <div class="feature-desc">创作币榜、自媒体收入榜，月榜 TOP10 各奖 100 创作币 —— 写得好就上榜。</div>
          </div>
          <div class="feature-card reveal" data-reveal-delay="400">
            <div class="feature-icon">
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
        <router-link to="/guide" class="section-cta reveal" data-reveal-delay="500">查看完整玩法 · 看看别人赚了多少 →</router-link>
      </div>
    </section>

    <!-- 使用步骤 -->
    <section class="steps">
      <div class="steps-deco steps-deco-1"></div>
      <div class="steps-deco steps-deco-2"></div>
      <div class="steps-inner">
        <h2 class="steps-title reveal" data-reveal-delay="0">3 步起一个会增值的账号</h2>
        <p class="steps-subtitle reveal" data-reveal-delay="80">1 分钟注册，3 分钟第一篇，写不动也能保持账号在涨</p>
        <div class="steps-list">
          <div class="step-item reveal" data-reveal-delay="160">
            <div class="step-num">1</div>
            <div class="step-name">注册账号</div>
            <div class="step-desc">1 分钟（免费）</div>
          </div>
          <div class="step-item reveal" data-reveal-delay="280">
            <div class="step-num">2</div>
            <div class="step-name">输入主题</div>
            <div class="step-desc">1 句话（零门槛）</div>
          </div>
          <div class="step-item reveal" data-reveal-delay="400">
            <div class="step-num">3</div>
            <div class="step-name">AI 产出</div>
            <div class="step-desc">3 分钟可发（一篇成品）</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 最终 CTA -->
    <section class="cta-section">
      <div class="cta-card reveal" data-reveal-delay="0">
        <h2 class="cta-title">现在起号，3 个月后看复利</h2>
        <p class="cta-desc">
          内容慢慢写，账号先到位 ——<br />
          等你准备好赚钱时，雪球已经在滚。
        </p>
        <div class="cta-actions">
          <router-link to="/login" class="hero-btn">立即开始创作</router-link>
          <router-link to="/guide" class="hero-btn-secondary">查看玩法指南</router-link>
        </div>
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
import { ref, onMounted, onUnmounted } from 'vue'
import NavBar from '@/components/layout/NavBar.vue'
import { fetchHomeBanners } from '@/api/home.js'

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'

const banners = ref([])

async function loadBanners() {
  try {
    banners.value = await fetchHomeBanners()
  } catch (e) {
    banners.value = []
  }
}

// ---- 滚动揭示动画 ----
let observer = null

function initScrollReveal() {
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const delay = Number(entry.target.dataset.revealDelay) || 0
        setTimeout(() => {
          entry.target.classList.add('reveal-visible')
        }, delay)
        observer.unobserve(entry.target)
      }
    })
  }, {
    threshold: 0.12,
    rootMargin: '0px 0px -40px 0px'
  })
  document.querySelectorAll('.reveal').forEach(el => observer.observe(el))
}

// ---- Hero 装饰视差 ----
const scrollY = ref(0)
let ticking = false

function onScroll() {
  if (!ticking) {
    requestAnimationFrame(() => {
      scrollY.value = window.scrollY
      ticking = false
    })
    ticking = true
  }
}

onMounted(() => {
  loadBanners()
  // 等 DOM 渲染完再注册观察器
  requestAnimationFrame(initScrollReveal)
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  observer?.disconnect()
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #fff;
  position: relative;
}

/* ====================== 滚动揭示 ====================== */
.reveal {
  opacity: 0;
  transform: translateY(40px);
  transition: opacity 0.7s cubic-bezier(0.16, 1, 0.3, 1),
              transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
  will-change: opacity, transform;
}
.reveal.reveal-visible {
  opacity: 1;
  transform: translateY(0);
}
@media (prefers-reduced-motion: reduce) {
  .reveal {
    opacity: 1;
    transform: none;
    transition: none;
  }
}

/* ====================== Hero ====================== */
.hero {
  position: relative;
  background: linear-gradient(180deg, #FFE5EB 0%, #fff 100%);
  padding: 80px 48px 60px;
  text-align: left;
  overflow: hidden;
}
.hero-deco {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  pointer-events: none;
  z-index: 0;
}
.hero-deco-1 {
  width: 280px; height: 280px;
  background: radial-gradient(circle, rgba(255, 36, 66, 0.18), transparent 70%);
  top: -80px; left: -80px;
}
.hero-deco-2 {
  width: 220px; height: 220px;
  background: radial-gradient(circle, rgba(255, 107, 138, 0.18), transparent 70%);
  top: 40px; right: -60px;
}
.hero-deco-3 {
  width: 180px; height: 180px;
  background: radial-gradient(circle, rgba(255, 200, 210, 0.4), transparent 70%);
  bottom: -40px; left: 30%;
}
.hero-inner {
  max-width: 1140px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.45fr 1fr;
  gap: 48px;
  align-items: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(8px);
  border: 1px solid #FFCBD4;
  color: #FF2442;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.08);
}
.hero-badge-dot {
  width: 8px; height: 8px;
  background: #FF2442;
  border-radius: 50%;
  animation: pulse 1.6s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}

.hero-title {
  font-size: 46px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
  line-height: 1.2;
  letter-spacing: -0.01em;
}

.hero-desc {
  font-size: 17px;
  color: #595959;
  margin-bottom: 32px;
  line-height: 1.75;
}

.hero-btn {
  display: inline-block;
  padding: 16px 44px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border-radius: 30px;
  font-size: 18px;
  font-weight: 600;
  box-shadow: 0 10px 30px rgba(255,36,66,0.4);
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
}
.hero-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 36px rgba(255,36,66,0.5);
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
.hero-guide-link a:hover { color: #e61e3a; }

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
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(4px);
  color: #FF2442;
  border: 2px solid #FF2442;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
}
.hero-btn-secondary:hover {
  background: #FF2442;
  color: #fff;
  transform: translateY(-2px);
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

/* ===== Hero 内嵌 Banner 侧卡 ===== */
.hero-banner-card {
  display: block;
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(255, 36, 66, 0.18);
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  text-decoration: none;
  color: #fff;
  aspect-ratio: 16 / 9;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
a.hero-banner-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 48px rgba(255, 36, 66, 0.32);
}
.hero-banner-card__img {
  width: 100%;
  height: 100%;
  object-fit: contain;  /* 始终完整显示图片,空隙由卡片渐变背景兜底 */
  display: block;
  transition: transform 0.4s ease;
}
a.hero-banner-card:hover .hero-banner-card__img {
  transform: scale(1.04);
}
.hero-banner-card__cta {
  position: absolute;
  left: 16px;
  bottom: 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.95);
  color: #FF2442;
  font-size: 14px;
  font-weight: 600;
  backdrop-filter: blur(6px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
}
.hero-banner-card__arrow {
  font-size: 16px;
  line-height: 1;
  transition: transform 0.2s ease;
}
a.hero-banner-card:hover .hero-banner-card__arrow {
  transform: translateX(3px);
}

/* ====================== 数据区 ====================== */
.stats {
  background: linear-gradient(180deg, #f8f9fa 0%, #fff 100%);
  padding: 56px 48px;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}
.stats-inner {
  max-width: 1000px;
  margin: 0 auto;
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, #FF4D6F, #FF2442);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}
.stat-label { color: #595959; font-size: 14px; }

/* ====================== 通用小标签 ====================== */
.section-tag {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 16px;
  border-radius: 20px;
  margin-bottom: 12px;
  letter-spacing: 0.02em;
}

/* ====================== 特色功能 ====================== */
.features { padding: 80px 48px; }
.features-inner { max-width: 1100px; margin: 0 auto; }
.features-header { text-align: center; margin-bottom: 48px; }
.features-title {
  font-size: 32px;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-weight: 700;
}
.features-subtitle { color: #595959; font-size: 15px; }

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  position: relative;
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  text-align: left;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}
.feature-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, #FF2442, #ff6b81);
  opacity: 0;
  transition: opacity 0.3s ease;
}
.feature-card:hover {
  transform: translateY(-6px);
  border-color: transparent;
  box-shadow: 0 16px 40px rgba(255, 36, 66, 0.15);
}
.feature-card:hover::before { opacity: 1; }
.feature-card:hover .feature-icon {
  background: linear-gradient(135deg, #FF2442, #ff6b81);
}
.feature-card:hover .feature-icon svg { stroke: #fff; }
.feature-card:hover .feature-name { color: #FF2442; }

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  transition: all 0.3s ease;
}
.feature-icon svg { width: 24px; height: 24px; transition: stroke 0.3s ease; }

.feature-name {
  font-weight: 600;
  font-size: 16px;
  color: #1a1a1a;
  margin-bottom: 8px;
  transition: color 0.3s ease;
}
.feature-desc { font-size: 14px; color: #595959; line-height: 1.65; }

.feature-card-asset {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border-color: transparent;
}
.feature-card-asset::before {
  background: linear-gradient(90deg, rgba(255,255,255,0.6), rgba(255,255,255,0.2));
}
.feature-card-asset .feature-icon {
  background: rgba(255, 255, 255, 0.2);
}
.feature-card-asset:hover .feature-icon {
  background: rgba(255, 255, 255, 0.35);
}
.feature-card-asset .feature-icon svg { stroke: #fff; }
.feature-card-asset:hover .feature-icon svg { stroke: #fff; }
.feature-card-asset .feature-name { color: #fff; }
.feature-card-asset:hover .feature-name { color: #fff; }
.feature-card-asset .feature-desc { color: rgba(255, 255, 255, 0.82); }
.feature-card-asset:hover {
  box-shadow: 0 16px 40px rgba(255, 36, 66, 0.35);
}

.asset-chart {
  width: 100%;
  height: 60px;
  margin-top: 14px;
  display: block;
}

/* ====================== 收益玩法矩阵 ====================== */
.earnings-section {
  background: linear-gradient(180deg, #fff 0%, #fff8f9 50%, #f8f9fa 100%);
  padding: 80px 48px;
}
.earnings-inner { max-width: 1100px; margin: 0 auto; }
.earnings-header { text-align: center; margin-bottom: 48px; }
.earnings-title {
  font-size: 32px;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-weight: 700;
}
.earnings-subtitle { color: #595959; font-size: 15px; }
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
  text-decoration: none;
}
.section-cta:hover {
  background: #FF2442;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(255, 36, 66, 0.25);
}

/* ====================== 使用步骤 ====================== */
.steps {
  position: relative;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 50%, #CC1730 100%);
  padding: 80px 48px;
  text-align: center;
  overflow: hidden;
}
.steps-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  pointer-events: none;
}
.steps-deco-1 {
  width: 200px; height: 200px;
  top: -60px; right: 10%;
}
.steps-deco-2 {
  width: 140px; height: 140px;
  bottom: -40px; left: 8%;
}
.steps-inner { max-width: 900px; margin: 0 auto; position: relative; z-index: 1; }
.steps-title {
  font-size: 32px;
  color: #fff;
  margin-bottom: 12px;
  font-weight: 700;
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
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 16px;
  padding: 32px 28px;
  text-align: center;
  transition: all 0.3s ease;
}
.step-item:hover {
  background: rgba(255,255,255,0.22);
  transform: translateY(-4px);
}
.step-num {
  width: 48px;
  height: 48px;
  background: #fff;
  color: #FF2442;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  margin: 0 auto 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.step-name {
  font-weight: 600;
  color: #fff;
  font-size: 17px;
  margin-bottom: 8px;
}
.step-desc {
  color: rgba(255,255,255,0.85);
  font-size: 14px;
}

/* ====================== 最终 CTA ====================== */
.cta-section {
  padding: 90px 48px;
  text-align: center;
}
.cta-card {
  max-width: 880px;
  margin: 0 auto;
  padding: 56px 48px;
  background: linear-gradient(135deg, #fff8f9 0%, #fff0f2 100%);
  border-radius: 24px;
  border: 1px solid #FFE4E8;
  box-shadow: 0 8px 32px rgba(255, 36, 66, 0.08);
}
.cta-title {
  font-size: 30px;
  color: #1a1a1a;
  margin-bottom: 14px;
  font-weight: 700;
}
.cta-desc {
  color: #595959;
  margin-bottom: 32px;
  font-size: 16px;
  line-height: 1.75;
}
.cta-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

/* ====================== 底部 ====================== */
.home-footer {
  padding: 18px 24px;
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

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .home-page { background: #141414; }

body[data-theme="dark"] .hero {
  background: linear-gradient(180deg, rgba(255, 36, 66, 0.15) 0%, #141414 100%);
}
body[data-theme="dark"] .hero-badge {
  background: rgba(31,31,31,0.85);
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
body[data-theme="dark"] .hero-btn:hover { box-shadow: 0 14px 36px rgba(255, 36, 66, 0.4); }
body[data-theme="dark"] .check-icon { background: #ff4d6f; }
body[data-theme="dark"] .hero-guide-link a { color: #ff4d6f; }
body[data-theme="dark"] .hero-btn-secondary {
  background: rgba(31,31,31,0.7);
  color: #ff4d6f;
  border-color: #ff4d6f;
}
body[data-theme="dark"] .hero-btn-secondary:hover {
  background: #ff4d6f;
  color: #fff;
}
body[data-theme="dark"] .hero-deco-1 { background: radial-gradient(circle, rgba(255, 36, 66, 0.25), transparent 70%); }
body[data-theme="dark"] .hero-deco-2 { background: radial-gradient(circle, rgba(255, 107, 138, 0.2), transparent 70%); }
body[data-theme="dark"] .hero-deco-3 { background: radial-gradient(circle, rgba(255, 36, 66, 0.15), transparent 70%); }

body[data-theme="dark"] .stats {
  background: linear-gradient(180deg, #1f1f1f 0%, #141414 100%);
  border-top-color: #303030;
  border-bottom-color: #303030;
}
body[data-theme="dark"] .stat-num {
  background: linear-gradient(135deg, #FF6B8A, #FF2442);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
body[data-theme="dark"] .stat-label { color: #a6a6a6; }

body[data-theme="dark"] .section-tag {
  background: linear-gradient(135deg, rgba(255, 36, 66, 0.15), rgba(255, 107, 138, 0.15));
  color: #ff6b8a;
}

body[data-theme="dark"] .features { background: #141414; }
body[data-theme="dark"] .feature-card {
  background: #1f1f1f;
  border-color: #2a2a2a;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}
body[data-theme="dark"] .feature-card:hover { box-shadow: 0 16px 40px rgba(255, 36, 66, 0.18); }
body[data-theme="dark"] .feature-card:hover .feature-name { color: #ff6b8a; }
body[data-theme="dark"] .feature-icon {
  background: linear-gradient(135deg, rgba(255, 36, 66, 0.18), rgba(255, 107, 138, 0.12));
}
body[data-theme="dark"] .feature-icon svg { stroke: #ff6b8a; }
body[data-theme="dark"] .feature-card:hover .feature-icon {
  background: linear-gradient(135deg, #FF2442, #ff6b81);
}
body[data-theme="dark"] .feature-card:hover .feature-icon svg { stroke: #fff; }
body[data-theme="dark"] .feature-card-asset {
  background: linear-gradient(135deg, #FF4D6F 0%, #CC1730 100%);
  border-color: transparent;
}
body[data-theme="dark"] .feature-card-asset .feature-icon {
  background: rgba(255, 255, 255, 0.15);
}
body[data-theme="dark"] .feature-card-asset:hover .feature-name { color: #fff; }

body[data-theme="dark"] .earnings-section {
  background: linear-gradient(180deg, #141414 0%, #1a1a1a 50%, #1f1f1f 100%);
}
body[data-theme="dark"] .earnings-title { color: #e0e0e0; }
body[data-theme="dark"] .earnings-subtitle { color: #a6a6a6; }
body[data-theme="dark"] .section-cta { color: #ff6b8a; border-color: #ff6b8a; }
body[data-theme="dark"] .section-cta:hover {
  background: #ff6b8a;
  color: #fff;
}

body[data-theme="dark"] .steps { background: linear-gradient(135deg, #c9183a 0%, #8a0f25 100%); }
body[data-theme="dark"] .step-item {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.18);
}
body[data-theme="dark"] .step-num { background: #e0e0e0; color: #FF2442; }

body[data-theme="dark"] .cta-section { background: #141414; }
body[data-theme="dark"] .cta-card {
  background: linear-gradient(135deg, #1f1f1f 0%, #2a2226 100%);
  border-color: #3a2a2e;
  box-shadow: 0 8px 32px rgba(255, 36, 66, 0.1);
}

body[data-theme="dark"] .home-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}
body[data-theme="dark"] .home-footer span + span::before { color: #303030; }

/* ========== 媒体查询：手机端 ≤768px ========== */
@media (max-width: 768px) {
  .hero { padding: 48px 20px 40px; }
  .hero-inner { grid-template-columns: 1fr; gap: 28px; }
  .hero-banner-card { border-radius: 16px; }
  .hero-badge { font-size: 12px; padding: 5px 12px; margin-bottom: 16px; }
  .hero-title { font-size: 26px; line-height: 1.3; margin-bottom: 14px; }
  .hero-desc { font-size: 15px; margin-bottom: 24px; }
  .hero-btn { padding: 14px 32px; border-radius: 24px; font-size: 16px; }
  .hero-checkmarks { flex-direction: column; gap: 10px; margin-top: 24px; }
  .check-item { justify-content: center; }
  .hero-guide-link { margin-top: 24px; }
  .hero-actions { flex-direction: column; gap: 12px; }
  .hero-btn-secondary { padding: 12px 28px; font-size: 15px; border-radius: 24px; }

  .stats { padding: 36px 20px; }
  .stats-inner { display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px 12px; }
  .stat-num { font-size: 24px; }
  .stat-label { font-size: 13px; }

  .features { padding: 50px 20px; }
  .features-header { margin-bottom: 32px; }
  .features-title { font-size: 24px; }
  .features-subtitle { font-size: 14px; }
  .features-grid { grid-template-columns: 1fr; gap: 16px; }
  .feature-card { padding: 22px; border-radius: 14px; }
  .feature-icon { width: 42px; height: 42px; margin-bottom: 14px; }
  .feature-icon svg { width: 22px; height: 22px; }
  .feature-name { font-size: 15px; }
  .feature-desc { font-size: 13px; }
  .feature-card-asset { padding: 22px; }
  .asset-chart { height: 50px; margin-top: 12px; }

  .earnings-section { padding: 50px 20px; }
  .earnings-header { margin-bottom: 32px; }
  .earnings-title { font-size: 24px; }
  .earnings-subtitle { font-size: 14px; }
  .earnings-grid { grid-template-columns: 1fr; gap: 16px; margin-bottom: 32px; }
  .section-cta { padding: 10px 24px; font-size: 14px; }

  .steps { padding: 50px 20px; }
  .steps-title { font-size: 24px; }
  .steps-subtitle { font-size: 14px; margin-bottom: 32px; }
  .steps-list { flex-direction: column; gap: 16px; }
  .step-item { padding: 22px; }

  .cta-section { padding: 50px 20px; }
  .cta-card { padding: 36px 24px; border-radius: 18px; }
  .cta-title { font-size: 22px; }
  .cta-desc { font-size: 14px; margin-bottom: 24px; }
  .cta-actions { flex-direction: column; gap: 12px; }

  .home-footer { display: flex; flex-direction: column; gap: 4px; font-size: 12px; padding: 16px 20px; }
  .home-footer span + span::before { display: none; }
}
</style>