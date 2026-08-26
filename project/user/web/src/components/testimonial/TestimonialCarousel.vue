<template>
  <section class="testimonials-section" @mouseenter="pauseAutoScroll" @mouseleave="resumeAutoScroll">
    <div class="testimonials-header reveal" data-reveal-delay="0">
      <div class="section-tag">用户真实反馈</div>
      <h2 class="testimonials-title">他们已经用 AI 跑通了自媒体流程</h2>
      <p class="testimonials-subtitle">看看真实用户如何用爱创作定位、写作、变现</p>
    </div>

    <div
      v-if="testimonials.length > 0"
      ref="scrollRef"
      class="testimonials-carousel"
      @scroll="onScroll"
    >
      <TestimonialCard
        v-for="item in testimonials"
        :key="item.id"
        :avatar-url="item.avatarUrl"
        :name="item.name"
        :title="item.title"
        :star-rating="item.starRating"
        :review-text="item.reviewText"
        class="testimonial-slide reveal"
        :data-reveal-delay="(item.id % 4) * 100"
      />
    </div>

    <div v-else class="testimonials-empty">暂无用户评价</div>

    <div v-if="testimonials.length > 1" class="testimonials-dots">
      <span
        v-for="(_, index) in dotCount"
        :key="index"
        class="testimonials-dot"
        :class="{ active: index === activeDot }"
        @click="scrollToDot(index)"
      />
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import TestimonialCard from './TestimonialCard.vue'

const props = defineProps({
  testimonials: { type: Array, default: () => [] }
})

const scrollRef = ref(null)
const activeDot = ref(0)
let autoScrollTimer = null

const slidesPerView = ref(3)

function updateSlidesPerView() {
  if (typeof window === 'undefined') return
  slidesPerView.value = window.innerWidth <= 768 ? 1 : 3
}

const slideCount = computed(() => props.testimonials.length)

const dotCount = computed(() => {
  const count = slideCount.value
  if (count <= slidesPerView.value) return 0
  return Math.max(1, count - slidesPerView.value + 1)
})

function getMetrics() {
  if (!scrollRef.value) return null
  const firstSlide = scrollRef.value.firstElementChild
  if (!firstSlide) return null
  const slideWidth = firstSlide.offsetWidth
  const style = window.getComputedStyle(scrollRef.value)
  const gap = parseFloat(style.gap) || 20
  return { slideWidth, gap, containerWidth: scrollRef.value.clientWidth, scrollWidth: scrollRef.value.scrollWidth }
}

function onScroll() {
  if (!scrollRef.value) return
  const metrics = getMetrics()
  if (!metrics) return
  const { slideWidth, gap } = metrics
  activeDot.value = Math.round(scrollRef.value.scrollLeft / (slideWidth + gap))
}

function scrollToIndex(index, behavior = 'smooth') {
  if (!scrollRef.value) return
  const metrics = getMetrics()
  if (!metrics) return
  const { slideWidth, gap } = metrics
  const targetIndex = Math.max(0, Math.min(index, slideCount.value - 1))
  scrollRef.value.scrollTo({
    left: targetIndex * (slideWidth + gap),
    behavior
  })
}

function scrollNext() {
  if (!scrollRef.value) return
  const metrics = getMetrics()
  if (!metrics) return
  const { slideWidth, gap } = metrics
  const step = slideWidth + gap
  const maxIndex = Math.max(0, slideCount.value - slidesPerView.value)
  const currentIndex = Math.round(scrollRef.value.scrollLeft / step)
  const nextIndex = currentIndex + 1

  if (nextIndex > maxIndex) {
    scrollRef.value.scrollTo({ left: 0, behavior: 'smooth' })
  } else {
    scrollRef.value.scrollTo({ left: nextIndex * step, behavior: 'smooth' })
  }
}

function startAutoScroll() {
  stopAutoScroll()
  if (slideCount.value <= slidesPerView.value) return
  autoScrollTimer = setInterval(() => {
    scrollNext()
  }, 5000)
}

function stopAutoScroll() {
  if (autoScrollTimer) {
    clearInterval(autoScrollTimer)
    autoScrollTimer = null
  }
}

function pauseAutoScroll() {
  stopAutoScroll()
}

function resumeAutoScroll() {
  startAutoScroll()
}

onMounted(() => {
  updateSlidesPerView()
  window.addEventListener('resize', updateSlidesPerView)
  nextTick(() => startAutoScroll())
})

onUnmounted(() => {
  window.removeEventListener('resize', updateSlidesPerView)
  stopAutoScroll()
})

watch(() => props.testimonials, async () => {
  activeDot.value = 0
  await nextTick()
  startAutoScroll()
}, { flush: 'post' })
</script>

<style scoped>
.testimonials-section {
  padding: 80px 48px;
  background: linear-gradient(180deg, #fff 0%, #fff8f9 50%, #fff 100%);
  overflow: hidden;
}
.testimonials-header {
  max-width: 1100px;
  margin: 0 auto 48px;
  text-align: center;
}
.testimonials-title {
  font-size: 32px;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-weight: 700;
}
.testimonials-subtitle {
  color: #595959;
  font-size: 15px;
}
.testimonials-carousel {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: stretch;
  gap: 20px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 8px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.testimonials-carousel::-webkit-scrollbar {
  display: none;
}
.testimonial-slide {
  flex: 0 0 calc((100% - 40px) / 3);
  scroll-snap-align: start;
  min-width: 280px;
  display: flex;
  align-items: stretch;
}
.testimonial-slide > * {
  flex: 1 1 auto;
  min-height: 0;
  align-self: stretch;
}
.testimonials-empty {
  text-align: center;
  color: #8c8c8c;
  padding: 48px 0;
}
.testimonials-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 28px;
}
.testimonials-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e0e0e0;
  cursor: pointer;
  transition: all 0.2s ease;
}
.testimonials-dot.active {
  background: #FF2442;
  width: 24px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .testimonials-section {
    padding: 50px 20px;
  }
  .testimonials-title {
    font-size: 24px;
  }
  .testimonials-subtitle {
    font-size: 14px;
  }
  .testimonials-carousel {
    gap: 12px;
  }
  .testimonial-slide {
    flex: 0 0 85vw;
    min-width: 260px;
  }
}

body[data-theme="dark"] .testimonials-section {
  background: linear-gradient(180deg, #141414 0%, #1a1a1a 50%, #141414 100%);
}
body[data-theme="dark"] .testimonials-title {
  color: #e0e0e0;
}
body[data-theme="dark"] .testimonials-subtitle {
  color: #a6a6a6;
}
body[data-theme="dark"] .testimonials-empty {
  color: #a6a6a6;
}
body[data-theme="dark"] .testimonials-dot {
  background: #3a3a3a;
}
body[data-theme="dark"] .testimonials-dot.active {
  background: #ff4d6f;
}
</style>
