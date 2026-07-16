<template>
  <a-drawer
    v-model:open="open"
    placement="right"
    :width="isMobile ? '100%' : 360"
    class="queue-drawer"
  >
    <template #title>
      <div class="drawer-title-row">
        <span>生成队列</span>
        <span v-if="isMobile" class="quota-text">本月剩余 <strong>{{ quotaRemaining }}</strong> / {{ quotaTotal }} 次</span>
      </div>
    </template>
    <template #extra>
      <button class="queue-more-btn" @click="goWorks">查看更多 →</button>
    </template>
    <div v-if="queueList.length === 0" class="queue-panel-empty">
      <InboxOutlined class="empty-icon" />
      <div class="empty-text">暂无生成任务</div>
      <div class="empty-hint">点击「生成文章」开始创作</div>
    </div>
    <div v-else class="queue-panel-list">
      <div
        v-for="item in queueList"
        :key="item.id"
        :class="['queue-panel-item', item.status]"
        :style="item.status === 'completed' ? 'cursor: pointer' : ''"
        @click="item.status === 'completed' && goWorks()"
      >
        <div class="queue-item-top">
          <div class="queue-item-icon">
            <LoadingOutlined v-if="item.status === 'generating'" :spin="true" />
            <CheckCircleOutlined v-else-if="item.status === 'completed'" />
            <ClockCircleOutlined v-else-if="item.status === 'queued'" />
            <CloseCircleOutlined v-else />
          </div>
          <div class="queue-item-info">
            <a-tooltip :title="item.title" placement="top">
              <span class="queue-item-title">{{ item.title }}</span>
            </a-tooltip>
            <div class="queue-item-meta">
              <span class="queue-item-status-badge" :class="item.status">
                {{ item.status === 'generating' ? `生成中 ${Math.min(100, Math.round(item.progress))}%` : statusText(item.status) }}
              </span>
            </div>
          </div>
        </div>
        <div v-if="item.status === 'generating'" class="queue-item-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: Math.min(100, Math.round(item.progress)) + '%' }"></div>
          </div>
          <div class="progress-hint">已完成 {{ Math.min(100, Math.round(item.progress)) }}%</div>
        </div>
      </div>
    </div>
  </a-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { InboxOutlined, LoadingOutlined, CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import { useGenerationQueue, statusText } from './useGenerationQueue.js'
import { useIsMobile } from '@/composables/useMobile.js'
import { useBenefits } from '@/composables/useBenefits.js'

const props = defineProps({ open: Boolean })
const emit = defineEmits(['update:open'])
const open = computed({
  get: () => props.open,
  set: (v) => emit('update:open', v)
})

const router = useRouter()
const isMobile = useIsMobile()
const { queueList } = useGenerationQueue()
const { benefits } = useBenefits()
const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)

const goWorks = () => {
  open.value = false
  router.push('/console/works')
}
</script>

<style scoped>
.drawer-title-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.drawer-title-row .quota-text {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-weight: 400;
}

.drawer-title-row .quota-text strong {
  color: var(--color-primary);
}

.queue-more-btn {
  background: none;
  border: none;
  color: #8c8c8c;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.queue-more-btn:hover {
  color: var(--color-primary);
  background: #fff0f2;
}

.queue-panel-empty {
  text-align: center;
  padding: 24px 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}

.queue-panel-empty .empty-icon {
  font-size: 32px;
  color: #d9d9d9;
  margin-bottom: 8px;
}

.queue-panel-empty .empty-text {
  color: #8c8c8c;
  font-size: 14px;
  margin-bottom: 4px;
}

.queue-panel-empty .empty-hint {
  color: #bfbfbf;
  font-size: 12px;
}

.queue-panel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-x: hidden;
  flex: 1;
  padding-right: 4px;
}

.queue-panel-item {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 14px;
  transition: all 0.2s;
}

.queue-panel-item:hover {
  border-color: #ffd1d9;
  box-shadow: 0 2px 12px rgba(255, 36, 66, 0.08);
}

.queue-panel-item.generating {
  background: #fff;
  border-color: #ffd1d9;
}

.queue-panel-item.completed {
  background: #fff;
  border-color: #d9f7be;
}

.queue-panel-item.completed:hover {
  border-color: #b7eb8f;
  box-shadow: 0 2px 8px rgba(7, 193, 96, 0.08);
}

.queue-panel-item.queued {
  background: #fff;
  border-color: #e8e8e8;
}

.queue-panel-item.failed {
  background: #fff;
  border-color: #ffccc7;
}

.queue-item-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.queue-item-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.queue-panel-item.generating .queue-item-icon {
  background: #fff0f2;
  color: #ff2442;
}

.queue-panel-item.completed .queue-item-icon {
  background: #f0fff2;
  color: #07c160;
}

.queue-panel-item.queued .queue-item-icon {
  background: #f5f5f5;
  color: #8c8c8c;
}

.queue-panel-item.failed .queue-item-icon {
  background: #fff0f0;
  color: #ff4d4f;
}

.queue-item-info {
  flex: 1;
  min-width: 0;
}

.queue-item-title {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}

.queue-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.queue-item-status-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.queue-item-status-badge.generating {
  background: #ffeaea;
  color: #ff2442;
}

.queue-item-status-badge.completed {
  background: #e6fff2;
  color: #07c160;
}

.queue-item-status-badge.queued {
  background: #f5f5f5;
  color: #595959;
}

.queue-item-status-badge.failed {
  background: #fff0f0;
  color: #ff4d4f;
}

.queue-item-progress {
  margin-top: 12px;
}

.queue-item-progress .progress-bar {
  height: 6px;
  background: rgba(255, 36, 66, 0.15);
  border-radius: 3px;
  overflow: hidden;
}

.queue-item-progress .progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff2442, #ff6b81);
  border-radius: 3px;
  transition: width 0.3s;
}

.queue-item-progress .progress-hint {
  margin-top: 6px;
  font-size: 11px;
  color: #8c8c8c;
  line-height: 1.4;
}


body[data-theme="dark"] .queue-panel-item {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .queue-panel-item:hover {
  border-color: #434343;
  box-shadow: none;
}

body[data-theme="dark"] .queue-panel-item.generating {
  border-color: rgba(255, 36, 66, 0.35);
}

body[data-theme="dark"] .queue-panel-item.completed {
  border-color: rgba(7, 193, 96, 0.35);
}

body[data-theme="dark"] .queue-panel-item.queued {
  border-color: #303030;
}

body[data-theme="dark"] .queue-panel-item.failed {
  border-color: rgba(255, 77, 79, 0.35);
}

body[data-theme="dark"] .queue-panel-item.generating .queue-item-icon {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6a;
}

body[data-theme="dark"] .queue-panel-item.completed .queue-item-icon {
  background: rgba(7, 193, 96, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .queue-panel-item.queued .queue-item-icon {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .queue-panel-item.failed .queue-item-icon {
  background: rgba(255, 77, 79, 0.15);
  color: #ff7875;
}

body[data-theme="dark"] .queue-item-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .queue-item-status-badge.generating {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6a;
}

body[data-theme="dark"] .queue-item-status-badge.completed {
  background: rgba(7, 193, 96, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .queue-item-status-badge.queued {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .queue-item-status-badge.failed {
  background: rgba(255, 77, 79, 0.15);
  color: #ff7875;
}

body[data-theme="dark"] .queue-item-progress .progress-hint {
  color: #6a6a6a;
}

body[data-theme="dark"] .queue-more-btn:hover {
  background: #2a2a2a;
}
</style>

<style>
/* a-drawer 内容 teleport 到 body，暗色覆盖需全局；
   class="queue-drawer" 挂在 .ant-drawer-content 同一元素上，须用复合选择器 */
body[data-theme="dark"] .queue-drawer.ant-drawer-content {
  background: #1f1f1f;
}

body[data-theme="dark"] .queue-drawer .ant-drawer-header {
  background: #1f1f1f;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .queue-drawer .ant-drawer-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .queue-drawer .ant-drawer-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .queue-drawer .queue-panel-empty .empty-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .queue-drawer .queue-panel-empty .empty-hint {
  color: #6a6a6a;
}
</style>
