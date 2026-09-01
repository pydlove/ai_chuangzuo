<template>
  <MobileGuide v-if="isMobile" />
  <div v-else class="guide-page">
    <NavBar :links="landingNavLinks" :cta-to="landingTopCta.to" :cta-label="landingTopCta.label" />

    <!-- 主体 -->
    <div class="guide-body">
      <GuideSidebar
        :sections="guideSections"
        :active-id="activeArticleId"
        @select="handleSelect"
      />
      <div class="guide-main">
        <div class="guide-hero">
          <h1>{{ guideHero.pc.title }}</h1>
          <p>{{ guideHero.pc.desc }}</p>
        </div>
        <div class="guide-articles-wrap">
          <template v-for="section in guideSections" :key="section.id">
            <img
              v-if="section.id === 'money'"
              class="guide-money-banner"
              :src="guideMoneyBanner.src"
              :alt="guideMoneyBanner.alt"
            />
            <GuideArticle :section="section" />
          </template>
        </div>
        <div class="guide-footer-cta">
          <h3>{{ guideCta.title }}</h3>
          <p>{{ guideCta.desc }}</p>
          <router-link :to="guideCta.btn.to" class="guide-cta-btn">{{ guideCta.btn.text }}</router-link>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { guideSections, guideHero, guideMoneyBanner, guideCta } from '@/data/guide-content.js'
import { landingNavLinks, landingTopCta } from '@/data/siteConfig.js'
import GuideSidebar from '@/components/guide/GuideSidebar.vue'
import GuideArticle from '@/components/guide/GuideArticle.vue'
import MobileGuide from '@/views/MobileGuide.vue'
import NavBar from '@/components/layout/NavBar.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { useDevice } from '@/composables/useDevice.js'

const { isMobile } = useDevice()

const route = useRoute()
const router = useRouter()

const activeArticleId = ref('')

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
.guide-money-banner {
  display: block;
  width: 100%;
  border-radius: 12px;
  margin-bottom: 32px;
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
body[data-theme="dark"] .guide-hero h1,
body[data-theme="dark"] .guide-footer-cta h3 {
  color: #e0e0e0;
}
body[data-theme="dark"] .guide-hero p,
body[data-theme="dark"] .guide-footer-cta p {
  color: #a6a6a6;
}
body[data-theme="dark"] .guide-footer-cta {
  background: linear-gradient(135deg, #331018 0%, #1f1f1f 100%);
}
body[data-theme="dark"] .guide-cta-btn {
  background: linear-gradient(135deg, #ff6b8a 0%, #ff2442 100%);
}
</style>