<template>
  <div v-if="topics.length > 0 || refreshing" class="topic-capsules">
    <span class="topic-capsules-label">没灵感？点一个快速开始：</span>
    <div v-if="refreshing" class="topic-capsules-loading">
      <span class="topic-loading-text">
        <span
          v-for="(ch, i) in loadingChars"
          :key="i"
          class="topic-loading-char"
          :style="{ animationDelay: (i * 0.08) + 's' }"
        >{{ ch }}</span>
      </span>
      <span class="topic-dots"><span></span><span></span><span></span></span>
    </div>
    <div v-else class="topic-capsules-grid">
      <a-tooltip
        v-for="topic in topics"
        :key="topic.id"
        :title="topic.title"
        placement="top"
      >
        <button
          :class="['topic-capsule', { used: isUsed(topic) }]"
          :disabled="isUsed(topic)"
          @click="isUsed(topic) ? null : applyTopic(topic)"
        >
          {{ topic.title }}
        </button>
      </a-tooltip>
    </div>
    <button class="refresh-capsule" :disabled="refreshing" @click="refreshTopics">
      {{ refreshing ? '思考中…' : '换一批' }}
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { fetchRandomTopics, markTopicUsed } from '@/api/topic.js'
import { useCreateForm } from './useCreateForm.js'
import { useGenerationQueue } from './useGenerationQueue.js'

const emit = defineEmits(['apply'])
const { customTitle, customRequirement } = useCreateForm()
const { queueList } = useGenerationQueue()

const topics = ref([])
const refreshing = ref(false)
const usedIds = ref(new Set())
const lastAppliedId = ref(null)
const lastAppliedTaskId = ref(null)
// 至少给用户一段"思考"时间，模拟大模型流式感，同时防止疯狂点击打后端
const MIN_THINK_MS = 3000

// 拆成单字，每个字独立律动形成"波浪"
const loadingText = '灵犀同学正在帮你想新灵感'
const loadingChars = loadingText.split('')

const loadTopics = async () => {
  try {
    const list = await fetchRandomTopics(6)
    topics.value = (list || []).map(t => ({ id: t.id, title: t.title, summary: t.summary }))
  } catch {
    topics.value = []
  }
}

onMounted(loadTopics)

const isUsed = (topic) => usedIds.value.has(topic.id)

const applyTopic = (topic) => {
  customTitle.value = topic.title
  customRequirement.value = topic.summary || ''
  lastAppliedId.value = topic.id
  lastAppliedTaskId.value = null
  emit('apply', topic)
}

const markUsed = (taskId) => {
  const id = lastAppliedId.value
  if (!id) return
  lastAppliedTaskId.value = taskId || null
  if (usedIds.value.has(id)) return
  usedIds.value.add(id)
  markTopicUsed(id).catch(() => {})
}

const unmarkUsed = () => {
  const id = lastAppliedId.value
  if (!id) return
  usedIds.value.delete(id)
  lastAppliedTaskId.value = null
}

// 监听关联任务状态：停止/失败时回滚，重新生成时恢复
watch(queueList, (list) => {
  const taskId = lastAppliedTaskId.value
  if (!taskId) return
  const task = list.find(t => t.id === taskId)
  if (!task) return
  const id = lastAppliedId.value
  if (!id) return
  if (task.status === 'cancelled' || task.status === 'failed') {
    if (usedIds.value.has(id)) {
      usedIds.value.delete(id)
    }
  } else if (task.status === 'queued' || task.status === 'generating') {
    if (!usedIds.value.has(id)) {
      usedIds.value.add(id)
    }
  }
}, { deep: true })

const refreshTopics = () => {
  if (refreshing.value) return // 已经在思考中，忽略重复点击
  refreshing.value = true
  // 取 fetch 与最少思考时长的较长者，确保动效稳定出现
  Promise.all([
    loadTopics(),
    new Promise(resolve => setTimeout(resolve, MIN_THINK_MS))
  ]).finally(() => {
    refreshing.value = false
  })
}

defineExpose({ loadTopics, markUsed })
</script>

<style scoped>
.topic-capsules {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
  margin-bottom: 16px;
  flex-shrink: 0;
  min-width: 0;
}

.topic-capsules-label {
  font-size: 13px;
  color: #595959;
  flex-shrink: 0;
  line-height: 20px;
}

.topic-capsules-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.topic-capsules-grid > * {
  display: flex;
  min-width: 0;
}

.topic-capsule {
  width: 100%;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  font-size: 12px;
  color: #595959;
  cursor: pointer;
  transition: all 0.15s;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  box-sizing: border-box;
  text-align: left;
}

.topic-capsule:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.topic-capsule.used {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-capsule {
  align-self: flex-start;
  padding: 6px 16px;
  background: none;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  font-size: 12px;
  color: #8c8c8c;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  margin-top: 4px;
}

.refresh-capsule:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.refresh-capsule:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  border-color: var(--color-border-light);
  color: var(--color-text-secondary);
}

.topic-capsules-loading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 16px;
  padding: 14px 18px;
  min-height: 76px; /* 与两行 capsule 网格等高，避免布局跳动 */
  box-sizing: border-box;
}

.topic-loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  display: inline-block;
}

.topic-loading-char {
  display: inline-block;
  animation: topic-char-wave 1.4s infinite ease-in-out;
}

@keyframes topic-char-wave {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-4px); }
}

.topic-dots {
  display: inline-flex;
  gap: 3px;
  align-items: center;
}

.topic-dots span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: topic-dot-bounce 1.2s infinite ease-in-out;
}

.topic-dots span:nth-child(2) { animation-delay: 0.15s; }
.topic-dots span:nth-child(3) { animation-delay: 0.3s; }

@keyframes topic-dot-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-3px); opacity: 1; }
}

@media (max-width: 768px) {
  .refresh-capsule {
    align-self: center;
  }
}

body[data-theme="dark"] .topic-capsule {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .topic-capsule:hover {
  background: #333;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .topic-capsules-loading {
  background: #1f1f1f;
  border-color: #2e2e2e;
}
</style>
