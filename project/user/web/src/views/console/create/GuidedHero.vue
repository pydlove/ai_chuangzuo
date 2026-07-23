<template>
  <div class="guided-hero">
    <div class="hero-header">
      <div class="hero-title-row">
        <img class="hero-avatar" src="/ai-avatar.png" alt="AI" />
        <h1 class="hero-brand">灵犀AI智能体</h1>
      </div>
      <div class="hero-slogan">{{ heroSlogan }}</div>
    </div>

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
        <span class="hero-inspire-text">灵犀同学正在帮您思考选题灵感</span>
        <span class="hero-dots"><span></span><span></span><span></span></span>
      </div>
      <div v-else class="hero-inspire-result">
        <div class="hero-inspire-status">灵犀同学帮你推荐了 {{ topics.length }} 个标题，请您参考</div>
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
      <svg class="inspire-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M9 18h6"/>
        <path d="M10 22h4"/>
        <path d="M15.09 14c.18-.9.27-1.48.27-2.26A5.24 5.24 0 0 0 10 6.5 5.24 5.24 0 0 0 4.64 11.74c0 .78.09 1.36.27 2.26"/>
        <path d="M12 2v2"/>
        <path d="M4.22 4.22l1.42 1.42"/>
        <path d="M19.78 4.22l-1.42 1.42"/>
      </svg>
      <span>没灵感？试试点我</span>
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { fetchRandomTopics, markTopicUsed } from '@/api/topic.js'
import { useCreateForm } from './useCreateForm.js'

const emit = defineEmits(['submit'])
const topicInput = ref('')
const heroSlogan = ref('输入一个主题，AI 自动帮你写选题、写正文、排排版')
const { customTitle, customRequirement } = useCreateForm()

const submit = () => {
  const text = topicInput.value.trim()
  if (!text) return
  // 带上已选灵感的 summary（如果有）
  customTitle.value = text
  // requirement 由 applyTopic 设置；如果是手动敲字没点灵感，留空
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
    topics.value = (list || []).map(t => ({ id: t.id, title: t.title, summary: t.summary }))
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
  // 把标题+观点一起回填（不仅标题），后续进入主题步骤时 description 会预填
  topicInput.value = t.title
  customTitle.value = t.title
  customRequirement.value = t.summary || ''
  markTopicUsed(t.id).catch(() => {})
}
</script>

<style scoped>
.guided-hero {
  min-height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  padding: 24px;
  gap: 28px;
}

.hero-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  align-self: center;
}

.hero-title-row {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.hero-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.hero-brand {
  margin: 0;
  font-size: 36px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 4px;
  text-align: left;
}

.hero-slogan {
  font-size: 14px;
  color: var(--color-text-secondary);
  text-align: center;
  line-height: 1.6;
  max-width: 420px;
}

.hero-input-box {
  width: min(720px, 100%);
  align-self: center;
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
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px dashed var(--color-border-default);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 9px 22px;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.2s;
  align-self: flex-start;
  margin-left: calc((100% - min(720px, 100%)) / 2);
}

@media (max-width: 768px) {
  .hero-inspire-btn {
    margin-left: 0;
  }
}

.hero-inspire-btn .inspire-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
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
  align-self: flex-start;
  gap: 12px;
  margin-left: calc((100% - min(720px, 100%)) / 2);
}

@media (max-width: 768px) {
  .hero-inspire {
    margin-left: 0;
  }
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
    font-size: 32px;
    letter-spacing: 4px;
  }
  .hero-avatar {
    width: 48px;
    height: 48px;
  }
  .hero-slogan {
    font-size: 13px;
    max-width: 320px;
  }
  .guided-hero {
    gap: 24px;
    min-height: calc(100vh - 140px);
  }
}
</style>