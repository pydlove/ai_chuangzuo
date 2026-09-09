<template>
  <div class="mobile-home">
    <!-- 顶部导航 -->
    <header class="mh-header">
      <router-link to="/" class="mh-header__brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          :alt="homeBrand"
          class="mh-header__logo"
        />
        <span class="mh-header__name">{{ homeBrand }}</span>
      </router-link>
      <div class="mh-header__actions">
        <button class="mh-header__menu" aria-label="菜单" @click="menuOpen = true">
          <Icon name="menu" :size="20" />
        </button>
        <router-link :to="landingTopCta.to" class="mh-header__cta">{{ landingTopCta.label }}</router-link>
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
        <template v-for="link in landingNavLinks" :key="link.to || link.href">
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
        {{ homeHero.badge }}
      </div>
      <h1 class="mh-hero__title">{{ homeHero.title }}</h1>
      <p class="mh-hero__desc" v-html="homeHero.desc.replace(/\n/g, '<br>')"></p>
      <div class="mh-hero__actions">
        <router-link :to="homeHero.primaryBtn.to" class="mh-btn mh-btn--primary">{{ homeHero.primaryBtn.text }}</router-link>
        <router-link :to="homeHero.secondaryBtn.to" class="mh-btn mh-btn--secondary">{{ homeHero.secondaryBtn.text }}</router-link>
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
      <div v-for="stat in homeStats.mobile" :key="stat.label" class="mh-stats__item">
        <div class="mh-stats__num">{{ stat.num }}</div>
        <div class="mh-stats__label">{{ stat.label }}</div>
      </div>
    </section>

    <!-- 为什么选择 -->
    <section class="mh-section">
      <div class="mh-section__tag">{{ homeFeatures.tag }}</div>
      <h2 class="mh-section__title">{{ homeFeatures.title }}</h2>
      <p class="mh-section__subtitle">{{ homeFeatures.subtitle }}</p>

      <div class="mh-feature-list">
        <div v-for="feature in homeFeatures.items.slice(0, 4)" :key="feature.name" class="mh-feature-card">
          <div class="mh-feature-card__icon">
            <Icon :name="feature.icon" :size="22" :stroke-width="2" />
          </div>
          <div class="mh-feature-card__text">
            <div class="mh-feature-card__name">{{ feature.name }}</div>
            <div class="mh-feature-card__desc">{{ feature.desc }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 收益玩法 -->
    <section class="mh-section mh-section--earn">
      <div class="mh-section__tag">{{ homeEarnings.tag }}</div>
      <h2 class="mh-section__title">{{ homeEarnings.title }}</h2>
      <p class="mh-section__subtitle">{{ homeEarnings.subtitle }}</p>

      <div class="mh-earn-grid">
        <div v-for="(item, index) in homeEarnings.items" :key="item.name" class="mh-earn-card">
          <div class="mh-earn-card__icon">{{ index + 1 }}</div>
          <div class="mh-earn-card__name">{{ item.name }}</div>
          <div class="mh-earn-card__desc">{{ item.desc }}</div>
        </div>
      </div>

      <router-link :to="homeEarnings.link.mobile.to" class="mh-link-btn">{{ homeEarnings.link.mobile.text }}</router-link>
    </section>

    <!-- 用户评价 -->
    <TestimonialCarousel :testimonials="testimonials" :load-more="loadMoreTestimonials" />

    <!-- 使用步骤 -->
    <section class="mh-section mh-section--steps">
      <h2 class="mh-section__title">{{ homeSteps.title }}</h2>
      <p class="mh-section__subtitle">{{ homeSteps.subtitle }}</p>

      <div class="mh-steps">
        <div v-for="step in homeSteps.items" :key="step.name" class="mh-step">
          <div class="mh-step__num">{{ step.num }}</div>
          <div class="mh-step__name">{{ step.name }}</div>
          <div class="mh-step__desc">{{ step.desc }}</div>
        </div>
      </div>
    </section>

    <!-- 会员价格 -->
    <section class="mh-section mh-pricing">
      <div class="mh-section__tag">{{ homePricing.tag }}</div>
      <h2 class="mh-section__title">{{ homePricing.title }}</h2>
      <p class="mh-section__subtitle">{{ homePricing.subtitle }}</p>

      <div v-if="pricingLoading" class="mh-pricing__loading">套餐加载中…</div>
      <div v-else class="mh-pricing__list">
        <div
          v-for="plan in pricingPlans"
          :key="plan.key"
          :class="['mh-pricing-card', { recommended: plan.recommended }]"
        >
          <div v-if="plan.recommended" class="mh-pricing-card__badge">最受欢迎</div>
          <div class="mh-pricing-card__name">{{ plan.name }}<span v-if="plan.nameEn" class="mh-pricing-card__name-en">{{ plan.nameEn }}</span></div>
          <div class="mh-pricing-card__articles">{{ plan.monthly?.articles }}</div>
          <div class="mh-pricing-card__price">
            <template v-if="showFirstMonth(plan)">
              <span class="mh-pricing-card__first-tag">首月特惠</span>
              <span class="mh-pricing-card__original">¥{{ plan.monthly?.current }}</span>
              <span class="mh-pricing-card__current">¥{{ plan.monthly?.firstMonth }}</span>
              <span class="mh-pricing-card__period">/首月</span>
              <div class="mh-pricing-card__renew">次月起 ¥{{ plan.monthly?.current }}/月</div>
            </template>
            <template v-else>
              <span class="mh-pricing-card__current">¥{{ plan.monthly?.current }}</span>
              <span class="mh-pricing-card__period">/月</span>
            </template>
          </div>
          <router-link to="/pricing" class="mh-pricing-card__btn">{{ homePricing.ctaText }}</router-link>
        </div>
      </div>
    </section>

    <!-- 最终 CTA -->
    <section class="mh-cta">
      <h2 class="mh-cta__title">{{ homeFinalCta.title }}</h2>
      <p class="mh-cta__desc">{{ homeFinalCta.desc }}</p>
      <router-link :to="homeFinalCta.primaryBtn.to" class="mh-btn mh-btn--primary">{{ homeFinalCta.primaryBtn.text }}</router-link>
      <router-link :to="homeFinalCta.secondaryBtn.to" class="mh-btn mh-btn--secondary">{{ homeFinalCta.secondaryBtn.text }}</router-link>
    </section>

    <HomeFooter class="mh-footer-links" />
    <AppFooter variant="mobile" class="mh-footer" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import TestimonialCarousel from '@/components/testimonial/TestimonialCarousel.vue'
import Icon from '@/components/common/Icon.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import HomeFooter from '@/components/layout/HomeFooter.vue'
import { fetchHomeBanners, fetchHomeTestimonials } from '@/api/home.js'
import { getPlanCatalog, getMyMembership } from '@/api/membership.js'
import { STORAGE_KEYS } from '@/constants/storage.js'
import { landingNavLinks, landingTopCta } from '@/data/siteConfig.js'
import {
  homeBrand,
  homeHero,
  homeStats,
  homeFeatures,
  homeEarnings,
  homeSteps,
  homePricing,
  homeFinalCta
} from '@/data/homeContent.js'

const route = useRoute()
const menuOpen = ref(false)

const TESTIMONIAL_PAGE_SIZE = 20
const banners = ref([])
const testimonials = ref([])
const testimonialPage = ref(1)
const hasMoreTestimonials = ref(true)
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
    const list = await fetchHomeTestimonials(1, TESTIMONIAL_PAGE_SIZE)
    testimonials.value = list
    testimonialPage.value = 1
    hasMoreTestimonials.value = list.length >= TESTIMONIAL_PAGE_SIZE
  } catch (e) {
    testimonials.value = []
    hasMoreTestimonials.value = false
  }
}

// ---- 会员价格区块 ----
const pricingPlans = ref([])
const pricingLoading = ref(false)
// 未登录默认按首购展示（是否真享首月价由支付服务端校验）
const pricingFirstPurchase = ref(true)

function showFirstMonth(plan) {
  return pricingFirstPurchase.value && plan?.monthly?.firstMonth != null
}

async function loadPricing() {
  pricingLoading.value = true
  try {
    if (localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)) {
      try {
        const membershipRes = await getMyMembership()
        pricingFirstPurchase.value = (membershipRes.data || membershipRes)?.firstPurchase !== false
      } catch {
        pricingFirstPurchase.value = true
      }
    }
    const res = await getPlanCatalog()
    pricingPlans.value = (res.data?.plans || []).filter((plan) => plan?.monthly?.current != null)
  } catch (e) {
    pricingPlans.value = []
  } finally {
    pricingLoading.value = false
  }
}

// 加载下一页评价；返回 false 表示没有更多（调用方应回到第一个）
async function loadMoreTestimonials() {
  if (!hasMoreTestimonials.value) return false
  try {
    const list = await fetchHomeTestimonials(testimonialPage.value + 1, TESTIMONIAL_PAGE_SIZE)
    if (!list.length) {
      hasMoreTestimonials.value = false
      return false
    }
    // 模拟评价与用户评价是两张表各自的自增 ID，会重叠，去重必须带 source
    const existingKeys = new Set(testimonials.value.map((item) => `${item.source || 'item'}-${item.id}`))
    const fresh = list.filter((item) => !existingKeys.has(`${item.source || 'item'}-${item.id}`))
    if (!fresh.length) {
      hasMoreTestimonials.value = false
      return false
    }
    testimonials.value = [...testimonials.value, ...fresh]
    testimonialPage.value += 1
    hasMoreTestimonials.value = list.length >= TESTIMONIAL_PAGE_SIZE
    return true
  } catch (e) {
    return false
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
  loadPricing()
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

/* 会员价格 */
.mh-pricing { background: #f8f9fa; }
.mh-pricing__loading {
  text-align: center;
  color: #8c8c8c;
  padding: 24px 0;
}
.mh-pricing__list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.mh-pricing-card {
  position: relative;
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  border: 1px solid #f0f0f0;
  text-align: center;
}
.mh-pricing-card.recommended {
  border: 2px solid #FF2442;
}
.mh-pricing-card__badge {
  position: absolute;
  top: -11px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 12px;
  border-radius: 10px;
  white-space: nowrap;
}
.mh-pricing-card__name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 2px;
}
.mh-pricing-card__name-en {
  font-size: 11px;
  color: #8c8c8c;
  margin-left: 4px;
}
.mh-pricing-card__articles {
  font-size: 12px;
  color: #595959;
  margin-bottom: 14px;
}
.mh-pricing-card__price { margin-bottom: 16px; }
.mh-pricing-card__first-tag {
  display: inline-block;
  font-size: 11px;
  color: #FF2442;
  background: #FFF0F2;
  border: 1px solid #FFCBD4;
  border-radius: 8px;
  padding: 1px 8px;
  margin-bottom: 6px;
}
.mh-pricing-card__original {
  font-size: 12px;
  color: #8c8c8c;
  text-decoration: line-through;
  margin-right: 6px;
}
.mh-pricing-card__current {
  font-size: 26px;
  font-weight: 700;
  color: #FF2442;
}
.mh-pricing-card__period { font-size: 12px; color: #595959; }
.mh-pricing-card__renew {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 4px;
}
.mh-pricing-card__btn {
  display: inline-block;
  padding: 9px 28px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
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
  background: #fafafa;
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
body[data-theme="dark"] .mh-pricing { background: #1f1f1f; }
body[data-theme="dark"] .mh-pricing-card {
  background: #1f1f1f;
  border-color: #2a2a2a;
}
body[data-theme="dark"] .mh-pricing-card__name { color: #e0e0e0; }
body[data-theme="dark"] .mh-pricing-card__articles,
body[data-theme="dark"] .mh-pricing-card__period { color: #a6a6a6; }
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
