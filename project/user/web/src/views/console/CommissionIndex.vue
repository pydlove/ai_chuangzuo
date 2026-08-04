<template>
  <div class="commission-page">
    <header class="commission-header">
      <h1 class="commission-header__title">约稿中心</h1>
      <p class="commission-header__subtitle">挑选合适的任务，使用你在爱创作中生成完成的文章参与投稿。稿件采纳后，奖励全额发放。</p>
    </header>

    <section class="commission-stats">
      <div class="stat-card">
        <strong>{{ activeTaskCount }}</strong>
        <span>进行中</span>
      </div>
      <div class="stat-card">
        <strong>{{ mySubmissionCount }}</strong>
        <span>我的投稿</span>
      </div>
      <div class="stat-card">
        <strong>{{ earnedCoinTotal }}</strong>
        <span>已获得</span>
      </div>
    </section>

    <section class="commission-rules">
      <div class="commission-rules__header" @click="rulesExpanded = !rulesExpanded">
        <h3>约稿规则</h3>
        <span class="commission-rules__toggle">{{ rulesExpanded ? '收起' : '展开' }}</span>
      </div>
      <ul v-show="rulesExpanded">
        <li>只能从爱创作中已生成完成的文章中选择投稿。</li>
        <li>投稿文章字数需符合任务要求，否则无法选中。</li>
        <li>同一篇文章在同一时间只能投递一个任务。</li>
        <li>投递期内可随时撤回，并改投其它文章。</li>
        <li>稿件被管理员采纳后，奖励全额发放至创作币账户。</li>
        <li>投稿状态以页面展示为准，评选期间请耐心等待。</li>
      </ul>
    </section>

    <div class="commission-tabs" role="tablist">
      <button :class="{ active: tab === 'all' }" role="tab" @click="tab = 'all'">全部任务</button>
      <button :class="{ active: tab === 'mine' }" role="tab" @click="tab = 'mine'">我投稿的</button>
    </div>

    <div v-if="tab === 'all'" class="commission-filter">
      <button v-for="item in filters" :key="String(item.value)" :class="['filter-chip', { active: status === item.value }]" @click="status = item.value">
        {{ item.label }}
      </button>
    </div>

    <div v-if="loading" class="empty-block">加载中...</div>
    <div v-else-if="visibleItems.length === 0" class="empty-block">{{ tab === 'mine' ? '还没有投递过稿件' : '暂无约稿任务' }}</div>
    <div v-else-if="tab === 'all'" class="task-list">
      <article v-for="item in visibleItems" :key="item.id" class="task-card" @click="goDetail(item.taskId || item.id)">
        <div class="task-card-top">
          <span :class="['status-tag', `status-${item.status}`]">{{ taskStatus(item.status) }}</span>
          <strong class="task-reward">{{ item.rewardCoin }}<small> 创作币 / 篇</small></strong>
        </div>
        <h2>{{ item.title || `任务 #${item.taskId || item.id}` }}</h2>
        <p class="task-desc">{{ item.description }}</p>
        <div class="task-facts">
          <span>{{ wordRangeText(item) }}</span>
          <span>采纳 {{ item.adoptedCount }}/{{ item.neededCount }} 篇</span>
          <span>{{ item.submissionCount || 0 }} 人投稿</span>
        </div>
        <div class="task-card-bottom"><span>{{ deadlineText(item) }}</span><span class="detail-link">查看详情 <span>→</span></span></div>
      </article>
    </div>
    <div v-else class="task-list">
      <article v-for="item in visibleItems" :key="item.id" class="submission-card" @click="goDetail(item.taskId || item.id)">
        <div class="task-card-top">
          <span :class="['status-tag', `submission-${item.status}`]">{{ submissionStatus(item.status) }}</span>
          <strong v-if="item.rewardCoin != null" class="task-reward">{{ item.rewardCoin }}<small> 创作币</small></strong>
        </div>
        <h2 class="submission-task-title">{{ item.title || `任务 #${item.taskId || item.id}` }}</h2>
        <p class="task-desc submission-article">《{{ item.articleTitle }}》 · {{ item.wordCount }} 字</p>
        <div class="submission-meta">
          <span>{{ submissionDeadlineText(item) }}</span>
          <span v-if="item.status === 1" class="reward-collected">奖励已发放</span>
        </div>
        <div class="task-card-bottom"><span class="detail-link">查看投稿详情 <span>→</span></span></div>
      </article>
    </div>

    <div v-if="total > pageSize" class="pagination-bar">
      <a-pagination
        v-model:current="page"
        v-model:page-size="pageSize"
        :total="total"
        :show-size-changer="true"
        :page-size-options="['10', '20', '50']"
        show-total
        size="small"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useCommission } from '@/composables/useCommission'

const router = useRouter()
const { tasks, mySubmissions, loading, page, pageSize, total, loadTasks, loadMySubmissions } = useCommission()
const tab = ref('all')
const status = ref(null)
const rulesExpanded = ref(false)
const filters = [
  { label: '全部', value: null },
  { label: '投递中', value: 0 },
  { label: '评选中', value: 1 },
  { label: '已完成', value: 2 }
]

const visibleItems = computed(() => tab.value === 'mine' ? mySubmissions.value : tasks.value)

const activeTaskCount = computed(() => tasks.value.filter(task => task.status === 0).length)
const mySubmissionCount = computed(() => mySubmissions.value.length)
const earnedCoinTotal = computed(() =>
  mySubmissions.value
    .filter(submission => submission.status === 1)
    .reduce((total, submission) => total + (Number(submission.rewardCoin) || 0), 0)
)

async function refresh() {
  try {
    if (tab.value === 'mine') await loadMySubmissions()
    else await loadTasks({ status: status.value })
  } catch (error) {
    message.error(error.message || '约稿任务加载失败')
  }
}

onMounted(refresh)
watch([tab, status], () => {
  page.value = 1
  refresh()
})
watch([page, pageSize], refresh)

function taskStatus(value) {
  return ['投递中', '评选中', '已完成'][value] || '未知状态'
}
function submissionStatus(value) {
  return ['等待采纳', '已采纳', '未采纳', '已撤回'][value] || '未知状态'
}
function deadlineText(task) {
  const now = new Date()
  if (task.status === 0) {
    const deadline = new Date(task.deadlineAt)
    if (deadline <= now) return `截止于 ${deadline.toLocaleString()}`
    const hours = Math.max(1, Math.ceil((deadline - now) / 3600000))
    return hours > 24 ? `投递还剩 ${Math.ceil(hours / 24)} 天` : `投递还剩 ${hours} 小时`
  }
  if (task.status === 1 && task.selectionDeadlineAt) {
    const deadline = new Date(task.selectionDeadlineAt)
    if (deadline <= now) return `评选截止已过 ${deadline.toLocaleString()}`
    const hours = Math.max(1, Math.ceil((deadline - now) / 3600000))
    return hours > 24 ? `评选还剩 ${Math.ceil(hours / 24)} 天` : `评选还剩 ${hours} 小时`
  }
  return `截止于 ${new Date(task.deadlineAt).toLocaleString()}`
}
function submissionDeadlineText(item) {
  const submittedAt = item.createdAt
  if (submittedAt) {
    const date = new Date(submittedAt)
    return `投稿于 ${date.toLocaleString()}`
  }
  return '投稿时间未知'
}
function wordRangeText(item) {
  const min = item.minWordCount
  const max = item.maxWordCount
  if (min != null && max != null) return `${min}-${max} 字`
  if (min != null) return `≥ ${min} 字`
  if (max != null) return `≤ ${max} 字`
  return '字数不限'
}
function goDetail(id) {
  router.push(`/console/commission/${id}`)
}
</script>

<style scoped>
.commission-page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 16px;
  box-sizing: border-box;
  background: #fafafa;
  min-height: 100%;
}

/* Header */
.commission-header {
  margin-bottom: 16px;
}
.commission-header__title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}
.commission-header__subtitle {
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
  margin: 0;
}

/* Stats */
.commission-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px 12px;
  text-align: center;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stat-card strong {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary, #ff2442);
}
.stat-card span {
  font-size: 12px;
  color: #8c8c8c;
}

/* Rules */
.commission-rules {
  background: #fff;
  border-radius: 16px;
  padding: 16px 18px;
  margin-bottom: 16px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}
.commission-rules__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  user-select: none;
}
.commission-rules__header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.commission-rules__toggle {
  font-size: 13px;
  color: var(--color-primary, #ff2442);
  font-weight: 500;
}
.commission-rules ul {
  margin: 12px 0 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.commission-rules li {
  color: #595959;
  font-size: 13px;
  line-height: 1.6;
}

/* Tabs */
.commission-tabs {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  gap: 8px;
  padding: 4px;
  background: #f5f5f5;
  border-radius: 999px;
  margin-bottom: 12px;
}
.commission-tabs button {
  flex: 1;
  padding: 9px 16px;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: #595959;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}
.commission-tabs button.active {
  background: #fff;
  color: var(--color-primary, #ff2442);
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* Filters */
.commission-filter {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  margin-bottom: 12px;
  overflow-x: auto;
  scrollbar-width: none;
  padding-bottom: 4px;
}
.commission-filter::-webkit-scrollbar { display: none; }
.filter-chip {
  flex: 0 0 auto;
  padding: 6px 14px;
  border: 1px solid #e8e8e8;
  border-radius: 999px;
  background: #fff;
  color: #595959;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.filter-chip.active {
  background: var(--color-primary, #ff2442);
  color: #fff;
  border-color: transparent;
}

/* Task list */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card,
.submission-card {
  position: relative;
  padding: 18px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
}
.task-card:active,
.submission-card:active {
  transform: scale(0.99);
}

.task-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.status-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}
.status-0 { color: var(--color-primary, #ff2442); background: rgba(255, 36, 66, 0.1); }
.status-1 { color: #ff4d6f; background: rgba(255, 77, 111, 0.1); }
.status-2 { color: #8c8c8c; background: #f5f5f5; }
.submission-0 { color: var(--color-primary, #ff2442); background: rgba(255, 36, 66, 0.1); }
.submission-1 { color: #ff4d6f; background: rgba(255, 77, 111, 0.1); }
.submission-2 { color: #8c8c8c; background: #f5f5f5; }
.submission-3 { color: #bfbfbf; background: #fafafa; }

.task-reward {
  font-size: 18px;
  color: var(--color-primary, #ff2442);
  font-weight: 700;
  white-space: nowrap;
}
.task-reward small {
  font-size: 11px;
  font-weight: 500;
  color: #8c8c8c;
  margin-left: 2px;
}

.task-card h2,
.submission-task-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
}
.task-desc {
  margin: 0 0 12px;
  color: #595959;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}
.task-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #8c8c8c;
  font-size: 12px;
  margin-bottom: 12px;
}
.task-card-bottom {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px dashed #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #8c8c8c;
}
.detail-link {
  color: var(--color-primary, #ff2442);
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.detail-link span { transition: transform 0.2s ease; display: inline-block; }
.task-card:active .detail-link span,
.submission-card:active .detail-link span { transform: translateX(3px); }

.submission-task-title { font-size: 16px; }
.submission-article { margin-bottom: 10px; }
.submission-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #8c8c8c;
  font-size: 12px;
  margin-bottom: 10px;
}
.reward-collected { color: var(--color-primary, #ff2442); font-weight: 500; }

.empty-block {
  padding: 56px 20px;
  text-align: center;
  color: #8c8c8c;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  font-size: 14px;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  padding: 12px 0;
}

/* 暗色主题 */
body[data-theme="dark"] .commission-page { background: #141414; }
body[data-theme="dark"] .commission-header__title { color: #f5f5f5; }
body[data-theme="dark"] .commission-header__subtitle { color: #a6a6a6; }
body[data-theme="dark"] .stat-card,
body[data-theme="dark"] .commission-rules,
body[data-theme="dark"] .filter-chip,
body[data-theme="dark"] .task-card,
body[data-theme="dark"] .submission-card,
body[data-theme="dark"] .empty-block { background: #1f1f1f; }
body[data-theme="dark"] .stat-card span { color: #8c8c8c; }
body[data-theme="dark"] .commission-rules__header h3 { color: #f5f5f5; }
body[data-theme="dark"] .commission-rules li { color: #a6a6a6; }
body[data-theme="dark"] .commission-tabs { background: #262626; }
body[data-theme="dark"] .commission-tabs button { color: #a6a6a6; }
body[data-theme="dark"] .commission-tabs button.active { background: #1f1f1f; }
body[data-theme="dark"] .filter-chip { border-color: #404040; color: #d9d9d9; }
body[data-theme="dark"] .task-card h2,
body[data-theme="dark"] .submission-task-title { color: #f5f5f5; }
body[data-theme="dark"] .task-desc,
body[data-theme="dark"] .task-facts,
body[data-theme="dark"] .submission-meta,
body[data-theme="dark"] .task-card-bottom { color: #a6a6a6; }
body[data-theme="dark"] .task-card-bottom { border-top-color: #303030; }
body[data-theme="dark"] .status-2 { color: #a6a6a6; background: #262626; }
body[data-theme="dark"] .submission-2 { color: #a6a6a6; background: #262626; }
body[data-theme="dark"] .submission-3 { color: #8c8c8c; background: #1a1a1a; }

/* PC 端适配 */
@media (min-width: 769px) {
  .commission-page {
    padding: 24px 32px;
  }
  .commission-header__title {
    font-size: 28px;
  }
  .commission-stats {
    gap: 14px;
    margin-bottom: 20px;
  }
  .stat-card {
    padding: 20px 16px;
  }
  .stat-card strong {
    font-size: 26px;
  }
  .commission-rules {
    padding: 20px 24px;
    margin-bottom: 20px;
  }
  .commission-rules ul {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px 28px;
  }
  .commission-tabs {
    position: static;
    display: inline-flex;
    width: auto;
    margin-bottom: 16px;
  }
  .commission-filter {
    flex-wrap: wrap;
    overflow-x: visible;
    margin-bottom: 16px;
  }
  .task-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }
  .task-card,
  .submission-card {
    padding: 22px;
    border-radius: 20px;
  }
  .task-card:hover,
  .submission-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.08);
  }
  .empty-block {
    padding: 72px 20px;
  }
  .pagination-bar {
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
