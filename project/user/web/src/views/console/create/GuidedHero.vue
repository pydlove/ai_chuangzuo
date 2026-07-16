<template>
  <div class="guided-hero">
    <h1 class="hero-brand">爱 创 作</h1>

    <div class="hero-input-box" :class="{ active: topicInput.length > 0 }">
      <input
        v-model="topicInput"
        type="text"
        class="hero-input"
        placeholder="输入主题开始创作…"
        @keyup.enter="submit"
      />
      <button class="hero-send" :disabled="!topicInput.trim()" @click="submit">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="19" x2="12" y2="5"/>
          <polyline points="5 12 12 5 19 12"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['submit'])
const topicInput = ref('')

const submit = () => {
  const text = topicInput.value.trim()
  if (!text) return
  emit('submit', text)
}
</script>

<style scoped>
.guided-hero {
  min-height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  gap: 48px;
}

.hero-brand {
  margin: 0;
  font-size: 64px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 8px;
  text-align: center;
}

.hero-input-box {
  width: min(720px, 100%);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 22px;
  padding: 14px 14px 14px 22px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.hero-input-box:focus-within,
.hero-input-box.active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(255, 36, 66, 0.08);
}

.hero-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 16px;
  color: var(--color-text-primary);
  padding: 6px 0;
}

.hero-input::placeholder {
  color: var(--color-text-placeholder);
}

.hero-send {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: var(--color-bg-page);
  color: var(--color-text-placeholder);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.hero-send:not(:disabled):hover {
  background: var(--color-primary);
  color: #fff;
}

.hero-send:not(:disabled) {
  background: var(--color-primary-light);
  color: var(--color-primary);
  cursor: pointer;
}

body[data-theme="dark"] .hero-brand {
  color: #f5f5f5;
}

body[data-theme="dark"] .hero-input-box {
  background: #1f1f1f;
  border-color: #2e2e2e;
}

body[data-theme="dark"] .hero-send {
  background: #2a2a2a;
  color: #6e6e6e;
}

body[data-theme="dark"] .hero-send:not(:disabled) {
  background: rgba(255, 36, 66, 0.18);
  color: #ff6b81;
}

@media (max-width: 768px) {
  .hero-brand {
    font-size: 44px;
    letter-spacing: 4px;
  }
  .guided-hero {
    gap: 36px;
    min-height: calc(100vh - 140px);
  }
}
</style>