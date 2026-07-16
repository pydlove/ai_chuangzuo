<template>
  <div v-if="topics.length > 0" class="topic-capsules">
    <span class="topic-capsules-label">没灵感？点一个快速开始：</span>
    <div class="topic-capsules-grid">
      <a-tooltip
        v-for="topic in topics"
        :key="topic.id"
        :title="topic.title"
        placement="top"
      >
        <button
          :class="['topic-capsule', { used: topic.used }]"
          :disabled="topic.used"
          @click="topic.used ? null : applyTopic(topic)"
        >
          {{ topic.title }}
        </button>
      </a-tooltip>
    </div>
    <button class="refresh-capsule" @click="refreshTopics">换一批</button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchRandomTopics, markTopicUsed } from '@/api/topic.js'
import { useCreateForm } from './useCreateForm.js'

const emit = defineEmits(['apply'])
const { customTitle, customRequirement } = useCreateForm()

const topics = ref([])

const loadTopics = async () => {
  try {
    const list = await fetchRandomTopics(6)
    topics.value = (list || []).map(t => ({ id: t.id, title: t.title, summary: t.summary, used: false }))
  } catch {
    topics.value = []
  }
}

onMounted(loadTopics)

const applyTopic = (topic) => {
  customTitle.value = topic.title
  customRequirement.value = topic.summary || ''
  topic.used = true
  markTopicUsed(topic.id).catch(() => {})
  emit('apply', topic)
}

const refreshTopics = () => {
  loadTopics()
}

defineExpose({ loadTopics })
</script>

<style scoped>
.topic-capsules {
  display: flex;
  flex-direction: column;
  gap: 10px;
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
  grid-template-columns: repeat(3, 1fr);
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


@media (max-width: 768px) {
  .topic-capsules-grid {
    grid-template-columns: repeat(2, 1fr);
  }

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
</style>
