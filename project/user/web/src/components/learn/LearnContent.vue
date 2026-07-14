<template>
  <div class="learn-content">
    <!-- 文章详情 -->
    <template v-if="article">
      <header class="learn-content-head">
        <!-- 面包屑 -->
        <nav v-if="categoryPath.length" class="learn-breadcrumb">
          <router-link to="/learn" class="learn-breadcrumb-item">创作学院</router-link>
          <template v-for="(seg, i) in categoryPath" :key="seg.id">
            <span class="learn-breadcrumb-sep">›</span>
            <router-link
              :to="`/learn?cat=${seg.id}`"
              class="learn-breadcrumb-item"
              :class="{ active: i === categoryPath.length - 1 }"
            >{{ seg.name }}</router-link>
          </template>
        </nav>

        <h1 class="learn-content-title">{{ article.title }}</h1>
        <p v-if="article.summary" class="learn-content-summary">{{ article.summary }}</p>

        <!-- 元信息条 -->
        <div class="learn-meta-bar">
          <span class="learn-meta-item">
            <CalendarOutlined class="learn-meta-icon" />
            {{ formatDate(article.publishedAt || article.updatedAt) }}
          </span>
          <span class="learn-meta-item">
            <ClockCircleOutlined class="learn-meta-icon" />
            约 {{ readingMinutes }} 分钟
          </span>
          <span class="learn-meta-item">
            <FileTextOutlined class="learn-meta-icon" />
            {{ wordCount }} 字
          </span>
          <router-link
            v-if="currentCategoryName"
            :to="`/learn?cat=${article.categoryId}`"
            class="learn-meta-tag"
          >
            <TagOutlined class="learn-meta-icon" />
            {{ currentCategoryName }}
          </router-link>
        </div>
      </header>
      <article class="learn-content-body">
        <LearnMarkdown v-if="article.contentType === 'markdown'" :source="article.content" />
        <LearnRichText v-else :html="article.content" />
      </article>

      <nav v-if="article.prevArticle || article.nextArticle" class="learn-nav">
        <router-link
          v-if="article.prevArticle"
          :to="`/learn/article/${article.prevArticle.id}`"
          class="learn-nav-card learn-nav-prev"
        >
          <span class="learn-nav-dir">← 上一篇</span>
          <span
            v-if="currentCategoryName && article.prevArticle.categoryName !== currentCategoryName"
            class="learn-nav-cat-chip"
          >{{ article.prevArticle.categoryName }}</span>
          <span class="learn-nav-title">{{ article.prevArticle.title }}</span>
        </router-link>

        <router-link
          v-if="article.nextArticle"
          :to="`/learn/article/${article.nextArticle.id}`"
          class="learn-nav-card learn-nav-next"
        >
          <span class="learn-nav-dir">下一篇 →</span>
          <span
            v-if="currentCategoryName && article.nextArticle.categoryName !== currentCategoryName"
            class="learn-nav-cat-chip"
          >{{ article.nextArticle.categoryName }}</span>
          <span class="learn-nav-title">{{ article.nextArticle.title }}</span>
        </router-link>
      </nav>

      <footer class="learn-content-foot">
        <div class="learn-cta-card">
          <BulbOutlined class="learn-cta-icon" />
          <div class="learn-cta-text">
            <div class="learn-cta-title">想把自己的账号也做成这样？</div>
            <div class="learn-cta-subtitle">用 AI 一分钟生成你的第一篇</div>
          </div>
          <router-link to="/login" class="learn-cta-btn">立即开始创作 →</router-link>
        </div>
      </footer>
    </template>

    <!-- 分类详情（列表） -->
    <template v-else-if="category && category.articles && category.articles.length">
      <header class="learn-content-head">
        <h1 class="learn-content-title">{{ category.name }}</h1>
      </header>
      <ul class="learn-article-list">
        <li v-for="a in category.articles" :key="a.id" class="learn-article-item">
          <a @click.prevent="$emit('load-article', a.id)" href="#">{{ a.title }}</a>
          <p v-if="a.summary" class="learn-article-summary">{{ a.summary }}</p>
          <div class="learn-article-meta">{{ formatDate(a.publishedAt || a.updatedAt) }}</div>
        </li>
      </ul>
    </template>

    <template v-else-if="category">
      <header class="learn-content-head">
        <h1 class="learn-content-title">{{ category.name }}</h1>
      </header>
      <div class="learn-content-empty">该分类下暂无已发布文章</div>
    </template>

    <template v-else>
      <div class="learn-content-empty">从左侧选择一个分类查看内容</div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LearnMarkdown from './LearnMarkdown.vue'
import LearnRichText from './LearnRichText.vue'
import {
  CalendarOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  TagOutlined,
  BulbOutlined
} from '@ant-design/icons-vue'

const props = defineProps({
  article: { type: Object, default: null },
  category: { type: Object, default: null },
  currentCategoryName: { type: String, default: '' },
  categoryPath: { type: Array, default: () => [] }
})
defineEmits(['load-article'])

const readingMinutes = computed(() => {
  if (!props.article?.content) return 0
  return Math.max(1, Math.ceil(props.article.content.length / 300))
})

const wordCount = computed(() => props.article?.content?.length || 0)

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.learn-content { min-height: 320px; }
.learn-content-head { border-bottom: 1px solid #eee; padding-bottom: 16px; margin-bottom: 24px; }

/* 面包屑 */
.learn-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.learn-breadcrumb-item {
  color: #8c8c8c;
  text-decoration: none;
}
.learn-breadcrumb-item:hover { color: #FF2442; }
.learn-breadcrumb-item.active { color: #262626; font-weight: 600; }
.learn-breadcrumb-sep { color: #d9d9d9; }

/* 元信息条 */
.learn-meta-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
}
.learn-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.learn-meta-icon { font-size: 14px; }
.learn-meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  background: #FFF5F7;
  color: #FF2442;
  border-radius: 9999px;
  font-size: 12px;
  text-decoration: none;
}
.learn-meta-tag:hover { background: #FFE8EC; }
.learn-content-title { font-size: 28px; font-weight: 700; color: #1a1a1a; margin: 0; }
.learn-content-summary { color: #666; font-size: 14px; margin: 8px 0 0; }
.learn-content-body { margin-bottom: 36px; }
.learn-content-empty { color: #999; padding: 80px 0; text-align: center; font-size: 14px; }

.learn-article-list { list-style: none; margin: 0; padding: 0; }
.learn-article-item { padding: 18px 0; border-bottom: 1px solid #f0f0f0; }
.learn-article-item a { font-size: 16px; font-weight: 600; color: #1a1a1a; cursor: pointer; }
.learn-article-item a:hover { color: #FF2442; }
.learn-article-summary { color: #666; font-size: 14px; margin: 6px 0 0; }
.learn-article-meta { color: #999; font-size: 12px; margin-top: 6px; }

/* 上下篇导航 */
.learn-nav {
  display: flex;
  gap: 12px;
  margin: 32px 0;
}
.learn-nav-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 20px;
  border: 1px solid #eee;
  border-radius: 12px;
  background: #fff;
  color: #1a1a1a;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 0;
}
.learn-nav-card:hover {
  border-color: #FF2442;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.12);
  transform: translateY(-2px);
}
.learn-nav-card:hover .learn-nav-title { color: #FF2442; }
.learn-nav-prev { text-align: left; align-items: flex-start; }
.learn-nav-next { text-align: right; align-items: flex-end; }
.learn-nav-dir {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 500;
}
.learn-nav-card:hover .learn-nav-dir { color: #FF2442; }
.learn-nav-cat-chip {
  display: inline-block;
  padding: 2px 10px;
  background: #FFF5F7;
  color: #FF2442;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 400;
}
.learn-nav-title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 100%;
  color: #1a1a1a;
}
@media (max-width: 991px) {
  .learn-nav { flex-direction: column; }
  .learn-nav-prev,
  .learn-nav-next { text-align: left; align-items: flex-start; }
}

/* CTA 卡片 */
.learn-content-foot {
  border-top: none;
  padding-top: 0;
  text-align: left;
}
.learn-cta-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 28px;
  background: linear-gradient(135deg, #FFF5F7 0%, #FFE8EC 100%);
  border-radius: 12px;
}
.learn-cta-icon {
  font-size: 32px;
  color: #FF2442;
  flex-shrink: 0;
}
.learn-cta-text { flex: 1; min-width: 0; }
.learn-cta-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.learn-cta-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 4px;
}
.learn-cta-btn {
  display: inline-block;
  padding: 10px 24px;
  background: #FF2442;
  color: #fff;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  transition: background 0.2s;
}
.learn-cta-btn:hover { background: #e61e3a; }
@media (max-width: 991px) {
  .learn-cta-card { flex-direction: column; text-align: center; }
  .learn-cta-btn { width: 100%; text-align: center; }
}
</style>
