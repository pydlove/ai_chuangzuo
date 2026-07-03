<template>
  <div class="guide-page">
    <!-- 顶部导航 -->
    <header class="guide-nav">
      <div class="nav-brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="nav-logo"
        />
        <span class="nav-brand-name">爱创作</span>
      </div>
      <div class="nav-links">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/pricing" class="nav-link">会员</router-link>
        <router-link to="/guide" class="nav-link active">玩法指南</router-link>
        <button
          class="theme-toggle"
          :title="currentTheme === 'light' ? '切换深色主题' : '切换浅色主题'"
          @click="toggleTheme"
        >
          <svg v-if="currentTheme === 'light'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5" />
            <line x1="12" y1="1" x2="12" y2="3" />
            <line x1="12" y1="21" x2="12" y2="23" />
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
            <line x1="1" y1="12" x2="3" y2="12" />
            <line x1="21" y1="12" x2="23" y2="12" />
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
            <line x1="18.36" y1="5.64" x2="19.78" y2="5.64" />
          </svg>
        </button>
        <router-link to="/login" class="nav-cta">开始创作</router-link>
      </div>
    </header>

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

const route = useRoute()
const router = useRouter()

const activeArticleId = ref('')

const THEME_KEY = 'aichuangzuo_theme'
const currentTheme = ref('light')

const toggleTheme = () => {
  const next = currentTheme.value === 'light' ? 'dark' : 'light'
  currentTheme.value = next
  document.body.setAttribute('data-theme', next)
  localStorage.setItem(THEME_KEY, next)
}

const loadTheme = () => {
  const saved = localStorage.getItem(THEME_KEY) || 'light'
  currentTheme.value = saved
  document.body.setAttribute('data-theme', saved)
}

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
  loadTheme()
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
.guide-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 48px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}
.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nav-logo {
  height: 32px;
  width: auto;
}
.nav-brand-name {
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 32px;
}
.nav-link {
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: color 0.2s;
}
.nav-link:hover,
.nav-link.active {
  color: #ff2442;
}
.nav-cta {
  padding: 8px 22px;
  background: #ff2442;
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.nav-cta:hover {
  background: #e61e3a;
}
.theme-toggle {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.theme-toggle:hover {
  background: #fff5f7;
  color: #ff2442;
}
.theme-toggle svg {
  width: 18px;
  height: 18px;
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
.guide-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}

@media (max-width: 768px) {
  .guide-nav {
    padding: 14px 16px;
  }
  .guide-body {
    flex-direction: column;
    padding: 16px;
    gap: 16px;
  }
  .guide-hero h1 {
    font-size: 24px;
  }
  .nav-links {
    gap: 16px;
  }
}

body[data-theme="dark"] .guide-page {
  background: #141414;
}
body[data-theme="dark"] .guide-nav,
body[data-theme="dark"] .guide-footer {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .nav-brand-name,
body[data-theme="dark"] .guide-hero h1,
body[data-theme="dark"] .guide-footer-cta h3 {
  color: #e0e0e0;
}
body[data-theme="dark"] .nav-link {
  color: #a6a6a6;
}
body[data-theme="dark"] .nav-link:hover,
body[data-theme="dark"] .nav-link.active {
  color: #ff4d6f;
}
body[data-theme="dark"] .nav-cta {
  background: linear-gradient(135deg, #ff6b8a 0%, #ff2442 100%);
}
body[data-theme="dark"] .theme-toggle {
  color: #a6a6a6;
}
body[data-theme="dark"] .theme-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
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
body[data-theme="dark"] .guide-footer span + span::before {
  color: #303030;
}
</style>
