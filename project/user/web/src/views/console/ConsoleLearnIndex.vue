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

    <!-- 分类标签 -->
    <div v-if="categoryTree.length" class="category-tabs-bar">
      <button
        :class="['category-tab', { active: isEmptyState }]"
        @click="goHome"
      >
        全部
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

    <!-- 内容区 -->
    <div class="console-learn-content">
      <!-- 全部：推荐文章 -->
      <template v-if="isEmptyState">
        <div v-if="recommendedArticles.length" class="article-list">
          <article
            v-for="(article, idx) in recommendedArticles"
            :key="article.id"
            class="article-card"
            @click="loadArticle(article.id)"
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
            <span class="article-arrow">›</span>
          </article>
        </div>

        <div v-else-if="!loading" class="console-learn-empty">
          <ReadOutlined class="console-learn-empty-icon" />
          <div class="console-learn-empty-title">欢迎来到创作学院</div>
          <div class="console-learn-empty-subtitle">推荐内容正在准备中，请先选择一个分类</div>
        </div>
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
          <p class="category-detail-count">共 {{ currentCategory.total || currentCategory.articles?.length || 0 }} 篇文章</p>
        </header>

        <div v-if="currentCategory.articles?.length" class="article-list">
          <article
            v-for="(article, idx) in currentCategory.articles"
            :key="article.id"
            class="article-card"
            @click="loadArticle(article.id)"
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
            <span class="article-arrow">›</span>
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
</template>

<script setup>
import { computed } from 'vue'
import {
  BookOutlined,
  ReadOutlined,
  ClockCircleOutlined,
  CalendarOutlined
} from '@ant-design/icons-vue'
import LearnContent from '@/components/learn/LearnContent.vue'
import { useLearn } from '@/composables/useLearn.js'

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
  onSelectCategory,
  loadArticle,
  goHome
} = useLearn('/console/learn')

const loading = computed(() => {
  // 空状态等待推荐文章，分类状态等待分类数据，文章状态等待文章数据
  if (isEmptyState.value) return recommendedArticles.value === null || recommendedArticles.value === undefined
  if (currentCategory.value) return false
  if (currentArticle.value) return false
  return true
})

function readingMinutes(content) {
  if (!content) return 1
  return Math.max(1, Math.ceil(content.length / 300))
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
  padding: 24px 32px;
  box-sizing: border-box;
}

/* Header */
.console-learn-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
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

/* Empty & loading */
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
    padding: 16px 12px;
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

  .console-learn-content {
    background: transparent;
    box-shadow: none;
    padding: 0;
    border-radius: 0;
    min-height: auto;
  }

  .category-tabs-bar {
    position: sticky;
    top: 0;
    z-index: 10;
    background: #fafafa;
    padding: 10px 0;
    margin: 0 0 12px;
  }

  .category-tab {
    padding: 7px 14px;
    border-radius: 999px;
    font-size: 13px;
  }

  .category-detail-head {
    background: #fff;
    border-radius: 18px;
    padding: 16px;
    margin-bottom: 12px;
    border-bottom: 0;
    box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  }

  .category-breadcrumb {
    margin-bottom: 6px;
  }

  .category-detail-title {
    font-size: 17px;
  }

  .article-list {
    gap: 0;
  }

  .article-card {
    position: relative;
    flex-direction: column;
    align-items: stretch;
    padding: 12px;
    background: #fff;
    border: 0;
    border-radius: 18px;
    box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
    margin-bottom: 12px;
    gap: 10px;
  }

  .article-card:last-child {
    margin-bottom: 0;
  }

  .article-card:hover {
    transform: none;
    box-shadow: 0 4px 18px rgba(0, 0, 0, 0.06);
  }

  .article-cover {
    display: block;
    width: 100%;
    height: 140px;
    object-fit: cover;
    border-radius: 12px;
  }

  .article-rank {
    position: absolute;
    top: 20px;
    left: 20px;
    width: 26px;
    height: 26px;
    border-radius: 8px;
    font-size: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .article-meta-top {
    gap: 8px;
    margin-bottom: 4px;
  }

  .article-category {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 999px;
  }

  .article-reading,
  .article-date {
    font-size: 11px;
  }

  .article-title {
    font-size: 15px;
  }

  .article-summary {
    font-size: 12px;
    -webkit-line-clamp: 2;
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
}

/* 移动端暗色 */
@media (max-width: 768px) {
  body[data-theme="dark"] .category-tabs-bar { background: #141414; }
  body[data-theme="dark"] .category-detail-head,
  body[data-theme="dark"] .article-card,
  body[data-theme="dark"] .console-learn-empty,
  body[data-theme="dark"] .console-learn-loading {
    background: #1f1f1f;
    box-shadow: 0 4px 18px rgba(0, 0, 0, 0.2);
  }
  body[data-theme="dark"] .article-cover {
    border-radius: 12px;
  }
  body[data-theme="dark"] .article-rank {
    background: rgba(0, 0, 0, 0.45);
    color: rgba(255, 255, 255, 0.9);
  }
}
</style>
