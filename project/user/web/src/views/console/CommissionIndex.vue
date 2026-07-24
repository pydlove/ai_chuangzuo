<template>
  <div class="commission-page">
    <div class="commission-header">
      <h2 class="commission-title">约稿中心</h2>
      <p class="commission-subtitle">
        发布征集任务,平台抽成 10%。投稿人奖励以创作币结算,可提现。
      </p>
    </div>

    <div class="commission-tabs">
      <button
        :class="['commission-tab', { active: tab === 'all' }]"
        @click="tab = 'all'"
      >
        全部任务
      </button>
      <button
        :class="['commission-tab', { active: tab === 'published' }]"
        @click="tab = 'published'"
      >
        我发布的
      </button>
      <button
        :class="['commission-tab', { active: tab === 'submitted' }]"
        @click="tab = 'submitted'"
      >
        我投稿的
      </button>
    </div>

    <div class="commission-filter">
      <button
        :class="['filter-btn', { active: filter === 'all' }]"
        @click="filter = 'all'"
      >全部</button>
      <button
        :class="['filter-btn', { active: filter === 'open' }]"
        @click="filter = 'open'"
      >进行中</button>
      <button
        :class="['filter-btn', { active: filter === 'closed' }]"
        @click="filter = 'closed'"
      >已结束</button>
    </div>

    <!-- 当前用户切换(演示用) -->
    <div class="demo-user-switch">
      <span class="demo-user-label">演示账号:</span>
      <select v-model="currentUserId" class="demo-user-select">
        <option v-for="(name, id) in demoUserOptions" :key="id" :value="id">
          {{ name }} ({{ id }})
        </option>
      </select>
    </div>

    <div v-if="visibleTasks.length === 0" class="commission-empty">
      <div class="commission-empty-icon">📝</div>
      <p class="commission-empty-text">{{ emptyText }}</p>
      <button v-if="tab !== 'submitted'" class="empty-btn" @click="goPublish">
        去发布
      </button>
    </div>

    <div v-else class="commission-list">
      <div
        v-for="t in visibleTasks"
        :key="t.id"
        class="task-card"
        @click="goDetail(t)"
      >
        <div class="task-card-row1">
          <span class="task-title">{{ t.title }}</span>
          <span class="task-reward">{{ t.rewardCoin }} 创作币</span>
        </div>
        <div class="task-card-row2">
          <span class="task-publisher">{{ t.publisherNickname }}</span>
          <span class="task-dot">·</span>
          <span class="task-words">{{ t.requirements.minWordCount }}-{{ t.requirements.maxWordCount }} 字</span>
          <span v-if="t.requirements.styleHint" class="task-dot">·</span>
          <span v-if="t.requirements.styleHint" class="task-style">{{ t.requirements.styleHint }}</span>
        </div>
        <div class="task-card-row3">
          <span class="task-deadline" :class="{ 'deadline-danger': deadlineInfo(t).danger }">
            {{ deadlineInfo(t).text }}
          </span>
          <span class="task-submissions">已有 {{ getSubmissionCount(t.id) }} 人投稿</span>
          <span :class="['task-status', `status-${t.status.toLowerCase()}`]">{{ statusLabel(t.status) }}</span>
        </div>
        <div class="task-card-row4">
          <span v-if="t.publisherId === currentUserId" class="task-badge mine-pub">我发布的</span>
          <span v-else-if="mySubFor(t)" class="task-badge mine-sub">我投递的</span>
          <button
            class="task-action"
            @click.stop="goDetail(t)"
          >{{ actionLabel(t) }}</button>
        </div>
      </div>
    </div>

    <button class="commission-fab" @click="goPublish">+ 发布约稿</button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCommission } from '@/composables/useCommission'
import { DEMO_USER_NICKNAMES } from '@/data/commissionSeed'

const router = useRouter()
const {
  tasks, submissions, currentUserId,
  myPublishedTasks, mySubmissions,
  getSubmissionsOfTask, startReconcile,
  setCurrentUserId
} = useCommission()

const tab = ref('all')              // all | published | submitted
const filter = ref('all')           // all | open | closed
const now = ref(Date.now())

const demoUserOptions = DEMO_USER_NICKNAMES

let tick = null
onMounted(() => {
  startReconcile()
  tick = setInterval(() => { now.value = Date.now() }, 60_000)
})
onUnmounted(() => { if (tick) clearInterval(tick) })

const visibleTasks = computed(() => {
  let list
  if (tab.value === 'published') {
    list = myPublishedTasks.value
  } else if (tab.value === 'submitted') {
    const ids = new Set(mySubmissions.value.map(s => s.taskId))
    list = tasks.value.filter(t => ids.has(t.id))
  } else {
    list = tasks.value
  }
  if (filter.value === 'open') list = list.filter(t => t.status === 'OPEN')
  if (filter.value === 'closed') list = list.filter(t => t.status !== 'OPEN')
  return [...list].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

const emptyText = computed(() => {
  if (tab.value === 'published') return '还没有发布过约稿任务,去发一个吧'
  if (tab.value === 'submitted') return '还没有投递过任何任务'
  return '暂无约稿任务'
})

function deadlineInfo(t) {
  const left = new Date(t.deadlineAt).getTime() - now.value
  if (left <= 0) {
    return t.status === 'OPEN'
      ? { text: '宽限期中,请尽快选择', danger: true }
      : { text: '已结束', danger: false }
  }
  const days = Math.floor(left / 86400000)
  const hours = Math.floor((left % 86400000) / 3600000)
  const mins = Math.floor((left % 3600000) / 60000)
  if (left < 2 * 3600000) return { text: `即将截止 (${hours}h ${mins}m)`, danger: true }
  if (days > 0) return { text: `还剩 ${days} 天 ${hours} 小时`, danger: false }
  return { text: `还剩 ${hours} 小时 ${mins} 分`, danger: false }
}

function statusLabel(s) {
  return { OPEN: '进行中', SETTLED: '已结算', EXPIRED: '已流局', CANCELLED: '已撤销' }[s] || s
}

function actionLabel(t) {
  if (t.publisherId === currentUserId.value) {
    if (t.status === 'OPEN') return '查看投稿'
    return '查看结果'
  }
  return '去投稿'
}

function mySubFor(t) {
  return submissions.value.find(s =>
    s.taskId === t.id && s.submitterId === currentUserId.value && !s.withdrawnAt
  )
}

function getSubmissionCount(taskId) {
  return submissions.value.filter(s => s.taskId === taskId && !s.withdrawnAt).length
}

function goDetail(t) { router.push(`/console/commission/${t.id}`) }
function goPublish() { router.push('/console/commission/publish') }

// 演示用:切换用户时联动 composable
watch(currentUserId, (id) => setCurrentUserId(id))
</script>

<style scoped>
.commission-page {
  max-width: 960px;
  margin: 0 auto;
}

.commission-header {
  margin-bottom: 16px;
}

.commission-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary, #1a1a1a);
  margin: 0 0 6px;
}

.commission-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.commission-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.commission-tab {
  padding: 10px 18px;
  font-size: 14px;
  color: #595959;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: -1px;
}

.commission-tab:hover {
  color: var(--color-primary, #FF2442);
}

.commission-tab.active {
  color: var(--color-primary, #FF2442);
  border-bottom-color: var(--color-primary, #FF2442);
  font-weight: 600;
}

.commission-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.filter-btn {
  padding: 4px 14px;
  font-size: 13px;
  color: #595959;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  border-color: var(--color-primary, #FF2442);
  color: var(--color-primary, #FF2442);
}

.filter-btn.active {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}

.demo-user-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fffbe6;
  border: 1px dashed #ffe58f;
  border-radius: 8px;
  font-size: 12px;
  color: #ad6800;
  margin-bottom: 16px;
}

.demo-user-label {
  font-weight: 500;
}

.demo-user-select {
  padding: 2px 8px;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  background: #fff;
  color: #ad6800;
  font-size: 12px;
  cursor: pointer;
}

.commission-empty {
  text-align: center;
  padding: 80px 0;
  background: #fff;
  border-radius: 12px;
}

.commission-empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.commission-empty-text {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 16px;
}

.empty-btn {
  padding: 8px 20px;
  background: var(--color-primary, #FF2442);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.empty-btn:hover {
  opacity: 0.85;
}

.commission-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.task-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.task-card-row1 {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
  gap: 12px;
}

.task-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-reward {
  flex-shrink: 0;
  padding: 2px 12px;
  background: #fff0f2;
  color: var(--color-primary, #FF2442);
  border-radius: 14px;
  font-size: 13px;
  font-weight: 700;
}

.task-card-row2 {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.task-publisher {
  color: #595959;
}

.task-dot {
  color: #d9d9d9;
}

.task-style {
  color: #595959;
  background: #fafafa;
  padding: 1px 8px;
  border-radius: 8px;
  font-size: 11px;
}

.task-card-row3 {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #595959;
  margin-bottom: 10px;
}

.task-deadline {
  font-weight: 500;
}

.task-deadline.deadline-danger {
  color: #fa8c16;
}

.task-submissions {
  color: #8c8c8c;
}

.task-status {
  margin-left: auto;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.task-status.status-open {
  background: #e6f4ff;
  color: #1677ff;
}

.task-status.status-settled {
  background: #e6f7ed;
  color: #07c160;
}

.task-status.status-expired {
  background: #f5f5f5;
  color: #8c8c8c;
}

.task-status.status-cancelled {
  background: #fff7e6;
  color: #fa8c16;
}

.task-card-row4 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.task-badge {
  padding: 2px 10px;
  font-size: 11px;
  border-radius: 10px;
}

.task-badge.mine-pub {
  background: #fff7e6;
  color: #fa8c16;
}

.task-badge.mine-sub {
  background: #f0f5ff;
  color: #1677ff;
}

.task-action {
  margin-left: auto;
  padding: 5px 14px;
  background: var(--color-primary, #FF2442);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.task-action:hover {
  background: #e0203b;
}

.commission-fab {
  position: fixed;
  right: 32px;
  bottom: 96px;
  padding: 12px 24px;
  background: var(--color-primary, #FF2442);
  color: #fff;
  border: none;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(255, 36, 66, 0.3);
  transition: all 0.2s;
  z-index: 10;
}

.commission-fab:hover {
  background: #e0203b;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.4);
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .commission-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .commission-tabs {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .commission-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .filter-btn {
  background: #262626;
  border-color: #404040;
  color: #a6a6a6;
}

body[data-theme="dark"] .commission-empty {
  background: #1f1f1f;
}

body[data-theme="dark"] .demo-user-switch {
  background: #2b2111;
  border-color: #594214;
  color: #ffa940;
}

body[data-theme="dark"] .demo-user-select {
  background: #141414;
  border-color: #594214;
  color: #ffa940;
}

body[data-theme="dark"] .task-card {
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

body[data-theme="dark"] .task-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .task-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .task-reward {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme="dark"] .task-publisher,
body[data-theme="dark"] .task-style {
  color: #a6a6a6;
}

body[data-theme="dark"] .task-style {
  background: #262626;
}

body[data-theme="dark"] .task-deadline {
  color: #a6a6a6;
}

body[data-theme="dark"] .task-deadline.deadline-danger {
  color: #ffa940;
}

body[data-theme="dark"] .task-status.status-open {
  background: rgba(22, 119, 255, 0.18);
  color: #69b1ff;
}

body[data-theme="dark"] .task-status.status-settled {
  background: rgba(7, 193, 96, 0.18);
  color: #36cfc9;
}

body[data-theme="dark"] .task-status.status-expired {
  background: #262626;
  color: #8c8c8c;
}

body[data-theme="dark"] .task-badge.mine-pub {
  background: rgba(250, 140, 22, 0.18);
  color: #ffa940;
}

body[data-theme="dark"] .task-badge.mine-sub {
  background: rgba(22, 119, 255, 0.18);
  color: #69b1ff;
}

/* ========== 移动端 ========== */
@media (max-width: 768px) {
  .commission-fab {
    right: 16px;
    bottom: 76px;
    padding: 10px 18px;
    font-size: 13px;
  }

  .task-card {
    padding: 14px 16px;
  }

  .task-card-row3 {
    flex-wrap: wrap;
  }

  .task-status {
    margin-left: 0;
  }

  .commission-tabs {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .commission-tabs::-webkit-scrollbar {
    display: none;
  }

  .demo-user-switch {
    font-size: 11px;
  }
}
</style>