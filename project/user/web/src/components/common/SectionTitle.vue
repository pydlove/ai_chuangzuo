<template>
  <div
    class="section-title-root"
    :class="{
      'section-title-root--bar': bar,
      'section-title-root--pill': pill,
      'section-title-root--centered': centered,
      'section-title-root--with-right': $slots.right,
      'section-title-root--disabled': disabled
    }"
    :style="rootStyle"
  >
    <div v-if="pill || tag || $slots.tag" class="section-title-tag">
      <slot name="tag">{{ tag }}</slot>
    </div>
    <div v-if="bar" class="section-title-bar" />
    <div class="section-title-body">
      <div class="section-title-main">
        <h2 v-if="title" class="section-title-text" :class="titleSizeClass">
          {{ title }}
        </h2>
        <span v-if="subtitle" class="section-title-sub">{{ subtitle }}</span>
        <slot />
      </div>
      <div v-if="$slots.right" class="section-title-right">
        <slot name="right" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  tag: { type: String, default: '' },
  bar: { type: Boolean, default: false },
  pill: { type: Boolean, default: false },
  centered: { type: Boolean, default: false },
  size: { type: String, default: 'md' },
  disabled: { type: Boolean, default: false },
  marginBottom: { type: [String, Number], default: null }
})

const titleSizeClass = computed(() => {
  const map = { sm: 'section-title-text--sm', md: 'section-title-text--md', lg: 'section-title-text--lg' }
  return map[props.size] || map.md
})

const rootStyle = computed(() => {
  const style = {}
  if (props.marginBottom != null) {
    style.marginBottom = typeof props.marginBottom === 'number' ? `${props.marginBottom}px` : props.marginBottom
  }
  return style
})
</script>

<style scoped>
.section-title-root {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.section-title-root--centered {
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0;
}

.section-title-root--with-right,
.section-title-root--bar {
  align-items: center;
}

.section-title-root--pill {
  align-items: center;
  gap: 10px;
}

.section-title-bar {
  width: 4px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 2px;
  flex-shrink: 0;
}

.section-title-tag {
  flex-shrink: 0;
}

.section-title-root--pill .section-title-tag {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #fff;
  background: linear-gradient(135deg, #ff2442 0%, #ff6b81 100%);
  padding: 4px 10px;
  border-radius: 6px;
}

.section-title-root--centered .section-title-tag {
  display: inline-block;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 16px;
  border-radius: 20px;
  margin-bottom: 12px;
  letter-spacing: 0.02em;
}

.section-title-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  min-width: 0;
}

.section-title-root--centered .section-title-body {
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.section-title-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
  margin-bottom: 10px;
}

.section-title-root--centered .section-title-main {
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.section-title-text {
  margin: 0;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
}

.section-title-text--sm { font-size: 15px; }
.section-title-text--md { font-size: 17px; }
.section-title-text--lg { font-size: 20px; }

.section-title-root--bar .section-title-text {
  font-size: 20px;
}

.section-title-root--centered .section-title-text {
  font-size: 32px;
  margin-bottom: 12px;
}

.section-title-sub {
  color: #8c8c8c;
  font-size: 13px;
  font-weight: 400;
  white-space: nowrap;
}

.section-title-root--centered .section-title-sub {
  font-size: 15px;
  color: #595959;
}

.section-title-right {
  flex-shrink: 0;
  margin-left: auto;
}

.section-title-root--disabled {
  opacity: 0.6;
}

@media (max-width: 768px) {
  .section-title-root--centered .section-title-text {
    font-size: 24px;
  }
  .section-title-root--centered .section-title-sub {
    font-size: 14px;
  }
}

body[data-theme="dark"] .section-title-text {
  color: #f0f0f0;
}

body[data-theme="dark"] .section-title-sub {
  color: #a6a6a6;
}

body[data-theme="dark"] .section-title-root--centered .section-title-sub {
  color: #a6a6a6;
}

body[data-theme="dark"] .section-title-root--centered .section-title-tag {
  background: linear-gradient(135deg, rgba(255, 36, 66, 0.15), rgba(255, 107, 138, 0.15));
  color: #ff6b8a;
}

body[data-theme="dark"] .section-title-bar {
  background: var(--color-primary);
}
</style>
