<template>
  <div v-if="isMobile" class="pull-to-refresh" :class="{ 'full-page': fullPage }">
    <div
      ref="contentRef"
      class="pull-content"
      :class="{ 'is-pulling': pulling, 'is-refreshing': refreshing }"
      :style="contentStyle"
    >
      <div class="pull-indicator">
        <span class="pull-arrow" :style="arrowStyle">↓</span>
        <span class="pull-text">{{ indicatorText }}</span>
      </div>
      <div class="pull-body">
        <slot />
      </div>
    </div>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useIsMobile } from '@/composables/useMobile.js'

const props = defineProps({
  fullPage: {
    type: Boolean,
    default: false
  },
  onRefresh: {
    type: Function,
    default: null
  },
  threshold: {
    type: Number,
    default: 70
  },
  maxDistance: {
    type: Number,
    default: 120
  },
  resistance: {
    type: Number,
    default: 0.55
  }
})

const isMobile = useIsMobile()
const contentRef = ref(null)
const pulling = ref(false)
const refreshing = ref(false)
const distance = ref(0)

const arrowStyle = computed(() => ({
  transform: `rotate(${Math.min((distance.value / props.threshold) * 180, 180)}deg)`
}))

const indicatorText = computed(() => {
  if (refreshing.value) return '刷新中…'
  return distance.value >= props.threshold ? '释放刷新' : '下拉刷新'
})

const contentStyle = computed(() => {
  if (distance.value <= 0 && !refreshing.value) return {}
  return {
    transform: `translate3d(0, ${distance.value}px, 0)`
  }
})

let startY = 0
let startScrollTop = 0
let currentY = 0

function isAtTop() {
  const el = contentRef.value
  if (!el) return true
  return el.scrollTop <= 0
}

function onTouchStart(e) {
  if (refreshing.value) return
  startY = e.touches[0].clientY
  currentY = startY
  startScrollTop = isAtTop() ? 0 : contentRef.value.scrollTop
}

function onTouchMove(e) {
  if (refreshing.value) return

  currentY = e.touches[0].clientY
  const delta = currentY - startY

  // 只有从顶部向下拉才触发刷新
  if (delta > 0 && startScrollTop <= 0 && isAtTop()) {
    if (!pulling.value) pulling.value = true
    e.preventDefault()
    distance.value = Math.min(delta * props.resistance, props.maxDistance)
  }
}

function onTouchEnd() {
  if (!pulling.value) return

  if (refreshing.value) return

  if (distance.value >= props.threshold) {
    refreshing.value = true
    distance.value = props.threshold

    const refreshFn = props.onRefresh
    if (typeof refreshFn === 'function') {
      Promise.resolve(refreshFn()).finally(resetPull)
    } else {
      requestAnimationFrame(() => {
        location.reload()
      })
    }
  } else {
    resetPull()
  }
}

function onTouchCancel() {
  if (!pulling.value) return
  resetPull()
}

function resetPull() {
  pulling.value = false
  refreshing.value = false
  distance.value = 0
  startY = 0
  currentY = 0
  startScrollTop = 0
}

onMounted(() => {
  const el = contentRef.value
  if (!el) return
  el.addEventListener('touchstart', onTouchStart, { passive: true })
  el.addEventListener('touchmove', onTouchMove, { passive: false })
  el.addEventListener('touchend', onTouchEnd)
  el.addEventListener('touchcancel', onTouchCancel)
})

onUnmounted(() => {
  const el = contentRef.value
  if (!el) return
  el.removeEventListener('touchstart', onTouchStart)
  el.removeEventListener('touchmove', onTouchMove)
  el.removeEventListener('touchend', onTouchEnd)
  el.removeEventListener('touchcancel', onTouchCancel)
})
</script>

<style scoped>
.pull-to-refresh {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  overflow: hidden;
  touch-action: none;
}

.pull-to-refresh.full-page {
  height: auto;
  min-height: 100vh;
  min-height: 100dvh;
}

.pull-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
  transition: transform 0.2s ease-out;
  touch-action: pan-y;
}

.pull-content.is-pulling {
  transition: none;
}

.pull-indicator {
  height: 50px;
  margin-top: -50px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #8c8c8c;
  font-size: 13px;
  user-select: none;
  pointer-events: none;
}

.pull-arrow {
  display: inline-block;
  font-size: 14px;
  line-height: 1;
  transition: transform 0.2s;
}

.pull-body {
  flex: 1;
  min-height: 0;
}
</style>
