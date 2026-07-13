<template>
  <div class="learn-content">
    <!-- 文章详情 -->
    <template v-if="article">
      <header class="learn-content-head">
        <h1 class="learn-content-title">{{ article.title }}</h1>
        <p v-if="article.summary" class="learn-content-summary">{{ article.summary }}</p>
        <div class="learn-content-meta">
          发布于 {{ formatDate(article.publishedAt || article.updatedAt) }}
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
          <span class="learn-nav-title">{{ article.prevArticle.title }}</span>
          <span
            v-if="currentCategoryName && article.prevArticle.categoryName !== currentCategoryName"
            class="learn-nav-cat"
          >《{{ article.prevArticle.categoryName }}》</span>
        </router-link>

        <router-link
          v-if="article.nextArticle"
          :to="`/learn/article/${article.nextArticle.id}`"
          class="learn-nav-card learn-nav-next"
        >
          <span class="learn-nav-dir">下一篇 →</span>
          <span class="learn-nav-title">{{ article.nextArticle.title }}</span>
          <span
            v-if="currentCategoryName && article.nextArticle.categoryName !== currentCategoryName"
            class="learn-nav-cat"
          >《{{ article.nextArticle.categoryName }}》</span>
        </router-link>
      </nav>

      <footer class="learn-content-foot">
        <router-link to="/login" class="learn-cta">想把自己的账号也做成这样？立即开始创作 →</router-link>
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
import LearnMarkdown from './LearnMarkdown.vue'
import LearnRichText from './LearnRichText.vue'

defineProps({
  article: { type: Object, default: null },
  category: { type: Object, default: null },
  currentCategoryName: { type: String, default: '' }
})
defineEmits(['load-article'])

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.learn-content { min-height: 320px; }
.learn-content-head { border-bottom: 1px solid #eee; padding-bottom: 16px; margin-bottom: 24px; }
.learn-content-title { font-size: 28px; font-weight: 700; color: #1a1a1a; margin: 0; }
.learn-content-summary { color: #666; font-size: 14px; margin: 8px 0 0; }
.learn-content-meta { color: #999; font-size: 13px; margin-top: 12px; }
.learn-content-body { margin-bottom: 36px; }
.learn-content-foot { border-top: 1px solid #eee; padding-top: 24px; text-align: center; }
.learn-cta { color: #FF2442; font-weight: 600; text-decoration: none; }
.learn-cta:hover { text-decoration: underline; }
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
  gap: 4px;
  padding: 16px 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
  color: #1a1a1a;
  text-decoration: none;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
  min-width: 0;
}
.learn-nav-card:hover {
  border-color: #FF2442;
  color: #FF2442;
}
.learn-nav-prev { text-align: left; align-items: flex-start; }
.learn-nav-next { text-align: right; align-items: flex-end; }
.learn-nav-dir {
  font-size: 12px;
  color: #999;
  font-weight: 500;
}
.learn-nav-card:hover .learn-nav-dir { color: #FF2442; }
.learn-nav-title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 100%;
}
.learn-nav-cat {
  font-size: 12px;
  color: #999;
  font-weight: 400;
}
@media (max-width: 991px) {
  .learn-nav { flex-direction: column; }
  .learn-nav-prev,
  .learn-nav-next { text-align: left; align-items: flex-start; }
}
</style>
