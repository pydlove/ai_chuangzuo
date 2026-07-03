<template>
  <aside class="guide-sidebar">
    <div class="gs-mobile-toggle" @click="mobileOpen = !mobileOpen">
      <span>目录</span>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="3" y1="12" x2="21" y2="12" />
        <line x1="3" y1="6" x2="21" y2="6" />
        <line x1="3" y1="18" x2="21" y2="18" />
      </svg>
    </div>
    <div v-if="mobileOpen" class="gs-backdrop" @click="mobileOpen = false" />
    <div :class="['gs-nav', { open: mobileOpen }]">
      <div
        v-for="section in sections"
        :key="section.id"
        class="gs-section"
      >
        <div class="gs-section-title" @click="toggleSection(section.id)">
          {{ section.title }}
        </div>
        <div v-show="expanded[section.id] !== false" class="gs-articles">
          <div
            v-for="article in section.articles"
            :key="article.id"
            :class="['gs-article', { active: activeId === article.id }]"
            @click="handleClick(section.id, article.id)"
          >
            {{ article.title }}
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  sections: { type: Array, required: true },
  activeId: { type: String, default: '' }
})

const emit = defineEmits(['select'])

const mobileOpen = ref(false)
const expanded = ref({})

watch(() => props.activeId, (id) => {
  if (!id) return
  const section = props.sections.find(s => s.articles.some(a => a.id === id))
  if (section) {
    expanded.value[section.id] = true
  }
}, { immediate: true })

const toggleSection = (id) => {
  expanded.value[id] = expanded.value[id] === false
}

const handleClick = (sectionId, articleId) => {
  mobileOpen.value = false
  emit('select', { sectionId, articleId })
}
</script>

<style scoped>
.guide-sidebar {
  width: 260px;
  flex-shrink: 0;
}
.gs-mobile-toggle {
  display: none;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}
.gs-mobile-toggle svg {
  width: 18px;
  height: 18px;
}
.gs-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;
}
.gs-nav {
  position: sticky;
  top: 24px;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}
.gs-section {
  margin-bottom: 8px;
}
.gs-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
}
.gs-section-title:hover {
  background: #f5f5f5;
}
.gs-articles {
  padding-left: 8px;
}
.gs-article {
  font-size: 14px;
  color: #595959;
  padding: 8px 12px 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.6;
}
.gs-article:hover {
  color: #ff2442;
  background: #fff5f7;
}
.gs-article.active {
  color: #ff2442;
  background: #fff0f2;
  font-weight: 500;
}

@media (max-width: 768px) {
  .guide-sidebar {
    width: auto;
    flex-shrink: 0;
  }
  .gs-mobile-toggle {
    display: flex;
  }
  .gs-backdrop {
    display: block;
  }
  .gs-nav {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 260px;
    background: #fff;
    z-index: 100;
    padding: 16px;
    transform: translateX(-100%);
    transition: transform 0.25s;
    box-shadow: 2px 0 12px rgba(0,0,0,0.1);
  }
  .gs-nav.open {
    transform: translateX(0);
  }
}

body[data-theme="dark"] .gs-mobile-toggle {
  color: #a6a6a6;
  border-bottom-color: #303030;
  background: #1f1f1f;
}
body[data-theme="dark"] .gs-section-title,
body[data-theme="dark"] .gs-article.active {
  color: #e0e0e0;
}
body[data-theme="dark"] .gs-article {
  color: #a6a6a6;
}
body[data-theme="dark"] .gs-section-title:hover,
body[data-theme="dark"] .gs-article:hover {
  background: #2a2a2a;
}
body[data-theme="dark"] .gs-article.active {
  background: rgba(255, 36, 66, 0.15);
}
body[data-theme="dark"] .gs-nav {
  background: #1f1f1f;
}
</style>
