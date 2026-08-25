<template>
  <MobileLearn v-if="isMobileView" />
  <div v-else class="learn-page">
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

    <header class="learn-hero">
      <div class="learn-hero-deco learn-hero-deco-lg"></div>
      <div class="learn-hero-deco learn-hero-deco-sm"></div>
      <div class="learn-hero-inner">
        <h1 class="learn-hero-title learn-hero-link" @click="goHome">创作学院</h1>
        <p class="learn-hero-subtitle learn-hero-link" @click="goHome">从 0 到 1 的自媒体实战指南</p>
      </div>
    </header>

    <div class="learn-body">
      <aside class="learn-sidebar">
        <LearnSidebar
          v-if="categoryTree.length"
          :nodes="categoryTree"
          :active-id="activeCategoryId"
          @select="onSelectCategory"
        />
        <div v-else class="learn-empty">内容正在筹备中…</div>
      </aside>

      <main class="learn-main">
        <!-- 空状态页：banner + 推荐课程 -->
        <template v-if="isEmptyState">
          <!-- Banner 轮播 -->
          <div v-if="banners.length" class="learn-banner-section">
            <a-carousel autoplay arrows :dots="true" dot-position="bottom" class="learn-banner-carousel">
              <template #prevArrow>
                <div class="learn-banner-arrow learn-banner-arrow-prev">
                  <LeftOutlined />
                </div>
              </template>
              <template #nextArrow>
                <div class="learn-banner-arrow learn-banner-arrow-next">
                  <RightOutlined />
                </div>
              </template>
              <div v-for="b in banners" :key="b.id" class="learn-banner-slide">
                <a v-if="b.linkUrl" :href="b.linkUrl" target="_blank" rel="noopener">
                  <img :src="b.imageUrl" :alt="'banner-' + b.id" class="learn-banner-img" />
                </a>
                <img v-else :src="b.imageUrl" :alt="'banner-' + b.id" class="learn-banner-img" />
              </div>
            </a-carousel>
          </div>

          <!-- 学院简介 -->
          <div class="learn-intro">
            <div class="learn-intro-icon">
              <BulbOutlined />
            </div>
            <p class="learn-intro-text">
              创作学院是爱创作为自媒体创作者打造的实战学习平台，涵盖内容定位、平台运营、爆款方法论等核心主题，帮助你从 0 到 1 建立系统化的内容创作能力。
            </p>
          </div>

          <!-- 推荐文章 -->
          <div v-if="recommendedArticles.length" class="learn-recommend-section">
            <div class="learn-recommend-header">
              <h2 class="learn-recommend-title">推荐文章</h2>
              <span class="learn-recommend-desc">精选必读内容，快速提升创作能力</span>
            </div>
            <div class="learn-recommend-grid">
              <a
                v-for="article in recommendedArticles"
                :key="article.id"
                class="learn-recommend-card learn-recommend-article"
                @click.prevent="handleArticleClick(article)"
                href="#"
              >
                <img
                  v-if="article.coverImageUrl"
                  :src="article.coverImageUrl"
                  class="learn-recommend-cover"
                  alt=""
                />
                <div class="learn-recommend-body">
                  <span v-if="article.categoryName" class="learn-recommend-category">{{ article.categoryName }}</span>
                  <h3 class="learn-recommend-article-title">{{ article.title }}</h3>
                  <p v-if="article.summary" class="learn-recommend-summary">{{ article.summary }}</p>
                </div>
                <span v-if="shouldShowPaidBadge(article)" class="learn-article-badge">{{ article.requiredPlanName }}</span>
                <span v-else class="learn-recommend-arrow">›</span>
              </a>
            </div>
          </div>

          <!-- 兜底空状态 -->
          <div v-if="!banners.length && !recommendedArticles.length" class="learn-content-empty">
            <ReadOutlined class="learn-empty-icon" />
            <div class="learn-empty-title">欢迎来到创作学院</div>
            <div class="learn-empty-subtitle">从左侧选择一个分类开始学习</div>
          </div>
        </template>

        <!-- 非空状态：文章详情 / 分类列表 -->
        <LearnContent
          v-else
          :article="currentArticle"
          :category="currentCategory"
          :current-category-name="currentCategoryName"
          :category-path="currentCategoryPath"
          :top-categories="topCategories"
          @load-article="loadArticle"
          @select-category="onSelectCategory"
        />
      </main>
    </div>

    <MobileTreeSheet
      v-model:open="mobileSheetOpen"
      :nodes="categoryTree"
      :active-id="activeCategoryId"
      @select="onSelectCategoryFromSheet"
    />

    <button
      v-if="isMobile"
      class="learn-tree-fab"
      @click="mobileSheetOpen = true"
    >分类</button>

    <footer class="learn-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备2025200943号-2</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { CATEGORY_ICONS } from '@/components/learn/learnCategoryIcons'
import { ReadOutlined, BulbOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import NavBar from '@/components/layout/NavBar.vue'
import LearnSidebar from '@/components/learn/LearnSidebar.vue'
import LearnContent from '@/components/learn/LearnContent.vue'
import MobileTreeSheet from '@/components/learn/MobileTreeSheet.vue'
import MobileLearn from '@/views/MobileLearn.vue'
import { useDevice } from '@/composables/useDevice.js'
import { useLearn } from '@/composables/useLearn.js'

const { isMobile: isMobileView } = useDevice()

const {
  categoryTree,
  currentArticle,
  currentCategory,
  banners,
  activeCategoryId,
  currentCategoryName,
  currentCategoryPath,
  topCategories,
  isEmptyState,
  recommendedArticles,
  onSelectCategory,
  loadArticle,
  goHome,
  handleArticleClick,
  shouldShowPaidBadge
} = useLearn()

const mobileSheetOpen = ref(false)

// 侧边栏收起后显示的「分类」快捷按钮（PC 响应式布局用）
const isMobile = ref(window.innerWidth < 992)

function getCategoryIcon(name) {
  return CATEGORY_ICONS[name] || null
}

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/lottery', label: '活动' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' }
]
const ctaTo = '/console/workbench'
const ctaLabel = '开始创作'

const onSelectCategoryFromSheet = id => {
  mobileSheetOpen.value = false
  onSelectCategory(id)
}

onMounted(() => {
  window.addEventListener('resize', () => {
    isMobile.value = window.innerWidth < 992
  }, { passive: true })
})
</script>

<style scoped>
.learn-page { min-height: 100vh; display: flex; flex-direction: column; background: #fafafa; }

/* Hero 区 */
.learn-hero {
  position: relative;
  background: linear-gradient(180deg, #FFF5F7 0%, #FFFFFF 100%);
  padding: 32px 24px;
  overflow: hidden;
}
.learn-hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}
.learn-hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}
.learn-hero-link { cursor: pointer; }
.learn-hero-link:hover { color: #FF2442; transition: color 0.2s ease; }
.learn-hero-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 8px 0 0;
}
.learn-hero-deco {
  position: absolute;
  border-radius: 50%;
  background: #FFE8EC;
}
.learn-hero-deco-lg {
  width: 200px; height: 200px;
  top: -60px; right: -40px;
}
.learn-hero-deco-sm {
  width: 80px; height: 80px;
  top: 20px; right: 160px;
}
@media (max-width: 991px) {
  .learn-hero { padding: 20px 16px; }
  .learn-hero-title { font-size: 24px; }
}

.learn-body { display: flex; flex: 1; max-width: 1200px; width: 100%; margin: 0 auto; padding: 24px 16px; gap: 24px; }
.learn-sidebar { width: 240px; flex-shrink: 0; position: sticky; top: 88px; align-self: flex-start; max-height: calc(100vh - 88px); overflow-y: auto; background: #fff; border-radius: 8px; padding: 12px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.learn-main { flex: 1; min-width: 0; background: #fff; border-radius: 8px; padding: 28px 32px; }
.learn-empty { padding: 32px 16px; text-align: center; color: #999; }
.learn-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}
.learn-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}
.learn-tree-fab {
  position: fixed; bottom: 24px; right: 24px;
  background: #FF2442; color: #fff; border: 0; border-radius: 24px;
  padding: 10px 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.15); cursor: pointer; z-index: 50;
  display: none;
}
@media (max-width: 991px) {
  .learn-body { flex-direction: column; padding: 16px; }
  .learn-sidebar { display: none; }
  .learn-tree-fab { display: inline-flex; }
  .learn-main { padding: 20px 16px; }
}

/* 暗色主题（footer 与 Home 对齐） */
body[data-theme="dark"] .learn-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}
body[data-theme="dark"] .learn-footer span + span::before {
  color: #303030;
}

/* 暗色主题 — 页面容器 */
body[data-theme="dark"] .learn-page { background: #141414; }
body[data-theme="dark"] .learn-main,
body[data-theme="dark"] .learn-sidebar { background: #1f1f1f; box-shadow: none; }
body[data-theme="dark"] .learn-empty { color: #595959; }

/* 暗色主题 — Hero */
body[data-theme="dark"] .learn-hero {
  background: linear-gradient(180deg, #2a1f22 0%, #1f1f1f 100%);
}
body[data-theme="dark"] .learn-hero-title { color: #e0e0e0; }
body[data-theme="dark"] .learn-hero-link:hover { color: #ff5e72; }
body[data-theme="dark"] .learn-hero-subtitle { color: #8c8c8c; }
body[data-theme="dark"] .learn-hero-deco { background: #3a2a2e; }

/* 暗色主题 — 学院简介 */
body[data-theme="dark"] .learn-intro {
  background: linear-gradient(135deg, #2a1f22 0%, #2f2226 100%);
  border-color: #3a2a2e;
}
body[data-theme="dark"] .learn-intro-text { color: #b0b0b0; }

/* 暗色主题 — 推荐课程 */
body[data-theme="dark"] .learn-recommend-title { color: #e0e0e0; }
body[data-theme="dark"] .learn-recommend-card {
  background: linear-gradient(135deg, #1f1f1f 0%, #2a2226 100%);
  border-color: #303030;
  color: #e0e0e0;
}
body[data-theme="dark"] .learn-recommend-card:hover {
  border-color: transparent;
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.15);
}
body[data-theme="dark"] .learn-recommend-icon-wrap {
  background: linear-gradient(135deg, #3a2a2e, #4a3036);
}
body[data-theme="dark"] .learn-recommend-name { color: #e0e0e0; }
body[data-theme="dark"] .learn-recommend-count { color: #8c8c8c; }
body[data-theme="dark"] .learn-recommend-arrow { color: #595959; }

/* 暗色主题 — 兜底空状态 */
body[data-theme="dark"] .learn-empty-icon { color: #434343; }
body[data-theme="dark"] .learn-empty-title { color: #b0b0b0; }
body[data-theme="dark"] .learn-empty-subtitle { color: #595959; }

/* 暗色主题 — 轮播箭头 */
body[data-theme="dark"] .learn-banner-arrow {
  background: rgba(40,40,40,0.85);
  color: #e0e0e0;
}
body[data-theme="dark"] .learn-banner-arrow:hover {
  background: #2f2f2f;
  color: #FF2442;
}
body[data-theme="dark"] .learn-banner-carousel :deep(.slick-dots li button) {
  background: rgba(255,255,255,0.3);
}

/* Banner 轮播 */
.learn-banner-section { margin-bottom: 24px; }
.learn-banner-carousel { border-radius: 12px; }
.learn-banner-carousel :deep(.slick-list) { border-radius: 12px; overflow: hidden; }
.learn-banner-carousel :deep(.slick-arrow) {
  z-index: 2;
  width: 40px; height: 40px;
  display: flex !important;
  align-items: center;
  justify-content: center;
  font-size: 0;
  overflow: hidden;
}
.learn-banner-carousel :deep(.slick-arrow > *) { font-size: 18px; }
.learn-banner-arrow {
  width: 40px; height: 40px;
  background: rgba(255,255,255,0.9);
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #333;
  transition: all 0.2s ease;
}
.learn-banner-arrow:hover {
  background: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  color: #FF2442;
}
.learn-banner-carousel :deep(.slick-dots li button) {
  background: rgba(255,255,255,0.5);
  width: 8px; height: 8px;
  border-radius: 50%;
}
.learn-banner-carousel :deep(.slick-dots li.slick-active button) {
  background: #FF2442;
  width: 20px;
  border-radius: 4px;
}
.learn-banner-slide { height: auto; }
.learn-banner-img { width: 100%; height: auto; display: block; }

/* 学院简介 */
.learn-intro {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #FFF5F7 0%, #FFF0F2 100%);
  border: 1px solid #FFE0E5;
  border-radius: 10px;
}
.learn-intro-icon {
  flex-shrink: 0;
  font-size: 20px;
  color: #FF2442;
  margin-top: 2px;
}
.learn-intro-text {
  font-size: 14px; line-height: 1.8; color: #434343; margin: 0;
}

/* 推荐课程 */
.learn-recommend-section { margin-bottom: 24px; }
.learn-recommend-header {
  display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px;
}
.learn-recommend-title {
  font-size: 16px; font-weight: 600; color: #1a1a1a; margin: 0;
}
.learn-recommend-desc {
  font-size: 13px; color: #8c8c8c;
}
.learn-recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}
.learn-recommend-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 24px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #fff8f9 100%);
  text-decoration: none;
  color: #1a1a1a;
  cursor: pointer;
  transition: all 0.25s ease;
  overflow: hidden;
}
.learn-recommend-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, #FF2442, #ff6b81);
  opacity: 0;
  transition: opacity 0.25s ease;
}
.learn-recommend-card:hover {
  border-color: transparent;
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.12);
  transform: translateY(-4px);
}
.learn-recommend-card:hover::before { opacity: 1; }
.learn-recommend-card:hover .learn-recommend-icon-wrap {
  transform: scale(1.1);
  background: linear-gradient(135deg, #FF2442, #ff6b81);
}
.learn-recommend-card:hover .learn-recommend-icon {
  color: #fff;
}
.learn-recommend-card:hover .learn-recommend-name {
  color: #FF2442;
}
.learn-recommend-icon-wrap {
  width: 44px; height: 44px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  margin-bottom: 12px;
  transition: all 0.25s ease;
}
.learn-recommend-icon { font-size: 22px; color: #FF2442; transition: color 0.25s ease; }
.learn-recommend-icon-fallback {
  font-size: 20px; font-weight: 700; color: #FF2442;
  line-height: 1;
  transition: color 0.25s ease;
}
.learn-recommend-card:hover .learn-recommend-icon-fallback { color: #fff; }
.learn-recommend-name { font-size: 15px; font-weight: 600; margin-bottom: 4px; transition: color 0.25s ease; }
.learn-recommend-count { font-size: 12px; color: #8c8c8c; }
.learn-recommend-arrow {
  position: absolute;
  top: 16px; right: 16px;
  font-size: 18px;
  color: #d9d9d9;
  transition: color 0.25s ease, transform 0.25s ease;
}
.learn-recommend-card:hover .learn-recommend-arrow {
  color: #FF2442;
  transform: translateX(3px);
}

/* 付费文章右上角徽章 */
.learn-article-badge {
  position: absolute;
  top: 12px; right: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #FF6B1A;
  background: #FFF3E0;
  border: 1px solid #FFD8A8;
  border-radius: 9999px;
  padding: 2px 10px;
  z-index: 1;
}
@media (max-width: 991px) {
  .learn-article-badge {
    top: 8px; right: 8px;
    font-size: 11px;
    padding: 1px 8px;
  }
}
body[data-theme="dark"] .learn-article-badge {
  background: rgba(255, 107, 26, 0.15);
  border-color: rgba(255, 107, 26, 0.35);
  color: #FF9F4D;
}

/* 兜底空状态 */
.learn-content-empty {
  text-align: center; padding: 48px 16px;
}
.learn-empty-icon { font-size: 48px; color: #d9d9d9; margin-bottom: 12px; }
.learn-empty-title { font-size: 16px; font-weight: 600; color: #262626; margin-bottom: 4px; }
.learn-empty-subtitle { font-size: 13px; color: #8c8c8c; }

.learn-recommend-article {
  align-items: flex-start;
  text-align: left;
  padding: 0;
  overflow: hidden;
}
.learn-recommend-cover {
  width: 100%;
  aspect-ratio: 2 / 1;
  object-fit: contain;
  background: #f5f5f5;
  display: block;
}
.learn-recommend-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.learn-recommend-category {
  align-self: flex-start;
  font-size: 12px;
  color: #FF2442;
  background: #FFF0F2;
  padding: 2px 8px;
  border-radius: 10px;
}
.learn-recommend-article-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.learn-recommend-summary {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 暗色主题 — 推荐文章 */
body[data-theme="dark"] .learn-recommend-cover {
  background: #2a2a2a;
}
body[data-theme="dark"] .learn-recommend-article-title { color: #e0e0e0; }
body[data-theme="dark"] .learn-recommend-summary { color: #a6a6a6; }
body[data-theme="dark"] .learn-recommend-category {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

@media (max-width: 991px) {
  .learn-banner-section { margin-bottom: 16px; }
}

/* PC：推荐文章使用横向卡片，封面按 2:1 比例完整展示 */
@media (min-width: 992px) {
  .learn-recommend-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .learn-recommend-article {
    flex-direction: column;
    align-items: stretch;
  }
  .learn-recommend-cover {
    border-radius: 0;
  }
  .learn-recommend-body {
    flex: 1;
    min-width: 0;
    padding: 16px;
    justify-content: flex-start;
  }
  .learn-recommend-article .learn-recommend-arrow {
    display: none;
  }
}
</style>
