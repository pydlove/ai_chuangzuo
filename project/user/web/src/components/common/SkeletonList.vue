<template>
  <div
    class="skeleton-list"
    :class="{
      [`skeleton-list--${type}`]: true,
      'skeleton-list--active': active
    }"
    :style="listStyle"
  >
    <template v-if="type === 'card'">
      <div
        v-for="i in rows"
        :key="i"
        class="skeleton-list__item skeleton-list__card"
      >
        <a-skeleton active :paragraph="{ rows: paragraphRows }" :title="false" />
      </div>
    </template>

    <template v-else>
      <div
        v-for="i in rows"
        :key="i"
        class="skeleton-list__item skeleton-list__row"
      >
        <div
          v-if="avatar"
          class="skeleton-list__avatar"
          :class="{ 'skeleton-list__avatar--round': avatarRound }"
        />
        <div class="skeleton-list__lines">
          <div
            v-for="(line, idx) in lineItems"
            :key="idx"
            class="skeleton-list__line"
            :style="{ width: line.width, height: line.height }"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'card',
    validator: (v) => ['card', 'list'].includes(v)
  },
  rows: { type: Number, default: 3 },
  gap: { type: [String, Number], default: null },
  paragraphRows: { type: Number, default: 3 },
  avatar: { type: Boolean, default: true },
  avatarRound: { type: Boolean, default: false },
  lines: { type: Number, default: 2 },
  lineWidths: { type: Array, default: null },
  lineHeights: { type: Array, default: null },
  active: { type: Boolean, default: false }
})

const listStyle = computed(() => {
  const style = {}
  if (props.gap != null) {
    style.gap = typeof props.gap === 'number' ? `${props.gap}px` : props.gap
  }
  return style
})

const defaultLineWidths = ['40%', '70%', '55%', '80%']
const defaultLineHeights = ['15px', '12px', '12px', '12px']

const lineItems = computed(() => {
  return Array.from({ length: props.lines }, (_, idx) => ({
    width: props.lineWidths?.[idx] ?? defaultLineWidths[idx % defaultLineWidths.length],
    height: props.lineHeights?.[idx] ?? defaultLineHeights[idx % defaultLineHeights.length]
  }))
})
</script>

<style scoped>
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-list__item {
  background: #fff;
}

.skeleton-list__card {
  border-radius: 12px;
  padding: 16px;
}

.skeleton-list__row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
}

.skeleton-list__avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #f5f5f5;
  flex-shrink: 0;
}

.skeleton-list__avatar--round {
  border-radius: 50%;
}

.skeleton-list__lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-list__line {
  background: #f5f5f5;
  border-radius: 4px;
  max-width: 100%;
}

.skeleton-list--active .skeleton-list__avatar,
.skeleton-list--active .skeleton-list__line {
  animation: skeleton-pulse 1.6s ease-in-out infinite;
}

@keyframes skeleton-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}

body[data-theme="dark"] .skeleton-list__item {
  background: #1f1f1f;
}

body[data-theme="dark"] .skeleton-list__avatar,
body[data-theme="dark"] .skeleton-list__line {
  background: #262626;
}

body[data-theme="dark"] .skeleton-list__card :deep(.ant-skeleton) {
  background: transparent;
}
</style>
