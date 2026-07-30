<template>
  <a-modal
    v-if="skill"
    class="skill-detail-modal"
    :open="visible"
    :footer="null"
    :width="560"
    centered
    :destroy-on-close="true"
    @update:open="$emit('update:visible', $event)"
  >
    <template #title>
      <div class="skill-detail-title">
        <div class="skill-detail-name">
          {{ skill.name }}
          <span v-if="skill.featured" class="skill-detail-featured">官方精选</span>
          <span v-if="isMine" class="skill-detail-mine">我的</span>
        </div>
        <div class="skill-detail-creator">
          <span class="skill-detail-creator-avatar">{{ (skill.creatorName || '匿').charAt(0) }}</span>
          <span>by {{ skill.creatorName || '匿名用户' }}</span>
        </div>
      </div>
    </template>

    <div class="skill-detail-wrap">
      <div class="skill-detail-body">
        <div class="skill-detail-stats">
          <div class="skill-detail-stat">
            <div class="skill-detail-stat-value">{{ skill.weeklyUses || 0 }}</div>
            <div class="skill-detail-stat-label">本周使用</div>
          </div>
          <div class="skill-detail-stat">
            <div class="skill-detail-stat-value">{{ skill.totalUses || 0 }}</div>
            <div class="skill-detail-stat-label">累计使用</div>
          </div>
          <div class="skill-detail-stat">
            <div class="skill-detail-stat-value">+{{ formatCoins(skill.weeklyEarnings) }}</div>
            <div class="skill-detail-stat-label">本周币</div>
          </div>
          <div class="skill-detail-stat">
            <div class="skill-detail-stat-value">{{ formatCoins(totalEarnings) }}</div>
            <div class="skill-detail-stat-label">累计币 (×2)</div>
          </div>
        </div>

        <div v-if="scopeTags.length" class="skill-detail-section">
          <div class="skill-detail-section-title">适用范围</div>
          <div class="skill-detail-scope-list">
            <span v-for="tag in scopeTags" :key="tag" class="skill-detail-scope-tag"># {{ tag }}</span>
          </div>
        </div>

        <div class="skill-detail-section">
          <div class="skill-detail-section-title">提示词</div>
          <div class="skill-detail-prompt">{{ skill.prompt || '暂无提示词' }}</div>
        </div>

        <div v-if="skill.excerpt1 || skill.excerpt2" class="skill-detail-section">
          <div class="skill-detail-section-title">示例片段</div>
          <div v-if="skill.excerpt1" class="skill-detail-excerpt">{{ skill.excerpt1 }}</div>
          <div v-if="skill.excerpt2" class="skill-detail-excerpt">{{ skill.excerpt2 }}</div>
        </div>
      </div>

      <div class="skill-detail-footer">
        <span v-if="skill.createdAt">发布于 {{ formatTimeAgo(skill.createdAt) }}</span>
        <span v-else>提示词市场</span>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  skill: { type: Object, default: null },
  visible: { type: Boolean, default: false },
  currentUserId: { type: String, default: '' }
})

defineEmits(['update:visible'])

const isMine = computed(() =>
  props.skill && String(props.skill.creatorId) === String(props.currentUserId)
)

const scopeTags = computed(() => {
  if (!props.skill || !props.skill.scope) return []
  return props.skill.scope.split(/[,，]/).map(t => t.trim()).filter(Boolean)
})

const totalEarnings = computed(() =>
  Number((props.skill?.totalUses || 0) * 2).toFixed(2)
)

const formatCoins = (n) => Number(n || 0).toFixed(2)

const formatTimeAgo = (value) => {
  if (!value) return ''
  const date = new Date(value)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 2592000) return `${Math.floor(diff / 86400)} 天前`
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.skill-detail-title {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.skill-detail-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}
.skill-detail-featured {
  font-size: 11px;
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.08);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}
.skill-detail-mine {
  font-size: 11px;
  color: #07c160;
  background: rgba(7, 193, 96, 0.08);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}
.skill-detail-creator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.skill-detail-creator-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff5577 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
}

.skill-detail-wrap {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.skill-detail-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.skill-detail-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 18px;
}
.skill-detail-stat { text-align: center; }
.skill-detail-stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.skill-detail-stat-label {
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin-top: 2px;
}

.skill-detail-section {
  margin-bottom: 16px;
}
.skill-detail-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}
.skill-detail-scope-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.skill-detail-scope-tag {
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg-page);
  padding: 4px 10px;
  border-radius: 999px;
}
.skill-detail-prompt {
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  padding: 14px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}
.skill-detail-excerpt {
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}
.skill-detail-excerpt:last-child { margin-bottom: 0; }

.skill-detail-footer {
  flex-shrink: 0;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-light);
  font-size: 12px;
  color: var(--color-text-placeholder);
  text-align: center;
}

@media (max-width: 640px) {
  .skill-detail-stats { grid-template-columns: repeat(2, 1fr); }
}
</style>

<style>
/* ant-modal  teleport 到 body，需要全局覆盖 */
.skill-detail-modal .ant-modal-body {
  height: 520px;
  max-height: 520px;
  padding: 16px 20px 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.skill-detail-modal .ant-modal-content {
  overflow: hidden;
}

@media (max-width: 640px) {
  .skill-detail-modal .ant-modal-body {
    height: 60vh;
    max-height: 60vh;
  }
}

body[data-theme="dark"] .skill-detail-modal .ant-modal-content,
body[data-theme="dark"] .skill-detail-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}
body[data-theme="dark"] .skill-detail-modal .ant-modal-title {
  color: #f0f0f0 !important;
}
body[data-theme="dark"] .skill-detail-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}
body[data-theme="dark"] .skill-detail-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}
</style>
