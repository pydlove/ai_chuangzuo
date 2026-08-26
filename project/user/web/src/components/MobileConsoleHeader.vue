<template>
  <header class="mobile-console-header">
    <router-link to="/" class="mch-brand">
      <img
        :src="logoUrl"
        alt="爱创作"
        class="mch-brand__logo"
      />
      <span class="mch-brand__name">爱创作</span>
    </router-link>
    <div class="mch-actions">
      <button class="mch-btn mch-btn--icon" aria-label="返回" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12" />
          <polyline points="12 19 5 12 12 5" />
        </svg>
      </button>
      <button class="mch-btn mch-btn--icon" aria-label="菜单" @click="menuOpen = true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="3" y1="6" x2="21" y2="6" />
          <line x1="3" y1="12" x2="21" y2="12" />
          <line x1="3" y1="18" x2="21" y2="18" />
        </svg>
      </button>
      <router-link to="/console/workbench" class="mch-cta">开始创作</router-link>
    </div>
  </header>

  <!-- 菜单抽屉 -->
  <div v-if="menuOpen" class="mch-menu-backdrop" @click="menuOpen = false"></div>
  <div :class="['mch-menu', { open: menuOpen }]">
    <div class="mch-menu__header">
      <span class="mch-menu__title">菜单</span>
      <button class="mch-menu__close" aria-label="关闭" @click="menuOpen = false">×</button>
    </div>
    <nav class="mch-menu__nav">
      <router-link
        v-for="link in navLinks"
        :key="link.to"
        :to="link.to"
        class="mch-menu__link"
        :class="{ active: route.path === link.to }"
        @click="menuOpen = false"
      >{{ link.label }}</router-link>
    </nav>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const props = defineProps({
  logoUrl: {
    type: String,
    default: 'https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png'
  }
})

const menuOpen = ref(false)

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/lottery', label: '活动' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' }
]

function goBack() {
  router.back()
}
</script>

<style scoped>
.mobile-console-header {
  display: none;
}

@media (max-width: 768px) {
  .mobile-console-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: sticky;
    top: 0;
    z-index: 50;
    padding: 12px 16px;
    background: rgba(255, 255, 255, 0.96);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid #f0f0f0;
    margin: -12px -12px 12px;
  }

  .mch-brand {
    display: flex;
    align-items: center;
    gap: 8px;
    text-decoration: none;
  }

  .mch-brand__logo {
    height: 28px;
    width: auto;
    max-width: 40px;
    object-fit: contain;
    border-radius: 6px;
  }

  .mch-brand__name {
    font-size: 16px;
    font-weight: 700;
    color: #1a1a1a;
  }

  .mch-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .mch-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    background: transparent;
    cursor: pointer;
  }

  .mch-btn--icon {
    width: 34px;
    height: 34px;
    border-radius: 8px;
    border: 1px solid #f0f0f0;
    background: #fff;
    color: #595959;
  }

  .mch-btn--icon svg {
    width: 18px;
    height: 18px;
  }

  .mch-cta {
    padding: 8px 18px;
    background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
    color: #fff;
    border-radius: 18px;
    font-size: 13px;
    font-weight: 600;
    text-decoration: none;
  }

  /* 菜单抽屉 */
  .mch-menu-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.3);
    z-index: 80;
  }

  .mch-menu {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    width: 240px;
    background: #fff;
    z-index: 90;
    transform: translateX(100%);
    transition: transform 0.25s ease;
    display: flex;
    flex-direction: column;
  }

  .mch-menu.open {
    transform: translateX(0);
  }

  .mch-menu__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-bottom: 1px solid #f0f0f0;
  }

  .mch-menu__title {
    font-size: 16px;
    font-weight: 700;
    color: #1a1a1a;
  }

  .mch-menu__close {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    border: none;
    background: #f5f5f5;
    color: #595959;
    font-size: 20px;
    line-height: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
  }

  .mch-menu__nav {
    padding: 12px 16px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .mch-menu__link {
    padding: 12px;
    border-radius: 8px;
    font-size: 15px;
    color: #1a1a1a;
    text-decoration: none;
  }

  .mch-menu__link.active,
  .mch-menu__link:active {
    background: #FFF5F7;
    color: #FF2442;
  }
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-console-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}

body[data-theme="dark"] .mch-brand__name {
  color: #e0e0e0;
}

body[data-theme="dark"] .mch-btn--icon {
  background: #1f1f1f;
  border-color: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .mch-menu {
  background: #1f1f1f;
}

body[data-theme="dark"] .mch-menu__header {
  border-color: #2a2a2a;
}

body[data-theme="dark"] .mch-menu__title {
  color: #e0e0e0;
}

body[data-theme="dark"] .mch-menu__close {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .mch-menu__link {
  color: #e0e0e0;
}

body[data-theme="dark"] .mch-menu__link.active,
body[data-theme="dark"] .mch-menu__link:active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}
</style>
