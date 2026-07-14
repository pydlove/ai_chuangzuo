<template>
  <div class="learn-content">
    <!-- 文章详情 -->
    <template v-if="article">
      <div class="learn-article-layout">
        <div class="learn-article-main">
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

          <img
            v-if="article.coverImageUrl"
            :src="article.coverImageUrl"
            class="learn-content-cover"
            alt=""
          />

          <article ref="contentRef" class="learn-content-body">
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
        </div>

        <!-- 目录侧边栏 -->
        <aside v-if="tocItems.length" class="learn-toc-sidebar">
          <nav class="learn-toc">
            <div class="learn-toc-title">
              <UnorderedListOutlined class="learn-toc-title-icon" />
              目录
            </div>
            <div class="learn-toc-track">
              <a
                v-for="item in tocItems"
                :key="item.id"
                :class="['learn-toc-item', { active: item.id === activeHeading, 'toc-h3': item.level === 3 }]"
                @click.prevent="scrollToHeading(item.id)"
                href="#"
              >
                <span class="learn-toc-dot"></span>
                <span class="learn-toc-text">{{ item.text }}</span>
              </a>
            </div>
          </nav>
        </aside>
      </div>
    </template>

    <!-- 分类详情（列表） -->
    <template v-else-if="category && category.articles && category.articles.length">
      <header class="learn-content-head">
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
        <h1 class="learn-content-title">{{ category.name }}</h1>
        <p class="learn-content-count">本分类下共 {{ category.total || category.articles.length }} 篇文章</p>
      </header>
      <ul class="learn-article-list">
        <li v-for="a in category.articles" :key="a.id" class="learn-article-card">
          <a @click.prevent="$emit('load-article', a.id)" href="#" class="learn-article-card-link">
            <div class="learn-article-card-body">
              <div class="learn-article-card-texts">
                <div class="learn-article-card-title">{{ a.title }}</div>
                <p v-if="a.summary" class="learn-article-card-summary">{{ a.summary }}</p>
                <div class="learn-article-card-meta">
                  <span class="learn-meta-item">
                    <CalendarOutlined class="learn-meta-icon" />
                    {{ formatDate(a.publishedAt || a.updatedAt) }}
                  </span>
                </div>
              </div>
              <img
                v-if="a.coverImageUrl"
                :src="a.coverImageUrl"
                class="learn-article-card-cover"
                alt=""
              />
            </div>
          </a>
        </li>
      </ul>
    </template>

    <template v-else-if="category">
      <header class="learn-content-head">
        <h1 class="learn-content-title">{{ category.name }}</h1>
      </header>
      <div class="learn-content-empty">
        <ReadOutlined class="learn-empty-icon" />
        <div class="learn-empty-title">该分类下暂无已发布文章</div>
      </div>
    </template>

    <template v-else>
      <div class="learn-content-empty">
        <ReadOutlined class="learn-empty-icon" />
        <div class="learn-empty-title">欢迎来到创作学院</div>
        <div class="learn-empty-subtitle">从左侧选择一个分类开始学习</div>
        <div v-if="topCategories.length" class="learn-empty-chips">
          <a
            v-for="cat in topCategories"
            :key="cat.id"
            class="learn-empty-chip"
            @click.prevent="$emit('select-category', cat.id)"
            href="#"
          >{{ cat.name }}</a>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick, onUnmounted } from 'vue'
import LearnMarkdown from './LearnMarkdown.vue'
import LearnRichText from './LearnRichText.vue'
import {
  CalendarOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  TagOutlined,
  BulbOutlined,
  ReadOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'

const props = defineProps({
  article: { type: Object, default: null },
  category: { type: Object, default: null },
  currentCategoryName: { type: String, default: '' },
  categoryPath: { type: Array, default: () => [] },
  topCategories: { type: Array, default: () => [] }
})
defineEmits(['load-article', 'select-category'])

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

// ---- 目录 ----
const contentRef = ref(null)
const tocItems = ref([])
const activeHeading = ref('')
let tocObserver = null

function buildToc() {
  if (tocObserver) { tocObserver.disconnect(); tocObserver = null }
  tocItems.value = []
  activeHeading.value = ''
  if (!contentRef.value) return

  const headings = contentRef.value.querySelectorAll('h2, h3')
  if (!headings.length) return

  tocItems.value = Array.from(headings).map((el, i) => {
    const id = `toc-${i}`
    el.id = id
    return { id, text: el.textContent, level: parseInt(el.tagName[1]) }
  })

  tocObserver = new IntersectionObserver(entries => {
    for (const entry of entries) {
      if (entry.isIntersecting) activeHeading.value = entry.target.id
    }
  }, { rootMargin: '-88px 0px -70% 0px' })
  headings.forEach(el => tocObserver.observe(el))
}

function scrollToHeading(id) {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

watch(() => props.article?.id, async id => {
  if (!id) { tocItems.value = []; return }
  await nextTick()
  buildToc()
}, { immediate: true })

onUnmounted(() => {
  if (tocObserver) tocObserver.disconnect()
})
</script>

<style scoped>
.learn-content { min-height: 320px; }

/* 文章详情布局：主内容 + 目录侧边栏 */
.learn-article-layout { display: flex; gap: 24px; }
.learn-article-main { flex: 1; min-width: 0; }

/* 目录侧边栏 */
.learn-toc-sidebar { width: 240px; flex-shrink: 0; }
.learn-toc {
  position: sticky;
  top: 88px;
  max-height: calc(100vh - 112px);
  overflow-y: auto;
  padding: 4px 0;
}
.learn-toc-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  padding-left: 16px;
}
.learn-toc-title-icon { font-size: 16px; color: #FF2442; }

/* 轨道线 */
.learn-toc-track {
  position: relative;
  padding-left: 20px;
}
.learn-toc-track::before {
  content: '';
  position: absolute;
  left: 3px;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: #f0f0f0;
  border-radius: 1px;
}

/* 目录项 */
.learn-toc-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 5px 8px;
  margin-left: -20px;
  padding-left: 0;
  text-decoration: none;
  color: #8c8c8c;
  font-size: 13px;
  line-height: 1.5;
  cursor: pointer;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
  position: relative;
}
.learn-toc-item:hover {
  color: #FF2442;
  background: #FFF5F7;
}
.learn-toc-item.active {
  color: #FF2442;
  font-weight: 600;
}
.learn-toc-item.active .learn-toc-dot {
  background: #FF2442;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.15);
}

/* 圆点：对齐轨道线中心 */
.learn-toc-dot {
  width: 8px;
  height: 8px;
  min-width: 8px;
  border-radius: 50%;
  background: #d9d9d9;
  margin-top: 5px;
  transition: all 0.2s;
  z-index: 1;
}
.learn-toc-item:hover .learn-toc-dot { background: #ffb3c1; }

/* 文字 */
.learn-toc-text {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* h3 缩进 */
.learn-toc-item.toc-h3 {
  padding-left: 12px;
  font-size: 12px;
}
.learn-toc-item.toc-h3 .learn-toc-dot {
  width: 6px;
  height: 6px;
  min-width: 6px;
  margin-top: 5px;
  margin-left: 1px;
}

@media (max-width: 991px) {
  .learn-toc-sidebar { display: none; }
}

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

/* 分类标题区 */
.learn-content-count {
  font-size: 13px;
  color: #8c8c8c;
  margin: 8px 0 0;
}

/* 文章卡片列表 */
.learn-article-list { list-style: none; margin: 0; padding: 0; }
.learn-article-card {
  margin-bottom: 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
  transition: all 0.2s ease;
}
.learn-article-card:hover {
  border-color: #FF2442;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.08);
}
.learn-article-card-link {
  display: block;
  padding: 16px 20px;
  text-decoration: none;
  color: inherit;
}
.learn-article-card-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.learn-article-card-texts { flex: 1; min-width: 0; }
.learn-article-card-cover {
  width: 140px;
  height: 84px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
  background: #f5f5f5;
}
@media (max-width: 991px) {
  .learn-article-card-cover { width: 96px; height: 64px; }
}
.learn-content-cover {
  width: 100%;
  max-height: 420px;
  object-fit: cover;
  border-radius: 12px;
  margin: 0 0 24px;
  display: block;
}
.learn-article-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.learn-article-card:hover .learn-article-card-title { color: #FF2442; }
.learn-article-card-summary {
  font-size: 14px;
  color: #595959;
  margin: 6px 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.learn-article-card-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

/* 空状态 */
.learn-content-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 12px;
  color: #8c8c8c;
  text-align: center;
}
.learn-empty-icon {
  font-size: 64px;
  color: #FFE8EC;
}
.learn-empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}
.learn-empty-subtitle {
  font-size: 14px;
  color: #8c8c8c;
}
.learn-empty-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 8px;
}
.learn-empty-chip {
  display: inline-block;
  padding: 8px 16px;
  border: 1px solid #eee;
  border-radius: 9999px;
  background: #fff;
  color: #262626;
  font-size: 14px;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
}
.learn-empty-chip:hover {
  border-color: #FF2442;
  color: #FF2442;
}

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
