<template>
  <header class="navbar">
    <div class="navbar-brand">
      <img
        src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
        alt="爱创作"
        class="navbar-logo"
      />
      <span class="navbar-brand-name">爱创作</span>
    </div>

    <nav class="navbar-links">
      <router-link
        v-for="link in links"
        :key="link.to"
        :to="link.to"
        class="navbar-link navbar-link-desktop"
        :class="{ active: resolvedActive === link.to }"
      >{{ link.label }}</router-link>

      <button
        class="theme-toggle theme-toggle-desktop"
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
      <router-link
        v-for="link in links"
        :key="link.to"
        :to="link.to"
        class="mobile-drawer-link"
        :class="{ active: resolvedActive === link.to }"
        @click="mobileMenuOpen = false"
      >{{ link.label }}</router-link>
    </nav>
    <div class="mobile-drawer-footer">
      <button class="mobile-drawer-theme" @click="toggleTheme">
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
        <span>{{ currentTheme === 'light' ? '切换深色主题' : '切换浅色主题' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'

const THEME_KEY = 'aichuangzuo_theme'

const props = defineProps({
  links: { type: Array, required: true },
  activePath: { type: String, default: '' },
  ctaTo: { type: String, default: '/login' },
  ctaLabel: { type: String, default: '开始创作' }
})

const route = useRoute()
const mobileMenuOpen = ref(false)
const currentTheme = ref('light')
const isTransitioning = ref(false)

const resolvedActive = computed(() => props.activePath || route.path)

const toggleTheme = (event) => {
  if (isTransitioning.value) return
  const btn = event?.currentTarget
  const rect = btn?.getBoundingClientRect()
  // 兜底:若按钮不可见(例如在抽屉里),从屏幕中心扩散
  const cx = rect ? rect.left + rect.width / 2 : window.innerWidth / 2
  const cy = rect ? rect.top + rect.height / 2 : window.innerHeight / 2
  const vw = window.innerWidth
  const vh = window.innerHeight
  const maxR = Math.hypot(Math.max(cx, vw - cx), Math.max(cy, vh - cy))

  const next = currentTheme.value === 'light' ? 'dark' : 'light'
  // 多段 radial-gradient:中心 = 当前主题,过渡到中间灰,外圈 = 新主题
  // 这样扩散时边缘有可见的灰度层次,而不是硬切到纯黑/纯白
  const maskBg = next === 'dark'
    ? 'radial-gradient(circle, #f8f9fa 0%, #d8d8d8 25%, #8a8a8a 55%, #3a3a3a 80%, #141414 100%)'
    : 'radial-gradient(circle, #141414 0%, #3a3a3a 25%, #8a8a8a 55%, #d8d8d8 80%, #f8f9fa 100%)'

  const mask = document.createElement('div')
  Object.assign(mask.style, {
    position: 'fixed',
    left: cx + 'px',
    top: cy + 'px',
    width: '0px',
    height: '0px',
    borderRadius: '50%',
    background: maskBg,
    transform: 'translate(-50%, -50%)',
    zIndex: '9999',
    pointerEvents: 'none',
    willChange: 'width, height'
  })
  document.body.appendChild(mask)

  // 强制 reflow,再加 transition,确保动画从 0 开始
  void mask.offsetWidth
  mask.style.transition = 'width 0.75s cubic-bezier(0.4, 0, 0.2, 1), height 0.75s cubic-bezier(0.4, 0, 0.2, 1)'
  mask.style.width = (maxR * 2) + 'px'
  mask.style.height = (maxR * 2) + 'px'

  isTransitioning.value = true

  // 动画进行到 60% 时切换主题,圆形已大部分覆盖,新主题色从圆形内透出来
  setTimeout(() => {
    currentTheme.value = next
    document.body.setAttribute('data-theme', next)
    localStorage.setItem(THEME_KEY, next)
  }, 450)

  // 动画结束后移除遮罩,避免拦截后续交互
  setTimeout(() => {
    if (mask.parentNode) mask.parentNode.removeChild(mask)
    isTransitioning.value = false
  }, 800)
}

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
  background: #FFF5F7;
  color: #FF2442;
}

.theme-toggle svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.navbar-link-desktop,
.theme-toggle-desktop {
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

.mobile-drawer-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

.mobile-drawer-theme {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #eee;
  background: #fff;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.mobile-drawer-theme:hover {
  border-color: #FFCBD4;
  color: #FF2442;
  background: #FFF5F7;
}

.mobile-drawer-theme svg {
  width: 18px;
  height: 18px;
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

  .navbar-link-desktop,
  .theme-toggle-desktop {
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

body[data-theme="dark"] .theme-toggle {
  color: #a6a6a6;
}

body[data-theme="dark"] .theme-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
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

body[data-theme="dark"] .mobile-drawer-header,
body[data-theme="dark"] .mobile-drawer-footer {
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

body[data-theme="dark"] .mobile-drawer-theme {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-drawer-theme:hover {
  border-color: rgba(255, 77, 111, 0.5);
  color: #ff4d6f;
  background: rgba(255, 36, 66, 0.15);
}
</style>