<template>
  <div class="guide-page">
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

    <!-- 主体 -->
    <div class="guide-body">
      <GuideSidebar
        :sections="guideSections"
        :active-id="activeArticleId"
        @select="handleSelect"
      />
      <div class="guide-main">
        <div class="guide-hero">
          <h1>玩法指南</h1>
          <p>3 分钟了解爱创作能做什么，以及如何把它变成收益。</p>
        </div>
        <div class="guide-articles-wrap">
          <GuideArticle
            v-for="section in guideSections"
            :key="section.id"
            :section="section"
          />
        </div>
        <div class="guide-footer-cta">
          <h3>准备好开始了吗？</h3>
          <p>每天 3 分钟，把内容变成账号流量和收入。</p>
          <router-link to="/login" class="guide-cta-btn">立即开始创作</router-link>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <footer class="guide-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { guideSections } from '@/data/guide-content.js'
import GuideSidebar from '@/components/guide/GuideSidebar.vue'
import GuideArticle from '@/components/guide/GuideArticle.vue'
import NavBar from '@/components/layout/NavBar.vue'

const route = useRoute()
const router = useRouter()

const activeArticleId = ref('')

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'

const handleSelect = ({ articleId }) => {
  const el = document.getElementById(articleId)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    router.replace({ hash: `#${articleId}` })
    activeArticleId.value = articleId
  }
}

const observerArticles = () => {
  const ids = guideSections.flatMap(s => s.articles.map(a => a.id))
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          activeArticleId.value = entry.target.id
          if (route.hash !== `#${entry.target.id}`) {
            router.replace({ hash: `#${entry.target.id}` })
          }
        }
      })
    },
    { rootMargin: '-20% 0px -60% 0px', threshold: 0 }
  )
  ids.forEach((id) => {
    const el = document.getElementById(id)
    if (el) observer.observe(el)
  })
}

onMounted(() => {
  nextTick(() => {
    observerArticles()
    if (route.hash) {
      const id = route.hash.slice(1)
      const el = document.getElementById(id)
      if (el) {
        el.scrollIntoView({ behavior: 'auto', block: 'start' })
        activeArticleId.value = id
      }
    }
  })
})
</script>

<style scoped>
.guide-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.guide-body {
  flex: 1;
  display: flex;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 24px;
  gap: 40px;
}
.guide-main {
  flex: 1;
  min-width: 0;
}
.guide-hero {
  margin-bottom: 40px;
}
.guide-hero h1 {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 10px;
}
.guide-hero p {
  font-size: 16px;
  color: #595959;
  margin: 0;
}
.guide-footer-cta {
  text-align: center;
  padding: 48px 24px;
  background: linear-gradient(135deg, #fff0f2 0%, #fff 100%);
  border-radius: 16px;
  margin-top: 24px;
}
.guide-footer-cta h3 {
  font-size: 22px;
  color: #1a1a1a;
  margin: 0 0 8px;
}
.guide-footer-cta p {
  font-size: 15px;
  color: #595959;
  margin: 0 0 20px;
}
.guide-cta-btn {
  display: inline-block;
  padding: 14px 36px;
  background: #ff2442;
  color: #fff;
  border-radius: 28px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.guide-cta-btn:hover {
  background: #e61e3a;
}
.guide-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}

@media (max-width: 768px) {
  .guide-body {
    padding: 12px 16px;
  }
  .guide-hero h1 {
    font-size: 24px;
  }
}

body[data-theme="dark"] .guide-page {
  background: #141414;
}
body[data-theme="dark"] .guide-footer {
  background: #1f1f1f;
  border-top-color: #303030;
}
body[data-theme="dark"] .guide-hero h1,
body[data-theme="dark"] .guide-footer-cta h3 {
  color: #e0e0e0;
}
body[data-theme="dark"] .guide-hero p,
body[data-theme="dark"] .guide-footer-cta p,
body[data-theme="dark"] .guide-footer {
  color: #a6a6a6;
}
body[data-theme="dark"] .guide-footer-cta {
  background: linear-gradient(135deg, #331018 0%, #1f1f1f 100%);
}
body[data-theme="dark"] .guide-cta-btn {
  background: linear-gradient(135deg, #ff6b8a 0%, #ff2442 100%);
}
</style>