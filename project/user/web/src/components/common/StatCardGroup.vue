<template>
  <div
    class="stat-card-group"
    :class="{
      'stat-card-group--inline': inline,
      [`stat-card-group--${columns}`]: columns > 0
    }"
    :style="groupStyle"
  >
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  columns: { type: Number, default: 0 },
  inline: { type: Boolean, default: false },
  gap: { type: [String, Number], default: null }
})

const groupStyle = computed(() => {
  const style = {}
  if (props.gap != null) {
    style.gap = typeof props.gap === 'number' ? `${props.gap}px` : props.gap
  }
  return style
})
</script>

<style scoped>
.stat-card-group {
  display: grid;
  grid-template-columns: repeat(var(--stat-columns, 2), 1fr);
  gap: 16px;
}

.stat-card-group--inline {
  display: flex;
  flex-wrap: wrap;
}

.stat-card-group--2 {
  --stat-columns: 2;
}

.stat-card-group--3 {
  --stat-columns: 3;
}

.stat-card-group--4 {
  --stat-columns: 4;
}

.stat-card-group--5 {
  --stat-columns: 5;
}

@media (max-width: 768px) {
  .stat-card-group {
    grid-template-columns: repeat(var(--stat-columns-mobile, var(--stat-columns, 1)), 1fr);
  }
}
</style>
