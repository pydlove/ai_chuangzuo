<template>
  <div class="slider-captcha" :class="{ 'is-passed': passed }">
    <div ref="trackRef" class="slider-track">
      <div class="slider-fill" :style="{ width: progress + '%' }"></div>
      <span class="slider-text">
        <template v-if="!passed">
          <svg class="slider-text-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 12l2 2 4-4"/>
            <path d="M21 12c0 5-4 9-9 9s-9-4-9-9 4-9 9-9c2.5 0 4.8 1 6.5 2.7"/>
          </svg>
          拖动滑块完成验证
        </template>
        <template v-else>
          <svg class="slider-text-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          验证成功
        </template>
      </span>
      <div
        ref="handleRef"
        class="slider-handle"
        :style="{ left: progress + '%' }"
        @mousedown="onDragStart"
        @touchstart.prevent="onDragStart"
      >
        <svg v-if="!passed" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const passed = computed(() => props.modelValue)

const trackRef = ref(null)
const handleRef = ref(null)
const progress = ref(props.modelValue ? 100 : 0)
let isDragging = false
let startX = 0
let startProgress = 0

watch(passed, (val) => {
  progress.value = val ? 100 : 0
})

const onDragStart = (e) => {
  if (passed.value) return
  isDragging = true
  startX = e.clientX ?? e.touches?.[0]?.clientX ?? 0
  startProgress = progress.value
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  document.addEventListener('touchmove', onDragMove, { passive: false })
  document.addEventListener('touchend', onDragEnd)
  e.preventDefault()
}

const onDragMove = (e) => {
  if (!isDragging || !trackRef.value) return
  if (e.type.startsWith('touch')) e.preventDefault()
  const clientX = e.clientX ?? e.touches?.[0]?.clientX
  if (clientX === undefined) return
  const trackRect = trackRef.value.getBoundingClientRect()
  const handleWidth = handleRef.value?.offsetWidth || 40
  const maxDelta = trackRect.width - handleWidth
  if (maxDelta <= 0) return
  const next = startProgress + ((clientX - startX) / maxDelta) * 100
  progress.value = Math.max(0, Math.min(100, next))
}

const onDragEnd = () => {
  if (!isDragging) return
  isDragging = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('touchmove', onDragMove)
  document.removeEventListener('touchend', onDragEnd)
  if (progress.value >= 95) {
    progress.value = 100
    emit('update:modelValue', true)
  } else {
    progress.value = 0
  }
}
</script>

<style scoped>
.slider-captcha {
  width: 100%;
  user-select: none;
  -webkit-user-select: none;
}

.slider-track {
  position: relative;
  height: 42px;
  background: #f5f5f5;
  border-radius: 21px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  cursor: default;
  touch-action: none;
  -webkit-touch-callout: none;
}

.slider-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, rgba(255, 36, 66, 0.12) 0%, rgba(255, 36, 66, 0.28) 100%);
  transition: background 0.2s;
  pointer-events: none;
}

.slider-captcha.is-passed .slider-fill {
  background: linear-gradient(90deg, rgba(82, 196, 26, 0.18) 0%, rgba(82, 196, 26, 0.4) 100%);
}

.slider-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 13px;
  color: #8c8c8c;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  pointer-events: none;
  z-index: 1;
  white-space: nowrap;
}

.slider-text-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.slider-captcha.is-passed .slider-text {
  color: #52c41a;
  font-weight: 500;
}

.slider-handle {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 38px;
  height: 38px;
  background: #fff;
  border: 2px solid #FF2442;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FF2442;
  cursor: grab;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.18);
  transition: box-shadow 0.2s, transform 0.1s;
  z-index: 2;
  touch-action: none;
  -webkit-touch-callout: none;
}

.slider-handle:hover {
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.28);
}

.slider-handle:active {
  cursor: grabbing;
}

.slider-captcha.is-passed .slider-handle {
  background: #52c41a;
  border-color: #52c41a;
  color: #fff;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
  cursor: default;
}

.slider-handle svg {
  width: 18px;
  height: 18px;
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .slider-track {
  background: #262626;
  border-color: #404040;
}

body[data-theme="dark"] .slider-handle {
  background: #1f1f1f;
  border-color: #ff4d6f;
  color: #ff4d6f;
  box-shadow: 0 2px 8px rgba(255, 77, 111, 0.2);
}

body[data-theme="dark"] .slider-fill {
  background: linear-gradient(90deg, rgba(255, 77, 111, 0.12) 0%, rgba(255, 77, 111, 0.28) 100%);
}

body[data-theme="dark"] .slider-captcha.is-passed .slider-fill {
  background: linear-gradient(90deg, rgba(82, 196, 26, 0.15) 0%, rgba(82, 196, 26, 0.3) 100%);
}

body[data-theme="dark"] .slider-captcha.is-passed .slider-handle {
  background: #52c41a;
  border-color: #52c41a;
  color: #fff;
}

body[data-theme="dark"] .slider-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .slider-captcha.is-passed .slider-text {
  color: #73d13d;
}
</style>
