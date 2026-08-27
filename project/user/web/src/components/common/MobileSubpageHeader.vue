<template>
  <header class="mobile-subpage-header">
    <div
      v-if="showBack"
      class="mobile-subpage-header__back"
      @click="handleBack"
    >
      <slot name="back-icon">
        <svg
          class="mobile-subpage-header__arrow"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </slot>
      <span class="mobile-subpage-header__back-text">{{ backText }}</span>
    </div>
    <div class="mobile-subpage-header__title">{{ title }}</div>
    <div v-if="$slots.right" class="mobile-subpage-header__right">
      <slot name="right" />
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  showBack: {
    type: Boolean,
    default: true
  },
  backText: {
    type: String,
    default: '返回'
  },
  autoBack: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['back'])

const router = useRouter()

const handleBack = () => {
  emit('back')
  if (props.autoBack) {
    router.back()
  }
}
</script>

<style scoped>
.mobile-subpage-header {
  display: flex;
  align-items: center;
  justify-content: center;
  position: sticky;
  top: 0;
  z-index: 50;
  width: 100%;
  height: 48px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #f0f0f0;
  box-sizing: border-box;
}

.mobile-subpage-header__back {
  position: absolute;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.mobile-subpage-header__arrow {
  width: 20px;
  height: 20px;
}

.mobile-subpage-header__title {
  width: 100%;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 60px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  box-sizing: border-box;
}

.mobile-subpage-header__right {
  position: absolute;
  right: 12px;
  display: flex;
  align-items: center;
}

body[data-theme="dark"] .mobile-subpage-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}

body[data-theme="dark"] .mobile-subpage-header__back {
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-subpage-header__title {
  color: #e0e0e0;
}
</style>
