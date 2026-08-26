<template>
  <div class="console-learn-page">
    <!-- 页面标题 -->
    <div class="console-learn-header">
      <div class="console-learn-header-main">
        <div class="console-learn-icon">
          <BookOutlined />
        </div>
        <div>
          <h2 class="console-learn-title">创作学院</h2>
          <p class="console-learn-desc">按分类浏览实战课程，学习账号定位、平台运营、爆款方法等内容，系统提升自媒体创作能力。</p>
        </div>
      </div>
    </div>

    <!-- 手机端宣传头图 -->
    <div v-if="!currentArticle" class="console-learn-hero-mobile">
      <div class="console-learn-hero-mobile__text">
        <img
          class="console-learn-hero-mobile__logo"
          src="/assets/images/创作学院logo-v1.png?v=2"
          alt="创作学院"
        />
        <p class="console-learn-hero-mobile__desc">get 一个爆款技能，让创作不再靠运气</p>
      </div>
      <div class="console-learn-hero-mobile__icon-wrap">
        <img
          class="console-learn-hero-mobile__icon"
          src="/assets/images/创作学院-v2.png?v=1"
          alt="创作学院"
        />
      </div>
    </div>

    <!-- 分类 + 内容区 -->
    <div class="console-learn-body">
      <!-- 分类标签：文章详情页不显示 -->
      <div v-if="!currentArticle" class="category-tabs-wrapper">
      <div
        v-if="categoryTree.length"
        ref="tabBarRef"
        class="category-tabs-bar"
        @scroll="checkTabOverflow"
      >
        <button
          :class="['category-tab', { active: isEmptyState && homeTab === 'all' }]"
          @click="goHome(); onSelectHomeTab('all')"
        >
          全部
        </button>
        <button
          :class="['category-tab', { active: isEmptyState && homeTab === 'recommended' }]"
          @click="goHome(); onSelectHomeTab('recommended')"
        >
          推荐
        </button>
        <button
          v-for="cat in categoryTree"
          :key="cat.id"
          :class="['category-tab', { active: activeCategoryId === cat.id }]"
          @click="onSelectCategory(cat.id)"
        >
          {{ cat.name }}
        </button>
      </div>
      <div
        v-if="showTabFade"
        class="category-tabs-fade"
        aria-hidden="true"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="console-learn-content">
      <!-- 全部 / 推荐 -->
      <template v-if="isEmptyState">
        <!-- 全部文章列表 -->
        <template v-if="homeTab === 'all'">
          <div v-if="allArticles.length" class="article-list">
            <article
              v-for="(article, idx) in allArticles"
              :key="article.id"
              class="article-card"
              @click="handleArticleClick(article)"
            >
              <span class="article-rank">{{ (allArticlesPage - 1) * allArticlesSize + idx + 1 }}</span>
              <img
                v-if="article.coverImageUrl"
                :src="article.coverImageUrl"
                class="article-cover"
                alt=""
              />
              <div class="article-body">
                <div class="article-meta-top">
                  <span v-if="article.categoryName" class="article-category">{{ article.categoryName }}</span>
                  <span class="article-reading">
                    <ClockCircleOutlined />
                    {{ readingMinutes(article.content) }} 分钟阅读
                  </span>
                </div>
                <h3 class="article-title">{{ article.title }}</h3>
                <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
              </div>
              <span v-if="shouldShowPaidBadge(article)" class="article-badge">{{ article.requiredPlanName }}</span>
              <span v-else class="article-arrow">›</span>
            </article>
          </div>

          <div v-if="allArticles.length && allArticlesTotal > allArticlesSize" class="all-articles-pagination">
            <a-pagination
              :current="allArticlesPage"
              :page-size="allArticlesSize"
              :total="allArticlesTotal"
              show-less-items
              @change="handleAllPageChange"
            />
          </div>

          <div v-if="!allArticles.length && !loading" class="console-learn-empty">
            <ReadOutlined class="console-learn-empty-icon" />
            <div class="console-learn-empty-title">欢迎来到创作学院</div>
            <div class="console-learn-empty-subtitle">全部内容正在准备中，请先选择一个分类</div>
          </div>
        </template>

        <!-- 推荐文章列表 -->
        <template v-if="homeTab === 'recommended'">
          <div v-if="recommendedArticles.length" class="article-list">
            <article
              v-for="(article, idx) in recommendedArticles"
              :key="article.id"
              class="article-card"
              @click="handleArticleClick(article)"
            >
              <span class="article-rank">{{ idx + 1 }}</span>
              <img
                v-if="article.coverImageUrl"
                :src="article.coverImageUrl"
                class="article-cover"
                alt=""
              />
              <div class="article-body">
                <div class="article-meta-top">
                  <span v-if="article.categoryName" class="article-category">{{ article.categoryName }}</span>
                  <span class="article-reading">
                    <ClockCircleOutlined />
                    {{ readingMinutes(article.content) }} 分钟阅读
                  </span>
                </div>
                <h3 class="article-title">{{ article.title }}</h3>
                <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
              </div>
              <span v-if="shouldShowPaidBadge(article)" class="article-badge">{{ article.requiredPlanName }}</span>
              <span v-else class="article-arrow">›</span>
            </article>
          </div>

          <div v-if="!recommendedArticles.length && !loading" class="console-learn-empty">
            <ReadOutlined class="console-learn-empty-icon" />
            <div class="console-learn-empty-title">欢迎来到创作学院</div>
            <div class="console-learn-empty-subtitle">推荐内容正在准备中，请先选择一个分类</div>
          </div>
        </template>
      </template>

      <!-- 分类详情：文章列表 -->
      <template v-else-if="currentCategory">
        <header class="category-detail-head">
          <nav class="category-breadcrumb">
            <a href="#" @click.prevent="goHome">创作学院</a>
            <span class="category-breadcrumb-sep">›</span>
            <span class="category-breadcrumb-active">{{ currentCategory.name }}</span>
          </nav>
          <h2 class="category-detail-title">{{ currentCategory.name }}</h2>
          <p class="category-detail-count">
            <template v-if="hasSubcategories">该分类下包含 {{ currentCategory.children.length }} 个子分类</template>
            <template v-else>共 {{ currentCategory.total || currentCategory.articles?.length || 0 }} 篇文章</template>
          </p>
        </header>

        <!-- 二级分类 tab -->
        <div v-if="hasSubcategories" class="subcategory-tabs-bar">
          <button
            v-for="sub in currentCategory.children"
            :key="sub.id"
            :class="['subcategory-tab', { active: activeSubCategoryId === sub.id }]"
            @click="onSelectSubCategory(sub.id)"
          >
            {{ sub.name }}
          </button>
        </div>

        <div v-if="displayLoading" class="console-learn-loading">
          <a-spin />
        </div>
        <div v-else-if="displayArticles.length" class="article-list">
          <article
            v-for="(article, idx) in displayArticles"
            :key="article.id"
            class="article-card"
            @click="handleArticleClick(article)"
          >
            <span class="article-rank">{{ idx + 1 }}</span>
            <img
              v-if="article.coverImageUrl"
              :src="article.coverImageUrl"
              class="article-cover"
              alt=""
            />
            <div class="article-body">
              <div class="article-meta-top">
                <span class="article-reading">
                  <ClockCircleOutlined />
                  {{ readingMinutes(article.content) }} 分钟阅读
                </span>
                <span class="article-date">
                  <CalendarOutlined />
                  {{ formatDate(article.publishedAt || article.updatedAt) }}
                </span>
              </div>
              <h3 class="article-title">{{ article.title }}</h3>
              <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
            </div>
            <span v-if="shouldShowPaidBadge(article)" class="article-badge">{{ article.requiredPlanName }}</span>
            <span v-else class="article-arrow">›</span>
          </article>
        </div>

        <div v-else class="console-learn-empty">
          <ReadOutlined class="console-learn-empty-icon" />
          <div class="console-learn-empty-title">该分类下暂无文章</div>
        </div>
      </template>

      <!-- 文章详情 -->
      <LearnContent
        v-else-if="currentArticle"
        :article="currentArticle"
        :category="currentCategory"
        :current-category-name="currentCategoryName"
        :category-path="currentCategoryPath"
        :top-categories="topCategories"
        base-path="/console/learn"
        @load-article="loadArticle"
        @select-category="onSelectCategory"
      />

      <div v-else-if="loading" class="console-learn-loading">
        <a-spin />
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import {
  BookOutlined,
  ReadOutlined,
  ClockCircleOutlined,
  CalendarOutlined
} from '@ant-design/icons-vue'
import LearnContent from '@/components/learn/LearnContent.vue'
import { useLearn } from '@/composables/useLearn.js'
import { fetchCategoryDetail } from '@/api/learn'

const {
  categoryTree,
  currentArticle,
  currentCategory,
  activeCategoryId,
  currentCategoryName,
  currentCategoryPath,
  topCategories,
  isEmptyState,
  recommendedArticles,
  allArticles,
  allArticlesPage,
  allArticlesSize,
  allArticlesTotal,
  loading,
  loadAllArticles,
  onSelectCategory,
  loadArticle,
  goHome,
  handleArticleClick,
  shouldShowPaidBadge
} = useLearn('/console/learn', { loadAll: true })

const hasSubcategories = computed(() =>
  Array.isArray(currentCategory.value?.children) && currentCategory.value.children.length > 0
)

// 二级分类 tab
const activeSubCategoryId = ref(null)
const subCategoryArticles = ref([])
const subCategoryLoading = ref(false)

const displayArticles = computed(() =>
  hasSubcategories.value ? subCategoryArticles.value : currentCategory.value?.articles || []
)
const displayLoading = computed(() => loading.value || subCategoryLoading.value)

async function loadSubCategoryArticles(id) {
  if (!id) return
  subCategoryLoading.value = true
  try {
    const res = await fetchCategoryDetail(id, 1, 50)
    const data = res.data || {}
    subCategoryArticles.value = data.articles || []
  } catch (e) {
    subCategoryArticles.value = []
  } finally {
    subCategoryLoading.value = false
  }
}

function onSelectSubCategory(id) {
  activeSubCategoryId.value = id
  loadSubCategoryArticles(id)
}

watch(
  () => currentCategory.value,
  (cat) => {
    if (cat?.children?.length) {
      activeSubCategoryId.value = cat.children[0].id
      loadSubCategoryArticles(cat.children[0].id)
    } else {
      activeSubCategoryId.value = null
      subCategoryArticles.value = []
    }
  },
  { immediate: true }
)

// 首页 Tab：全部 / 推荐
const homeTab = ref('all')
function onSelectHomeTab(tab) {
  homeTab.value = tab
}

const tabBarRef = ref(null)
const showTabFade = ref(false)

function checkTabOverflow() {
  const el = tabBarRef.value
  if (!el) return
  showTabFade.value = el.scrollWidth > el.clientWidth + 2
}

watch(categoryTree, () => nextTick(checkTabOverflow), { flush: 'post' })

onMounted(() => {
  checkTabOverflow()
  window.addEventListener('resize', checkTabOverflow, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('resize', checkTabOverflow)
})

function readingMinutes(content) {
  if (!content) return 1
  return Math.max(1, Math.ceil(content.length / 300))
}

async function handleAllPageChange(page) {
  await loadAllArticles(page, allArticlesSize.value)
  // 翻页后滚动到列表顶部
  const contentEl = document.querySelector('.console-learn-content')
  if (contentEl) {
    contentEl.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.console-learn-page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0;
  box-sizing: border-box;
}

.console-learn-body {
  padding: 24px 32px;
  box-sizing: border-box;
}

/* Header */
.console-learn-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 32px 20px;
}

.console-learn-header-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.console-learn-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fff0f2 0%, #ffe4e8 100%);
  color: #ff2442;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.12);
}

.console-learn-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary, #1f1f1f);
}

.console-learn-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-secondary, #595959);
}

/* 手机端宣传头图（默认隐藏） */
.console-learn-hero-mobile {
  display: none;
}

/* Category tabs */
.category-tabs-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}

.category-tabs-bar::-webkit-scrollbar {
  display: none;
}

.category-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
}

.category-tab:hover {
  border-color: #d9d9d9;
  color: #262626;
}

.category-tab.active {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}

/* 二级分类 tabs */
.subcategory-tabs-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}

.subcategory-tabs-bar::-webkit-scrollbar {
  display: none;
}

.subcategory-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
}

.subcategory-tab:hover {
  border-color: #d9d9d9;
  color: #262626;
}

.subcategory-tab.active {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}

/* Category tabs */
.category-tabs-wrapper {
  position: relative;
}

.category-tabs-fade {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 4px;
  width: 38px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 6px;
  pointer-events: none;
  background: linear-gradient(to right, transparent, #fafafa 70%);
  color: #8c8c8c;
  opacity: 0.95;
}

.category-tabs-fade svg {
  width: 14px;
  height: 14px;
}

.category-tabs-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}

.category-tabs-bar::-webkit-scrollbar {
  display: none;
}

/* Content card */
.console-learn-content {
  background: #fff;
  border-radius: 14px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
  min-height: 400px;
}

/* Category detail head */
.category-detail-head {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
  margin-bottom: 20px;
}

.category-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 10px;
}

.category-breadcrumb a {
  color: #8c8c8c;
  text-decoration: none;
}

.category-breadcrumb a:hover {
  color: #FF2442;
}

.category-breadcrumb-sep {
  color: #d9d9d9;
}

.category-breadcrumb-active {
  color: #262626;
  font-weight: 600;
}

.category-detail-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
}

.category-detail-count {
  margin: 6px 0 0;
  font-size: 13px;
  color: #8c8c8c;
}

/* Article list */
.article-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.article-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.article-card:hover {
  border-color: transparent;
  box-shadow: 0 4px 16px rgba(255, 36, 66, 0.08);
  transform: translateX(2px);
}

.article-card:hover .article-title {
  color: #FF2442;
}

.article-card:hover .article-arrow {
  color: #FF2442;
  transform: translateX(3px);
}

.article-rank {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f5f5f5;
  color: #8c8c8c;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.article-card:nth-child(1) .article-rank {
  background: #fff1f0;
  color: #cf1322;
}

.article-card:nth-child(2) .article-rank {
  background: #fff7e6;
  color: #d48806;
}

.article-card:nth-child(3) .article-rank {
  background: #f6ffed;
  color: #389e0d;
}

.article-body {
  flex: 1;
  min-width: 0;
}

.article-meta-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.article-category {
  font-size: 12px;
  color: #FF2442;
  background: #FFF0F2;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.article-reading,
.article-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.article-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.45;
  transition: color 0.2s;
}

.article-summary {
  margin: 6px 0 0;
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-arrow {
  font-size: 20px;
  color: #d9d9d9;
  flex-shrink: 0;
  transition: all 0.2s;
}

.all-articles-pagination {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}

/* 付费文章右上角徽章 */
.article-badge {
  position: absolute;
  top: 14px;
  right: 16px;
  font-size: 12px;
  font-weight: 600;
  color: #FF6B1A;
  background: #FFF3E0;
  border: 1px solid #FFD8A8;
  border-radius: 9999px;
  padding: 2px 10px;
  z-index: 1;
  flex-shrink: 0;
}
@media (max-width: 768px) {
  .article-badge {
    top: 10px;
    right: 12px;
    font-size: 11px;
    padding: 1px 8px;
  }
}
body[data-theme="dark"] .article-badge {
  background: rgba(255, 107, 26, 0.15);
  border-color: rgba(255, 107, 26, 0.35);
  color: #FF9F4D;
}

/* Empty & loading */
.console-learn-empty,
.console-learn-empty,
.console-learn-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  gap: 12px;
  color: #8c8c8c;
  text-align: center;
}

.console-learn-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.console-learn-empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.console-learn-empty-subtitle {
  font-size: 13px;
  color: #8c8c8c;
}

/* 暗色主题 */
body[data-theme="dark"] .console-learn-title,
body[data-theme="dark"] .category-detail-title,
body[data-theme="dark"] .category-breadcrumb-active,
body[data-theme="dark"] .console-learn-empty-title,
body[data-theme="dark"] .article-title {
  color: rgba(255, 255, 255, 0.92);
}

body[data-theme="dark"] .console-learn-desc,
body[data-theme="dark"] .category-detail-count,
body[data-theme="dark"] .console-learn-empty-subtitle,
body[data-theme="dark"] .article-summary,
body[data-theme="dark"] .article-reading,
body[data-theme="dark"] .article-date,
body[data-theme="dark"] .category-breadcrumb,
body[data-theme="dark"] .category-breadcrumb a {
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .console-learn-content,
body[data-theme="dark"] .category-tab {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .category-tab {
  border-color: #303030;
}

body[data-theme="dark"] .category-tab:hover {
  border-color: #434343;
  color: rgba(255, 255, 255, 0.85);
}

body[data-theme="dark"] .category-tabs-fade {
  background: linear-gradient(to right, transparent, #141414 70%);
  color: #8c8c8c;
}

body[data-theme="dark"] .article-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .article-card:hover {
  box-shadow: 0 4px 16px rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .article-rank {
  background: #262626;
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .subcategory-tab {
  background: #1f1f1f;
  border-color: #303030;
  color: rgba(255, 255, 255, 0.65);
}

body[data-theme="dark"] .subcategory-tab:hover {
  border-color: #434343;
  color: rgba(255, 255, 255, 0.85);
}

body[data-theme="dark"] .subcategory-tab.active {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}

body[data-theme="dark"] .category-detail-head {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .category-breadcrumb-sep {
  color: #434343;
}

body[data-theme="dark"] .console-learn-empty-icon,
body[data-theme="dark"] .article-arrow {
  color: #595959;
}

body[data-theme="dark"] .article-category {
  background: rgba(255, 36, 66, 0.12);
  color: #ff4d6f;
}

.article-cover { display: none; }

/* 响应式 */
@media (max-width: 768px) {
  .console-learn-page {
    padding: 0;
  }

  .console-learn-body {
    padding: 0 12px;
  }

  .console-learn-header {
    margin-bottom: 12px;
  }

  .console-learn-header-main {
    gap: 10px;
  }

  .console-learn-icon {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    font-size: 20px;
  }

  .console-learn-title {
    font-size: 18px;
  }

  .console-learn-desc {
    display: none;
  }

  .console-learn-header {
    display: none;
  }

  .console-learn-hero-mobile {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    height: 170px;
    margin: 0 0 12px;
    padding: 0 20px 0 22px;
    border-radius: 0;
    background: linear-gradient(45deg, #ffd6de 0%, #ffe8ed 40%, #fff0f3 70%, #fff5f7 100%);
    overflow: hidden;
    position: relative;
  }

  .console-learn-hero-mobile::before,
  .console-learn-hero-mobile::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    border: 18px solid rgba(255, 36, 66, 0.08);
    pointer-events: none;
  }

  .console-learn-hero-mobile::before {
    width: 120px;
    height: 120px;
    bottom: -30px;
    left: -30px;
  }

  .console-learn-hero-mobile::after {
    width: 80px;
    height: 80px;
    top: -20px;
    left: 60px;
    border-width: 14px;
    border-color: rgba(255, 36, 66, 0.06);
  }

  .console-learn-hero-mobile__text {
    position: relative;
    z-index: 1;
    flex: 1;
    min-width: 0;
  }

  .console-learn-hero-mobile__title {
    display: none;
  }

  .console-learn-hero-mobile__logo {
    height: 38px;
    width: auto;
    display: block;
  }

  .console-learn-hero-mobile__desc {
    margin: 8px 0 0;
    font-size: 12px;
    color: #595959;
  }

  .console-learn-hero-mobile__icon-wrap {
    width: 130px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    z-index: 1;
  }

  .console-learn-hero-mobile__icon {
    width: 120px;
    height: 120px;
    object-fit: contain;
    flex-shrink: 0;
  }

  .console-learn-content {
    background: transparent;
    box-shadow: none;
    padding: 0;
    border-radius: 0;
    min-height: auto;
  }

  .category-tabs-wrapper {
    position: sticky;
    top: 0;
    z-index: 10;
    display: block;
  }

  .category-tabs-bar {
    background: #fff;
    padding: 0;
    margin: 0 0 4px;
    border-bottom: 1px solid #f0f0f0;
    gap: 0;
  }

  .category-tabs-fade {
    background: linear-gradient(to right, transparent, #fff 70%);
    bottom: 1px;
  }

  .category-tab {
    padding: 12px 14px;
    border-radius: 0;
    font-size: 14px;
    background: transparent;
    border: none;
    border-bottom: 2px solid transparent;
    color: #595959;
    position: relative;
    margin-bottom: -1px;
  }

  .category-tab.active {
    background: transparent;
    border-color: var(--color-primary, #FF2442);
    color: var(--color-primary, #FF2442);
    font-weight: 600;
  }

  .category-detail-head {
    display: none;
  }

  .article-list {
    gap: 0;
    background: #fff;
    border-radius: 12px;
    overflow: hidden;
  }

  .article-card {
    position: relative;
    flex-direction: row;
    align-items: flex-start;
    padding: 29px 0;
    background: transparent;
    border: 0;
    border-radius: 0;
    box-shadow: none;
    margin: 0 12px;
    gap: 12px;
    border-bottom: 1px solid #f5f5f5;
  }

  .article-card:last-child {
    border-bottom: 0;
    margin-bottom: 0;
  }

  .article-card:hover {
    transform: none;
    box-shadow: none;
  }

  .article-cover {
    display: block;
    order: 3;
    width: 88px;
    height: 44px;
    aspect-ratio: auto;
    object-fit: cover;
    border-radius: 8px;
    flex-shrink: 0;
  }

  .article-rank {
    position: static;
    order: 1;
    width: 22px;
    height: auto;
    min-height: 22px;
    border-radius: 0;
    font-size: 16px;
    font-weight: 700;
    background: transparent;
    color: #bfbfbf;
    box-shadow: none;
    line-height: 1.3;
    padding-top: 2px;
  }

  .article-card:nth-child(1) .article-rank {
    background: transparent;
    color: #ff2442;
  }

  .article-card:nth-child(2) .article-rank {
    background: transparent;
    color: #ff7a45;
  }

  .article-card:nth-child(3) .article-rank {
    background: transparent;
    color: #ffa940;
  }

  .article-body {
    order: 2;
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  .article-meta-top {
    order: 2;
    gap: 8px;
    margin: 6px 0 0;
  }

  .article-category {
    font-size: 11px;
    padding: 0;
    border-radius: 0;
    background: transparent;
    color: #8c8c8c;
  }

  .article-reading,
  .article-date {
    font-size: 11px;
  }

  .article-title {
    order: 1;
    font-size: 15px;
    font-weight: 600;
    line-height: 1.45;
    color: #1a1a1a;
  }

  .article-summary {
    display: none;
  }

  .article-arrow {
    display: none;
  }

  .console-learn-empty,
  .console-learn-loading {
    background: #fff;
    border-radius: 18px;
    min-height: 260px;
    box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  }

  .subcategory-tabs-bar {
    margin-bottom: 16px;
  }

  .subcategory-tab {
    padding: 7px 14px;
    font-size: 12px;
  }
}

/* 移动端暗色 */
@media (max-width: 768px) {
  body[data-theme="dark"] .category-tabs-wrapper { background: #141414; }
  body[data-theme="dark"] .category-tabs-bar {
    background: #141414;
    border-bottom-color: #333;
  }
  body[data-theme="dark"] .category-tabs-fade {
    background: linear-gradient(to right, transparent, #141414 70%);
    color: #8c8c8c;
  }
  body[data-theme="dark"] .console-learn-empty,
  body[data-theme="dark"] .console-learn-loading {
    background: #1f1f1f;
    box-shadow: 0 4px 18px rgba(0, 0, 0, 0.2);
  }
  body[data-theme="dark"] .article-list {
    background: #1f1f1f;
  }
  body[data-theme="dark"] .article-card {
    background: transparent;
    border-bottom-color: #333;
    box-shadow: none;
  }
  body[data-theme="dark"] .article-cover {
    border-radius: 8px;
  }
  body[data-theme="dark"] .article-rank {
    background: transparent;
    color: #595959;
  }
  body[data-theme="dark"] .article-title {
    color: rgba(255, 255, 255, 0.9);
  }
  body[data-theme="dark"] .article-category {
    color: #8c8c8c;
  }
  body[data-theme="dark"] .article-reading,
  body[data-theme="dark"] .article-date {
    color: #8c8c8c;
  }
  body[data-theme="dark"] .console-learn-hero-mobile {
    background: linear-gradient(45deg, #2a181b 0%, #221416 40%, #1a1113 70%, #150f10 100%);
  }
  body[data-theme="dark"] .console-learn-hero-mobile::before,
  body[data-theme="dark"] .console-learn-hero-mobile::after {
    border-color: rgba(255, 77, 111, 0.1);
  }
  body[data-theme="dark"] .console-learn-hero-mobile__desc {
    color: #8c8c8c;
  }
}
</style>
