<template>
  <article :id="section.id" class="guide-article">
    <h2 class="ga-section-title">{{ section.title }}</h2>
    <div
      v-for="article in section.articles"
      :id="article.id"
      :key="article.id"
      class="ga-article"
    >
      <h3 class="ga-title">{{ article.title }}</h3>
      <div v-if="article.content" class="ga-content" v-html="article.content" />
      <component
        :is="componentMap[article.component]"
        v-else-if="article.component"
      />
    </div>
  </article>
</template>

<script setup>
import TimeCalculator from './TimeCalculator.vue'
import LeaderboardPreview from './LeaderboardPreview.vue'

const componentMap = {
  TimeCalculator,
  LeaderboardPreview
}

defineProps({
  section: { type: Object, required: true }
})
</script>

<style scoped>
.guide-article {
  padding-bottom: 48px;
  margin-bottom: 48px;
  border-bottom: 1px solid #f0f0f0;
}
.guide-article:last-child {
  border-bottom: none;
  margin-bottom: 0;
}
.ga-section-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 24px;
}
.ga-article {
  margin-bottom: 32px;
  scroll-margin-top: 24px;
}
.ga-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px;
}
.ga-content {
  font-size: 15px;
  color: #595959;
  line-height: 1.8;
}
.ga-content :deep(p) {
  margin: 0 0 12px;
}
.ga-content :deep(ul) {
  padding-left: 20px;
  margin: 0 0 12px;
}
.ga-content :deep(li) {
  margin-bottom: 6px;
}
.ga-content :deep(strong) {
  color: #1a1a1a;
}

body[data-theme="dark"] .guide-article {
  border-bottom-color: #303030;
}
body[data-theme="dark"] .ga-section-title,
body[data-theme="dark"] .ga-title,
body[data-theme="dark"] .ga-content :deep(strong) {
  color: #e0e0e0;
}
body[data-theme="dark"] .ga-content {
  color: #a6a6a6;
}
</style>
