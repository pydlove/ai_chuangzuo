<template>
  <header class="navbar">
    <div class="navbar-brand">
      <router-link to="/" class="navbar-brand-link">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="navbar-logo"
        />
        <span class="navbar-brand-name">爱创作</span>
      </router-link>
    </div>

    <nav class="navbar-links">
      <template v-for="link in links" :key="link.to || link.href">
        <a
          v-if="link.href"
          :href="link.href"
          target="_blank"
          rel="noopener"
          class="navbar-link navbar-link-desktop"
        >{{ link.label }}</a>
        <router-link
          v-else
          :to="link.to"
          class="navbar-link navbar-link-desktop"
          :class="{ active: resolvedActive === link.to }"
        >{{ link.label }}</router-link>
      </template>

      <router-link :to="ctaTo" class="navbar-cta">{{ ctaLabel }}</router-link>

      <button
        class="mobile-menu-toggle"
        aria-label="打开菜单"
        @click="mobileMenuOpen = !mobileMenuOpen"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="3" y1="6" x2="21" y2="6" />
          <line x1="3" y1="12" x2="21" y2="12" />
          <line x1="3" y1="18" x2="21" y2="18" />
        </svg>
      </button>
    </nav>
  </header>

  <div
    v-if="mobileMenuOpen"
    class="mobile-drawer-backdrop"
    @click="mobileMenuOpen = false"
  />
  <div :class="['mobile-drawer', { open: mobileMenuOpen }]">
    <div class="mobile-drawer-header">
      <span class="mobile-drawer-title">菜单</span>
      <button class="mobile-drawer-close" aria-label="关闭菜单" @click="mobileMenuOpen = false">×</button>
    </div>
    <nav class="mobile-drawer-nav">
      <template v-for="link in links" :key="link.to || link.href">
        <a
          v-if="link.href"
          :href="link.href"
          target="_blank"
          rel="noopener"
          class="mobile-drawer-link"
          @click="mobileMenuOpen = false"
        >{{ link.label }}</a>
        <router-link
          v-else
          :to="link.to"
          class="mobile-drawer-link"
          :class="{ active: resolvedActive === link.to }"
          @click="mobileMenuOpen = false"
        >{{ link.label }}</router-link>
      </template>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'

const THEME_KEY = 'aichuangzuo_theme'

const props = defineProps({
  links: { type: Array, required: true },
  activePath: { type: String, default: '' },
  ctaTo: { type: String, default: '/console/workbench' },
  ctaLabel: { type: String, default: '开始创作' }
})

const route = useRoute()
const mobileMenuOpen = ref(false)
const currentTheme = ref('light')

const resolvedActive = computed(() => props.activePath || route.path)

const loadTheme = () => {
  const saved = localStorage.getItem(THEME_KEY) || 'light'
  currentTheme.value = saved
  document.body.setAttribute('data-theme', saved)
}

watch(mobileMenuOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

onMounted(loadTheme)

onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 48px;
  box-sizing: border-box;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  user-select: none;
  cursor: default;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.navbar-brand-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.navbar-logo {
  height: 32px;
  width: auto;
}

.navbar-brand-name {
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
}

.navbar-links {
  display: flex;
  align-items: center;
  gap: 32px;
}

.navbar-link {
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: color 0.2s;
}

.navbar-link:hover,
.navbar-link.active {
  color: #FF2442;
}

.navbar-cta {
  padding: 8px 22px;
  background: #FF2442;
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.navbar-cta:hover {
  background: #E61E3A;
}

.navbar-link-desktop {
  display: inline-flex;
}

/* 汉堡按钮 */
.mobile-menu-toggle {
  display: none;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.mobile-menu-toggle:hover {
  background: #FFF5F7;
  color: #FF2442;
}

.mobile-menu-toggle svg {
  width: 20px;
  height: 20px;
}

/* 抽屉遮罩 */
.mobile-drawer-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;
}

/* 抽屉面板 */
.mobile-drawer {
  display: none;
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 260px;
  background: #fff;
  z-index: 100;
  transform: translateX(100%);
  transition: transform 0.25s ease;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.1);
  flex-direction: column;
}

.mobile-drawer.open {
  transform: translateX(0);
}

.mobile-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.mobile-drawer-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.mobile-drawer-close {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.mobile-drawer-close:hover {
  background: #f5f5f5;
  color: #FF2442;
}

.mobile-drawer-nav {
  flex: 1;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-drawer-link {
  padding: 12px;
  border-radius: 8px;
  font-size: 15px;
  color: #1a1a1a;
  transition: all 0.2s;
}

.mobile-drawer-link:hover,
.mobile-drawer-link.active {
  background: #FFF5F7;
  color: #FF2442;
}

/* ========== 媒体查询：手机端 ≤768px ========== */

@media (max-width: 768px) {
  .navbar {
    padding: 12px 16px;
  }

  .navbar-logo {
    height: 28px;
  }

  .navbar-brand-name {
    font-size: 16px;
  }

  .navbar-links {
    gap: 12px;
  }

  .navbar-link-desktop {
    display: none;
  }

  .navbar-cta {
    padding: 10px 20px;
    font-size: 13px;
    border-radius: 20px;
  }

  .mobile-menu-toggle {
    display: flex;
  }

  .mobile-drawer-backdrop {
    display: block;
  }

  .mobile-drawer {
    display: flex;
  }
}

/* ========== 暗色主题 ========== */

body[data-theme="dark"] .navbar {
  background: #1f1f1f;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .navbar-brand-name {
  color: #e0e0e0;
}

body[data-theme="dark"] .navbar-link {
  color: #a6a6a6;
}

body[data-theme="dark"] .navbar-link:hover,
body[data-theme="dark"] .navbar-link.active {
  color: #ff4d6f;
}

body[data-theme="dark"] .navbar-cta {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .navbar-cta:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .mobile-menu-toggle {
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-menu-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme="dark"] .mobile-drawer {
  background: #1f1f1f;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .mobile-drawer-header {
  border-color: #303030;
}

body[data-theme="dark"] .mobile-drawer-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .mobile-drawer-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-drawer-close:hover {
  background: #2a2a2a;
  color: #ff4d6f;
}

body[data-theme="dark"] .mobile-drawer-link {
  color: #e0e0e0;
}

body[data-theme="dark"] .mobile-drawer-link:hover,
body[data-theme="dark"] .mobile-drawer-link.active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}
</style>