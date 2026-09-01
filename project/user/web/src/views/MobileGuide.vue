<template>
  <div class="mobile-guide">
    <!-- 顶部导航 -->
    <header class="mg-header">
      <router-link to="/" class="mg-header__brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          :alt="guideBrand"
          class="mg-header__logo"
        />
        <span class="mg-header__name">{{ guideBrand }}</span>
      </router-link>
      <div class="mg-header__actions">
        <button class="mg-header__menu" aria-label="菜单" @click="menuOpen = true">
          <Icon name="menu" :size="20" />
        </button>
        <router-link :to="landingTopCta.to" class="mg-header__cta">{{ landingTopCta.label }}</router-link>
      </div>
    </header>

    <!-- 菜单抽屉 -->
    <div v-if="menuOpen" class="mg-menu-backdrop" @click="menuOpen = false"></div>
    <div :class="['mg-menu', { open: menuOpen }]">
      <div class="mg-menu__header">
        <span class="mg-menu__title">菜单</span>
        <button class="mg-menu__close" aria-label="关闭" @click="menuOpen = false">×</button>
      </div>
      <nav class="mg-menu__nav">
        <template v-for="link in landingNavLinks" :key="link.to || link.href">
          <a
            v-if="link.href"
            :href="link.href"
            target="_blank"
            rel="noopener"
            class="mg-menu__link"
            @click="menuOpen = false"
          >{{ link.label }}</a>
          <router-link
            v-else
            :to="link.to"
            class="mg-menu__link"
            :class="{ active: route.path === link.to }"
            @click="menuOpen = false"
          >{{ link.label }}</router-link>
        </template>
      </nav>
    </div>

    <!-- Hero -->
    <section class="mg-hero">
      <div class="mg-hero__badge">{{ guideHero.mobile.badge }}</div>
      <h1 class="mg-hero__title">{{ guideHero.mobile.title }}</h1>
      <p class="mg-hero__desc">{{ guideHero.mobile.desc }}</p>
    </section>

    <!-- Tab 栏 -->
    <div class="mg-tabs-wrap">
      <Tabs
        v-model="activeTab"
        :tabs="guideTabs"
        variant="pill"
        active-type="primary"
      />
    </div>

    <!-- 内容区 -->
    <main class="mg-body">
      <div
        v-for="(section, sIndex) in guideSections"
        v-show="activeTab === sIndex"
        :key="section.id"
        class="mg-section"
      >
        <div
          v-for="article in section.articles"
          :key="article.id"
          class="mg-accordion"
          :class="{ expanded: expandedIds.has(article.id) }"
        >
          <button
            class="mg-accordion__header"
            @click="toggleArticle(article.id)"
          >
            <span class="mg-accordion__title">{{ article.title }}</span>
            <span class="mg-accordion__icon"></span>
          </button>
          <div v-show="expandedIds.has(article.id)" class="mg-accordion__body">
            <div v-if="article.content" class="mg-accordion__content" v-html="article.content" />
            <component
              :is="componentMap[article.component]"
              v-else-if="article.component"
            />
          </div>
        </div>
      </div>
    </main>

    <!-- CTA -->
    <section class="mg-cta">
      <h2 class="mg-cta__title">{{ guideCta.title }}</h2>
      <p class="mg-cta__desc">{{ guideCta.desc }}</p>
      <router-link :to="guideCta.btn.to" class="mg-btn">{{ guideCta.btn.text }}</router-link>
    </section>

    <AppFooter variant="mobile" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { guideSections, guideHero, guideCta, guideBrand } from '@/data/guide-content.js'
import { landingNavLinks, landingTopCta } from '@/data/siteConfig.js'
import AppFooter from '@/components/layout/AppFooter.vue'
import TimeCalculator from '@/components/guide/TimeCalculator.vue'
import LeaderboardPreview from '@/components/guide/LeaderboardPreview.vue'
import Tabs from '@/components/common/Tabs.vue'
import Icon from '@/components/common/Icon.vue'

const route = useRoute()
const menuOpen = ref(false)
const activeTab = ref(0)
const expandedIds = ref(new Set())

const guideTabs = computed(() =>
  guideSections.map((section, index) => ({ label: section.title, value: index }))
)

const componentMap = {
  TimeCalculator,
  LeaderboardPreview
}

function toggleArticle(id) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedIds.value = next
}
</script>

<style scoped>
.mobile-guide {
  min-height: 100vh;
  background: #fff;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
}

/* 顶部导航 */
.mg-header {
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
.mg-header__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}
.mg-header__logo {
  height: 28px;
  width: auto;
  max-width: 40px;
  object-fit: contain;
  border-radius: 6px;
}
.mg-header__name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.mg-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mg-header__menu {
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
.mg-header__menu svg {
  width: 18px;
  height: 18px;
}
.mg-header__cta {
  padding: 8px 18px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

/* 菜单抽屉 */
.mg-menu-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 80;
}
.mg-menu {
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
.mg-menu.open {
  transform: translateX(0);
}
.mg-menu__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.mg-menu__title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.mg-menu__close {
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
.mg-menu__nav {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.mg-menu__link {
  padding: 12px;
  border-radius: 8px;
  font-size: 15px;
  color: #1a1a1a;
  text-decoration: none;
}
.mg-menu__link.active,
.mg-menu__link:active {
  background: #FFF5F7;
  color: #FF2442;
}

/* Hero */
.mg-hero {
  padding: 40px 20px 24px;
  background: linear-gradient(180deg, #FFE5EB 0%, #fff 100%);
  text-align: center;
}
.mg-hero__badge {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 16px;
  margin-bottom: 14px;
}
.mg-hero__title {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 8px;
  color: #1a1a1a;
}
.mg-hero__desc {
  font-size: 15px;
  color: #8c8c8c;
  margin: 0;
}

/* Tabs */
.mg-tabs-wrap {
  position: sticky;
  top: 57px;
  z-index: 40;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 12px 16px;
}

/* 内容区 */
.mg-body {
  padding: 20px 16px 40px;
  background: #f8f9fa;
  min-height: 300px;
}
.mg-accordion {
  background: #fff;
  border-radius: 14px;
  margin-bottom: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}
.mg-accordion__header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 16px;
  background: #fff;
  border: none;
  text-align: left;
  cursor: pointer;
}
.mg-accordion__title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.45;
}
.mg-accordion__icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  position: relative;
}
.mg-accordion__icon::before,
.mg-accordion__icon::after {
  content: '';
  position: absolute;
  background: #FF2442;
  border-radius: 2px;
  transition: transform 0.2s ease;
}
.mg-accordion__icon::before {
  width: 12px;
  height: 2px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.mg-accordion__icon::after {
  width: 2px;
  height: 12px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.mg-accordion.expanded .mg-accordion__icon::after {
  transform: translate(-50%, -50%) rotate(90deg);
}
.mg-accordion__body {
  padding: 0 16px 18px;
}
.mg-accordion__content {
  font-size: 14px;
  color: #595959;
  line-height: 1.75;
}
.mg-accordion__content :deep(p) {
  margin: 0 0 12px;
}
.mg-accordion__content :deep(p):last-child {
  margin-bottom: 0;
}
.mg-accordion__content :deep(ul) {
  padding-left: 18px;
  margin: 0 0 12px;
}
.mg-accordion__content :deep(li) {
  margin-bottom: 6px;
}
.mg-accordion__content :deep(strong) {
  color: #1a1a1a;
}

/* CTA */
.mg-cta {
  padding: 44px 20px;
  text-align: center;
  background: linear-gradient(135deg, #fff8f9 0%, #fff0f2 100%);
}
.mg-cta__title {
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 8px;
  color: #1a1a1a;
}
.mg-cta__desc {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 24px;
}
.mg-btn {
  display: inline-block;
  padding: 14px 44px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  text-decoration: none;
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.3);
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-guide {
  background: #141414;
  color: #e0e0e0;
}
body[data-theme="dark"] .mg-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mg-header__name { color: #e0e0e0; }
body[data-theme="dark"] .mg-header__menu {
  background: #1f1f1f;
  border-color: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .mg-menu {
  background: #1f1f1f;
}
body[data-theme="dark"] .mg-menu__header {
  border-color: #2a2a2a;
}
body[data-theme="dark"] .mg-menu__title { color: #e0e0e0; }
body[data-theme="dark"] .mg-menu__close {
  background: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .mg-menu__link { color: #e0e0e0; }
body[data-theme="dark"] .mg-menu__link.active,
body[data-theme="dark"] .mg-menu__link:active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}
body[data-theme="dark"] .mg-hero {
  background: linear-gradient(180deg, rgba(255, 36, 66, 0.18) 0%, #141414 100%);
}
body[data-theme="dark"] .mg-hero__title,
body[data-theme="dark"] .mg-cta__title {
  color: #e0e0e0;
}
body[data-theme="dark"] .mg-hero__desc,
body[data-theme="dark"] .mg-cta__desc,
body[data-theme="dark"] .mg-accordion__content {
  color: #a6a6a6;
}
body[data-theme="dark"] .mg-tabs-wrap {
  background: #141414;
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mg-body {
  background: #141414;
}
body[data-theme="dark"] .mg-accordion,
body[data-theme="dark"] .mg-accordion__header {
  background: #1f1f1f;
}
body[data-theme="dark"] .mg-accordion__title,
body[data-theme="dark"] .mg-accordion__content :deep(strong) {
  color: #e0e0e0;
}
body[data-theme="dark"] .mg-cta {
  background: linear-gradient(135deg, #1f1f1f 0%, #2a2226 100%);
}
</style>
