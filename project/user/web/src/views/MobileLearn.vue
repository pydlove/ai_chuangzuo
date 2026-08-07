<template>
  <div class="mobile-learn">
    <!-- 顶部导航 -->
    <header class="ml-header">
      <router-link to="/" class="ml-header__brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="ml-header__logo"
        />
        <span class="ml-header__name">爱创作</span>
      </router-link>
      <div class="ml-header__actions">
        <button v-if="showBack" class="ml-header__back" aria-label="返回" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
        </button>
        <button class="ml-header__menu" aria-label="菜单" @click="menuOpen = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
        <router-link to="/login" class="ml-header__cta">开始创作</router-link>
      </div>
    </header>

    <!-- 菜单抽屉 -->
    <div v-if="menuOpen" class="ml-menu-backdrop" @click="menuOpen = false"></div>
    <div :class="['ml-menu', { open: menuOpen }]">
      <div class="ml-menu__header">
        <span class="ml-menu__title">菜单</span>
        <button class="ml-menu__close" aria-label="关闭" @click="menuOpen = false">×</button>
      </div>
      <nav class="ml-menu__nav">
        <router-link
          v-for="link in navLinks"
          :key="link.to"
          :to="link.to"
          class="ml-menu__link"
          :class="{ active: route.path === link.to }"
          @click="menuOpen = false"
        >{{ link.label }}</router-link>
      </nav>
    </div>

    <!-- 空状态：学院首页 -->
    <template v-if="isEmptyState">
      <section class="ml-hero">
        <div class="ml-hero__badge">创作学院</div>
        <h1 class="ml-hero__title">从 0 到 1 的自媒体实战指南</h1>
        <p class="ml-hero__desc">涵盖内容定位、平台运营、爆款方法论等核心主题</p>
      </section>

      <div v-if="banners.length" class="ml-banner">
        <div class="ml-banner__carousel">
          <component
            v-for="(b, index) in banners"
            :key="b.id"
            :is="b.linkUrl ? 'a' : 'div'"
            v-bind="b.linkUrl ? { href: b.linkUrl, target: '_blank', rel: 'noopener' } : {}"
            class="ml-banner__slide"
            :class="{ active: index === activeBannerIndex }"
          >
            <img :src="b.imageUrl" :alt="'banner-' + b.id" />
          </component>
        </div>
        <div v-if="banners.length > 1" class="ml-banner__dots">
          <span
            v-for="(_, index) in banners"
            :key="index"
            :class="['ml-banner__dot', { active: index === activeBannerIndex }]"
            @click="activeBannerIndex = index"
          />
        </div>
      </div>

      <section class="ml-section">
        <div class="ml-intro">
          <div class="ml-intro__icon">💡</div>
          <p class="ml-intro__text">创作学院是爱创作为自媒体创作者打造的实战学习平台，帮助你从 0 到 1 建立系统化的内容创作能力。</p>
        </div>
      </section>

      <section v-if="recommendedArticles.length" class="ml-section">
        <div class="ml-section__header">
          <h2 class="ml-section__title">推荐文章</h2>
          <span class="ml-section__subtitle">精选必读内容</span>
        </div>
        <div class="ml-article-list">
          <div
            v-for="article in recommendedArticles"
            :key="article.id"
            class="ml-article-card"
            @click="handleArticleClick(article)"
          >
            <img v-if="article.coverImageUrl" :src="article.coverImageUrl" class="ml-article-card__cover" alt="" />
            <div class="ml-article-card__body">
              <div class="ml-article-card__title">{{ article.title }}</div>
              <p v-if="plainExcerpt(article)" class="ml-article-card__summary">{{ plainExcerpt(article) }}</p>
              <div class="ml-article-card__meta">{{ article.categoryName || '' }}</div>
            </div>
            <span v-if="shouldShowPaidBadge(article)" class="ml-article-card__badge">{{ article.requiredPlanName }}</span>
          </div>
        </div>
      </section>

      <section class="ml-section">
        <div class="ml-section__header">
          <h2 class="ml-section__title">全部课程</h2>
        </div>
        <div class="ml-tree">
          <MobileLearnNode
            v-for="node in categoryTree"
            :key="node.id"
            :node="node"
            :expanded-ids="expandedIds"
            @toggle="toggleNode"
            @select="onSelectCategory"
          />
        </div>
      </section>

      <section class="ml-cta">
        <h2 class="ml-cta__title">想把自己的账号也做起来？</h2>
        <p class="ml-cta__desc">用 AI 一分钟生成你的第一篇</p>
        <router-link to="/login" class="ml-btn">立即开始创作 →</router-link>
      </section>
    </template>

    <!-- 分类详情 -->
    <template v-else-if="currentCategory">
      <main class="ml-main">
        <nav class="ml-breadcrumb">
          <span class="ml-breadcrumb__item" @click="goHome">创作学院</span>
          <span
            v-for="(seg, i) in currentCategoryPath"
            :key="seg.id"
            class="ml-breadcrumb__item"
            :class="{ active: i === currentCategoryPath.length - 1 }"
            @click="i < currentCategoryPath.length - 1 && onSelectCategory(seg.id)"
          >
            {{ seg.name }}
          </span>
        </nav>

        <h1 class="ml-page-title">{{ currentCategory.name }}</h1>
        <p class="ml-page-count">本分类下共 {{ currentCategory.total || currentCategory.articles?.length || 0 }} 篇文章</p>

        <div v-if="currentCategory.articles?.length" class="ml-article-list">
          <div
            v-for="a in currentCategory.articles"
            :key="a.id"
            class="ml-article-card"
            @click="handleArticleClick(a)"
          >
            <img v-if="a.coverImageUrl" :src="a.coverImageUrl" class="ml-article-card__cover" alt="" />
            <div class="ml-article-card__body">
              <div class="ml-article-card__title">{{ a.title }}</div>
              <p v-if="plainExcerpt(a)" class="ml-article-card__summary">{{ plainExcerpt(a) }}</p>
              <div class="ml-article-card__meta">{{ formatDate(a.publishedAt || a.updatedAt) }}</div>
            </div>
            <span v-if="shouldShowPaidBadge(a)" class="ml-article-card__badge">{{ a.requiredPlanName }}</span>
          </div>
        </div>

        <div v-else class="ml-empty">
          <div class="ml-empty__title">该分类下暂无已发布文章</div>
        </div>
      </main>
    </template>

    <!-- 文章详情 -->
    <template v-else-if="currentArticle">
      <main class="ml-main">
        <nav class="ml-breadcrumb">
          <span class="ml-breadcrumb__item" @click="goHome">创作学院</span>
          <span
            v-for="(seg, i) in currentCategoryPath"
            :key="seg.id"
            class="ml-breadcrumb__item"
            :class="{ active: i === currentCategoryPath.length - 1 }"
            @click="i < currentCategoryPath.length - 1 && onSelectCategory(seg.id)"
          >
            {{ seg.name }}
          </span>
        </nav>

        <article class="ml-article">
          <h1 class="ml-article__title">{{ currentArticle.title }}</h1>
          <p v-if="currentArticle.summary" class="ml-article__summary">{{ currentArticle.summary }}</p>
          <div class="ml-article__meta">
            <span>{{ formatDate(currentArticle.publishedAt || currentArticle.updatedAt) }}</span>
            <span v-if="currentCategoryName">{{ currentCategoryName }}</span>
          </div>

          <img
            v-if="currentArticle.coverImageUrl"
            :src="currentArticle.coverImageUrl"
            class="ml-article__cover"
            alt=""
          />

          <!-- 正文 / 锁屏卡：isFree=1（免费）或 canRead=true 时渲染正文，其余（付费且不可读）渲染锁屏卡 -->
          <div v-if="currentArticle.isFree === 1 || currentArticle.canRead === true" class="ml-article__content">
            <LearnMarkdown v-if="currentArticle.contentType === 'markdown'" :source="currentArticle.content" />
            <LearnRichText v-else :html="currentArticle.content" />
          </div>
          <div v-else class="ml-locked">
            <div class="ml-locked__icon">
              <LockOutlined />
            </div>
            <div class="ml-locked__title">需要 {{ currentArticle.requiredPlanName || '更高' }} 套餐</div>
            <div class="ml-locked__sub">升级套餐即可阅读完整内容</div>
            <button class="ml-locked__btn" @click="$router.push('/pricing')">立即升级</button>
          </div>
        </article>

        <div v-if="currentArticle.prevArticle || currentArticle.nextArticle" class="ml-article-nav">
          <div
            v-if="currentArticle.prevArticle"
            class="ml-article-nav__card"
            @click="loadArticle(currentArticle.prevArticle.id)"
          >
            <div class="ml-article-nav__dir">← 上一篇</div>
            <div class="ml-article-nav__title">{{ currentArticle.prevArticle.title }}</div>
          </div>
          <div
            v-if="currentArticle.nextArticle"
            class="ml-article-nav__card ml-article-nav__card--next"
            @click="loadArticle(currentArticle.nextArticle.id)"
          >
            <div class="ml-article-nav__dir">下一篇 →</div>
            <div class="ml-article-nav__title">{{ currentArticle.nextArticle.title }}</div>
          </div>
        </div>

        <div class="ml-cta ml-cta--inline">
          <h2 class="ml-cta__title">想把自己的账号也做起来？</h2>
          <p class="ml-cta__desc">用 AI 一分钟生成你的第一篇</p>
          <router-link to="/login" class="ml-btn">立即开始创作 →</router-link>
        </div>
      </main>
    </template>

    <footer class="ml-footer">
      <div>© 2026 爱创作 · 杭州爱启云网络科技有限公司</div>
      <div>浙ICP备XXXXXXXX号-1</div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LockOutlined } from '@ant-design/icons-vue'
import { useLearn } from '@/composables/useLearn.js'
import LearnMarkdown from '@/components/learn/LearnMarkdown.vue'
import LearnRichText from '@/components/learn/LearnRichText.vue'
import MobileLearnNode from './MobileLearnNode.vue'

const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const expandedIds = ref(new Set())
const activeBannerIndex = ref(0)
let bannerTimer = null

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' }
]

const {
  categoryTree,
  currentArticle,
  currentCategory,
  banners,
  currentCategoryName,
  currentCategoryPath,
  isEmptyState,
  recommendedArticles,
  onSelectCategory,
  loadArticle,
  goHome,
  handleArticleClick,
  shouldShowPaidBadge
} = useLearn()

const showBack = computed(() => !isEmptyState.value)

function goBack() {
  if (route.params.id) {
    const catId = currentArticle.value?.categoryId
    if (catId) {
      router.replace({ path: '/learn', query: { cat: catId } })
      return
    }
  }
  goHome()
}

function toggleNode(id) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedIds.value = next
}

function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function plainExcerpt(article) {
  const raw = (article.content && article.content.trim()) ? article.content : (article.summary || article.excerpt || '')
  if (!raw) return ''
  const text = raw
    .replace(/```[\s\S]*?```/g, ' ')          // 代码块
    .replace(/<[^>]+>/g, ' ')                 // HTML 标签
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')    // 图片
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')  // 链接保留文字
    .replace(/^[#>\-\s]+/gm, ' ')             // 标题/引用/列表行首标记
    .replace(/[*_`~|]/g, ' ')                 // 行内标记与表格分隔
    .replace(/\s+/g, ' ')
    .trim()
  const deduped = text.startsWith(article.title) ? text.slice(article.title.length).trim() : text
  return deduped.length > 80 ? deduped.slice(0, 80) + '…' : deduped
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
  if (newBanners.length > 1) startBannerCarousel()
  else stopBannerCarousel()
}, { flush: 'post' })

onMounted(() => {
  if (banners.value.length > 1) startBannerCarousel()
})

onUnmounted(() => {
  stopBannerCarousel()
})
</script>

<style scoped>
.mobile-learn {
  min-height: 100vh;
  background: #f8f9fa;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
}

/* 顶部导航 */
.ml-header {
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
.ml-header__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}
.ml-header__logo {
  height: 28px;
  width: auto;
  max-width: 40px;
  object-fit: contain;
  border-radius: 6px;
}
.ml-header__name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.ml-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ml-header__menu,
.ml-header__back {
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
.ml-header__menu svg,
.ml-header__back svg {
  width: 18px;
  height: 18px;
}
.ml-header__cta {
  padding: 8px 18px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

/* 菜单抽屉 */
.ml-menu-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 80;
}
.ml-menu {
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
.ml-menu.open {
  transform: translateX(0);
}
.ml-menu__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.ml-menu__title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.ml-menu__close {
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
.ml-menu__nav {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ml-menu__link {
  padding: 12px;
  border-radius: 8px;
  font-size: 15px;
  color: #1a1a1a;
  text-decoration: none;
}
.ml-menu__link.active,
.ml-menu__link:active {
  background: #FFF5F7;
  color: #FF2442;
}

/* Hero */
.ml-hero {
  padding: 40px 20px 28px;
  background: linear-gradient(180deg, #FFF5F7 0%, #fff 100%);
  text-align: center;
}
.ml-hero__badge {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 16px;
  margin-bottom: 14px;
}
.ml-hero__title {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 8px;
  color: #1a1a1a;
}
.ml-hero__desc {
  font-size: 15px;
  color: #8c8c8c;
  margin: 0;
}

/* Banner */
.ml-banner {
  padding: 20px 16px 0;
  background: #fff;
}
.ml-banner__carousel {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: #f0f0f0;
}
.ml-banner__slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.5s ease;
  pointer-events: none;
}
.ml-banner__slide.active {
  opacity: 1;
  pointer-events: auto;
}
.ml-banner__slide img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.ml-banner__dots {
  display: flex;
  justify-content: center;
  gap: 6px;
  padding: 10px 0 4px;
}
.ml-banner__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d9d9d9;
}
.ml-banner__dot.active {
  background: #FF2442;
}

/* Section */
.ml-section {
  padding: 24px 16px;
  background: #fff;
  margin-bottom: 12px;
}
.ml-section__header {
  margin-bottom: 18px;
}
.ml-section__title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 4px;
}
.ml-section__subtitle {
  font-size: 13px;
  color: #8c8c8c;
}

/* Intro */
.ml-intro {
  display: flex;
  gap: 12px;
  padding: 18px;
  background: linear-gradient(135deg, #fff8f9 0%, #fff0f2 100%);
  border-radius: 14px;
}
.ml-intro__icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.ml-intro__text {
  margin: 0;
  font-size: 14px;
  color: #595959;
  line-height: 1.7;
}

/* 推荐课程 */
.ml-category-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.ml-category-card {
  padding: 18px 14px;
  background: #f8f9fa;
  border-radius: 14px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
}
.ml-category-card:active {
  transform: scale(0.98);
}
.ml-category-card__icon {
  width: 44px;
  height: 44px;
  margin: 0 auto 10px;
  background: linear-gradient(135deg, #FF4D6F, #FF2442);
  color: #fff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}
.ml-category-card__name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 2px;
}
.ml-category-card__count {
  font-size: 12px;
  color: #8c8c8c;
}

/* 分类树 */
.ml-tree {
  background: #f8f9fa;
  border-radius: 14px;
  padding: 8px 0;
}

/* CTA */
.ml-cta {
  padding: 44px 20px;
  text-align: center;
  background: linear-gradient(135deg, #fff8f9 0%, #fff0f2 100%);
  margin-bottom: 12px;
}
.ml-cta--inline {
  margin: 24px 16px;
  border-radius: 16px;
}
.ml-cta__title {
  font-size: 20px;
  font-weight: 800;
  margin-bottom: 6px;
  color: #1a1a1a;
}
.ml-cta__desc {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 20px;
}
.ml-btn {
  display: inline-block;
  padding: 13px 36px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.3);
}

/* 主内容区（分类详情 / 文章详情） */
.ml-main {
  padding: 20px 16px 40px;
  min-height: 60vh;
}
.ml-breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #8c8c8c;
}
.ml-breadcrumb__item {
  position: relative;
  padding-right: 12px;
}
.ml-breadcrumb__item:not(:last-child)::after {
  content: '›';
  position: absolute;
  right: 0;
  color: #d9d9d9;
}
.ml-breadcrumb__item:not(.active) {
  color: #FF2442;
  cursor: pointer;
}
.ml-breadcrumb__item.active {
  color: #1a1a1a;
  font-weight: 500;
}
.ml-page-title {
  font-size: 24px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0 0 6px;
}
.ml-page-count {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 20px;
}

/* 文章卡片列表 */
.ml-article-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ml-article-card {
  position: relative;
  display: flex;
  gap: 14px;
  padding: 16px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  cursor: pointer;
}
.ml-article-card__badge {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 11px;
  font-weight: 600;
  color: #FF6B1A;
  background: #FFF3E0;
  border: 1px solid #FFD8A8;
  border-radius: 9999px;
  padding: 1px 8px;
  z-index: 1;
}
body[data-theme="dark"] .ml-article-card__badge {
  background: rgba(255, 107, 26, 0.15);
  border-color: rgba(255, 107, 26, 0.35);
  color: #FF9F4D;
}
.ml-article-card__cover {
  width: 120px;
  height: 73px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
  background: #f5f5f5;
}
.ml-article-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.ml-article-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 6px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ml-article-card__summary {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.5;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ml-article-card__meta {
  margin-top: auto;
  font-size: 12px;
  color: #bfbfbf;
}

/* 空状态 */
.ml-empty {
  padding: 48px 20px;
  text-align: center;
  background: #fff;
  border-radius: 14px;
}
.ml-empty__title {
  font-size: 15px;
  color: #8c8c8c;
}

/* 文章详情 */
.ml-article {
  background: #fff;
  border-radius: 16px;
  padding: 24px 18px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.ml-article__title {
  font-size: 22px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0 0 10px;
  line-height: 1.4;
}
.ml-article__summary {
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
  margin-bottom: 14px;
}
.ml-article__meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 16px;
}
.ml-article__cover {
  width: 100%;
  border-radius: 12px;
  margin-bottom: 20px;
}
.ml-article__content {
  font-size: 15px;
  line-height: 1.75;
  color: #262626;
}

/* 付费锁定卡（移动端） */
.ml-locked {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 36px 20px;
  background: linear-gradient(135deg, #FFF8F0 0%, #FFF1E0 100%);
  border: 1px solid #FFE0B2;
  border-radius: 14px;
  text-align: center;
}
.ml-locked__icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #FFF3E0;
  color: #FF6B1A;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ml-locked__title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.ml-locked__sub {
  font-size: 13px;
  color: #8c8c8c;
}
.ml-locked__btn {
  margin-top: 4px;
  padding: 9px 24px;
  background: #FF6B1A;
  color: #fff;
  border: 0;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.ml-locked__btn:active { background: #E55A0B; }
body[data-theme="dark"] .ml-locked {
  background: rgba(255, 107, 26, 0.08);
  border-color: rgba(255, 107, 26, 0.25);
}
body[data-theme="dark"] .ml-locked__icon { background: rgba(255, 107, 26, 0.15); }
body[data-theme="dark"] .ml-locked__title { color: rgba(255, 255, 255, 0.92); }
body[data-theme="dark"] .ml-locked__sub { color: rgba(255, 255, 255, 0.55); }

/* 文章导航 */
.ml-article-nav {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 20px;
}
.ml-article-nav__card {
  padding: 16px;
  background: #fff;
  border-radius: 14px;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}
.ml-article-nav__card--next {
  text-align: right;
}
.ml-article-nav__dir {
  font-size: 12px;
  color: #FF2442;
  margin-bottom: 6px;
}
.ml-article-nav__title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Footer */
.ml-footer {
  padding: 24px 20px 32px;
  text-align: center;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.8;
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-learn {
  background: #141414;
  color: #e0e0e0;
}
body[data-theme="dark"] .ml-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .ml-header__name { color: #e0e0e0; }
body[data-theme="dark"] .ml-header__menu,
body[data-theme="dark"] .ml-header__back {
  background: #1f1f1f;
  border-color: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .ml-menu {
  background: #1f1f1f;
}
body[data-theme="dark"] .ml-menu__header {
  border-color: #2a2a2a;
}
body[data-theme="dark"] .ml-menu__title { color: #e0e0e0; }
body[data-theme="dark"] .ml-menu__close {
  background: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .ml-menu__link { color: #e0e0e0; }
body[data-theme="dark"] .ml-menu__link.active,
body[data-theme="dark"] .ml-menu__link:active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}
body[data-theme="dark"] .ml-hero {
  background: linear-gradient(180deg, #2a1f22 0%, #141414 100%);
}
body[data-theme="dark"] .ml-hero__title,
body[data-theme="dark"] .ml-page-title,
body[data-theme="dark"] .ml-cta__title,
body[data-theme="dark"] .ml-article__title,
body[data-theme="dark"] .ml-article-nav__title,
body[data-theme="dark"] .ml-breadcrumb__item.active {
  color: #e0e0e0;
}
body[data-theme="dark"] .ml-hero__desc,
body[data-theme="dark"] .ml-section__subtitle,
body[data-theme="dark"] .ml-intro__text,
body[data-theme="dark"] .ml-page-count,
body[data-theme="dark"] .ml-article-card__summary,
body[data-theme="dark"] .ml-article-card__meta,
body[data-theme="dark"] .ml-article__summary,
body[data-theme="dark"] .ml-article__meta,
body[data-theme="dark"] .ml-empty__title {
  color: #a6a6a6;
}
body[data-theme="dark"] .ml-banner,
body[data-theme="dark"] .ml-section,
body[data-theme="dark"] .ml-article-card,
body[data-theme="dark"] .ml-article,
body[data-theme="dark"] .ml-article-nav__card,
body[data-theme="dark"] .ml-footer {
  background: #1f1f1f;
}
body[data-theme="dark"] .ml-intro {
  background: linear-gradient(135deg, #2a1f22 0%, #2f2226 100%);
}
body[data-theme="dark"] .ml-category-card,
body[data-theme="dark"] .ml-tree {
  background: #2a2a2a;
}
body[data-theme="dark"] .ml-category-card__name {
  color: #e0e0e0;
}
body[data-theme="dark"] .ml-cta {
  background: linear-gradient(135deg, #1f1f1f 0%, #2a2226 100%);
}
body[data-theme="dark"] .ml-footer {
  border-top-color: #2a2a2a;
}
body[data-theme="dark"] .ml-article__content {
  color: #d9d9d9;
}
</style>
