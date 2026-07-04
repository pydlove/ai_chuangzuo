<template>
  <aside class="guide-sidebar">
    <div class="gs-mobile-toggle" @click="mobileOpen = !mobileOpen" aria-label="打开目录">
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
  gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  border-radius: 20px;
  background: #fff;
}
.gs-mobile-toggle svg {
  width: 16px;
  height: 16px;
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
    width: 0;
    flex-shrink: 0;
    overflow: hidden;
  }
  .gs-mobile-toggle {
    display: flex;
    position: fixed;
    right: 16px;
    bottom: 24px;
    width: 56px;
    height: 56px;
    padding: 0;
    border-radius: 50%;
    border: none;
    background: #ff2442;
    color: #fff;
    box-shadow: 0 6px 18px rgba(255, 36, 66, 0.35);
    z-index: 90;
    justify-content: center;
  }
  .gs-mobile-toggle:hover {
    background: #e61e3a;
  }
  .gs-mobile-toggle svg {
    width: 22px;
    height: 22px;
  }
  .gs-backdrop {
    display: block;
  }
  .gs-nav {
    position: fixed;
    top: auto;
    left: 0;
    right: 0;
    bottom: 0;
    max-height: 70vh;
    background: #fff;
    z-index: 100;
    padding: 12px 16px 24px;
    border-top-left-radius: 16px;
    border-top-right-radius: 16px;
    transform: translateY(100%);
    transition: transform 0.28s ease;
    box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.12);
    overflow-y: auto;
    pointer-events: none;
  }
  .gs-nav.open {
    transform: translateY(0);
    pointer-events: auto;
  }
  .gs-nav::before {
    content: '';
    display: block;
    width: 36px;
    height: 4px;
    background: #e0e0e0;
    border-radius: 2px;
    margin: 0 auto 12px;
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
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.5);
}
body[data-theme="dark"] .gs-nav::before {
  background: #404040;
}
body[data-theme="dark"] .gs-mobile-toggle {
  background: linear-gradient(135deg, #ff6b8a 0%, #ff2442 100%);
}
body[data-theme="dark"] .gs-mobile-toggle:hover {
  background: linear-gradient(135deg, #ff4d6f 0%, #e61e3a 100%);
}
</style>
