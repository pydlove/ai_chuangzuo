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
  category: { type: Object, default: null }
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
</style>
