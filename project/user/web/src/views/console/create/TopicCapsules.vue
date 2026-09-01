<template>
  <div v-if="topics.length > 0 || refreshing || isTopicInspirationLocked" class="topic-capsules">
    <div class="topic-capsules-header">
      <span class="topic-capsules-label">{{ isMobile ? '没灵感？试试这些' : '没灵感？点一个快速开始：' }}</span>
      <button v-if="!isTopicInspirationLocked" class="refresh-capsule" :disabled="refreshing" @click="refreshTopics">
        {{ refreshing ? '思考中…' : (isMobile ? '换一换' : '换一批') }}
      </button>
    </div>
    <div v-if="isTopicInspirationLocked" class="topic-capsules-locked" @click="handleLockedClick">
      <div class="topic-capsules-locked-badge pro">专业版</div>
      <span class="topic-capsules-locked-text">AI 选题灵感至少需要专业版</span>
    </div>
    <div v-else-if="refreshing" class="topic-capsules-loading">
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
      <template v-for="topic in topics" :key="topic.id">
        <a-tooltip
          v-if="!isMobile"
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
        <button
          v-else
          :class="['topic-capsule topic-capsule--mobile', { used: isUsed(topic) }]"
          :disabled="isUsed(topic)"
          @click="isUsed(topic) ? null : applyTopic(topic)"
        >
          <svg class="topic-capsule-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
          {{ topic.title }}
        </button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { fetchRandomTopics, markTopicUsed } from '@/api/topic.js'
import { useCreateForm } from './useCreateForm.js'
import { useGenerationQueue } from './useGenerationQueue.js'
import { useDevice } from '@/composables/useDevice.js'
import { useBenefits } from '@/composables/useBenefits.js'

const emit = defineEmits(['apply'])
const { customTitle, customRequirement } = useCreateForm()
const { queueList } = useGenerationQueue()
const { isMobile } = useDevice()
const { planKey } = useBenefits()

const topics = ref([])
const refreshing = ref(false)
const usedIds = ref(new Set())
const lastAppliedId = ref(null)
const lastAppliedTaskId = ref(null)
// 至少给用户一段"思考"时间，模拟大模型流式感，同时防止疯狂点击打后端
const MIN_THINK_MS = 3000

const isTopicInspirationLocked = computed(() => planKey.value !== 'pro' && planKey.value !== 'flagship')
const topicInspirationBadge = computed(() => {
  if (!isTopicInspirationLocked.value) return null
  return { text: '专业版', tier: 'pro' }
})

// 拆成单字，每个字独立律动形成"波浪"
const loadingText = '小爱正在帮你想新灵感'
const loadingChars = loadingText.split('')

const loadTopics = async () => {
  if (isTopicInspirationLocked.value) return
  try {
    // 手机端一行一个，最多显示三行，因此只取 3 条；桌面端取 6 条
    const count = isMobile.value ? 3 : 6
    const list = await fetchRandomTopics(count)
    topics.value = (list || []).slice(0, count).map(t => ({ id: t.id, title: t.title, summary: t.summary }))
  } catch {
    topics.value = []
  }
}

onMounted(() => {
  if (isTopicInspirationLocked.value) return
  loadTopics()
})

// 父组件 CreateIndex 的 loadBenefits 是异步的，子组件 onMounted 时 planKey 可能还是默认值。
// 监听锁状态：一旦权益加载完成并解锁，且当前没有数据，就补一次加载。
watch(isTopicInspirationLocked, (locked) => {
  if (!locked && topics.value.length === 0 && !refreshing.value) {
    loadTopics()
  }
})

const isUsed = (topic) => usedIds.value.has(topic.id)

const applyTopic = (topic) => {
  if (isTopicInspirationLocked.value) return
  customTitle.value = topic.title
  customRequirement.value = topic.summary || ''
  lastAppliedId.value = topic.id
  lastAppliedTaskId.value = null
  emit('apply', topic)
}

const handleLockedClick = () => {
  message.info(`AI 选题灵感需要 ${topicInspirationBadge.value?.text || '更高套餐'}，请升级套餐后使用`)
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
  if (isTopicInspirationLocked.value) {
    handleLockedClick()
    return
  }
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

.topic-capsules-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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

.topic-capsules-locked {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  background: #f5f5f5;
  border: 1px dashed #d9d9d9;
  border-radius: 16px;
  padding: 14px 18px;
  min-height: 44px;
  box-sizing: border-box;
  cursor: not-allowed;
}

.topic-capsules-locked-badge {
  position: absolute;
  top: -7px;
  right: -6px;
  z-index: 10;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
  pointer-events: none;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

.topic-capsules-locked-badge.pro {
  color: #874d00;
  background: linear-gradient(135deg, #fff1b8, #ffd666);
}

.topic-capsules-locked-badge.flagship {
  color: #fff;
  background: linear-gradient(135deg, #a05013, #db3708);
  box-shadow: 0 2px 6px rgba(219, 55, 8, 0.45);
  z-index: 11;
}

.topic-capsules-locked-text {
  font-size: 13px;
  color: #8c8c8c;
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
  .topic-capsules {
    gap: 10px;
    margin-top: 10px;
    margin-bottom: 10px;
  }

  .topic-capsules-label {
    font-size: 12px;
    color: #8c8c8c;
  }

  .refresh-capsule {
    align-self: center;
    margin-top: 0;
    padding: 4px 10px;
    font-size: 12px;
    border-radius: 12px;
  }

  .topic-capsules {
    gap: 8px;
    margin-top: 10px;
    margin-bottom: 10px;
  }

  .topic-capsules-label {
    font-size: 12px;
  }

  .topic-capsules-grid {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .topic-capsule--mobile {
    width: 100%;
    padding: 7px 10px;
    border-radius: 12px;
    font-size: 12px;
    color: #595959;
    background: #fff;
    border: 1px solid #f0f0f0;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
    display: inline-flex;
    align-items: center;
    gap: 5px;
    text-align: left;
    box-sizing: border-box;
  }

  .topic-capsule--mobile:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
    background: #fff0f2;
  }

  .topic-capsule-icon {
    flex-shrink: 0;
    width: 12px;
    height: 12px;
    color: var(--color-primary);
    opacity: 0.8;
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

body[data-theme="dark"] .topic-capsules-locked {
  background: #2a2a2a;
  border-color: #434343;
}

body[data-theme="dark"] .topic-capsules-locked-text {
  color: #a6a6a6;
}
</style>
