<template>
  <div class="commission-page">
    <!-- PC 端 hero -->
    <section v-if="!isMobile" class="commission-hero">
      <div class="hero-copy">
        <span class="eyebrow">OFFICIAL COMMISSION</span>
        <h1>约稿中心</h1>
        <p>挑选合适的任务，使用你在爱创作工坊中生成完成的文章参与投稿。稿件采纳后，奖励全额发放。</p>
      </div>
      <div class="hero-orbit" aria-hidden="true"><span></span><span></span></div>
      <div class="hero-stats">
        <StatCard variant="glass" :value="activeTaskCount" label="进行中的任务" />
        <StatCard variant="glass" :value="mySubmissionCount" label="我的投稿" />
        <StatCard variant="glass" :value="earnedCoinTotal" label="已获得创作币" />
      </div>
    </section>

    <!-- 手机端头部、统计 -->
    <template v-if="isMobile">
      <header class="commission-header">
        <div class="commission-header__left">
          <div class="commission-header__donuts" aria-hidden="true">
            <span></span>
            <span></span>
          </div>
          <img
            src="/assets/images/约稿中心logo-v3.png?v=3"
            alt="约稿中心"
            class="commission-header__logo"
          />
          <p class="commission-header__subtitle">挑选合适任务，用已生成的文章投稿，采纳后奖励全额发放。</p>
        </div>
        <img
          src="/assets/images/约稿中心宣传图-v1.png"
          alt=""
          class="commission-header__illustration"
        />
      </header>

      <StatCardGroup class="commission-stats" :columns="2" gap="12px">
        <StatCard
          variant="commission"
          :value="activeTaskCount"
          label="进行中"
          image="/assets/images/进行中的投稿-v1.png"
        />
        <StatCard
          variant="commission"
          :value="mySubmissionCount"
          label="我的投稿"
          image="/assets/images/我的投稿-v1.png"
        />
      </StatCardGroup>
    </template>

    <section class="commission-rules">
      <div class="commission-rules__header" @click="rulesExpanded = !rulesExpanded">
        <h3>约稿规则</h3>
        <span v-if="isMobile" class="commission-rules__toggle">{{ rulesExpanded ? '收起' : '展开' }}</span>
      </div>
      <ul v-show="rulesExpanded">
        <li>只能从爱创作工坊中已生成完成的文章中选择投稿。</li>
        <li>投稿文章字数需符合任务要求，否则无法选中。</li>
        <li>同一篇文章在同一时间只能投递一个任务。</li>
        <li>投递期内可随时撤回，并改投其它文章。</li>
        <li>稿件被管理员采纳后，奖励全额发放至创作币账户。</li>
        <li>投稿状态以页面展示为准，评选期间请耐心等待。</li>
      </ul>
    </section>

    <Tabs
      v-model="activeTab"
      class="commission-tabs"
      :tabs="tabItems"
      variant="segment"
      active-type="surface"
      shape="pill"
      sticky
    />

    <div v-if="loading" class="empty-block">加载中...</div>
    <div v-else-if="visibleItems.length === 0" class="empty-block">{{ activeTab === 'mine' ? '还没有投递过稿件' : '暂无约稿任务' }}</div>
    <div v-else-if="activeTab !== 'mine'" class="task-list">
      <ListCard
        v-for="item in visibleItems"
        :key="item.id"
        tag="article"
        clickable
        custom-class="task-card"
        @click="goDetail(item.taskId || item.id)"
      >
        <div class="task-card-top">
          <span :class="['status-tag', `status-${item.status}`]">{{ taskStatus(item.status) }}</span>
          <strong class="task-reward">{{ item.rewardCoin }}<small> 创作币 / 篇</small></strong>
        </div>
        <h2>{{ item.title || `任务 #${item.taskId || item.id}` }}</h2>
        <p class="task-desc">{{ item.description }}</p>
        <div class="task-facts">
          <span>{{ wordRangeText(item) }}</span>
          <span>采纳 {{ item.adoptedCount }}/{{ item.neededCount }} 篇</span>
          <span class="task-submission-count">{{ item.submissionCount || 0 }} 人投稿</span>
        </div>
        <div class="task-card-bottom"><span>{{ deadlineText(item) }}</span><span class="detail-link">查看详情 <span>→</span></span></div>
        <div class="task-free-create" @click.stop="freeCreateVisible = true">
          <span class="task-free-create-icon">✨</span>
          <span class="task-free-create-text">还没有类似的文章，去自由创作</span>
          <span class="task-free-create-arrow">→</span>
        </div>
      </ListCard>
    </div>
    <div v-else class="task-list">
      <ListCard
        v-for="item in visibleItems"
        :key="item.id"
        tag="article"
        clickable
        custom-class="submission-card"
        @click="goDetail(item.taskId || item.id)"
      >
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
      </ListCard>
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

    <FreeCreateModal
      v-model:visible="freeCreateVisible"
      :plan="currentPlan"
      @success="refresh"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useCommission } from '@/composables/useCommission'
import { useIsMobile } from '@/composables/useMobile.js'
import Tabs from '@/components/common/Tabs.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatCardGroup from '@/components/common/StatCardGroup.vue'
import ListCard from '@/components/common/ListCard.vue'
import FreeCreateModal from '@/views/console/create/FreeCreateModal.vue'
import { useSelfMediaPlan } from '@/composables/useSelfMediaPlan.js'

const router = useRouter()
const isMobile = useIsMobile()
const { tasks, mySubmissions, stats, loading, page, pageSize, total, loadTasks, loadMySubmissions, loadStats } = useCommission()
const { currentPlan, fetchCurrentPlan } = useSelfMediaPlan()
const activeTab = ref('all')
const rulesExpanded = ref(!isMobile.value)
const freeCreateVisible = ref(false)
const tabItems = [
  { value: 'all', label: '全部' },
  { value: 'pending', label: '投递中' },
  { value: 'selecting', label: '评选中' },
  { value: 'completed', label: '已完成' },
  { value: 'mine', label: '我投稿的' }
]
const statusMap = {
  all: null,
  pending: 0,
  selecting: 1,
  completed: 2
}

const visibleItems = computed(() => activeTab.value === 'mine' ? mySubmissions.value : tasks.value)

const activeTaskCount = computed(() => Number(stats.value?.activeTaskCount) || 0)
const mySubmissionCount = computed(() => Number(stats.value?.mySubmissionCount) || 0)
const earnedCoinTotal = computed(() => Number(stats.value?.earnedCoinTotal) || 0)

async function refresh() {
  try {
    if (activeTab.value === 'mine') await loadMySubmissions()
    else await loadTasks({ status: statusMap[activeTab.value] })
  } catch (error) {
    message.error(error.message || '约稿任务加载失败')
  }
}

onMounted(async () => {
  await Promise.all([refresh(), loadStats(), fetchCurrentPlan()])
})
watch(activeTab, () => {
  page.value = 1
  refresh()
})
watch([page, pageSize], refresh)
watch(isMobile, (mobile) => {
  rulesExpanded.value = !mobile
})

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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: -16px -16px 16px;
  padding: 20px 18px;
  background: linear-gradient(180deg, #FFF5F7 0%, #fff 100%);
  border-bottom: 1px solid #f0f0f0;
}
.commission-header__left {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}
.commission-header__logo {
  height: 48px;
  width: auto;
  max-width: 150px;
  object-fit: contain;
  border-radius: 10px;
  position: relative;
  z-index: 1;
}
.commission-header__subtitle {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.5;
  margin: 0;
  position: relative;
  z-index: 1;
}
.commission-header__donuts {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.commission-header__donuts span {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, transparent 42%, rgba(255, 36, 66, 0.12) 43%, rgba(255, 36, 66, 0.12) 66%, transparent 67%);
}
.commission-header__donuts span:nth-child(1) {
  width: 56px;
  height: 56px;
  top: -10px;
  left: 95px;
}
.commission-header__donuts span:nth-child(2) {
  width: 92px;
  height: 92px;
  bottom: -30px;
  left: -22px;
}
.commission-header__illustration {
  height: 110px;
  width: auto;
  object-fit: contain;
  flex-shrink: 0;
}

/* Stats */
.commission-stats {
  margin-bottom: 16px;
}

.commission-tabs {
  margin-bottom: 16px;
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

/* Task list */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card,
.submission-card {
  padding: 18px;
  border-radius: 18px;
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
.task-submission-count {
  color: var(--color-primary, #ff2442);
  font-weight: 600;
  background: rgba(255, 36, 66, 0.08);
  padding: 2px 8px;
  border-radius: 999px;
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

.task-free-create {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 12px;
  background: #fff7f9;
  border: 1px dashed rgba(255, 36, 66, 0.25);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.task-free-create:hover {
  background: #ffedf1;
  border-color: rgba(255, 36, 66, 0.45);
}
.task-free-create-icon {
  font-size: 16px;
  line-height: 1;
}
.task-free-create-text {
  flex: 1;
  font-size: 13px;
  color: #595959;
  line-height: 1.5;
}
.task-free-create-arrow {
  color: var(--color-primary, #ff2442);
  font-size: 13px;
  font-weight: 500;
  transition: transform 0.2s ease;
}
.task-free-create:hover .task-free-create-arrow {
  transform: translateX(3px);
}

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
.pagination-bar :deep(.ant-pagination-item-active) {
  background: var(--color-primary, #ff2442);
  border-color: var(--color-primary, #ff2442);
}
.pagination-bar :deep(.ant-pagination-item-active a) {
  color: #fff;
}
.pagination-bar :deep(.ant-pagination-item:hover),
.pagination-bar :deep(.ant-pagination-prev:hover .ant-pagination-item-link),
.pagination-bar :deep(.ant-pagination-next:hover .ant-pagination-item-link) {
  border-color: var(--color-primary, #ff2442);
  color: var(--color-primary, #ff2442);
}
.pagination-bar :deep(.ant-pagination-item:hover a) {
  color: var(--color-primary, #ff2442);
}
.pagination-bar :deep(.ant-pagination-options-size-changer .ant-select-selector:hover),
.pagination-bar :deep(.ant-pagination-options-size-changer .ant-select-focused .ant-select-selector) {
  border-color: var(--color-primary, #ff2442);
}
.pagination-bar :deep(.ant-select-item-option-selected) {
  background-color: rgba(255, 36, 66, 0.12);
  color: var(--color-primary, #ff2442);
}

/* 暗色主题 */
body[data-theme="dark"] .commission-page { background: #141414; }
body[data-theme="dark"] .commission-header {
  background: linear-gradient(180deg, #2a1f22 0%, #141414 100%);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .commission-header__subtitle { color: #a6a6a6; }
body[data-theme="dark"] .commission-header__donuts span { background: radial-gradient(circle, transparent 42%, rgba(255, 36, 66, 0.22) 43%, rgba(255, 36, 66, 0.22) 66%, transparent 67%); }
body[data-theme="dark"] .commission-rules,
body[data-theme="dark"] .task-card,
body[data-theme="dark"] .submission-card,
body[data-theme="dark"] .empty-block { background: #1f1f1f; }
body[data-theme="dark"] .commission-rules__header h3 { color: #f5f5f5; }
body[data-theme="dark"] .commission-rules li { color: #a6a6a6; }
body[data-theme="dark"] .task-card h2,
body[data-theme="dark"] .submission-task-title { color: #f5f5f5; }
body[data-theme="dark"] .task-desc,
body[data-theme="dark"] .task-facts,
body[data-theme="dark"] .submission-meta,
body[data-theme="dark"] .task-card-bottom { color: #a6a6a6; }
body[data-theme="dark"] .task-card-bottom { border-top-color: #303030; }
body[data-theme="dark"] .task-free-create {
  background: #2a1f22;
  border-color: rgba(255, 77, 111, 0.35);
}
body[data-theme="dark"] .task-free-create:hover {
  background: #3a252a;
  border-color: rgba(255, 77, 111, 0.55);
}
body[data-theme="dark"] .task-free-create-text { color: #a6a6a6; }
body[data-theme="dark"] .task-submission-count {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}
body[data-theme="dark"] .status-2 { color: #a6a6a6; background: #262626; }
body[data-theme="dark"] .submission-2 { color: #a6a6a6; background: #262626; }
body[data-theme="dark"] .submission-3 { color: #8c8c8c; background: #1a1a1a; }
body[data-theme="dark"] .pagination-bar :deep(.ant-pagination-item-active a) { color: #fff; }
body[data-theme="dark"] .pagination-bar :deep(.ant-select-item-option-selected) { background-color: rgba(255, 36, 66, 0.22); }

/* 移动端：统计卡片改为左右结构（图标在左，数字/标签在右水平排列） */
@media (max-width: 768px) {
  .commission-stats :deep(.stat-card--commission) {
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    padding: 12px 14px;
    gap: 12px;
  }

  .commission-stats :deep(.stat-card--commission) .stat-card__image {
    position: static;
    order: -1;
    width: 44px;
    height: 44px;
    margin: 0;
    flex-shrink: 0;
  }

  .commission-stats :deep(.stat-card--commission) .stat-card__body {
    flex-direction: row;
    align-items: baseline;
    justify-content: flex-start;
    gap: 8px;
    flex: 0 1 auto;
    min-width: 0;
  }

  .commission-stats :deep(.stat-card--commission) .stat-card__value {
    font-size: 22px;
  }

  .commission-stats :deep(.stat-card--commission) .stat-card__label {
    font-size: 13px;
  }
}

/* PC 端适配 */
@media (min-width: 769px) {
  .commission-page {
    padding: 28px 40px;
  }

  /* 渐变 hero + 横向统计 */
  .commission-hero {
    position: relative;
    padding: 32px 36px;
    margin-bottom: 24px;
    border-radius: 24px;
    background: linear-gradient(135deg, #ffe7ec 0%, #f3e8ff 60%, #fde7f3 100%);
    display: grid;
    grid-template-columns: 1.4fr auto;
    grid-template-areas: 'copy stats';
    gap: 24px;
    overflow: hidden;
    box-shadow: 0 8px 28px rgba(255, 36, 66, 0.08);
  }
  .hero-copy { grid-area: copy; position: relative; z-index: 2; }
  .eyebrow {
    display: inline-block;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 2px;
    color: var(--color-primary, #ff2442);
    background: rgba(255, 255, 255, 0.6);
    padding: 4px 10px;
    border-radius: 999px;
  }
  .commission-hero h1 {
    margin: 12px 0 8px;
    font-size: 28px;
    font-weight: 700;
    color: #1f1f1f;
  }
  .commission-hero p {
    margin: 0;
    color: #595959;
    font-size: 14px;
    line-height: 1.7;
    max-width: 520px;
  }
  .hero-orbit {
    position: absolute;
    top: -40px;
    right: 38%;
    width: 220px;
    height: 220px;
    pointer-events: none;
    opacity: 0.6;
  }
  .hero-orbit span {
    position: absolute;
    border-radius: 50%;
    border: 1.5px solid rgba(255, 36, 66, 0.25);
  }
  .hero-orbit span:nth-child(1) {
    width: 220px;
    height: 220px;
    top: 0;
    left: 0;
    border-style: dashed;
    animation: orbit-spin 22s linear infinite;
  }
  .hero-orbit span:nth-child(2) {
    width: 130px;
    height: 130px;
    top: 45px;
    left: 45px;
    background: radial-gradient(circle at 30% 30%, rgba(255, 36, 66, 0.18), transparent 70%);
  }
  @keyframes orbit-spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }
  .hero-stats {
    grid-area: stats;
    display: grid;
    grid-template-columns: repeat(3, minmax(120px, 1fr));
    gap: 12px;
    align-self: center;
    position: relative;
    z-index: 2;
  }

  /* 规则区：PC 上默认展开、无折叠按钮 */
  .commission-rules {
    padding: 20px 24px;
    margin-bottom: 20px;
  }
  .commission-rules__header {
    cursor: default;
  }
  .commission-rules__toggle {
    display: none;
  }
  .commission-rules ul {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px 28px;
    margin-top: 12px;
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
  .empty-block {
    padding: 72px 20px;
  }
  .pagination-bar {
    justify-content: flex-end;
    margin-top: 20px;
  }

  /* PC 暗色 */
  body[data-theme="dark"] .commission-hero {
    background: linear-gradient(135deg, #3a1f2a 0%, #2a1f3d 60%, #2a1f30 100%);
  }
  body[data-theme="dark"] .eyebrow {
    background: rgba(255, 255, 255, 0.12);
    color: #ff6b81;
  }
  body[data-theme="dark"] .commission-hero h1 { color: #f5f5f5; }
  body[data-theme="dark"] .commission-hero p { color: #bfbfbf; }
  body[data-theme="dark"] .hero-orbit span { border-color: rgba(255, 36, 66, 0.35); }
  body[data-theme="dark"] .hero-orbit span:nth-child(2) {
    background: radial-gradient(circle at 30% 30%, rgba(255, 36, 66, 0.22), transparent 70%);
  }
}

/* 大屏三列 */
@media (min-width: 1280px) {
  .task-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
