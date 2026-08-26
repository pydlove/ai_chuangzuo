<template>
  <div class="mobile-home">
    <!-- 顶部导航 -->
    <header class="mh-header">
      <router-link to="/" class="mh-header__brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="mh-header__logo"
        />
        <span class="mh-header__name">爱创作</span>
      </router-link>
      <div class="mh-header__actions">
        <button class="mh-header__menu" aria-label="菜单" @click="menuOpen = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
        <router-link to="/console/workbench" class="mh-header__cta">开始创作</router-link>
      </div>
    </header>

    <!-- 菜单抽屉 -->
    <div v-if="menuOpen" class="mh-menu-backdrop" @click="menuOpen = false"></div>
    <div :class="['mh-menu', { open: menuOpen }]">
      <div class="mh-menu__header">
        <span class="mh-menu__title">菜单</span>
        <button class="mh-menu__close" aria-label="关闭" @click="menuOpen = false">×</button>
      </div>
      <nav class="mh-menu__nav">
        <template v-for="link in navLinks" :key="link.to || link.href">
          <a
            v-if="link.href"
            :href="link.href"
            target="_blank"
            rel="noopener"
            class="mh-menu__link"
            @click="menuOpen = false"
          >{{ link.label }}</a>
          <router-link
            v-else
            :to="link.to"
            class="mh-menu__link"
            :class="{ active: route.path === link.to }"
            @click="menuOpen = false"
          >{{ link.label }}</router-link>
        </template>
      </nav>
    </div>

    <!-- Hero -->
    <section class="mh-hero">
      <div class="mh-hero__badge">
        <span class="mh-hero__badge-dot"></span>
        AI 写作助手 · 多平台变现 · 账号长期增值
      </div>
      <h1 class="mh-hero__title">会增值的自媒体账号，从第一篇文章开始</h1>
      <p class="mh-hero__desc">
        不知道写什么、怎么发、怎么变现？AI 先帮你定平台、定赛道、定人设，再把个人素材变成可发布的多平台文章，3 分钟成稿，边写边赚，让账号持续增值。
      </p>
      <div class="mh-hero__actions">
        <router-link to="/console/workbench" class="mh-btn mh-btn--primary">立即开始创作</router-link>
        <router-link to="/guide" class="mh-btn mh-btn--secondary">看看能赚多少钱</router-link>
      </div>

      <div v-if="banners.length" class="mh-hero__carousel">
        <component
          v-for="(banner, index) in banners"
          :key="banner.id"
          :is="banner.linkUrl ? 'a' : 'div'"
          v-bind="banner.linkUrl ? { href: banner.linkUrl, target: '_blank', rel: 'noopener' } : {}"
          class="mh-hero__banner"
          :class="{ active: index === activeBannerIndex }"
        >
          <img :src="banner.imageUrl" :alt="'banner-' + banner.id" />
        </component>
        <div v-if="banners.length > 1" class="mh-hero__dots">
          <span
            v-for="(_, index) in banners"
            :key="index"
            :class="['mh-hero__dot', { active: index === activeBannerIndex }]"
            @click="activeBannerIndex = index"
          />
        </div>
      </div>
    </section>

    <!-- 快速数据 -->
    <section class="mh-stats">
      <div class="mh-stats__item">
        <div class="mh-stats__num">¥ 800 万 +</div>
        <div class="mh-stats__label">累计为创作者带来收益</div>
      </div>
      <div class="mh-stats__item">
        <div class="mh-stats__num">5000+</div>
        <div class="mh-stats__label">累计注册账号</div>
      </div>
      <div class="mh-stats__item">
        <div class="mh-stats__num">6 大</div>
        <div class="mh-stats__label">已覆盖变现平台</div>
      </div>
      <div class="mh-stats__item">
        <div class="mh-stats__num">3 分钟</div>
        <div class="mh-stats__label">平均成稿时间</div>
      </div>
    </section>

    <!-- 为什么选择 -->
    <section class="mh-section">
      <div class="mh-section__tag">为什么选择爱创作</div>
      <h2 class="mh-section__title">把时间变成账号资产</h2>
      <p class="mh-section__subtitle">不教你写文案，只帮你把内容变成流量、收益和长期复利</p>

      <div class="mh-feature-list">
        <div class="mh-feature-card">
          <div class="mh-feature-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
          </div>
          <div class="mh-feature-card__text">
            <div class="mh-feature-card__name">3 分钟成稿</div>
            <div class="mh-feature-card__desc">输入方向并注入个人素材，AI 自动完成标题、结构和正文，降低 AI 大路货同质化</div>
          </div>
        </div>

        <div class="mh-feature-card">
          <div class="mh-feature-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="7"/>
              <rect x="14" y="3" width="7" height="7"/>
              <rect x="14" y="14" width="7" height="7"/>
              <rect x="3" y="14" width="7" height="7"/>
            </svg>
          </div>
          <div class="mh-feature-card__text">
            <div class="mh-feature-card__name">一稿多发跨平台</div>
            <div class="mh-feature-card__desc">公众号、小红书、抖音、百家号、头条、知乎全部适配</div>
          </div>
        </div>

        <div class="mh-feature-card">
          <div class="mh-feature-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
          </div>
          <div class="mh-feature-card__text">
            <div class="mh-feature-card__name">爆款结构</div>
            <div class="mh-feature-card__desc">高打开率标题、钩子开头、金句结尾，不用懂写作也能出爆款</div>
          </div>
        </div>

        <div class="mh-feature-card">
          <div class="mh-feature-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
          </div>
          <div class="mh-feature-card__text">
            <div class="mh-feature-card__name">导出即发布</div>
            <div class="mh-feature-card__desc">生成后预览、微调、导出 Word，复制到任何平台直接发布</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 收益玩法 -->
    <section class="mh-section mh-section--earn">
      <div class="mh-section__tag">4 种变现路径</div>
      <h2 class="mh-section__title">边写边赚</h2>
      <p class="mh-section__subtitle">平台内赚创作币 + 返利 + 奖金，平台外赚自媒体收入</p>

      <div class="mh-earn-grid">
        <div class="mh-earn-card">
          <div class="mh-earn-card__icon">1</div>
          <div class="mh-earn-card__name">创作币奖励</div>
          <div class="mh-earn-card__desc">完成任务、活动、上榜，1 创作币 = 1 元，可抵扣会员或提现</div>
        </div>
        <div class="mh-earn-card">
          <div class="mh-earn-card__icon">2</div>
          <div class="mh-earn-card__name">邀请好友返利</div>
          <div class="mh-earn-card__desc">好友验证后双方得币；首单返 10%、后续返 5%；3 人 +30 币、5 人 +50 币</div>
        </div>
        <div class="mh-earn-card">
          <div class="mh-earn-card__icon">3</div>
          <div class="mh-earn-card__name">排行榜奖金</div>
          <div class="mh-earn-card__desc">创作币榜月度 TOP10 各奖 100 币；自媒体收入榜记录真实收益</div>
        </div>
        <div class="mh-earn-card">
          <div class="mh-earn-card__icon">4</div>
          <div class="mh-earn-card__name">自媒体收入申报</div>
          <div class="mh-earn-card__desc">申报公众号、小红书、抖音等平台真实收入，审核通过后计入榜单</div>
        </div>
      </div>

      <router-link to="/guide" class="mh-link-btn">查看完整玩法 →</router-link>
    </section>

    <!-- 用户评价 -->
    <TestimonialCarousel :testimonials="testimonials" />

    <!-- 使用步骤 -->
    <section class="mh-section mh-section--steps">
      <h2 class="mh-section__title">3 步起一个会增值的账号</h2>
      <p class="mh-section__subtitle">1 分钟注册，3 分钟第一篇，写不动也能保持账号在涨</p>

      <div class="mh-steps">
        <div class="mh-step">
          <div class="mh-step__num">1</div>
          <div class="mh-step__name">注册账号</div>
          <div class="mh-step__desc">1 分钟（免费）</div>
        </div>
        <div class="mh-step">
          <div class="mh-step__num">2</div>
          <div class="mh-step__name">输入主题</div>
          <div class="mh-step__desc">1 句话（零门槛）</div>
        </div>
        <div class="mh-step">
          <div class="mh-step__num">3</div>
          <div class="mh-step__name">AI 产出</div>
          <div class="mh-step__desc">3 分钟可发（一篇成品）</div>
        </div>
      </div>
    </section>

    <!-- 最终 CTA -->
    <section class="mh-cta">
      <h2 class="mh-cta__title">现在起号，搭一条可执行的自媒体流水线</h2>
      <p class="mh-cta__desc">先定位，再创作，持续迭代。3 分钟成稿，边写边赚，让账号资产开始滚雪球。</p>
      <router-link to="/console/workbench" class="mh-btn mh-btn--primary">立即开始创作</router-link>
      <router-link to="/guide" class="mh-btn mh-btn--secondary">查看玩法指南</router-link>
    </section>

    <footer class="mh-footer">
      <div>© 2026 爱创作 · 杭州爱启云网络科技有限公司</div>
      <div>浙ICP备2025200943号-2</div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import TestimonialCarousel from '@/components/testimonial/TestimonialCarousel.vue'
import { fetchHomeBanners, fetchHomeTestimonials } from '@/api/home.js'

const route = useRoute()
const menuOpen = ref(false)

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/lottery', label: '活动' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' },
  { label: '帮助文档', href: 'https://fxbi16ko1px.feishu.cn/docx/BXVqdp4XwodssXxlfECcUfODnib?from=from_copylink' }
]

const banners = ref([])
const testimonials = ref([])
const activeBannerIndex = ref(0)
let bannerTimer = null

async function loadBanners() {
  try {
    banners.value = await fetchHomeBanners()
  } catch (e) {
    banners.value = []
  }
}

async function loadTestimonials() {
  try {
    testimonials.value = await fetchHomeTestimonials()
  } catch (e) {
    testimonials.value = []
  }
}

function startBannerCarousel() {
  stopBannerCarousel()
  if (banners.value.length <= 1) return
  bannerTimer = setInterval(() => {
    activeBannerIndex.value = (activeBannerIndex.value + 1) % banners.value.length
  }, 5000)
}

function stopBannerCarousel() {
  if (bannerTimer) {
    clearInterval(bannerTimer)
    bannerTimer = null
  }
}

watch(banners, (newBanners) => {
  activeBannerIndex.value = 0
  if (newBanners.length > 1) {
    startBannerCarousel()
  } else {
    stopBannerCarousel()
  }
}, { flush: 'post' })

onMounted(() => {
  loadBanners()
  loadTestimonials()
})

onUnmounted(() => {
  stopBannerCarousel()
})
</script>

<style scoped>
.mobile-home {
  min-height: 100vh;
  background: #fff;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
}

/* 顶部导航 */
.mh-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #f0f0f0;
}
.mh-header__brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mh-header__logo {
  height: 28px;
  width: auto;
  max-width: 40px;
  object-fit: contain;
  border-radius: 6px;
}
.mh-header__name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.mh-header__cta {
  padding: 8px 18px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}
.mh-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mh-header__menu {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  background: #fff;
  color: #595959;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mh-header__menu svg {
  width: 18px;
  height: 18px;
}

/* 菜单抽屉 */
.mh-menu-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 80;
}
.mh-menu {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 240px;
  background: #fff;
  z-index: 90;
  transform: translateX(100%);
  transition: transform 0.25s ease;
  display: flex;
  flex-direction: column;
}
.mh-menu.open {
  transform: translateX(0);
}
.mh-menu__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.mh-menu__title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.mh-menu__close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #f5f5f5;
  color: #595959;
  font-size: 20px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mh-menu__nav {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.mh-menu__link {
  padding: 12px;
  border-radius: 8px;
  font-size: 15px;
  color: #1a1a1a;
  text-decoration: none;
}
.mh-menu__link.active,
.mh-menu__link:active {
  background: #FFF5F7;
  color: #FF2442;
}

/* Hero */
.mh-hero {
  padding: 40px 20px 32px;
  background: linear-gradient(180deg, #FFE5EB 0%, #fff 100%);
  text-align: center;
}
.mh-hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #FFCBD4;
  color: #FF2442;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 20px;
}
.mh-hero__badge-dot {
  width: 6px;
  height: 6px;
  background: #FF2442;
  border-radius: 50%;
  animation: mh-pulse 1.6s ease-in-out infinite;
}
@keyframes mh-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}
.mh-hero__title {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.35;
  margin-bottom: 16px;
  color: #1a1a1a;
}
.mh-hero__desc {
  font-size: 15px;
  color: #595959;
  line-height: 1.7;
  margin-bottom: 28px;
}
.mh-hero__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 32px;
}
.mh-btn {
  display: block;
  width: 100%;
  padding: 14px 0;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  text-decoration: none;
  transition: transform 0.15s ease;
}
.mh-btn:active {
  transform: scale(0.98);
}
.mh-btn--primary {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.35);
}
.mh-btn--secondary {
  background: #fff;
  color: #FF2442;
  border: 1.5px solid #FF2442;
}

/* Hero Banner */
.mh-hero__carousel {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: #f5f5f5;
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.18);
}
.mh-hero__banner {
  opacity: 0;
  transition: opacity 0.5s ease;
  pointer-events: none;
}
.mh-hero__banner.active {
  opacity: 1;
  pointer-events: auto;
  position: relative;
}
.mh-hero__banner:not(.active) {
  position: absolute;
  inset: 0;
}
.mh-hero__banner img {
  width: 100%;
  height: auto;
  display: block;
}
.mh-hero__dots {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
}
.mh-hero__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
}
.mh-hero__dot.active {
  background: #fff;
}

/* 数据区 */
.mh-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-around;
  gap: 16px 8px;
  padding: 28px 16px;
  background: #fff;
  border-bottom: 1px solid #f5f5f5;
}
.mh-stats__item {
  text-align: center;
}
.mh-stats__num {
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(135deg, #FF4D6F, #FF2442);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 4px;
}
.mh-stats__label {
  font-size: 12px;
  color: #8c8c8c;
}

/* 通用区块 */
.mh-section {
  padding: 44px 20px;
}
.mh-section__tag {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 16px;
  margin-bottom: 12px;
}
.mh-section__title {
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 8px;
  color: #1a1a1a;
}
.mh-section__subtitle {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 1.6;
  margin-bottom: 28px;
}
.mh-section--earn {
  background: linear-gradient(180deg, #fff 0%, #fff8f9 100%);
}
.mh-section--steps {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
}
.mh-section--steps .mh-section__title,
.mh-section--steps .mh-section__subtitle {
  color: #fff;
}

/* 功能卡片 */
.mh-feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.mh-feature-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}
.mh-feature-card__icon {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mh-feature-card__icon svg {
  width: 22px;
  height: 22px;
}
.mh-feature-card__name {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
  color: #1a1a1a;
}
.mh-feature-card__desc {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.55;
}

/* 收益玩法 */
.mh-earn-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 24px;
}
.mh-earn-card {
  padding: 18px 14px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  text-align: center;
}
.mh-earn-card__icon {
  width: 32px;
  height: 32px;
  margin: 0 auto 10px;
  background: linear-gradient(135deg, #FF4D6F, #FF2442);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}
.mh-earn-card__name {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 6px;
  color: #1a1a1a;
}
.mh-earn-card__desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
}
.mh-link-btn {
  display: block;
  width: max-content;
  margin: 0 auto;
  padding: 10px 24px;
  border: 1.5px solid #FF2442;
  color: #FF2442;
  border-radius: 22px;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
}

/* 步骤 */
.mh-steps {
  display: flex;
  gap: 12px;
}
.mh-step {
  flex: 1;
  padding: 18px 10px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 14px;
  text-align: center;
  backdrop-filter: blur(6px);
}
.mh-step__num {
  width: 36px;
  height: 36px;
  margin: 0 auto 10px;
  background: #fff;
  color: #FF2442;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 800;
}
.mh-step__name {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 4px;
}
.mh-step__desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

/* CTA */
.mh-cta {
  padding: 44px 20px;
  text-align: center;
  background: #fff;
}
.mh-cta__title {
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 10px;
  color: #1a1a1a;
}
.mh-cta__desc {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 1.7;
  margin-bottom: 24px;
}
.mh-cta .mh-btn {
  margin-bottom: 12px;
}

/* Footer */
.mh-footer {
  padding: 24px 20px 32px;
  text-align: center;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.8;
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-home {
  background: #141414;
  color: #e0e0e0;
}
body[data-theme="dark"] .mh-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mh-header__name { color: #e0e0e0; }
body[data-theme="dark"] .mh-hero {
  background: linear-gradient(180deg, rgba(255, 36, 66, 0.18) 0%, #141414 100%);
}
body[data-theme="dark"] .mh-hero__title,
body[data-theme="dark"] .mh-section__title,
body[data-theme="dark"] .mh-feature-card__name,
body[data-theme="dark"] .mh-earn-card__name,
body[data-theme="dark"] .mh-cta__title {
  color: #e0e0e0;
}
body[data-theme="dark"] .mh-hero__desc,
body[data-theme="dark"] .mh-section__subtitle,
body[data-theme="dark"] .mh-feature-card__desc,
body[data-theme="dark"] .mh-earn-card__desc,
body[data-theme="dark"] .mh-cta__desc,
body[data-theme="dark"] .mh-stats__label {
  color: #a6a6a6;
}
body[data-theme="dark"] .mh-feature-card,
body[data-theme="dark"] .mh-earn-card {
  background: #1f1f1f;
  border-color: #2a2a2a;
}
body[data-theme="dark"] .mh-feature-card__icon {
  background: rgba(255, 36, 66, 0.15);
}
body[data-theme="dark"] .mh-btn--secondary {
  background: #1f1f1f;
  border-color: #ff4d6f;
  color: #ff4d6f;
}
body[data-theme="dark"] .mh-link-btn {
  border-color: #ff4d6f;
  color: #ff4d6f;
}
body[data-theme="dark"] .mh-stats {
  background: #141414;
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mh-section--earn {
  background: linear-gradient(180deg, #141414 0%, #1a1a1a 100%);
}
body[data-theme="dark"] .mh-section--steps {
  background: linear-gradient(135deg, #c9183a 0%, #8a0f25 100%);
}
body[data-theme="dark"] .mh-footer {
  background: #1f1f1f;
  border-top-color: #2a2a2a;
  color: #a6a6a6;
}
</style>
