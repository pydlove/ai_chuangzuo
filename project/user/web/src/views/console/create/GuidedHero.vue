<template>
  <div class="guided-hero">
    <h1 class="hero-brand">爱 创 作</h1>

    <div class="hero-input-box" :class="{ active: topicInput.length > 0 }">
      <textarea
        v-model="topicInput"
        class="hero-input"
        placeholder="输入主题开始创作…"
        rows="3"
        @keydown.enter.exact.prevent="submit"
      ></textarea>
      <button class="hero-send" :disabled="!topicInput.trim()" @click="submit">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="19" x2="12" y2="5"/>
          <polyline points="5 12 12 5 19 12"/>
        </svg>
      </button>
    </div>

    <!-- 灵感入口：折叠态 / 思考中 / 推荐结果 -->
    <div class="hero-inspire" v-if="inspireExpanded">
      <div v-if="loadingInspire" class="hero-inspire-loading">
        <span class="hero-inspire-text">小爱正在帮您思考选题灵感</span>
        <span class="hero-dots"><span></span><span></span><span></span></span>
      </div>
      <div v-else class="hero-inspire-result">
        <div class="hero-inspire-status">小爱帮你推荐了 {{ topics.length }} 个标题，请您参考</div>
        <div class="hero-topics">
          <button
            v-for="(t, i) in topics"
            :key="t.id"
            v-show="i < revealedCount"
            class="hero-topic"
            @click="applyTopic(t)"
          >
            {{ t.title }}
          </button>
        </div>
        <button class="hero-refresh" @click="refreshInspire">换一批</button>
      </div>
    </div>
    <button v-else class="hero-inspire-btn" @click="expandInspire">
      💡 没灵感？试试点我
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { fetchRandomTopics, markTopicUsed } from '@/api/topic.js'

const emit = defineEmits(['submit'])
const topicInput = ref('')

const submit = () => {
  const text = topicInput.value.trim()
  if (!text) return
  emit('submit', text)
}

// ===== 灵感气泡：折叠 → 思考中 → 推荐结果（流式展开） =====
const inspireExpanded = ref(false)
const loadingInspire = ref(false)
const topics = ref([])
const revealedCount = ref(0)
let expandTimer = null
let revealTimer = null

const loadTopics = async () => {
  try {
    const list = await fetchRandomTopics(6)
    topics.value = (list || []).map(t => ({ id: t.id, title: t.title }))
  } catch {
    topics.value = []
  }
}

const startStream = async () => {
  loadingInspire.value = true
  revealedCount.value = 0
  if (revealTimer) clearInterval(revealTimer)
  if (expandTimer) clearTimeout(expandTimer)

  await loadTopics()

  expandTimer = setTimeout(() => {
    loadingInspire.value = false
    revealTimer = setInterval(() => {
      revealedCount.value++
      if (revealedCount.value >= topics.value.length) {
        clearInterval(revealTimer)
      }
    }, 150)
  }, 600)
}

const expandInspire = () => {
  if (inspireExpanded.value) return
  inspireExpanded.value = true
  startStream()
}

const refreshInspire = () => {
  startStream()
}

const applyTopic = (t) => {
  topicInput.value = t.title
  markTopicUsed(t.id).catch(() => {})
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
  gap: 28px;
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
  height: 130px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 22px;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.hero-input-box:focus-within,
.hero-input-box.active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(255, 36, 66, 0.08);
}

.hero-input {
  flex: 1;
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  font-family: inherit;
  font-size: 16px;
  line-height: 1.6;
  color: var(--color-text-primary);
  padding: 0;
  margin: 0;
}

.hero-input::placeholder {
  color: var(--color-text-placeholder);
}

.hero-send {
  align-self: flex-end;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
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

/* ===== 灵感入口 ===== */
.hero-inspire-btn {
  border: 1px dashed var(--color-border-default);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 9px 22px;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.hero-inspire-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.hero-inspire {
  width: min(720px, 100%);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.hero-inspire-loading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 18px;
  padding: 10px 18px;
}

.hero-inspire-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.hero-dots {
  display: inline-flex;
  gap: 3px;
}

.hero-dots span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: hero-dot-bounce 1.2s infinite ease-in-out;
}

.hero-dots span:nth-child(2) { animation-delay: 0.15s; }
.hero-dots span:nth-child(3) { animation-delay: 0.3s; }

@keyframes hero-dot-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-3px); opacity: 1; }
}

.hero-inspire-status {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.hero-topics {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.hero-topic {
  width: 100%;
  text-align: left;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  padding: 12px 18px;
  font-size: 14px;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.hero-topic:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.hero-refresh {
  align-self: flex-start;
  margin-top: 4px;
  padding: 6px 18px;
  background: none;
  border: 1px solid var(--color-border-default);
  border-radius: 16px;
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.hero-refresh:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .hero-brand {
  color: #f5f5f5;
}

body[data-theme="dark"] .hero-input-box,
body[data-theme="dark"] .hero-inspire-loading,
body[data-theme="dark"] .hero-topic,
body[data-theme="dark"] .hero-inspire-btn {
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
    gap: 24px;
    min-height: calc(100vh - 140px);
  }
}
</style>