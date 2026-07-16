<template>
  <div class="topic-suggestion">
    <button v-if="collapsed" class="inspire-btn" @click="expand">
      💡 没灵感？试试点我
    </button>
    <div v-else>
      <div class="suggestion-status">已为你想到几个方向 👇</div>
      <div v-if="typing" class="typing-cursor"><span></span><span></span><span></span></div>
      <div v-else class="suggestion-titles">
        <div v-for="(topic, i) in topics" :key="topic.id" v-show="i < revealedCount" class="suggestion-title-card">
          <a-tooltip :title="topic.title" placement="top">
            <button class="suggestion-title" :disabled="topic.used" @click="select(topic)">
              {{ topic.title }}
            </button>
          </a-tooltip>
        </div>
        <button v-if="!expanding" class="refresh-suggestion" @click="refresh">换一批</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchRandomTopics, markTopicUsed } from '@/api/topic.js'
import { useCreateForm } from './useCreateForm.js'

const emit = defineEmits(['select'])
const { customTitle, customRequirement } = useCreateForm()

const collapsed = ref(true)
const expanding = ref(false)
const revealedCount = ref(0)
const typing = ref(false)
const topics = ref([])
let expandTimer = null
let revealTimer = null

const loadTopics = async () => {
  try {
    const list = await fetchRandomTopics(6)
    topics.value = (list || []).map(t => ({ id: t.id, title: t.title, summary: t.summary, used: false }))
  } catch {
    topics.value = []
  }
}

const startStream = () => {
  expanding.value = true
  typing.value = true
  revealedCount.value = 0
  if (revealTimer) clearInterval(revealTimer)
  if (expandTimer) clearTimeout(expandTimer)
  expandTimer = setTimeout(() => {
    typing.value = false
    revealTimer = setInterval(() => {
      revealedCount.value++
      if (revealedCount.value >= topics.value.length) {
        clearInterval(revealTimer)
        expanding.value = false
      }
    }, 150)
  }, 600)
}

const expand = () => {
  if (!collapsed.value) return
  collapsed.value = false
  startStream()
}

const refresh = async () => {
  await loadTopics()
  startStream()
}

const select = (topic) => {
  if (topic.used) return
  customTitle.value = topic.title
  customRequirement.value = topic.summary || ''
  topic.used = true
  markTopicUsed(topic.id).catch(() => {})
  emit('select', topic)
}

onMounted(loadTopics)
defineExpose({ loadTopics })
</script>

<style scoped>
.topic-suggestion { margin-top: 4px; }

.inspire-btn {
  background: var(--color-bg-card);
  border: 1px dashed var(--color-border-default);
  border-radius: 18px;
  padding: 8px 18px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.inspire-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.suggestion-status {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 10px;
}

.typing-cursor {
  display: inline-flex;
  gap: 4px;
  padding: 10px 14px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
}

.typing-cursor span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: cursor-bounce 1.2s infinite ease-in-out;
}

.typing-cursor span:nth-child(2) { animation-delay: 0.15s; }
.typing-cursor span:nth-child(3) { animation-delay: 0.3s; }

@keyframes cursor-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.suggestion-titles {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggestion-title-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  padding: 10px 14px;
  transition: all 0.2s;
}

.suggestion-title-card:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.suggestion-title {
  width: 100%;
  background: none;
  border: none;
  padding: 0;
  text-align: left;
  font-size: 14px;
  color: var(--color-text-primary);
  cursor: pointer;
  font-weight: 500;
}

.suggestion-title:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-suggestion {
  align-self: flex-start;
  margin-top: 4px;
  padding: 6px 16px;
  background: none;
  border: 1px solid var(--color-border-default);
  border-radius: 16px;
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.refresh-suggestion:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .inspire-btn {
  background: #1f1f1f;
  border-color: #434343;
  color: #a6a6a6;
}
</style>
