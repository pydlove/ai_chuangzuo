<template>
  <div class="commission-page">
    <section class="commission-hero">
      <div class="hero-copy">
        <span class="eyebrow">OFFICIAL COMMISSION</span>
        <h1>官方约稿任务</h1>
        <p>挑选合适的任务，使用你在爱创作中生成完成的文章参与投稿。稿件采纳后，奖励全额发放。</p>
      </div>
      <div class="hero-orbit" aria-hidden="true"><span></span><span></span></div>
      <div class="hero-stats">
        <div><strong>{{ activeTaskCount }}</strong><span>进行中的任务</span></div>
        <div><strong>{{ mySubmissionCount }}</strong><span>我的投稿</span></div>
        <div><strong>{{ earnedCoinTotal }}</strong><span>已获得创作币</span></div>
      </div>
    </section>

    <div class="commission-switcher" role="tablist">
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
    <div v-else-if="tab === 'all'" class="task-grid">
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
        </div>
        <div class="task-card-bottom"><span>{{ deadlineText(item) }}</span><span class="detail-link">查看详情 <span>→</span></span></div>
      </article>
    </div>
    <div v-else class="task-grid submission-grid">
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useCommission } from '@/composables/useCommission'

const router = useRouter()
const { tasks, mySubmissions, loading, loadTasks, loadMySubmissions } = useCommission()
const tab = ref('all')
const status = ref(null)
const filters = [
  { label: '全部', value: null },
  { label: '招募中', value: 0 },
  { label: '已截止待采纳', value: 1 },
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
    if (tab.value === 'mine') await loadMySubmissions({ page: 1, pageSize: 50 })
    else await loadTasks({ status: status.value, page: 1, pageSize: 50 })
  } catch (error) {
    message.error(error.message || '约稿任务加载失败')
  }
}

onMounted(refresh)
watch([tab, status], refresh)

function taskStatus(value) {
  return ['招募中', '已截止待采纳', '已完成'][value] || '未知状态'
}
function submissionStatus(value) {
  return ['等待采纳', '已采纳', '未采纳', '已撤回'][value] || '未知状态'
}
function deadlineText(task) {
  const deadline = new Date(task.deadlineAt)
  if (task.status !== 0 || deadline <= new Date()) return `截止于 ${deadline.toLocaleString()}`
  const hours = Math.max(1, Math.ceil((deadline - Date.now()) / 3600000))
  return hours > 24 ? `还剩 ${Math.ceil(hours / 24)} 天` : `还剩 ${hours} 小时`
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
.commission-page { max-width: 1120px; margin: 0 auto; }

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
.commission-hero h1 { margin: 12px 0 8px; font-size: 28px; font-weight: 700; color: #1f1f1f; }
.commission-hero p { margin: 0; color: #595959; font-size: 14px; line-height: 1.7; max-width: 520px; }

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
  width: 220px; height: 220px;
  top: 0; left: 0;
  border-style: dashed;
  animation: orbit-spin 22s linear infinite;
}
.hero-orbit span:nth-child(2) {
  width: 130px; height: 130px;
  top: 45px; left: 45px;
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
.hero-stats > div {
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(6px);
  border-radius: 16px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 120px;
}
.hero-stats strong {
  font-size: 24px;
  color: var(--color-primary, #ff2442);
  font-weight: 700;
}
.hero-stats span {
  font-size: 12px;
  color: #595959;
}

.commission-switcher {
  display: inline-flex;
  padding: 4px;
  background: #f5f5f5;
  border-radius: 999px;
  margin-bottom: 16px;
}
.commission-switcher button {
  padding: 8px 22px;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: #595959;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.commission-switcher button.active {
  background: #fff;
  color: var(--color-primary, #ff2442);
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.commission-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}
.filter-chip {
  padding: 6px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 999px;
  background: #fff;
  color: #595959;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.filter-chip:hover { border-color: var(--color-primary, #ff2442); color: var(--color-primary, #ff2442); }
.filter-chip.active {
  background: var(--color-primary, #ff2442);
  color: #fff;
  border-color: transparent;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.submission-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }

.task-card,
.submission-card {
  position: relative;
  padding: 22px 22px 20px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
}
.task-card:hover,
.submission-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.08);
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
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}
.status-0 { color: #1677ff; background: #e6f4ff; }
.status-1 { color: #fa8c16; background: #fff4e6; }
.status-2 { color: #07c160; background: #e6f7ed; }
.submission-0 { color: #1677ff; background: #e6f4ff; }
.submission-1 { color: #07c160; background: #e6f7ed; }
.submission-2 { color: #8c8c8c; background: #f5f5f5; }
.submission-3 { color: #bfbfbf; background: #fafafa; }

.task-reward {
  font-size: 22px;
  color: var(--color-primary, #ff2442);
  font-weight: 700;
  white-space: nowrap;
}
.task-reward small {
  font-size: 12px;
  font-weight: 500;
  color: #8c8c8c;
  margin-left: 2px;
}

.task-card h2 {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.4;
}
.task-desc {
  margin: 0 0 14px;
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
  gap: 14px;
  color: #595959;
  font-size: 12px;
  margin-bottom: 14px;
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
}
.detail-link span { margin-left: 4px; transition: transform 0.2s ease; display: inline-block; }
.task-card:hover .detail-link span,
.submission-card:hover .detail-link span { transform: translateX(3px); }

.submission-task-title { font-size: 16px; }
.submission-article { margin-bottom: 12px; }
.submission-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #8c8c8c;
  font-size: 12px;
  margin-bottom: 12px;
}
.reward-collected { color: #07c160; font-weight: 500; }

.empty-block {
  padding: 72px 20px;
  text-align: center;
  color: #8c8c8c;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}

body[data-theme="dark"] .task-card,
body[data-theme="dark"] .submission-card,
body[data-theme="dark"] .empty-block { background: #1f1f1f; }
body[data-theme="dark"] .commission-hero { background: linear-gradient(135deg, #3a1f2a 0%, #2a1f3d 60%, #3a1f30 100%); }
body[data-theme="dark"] .hero-stats > div { background: rgba(0, 0, 0, 0.35); }
body[data-theme="dark"] .hero-stats span { color: #bfbfbf; }
body[data-theme="dark"] .commission-hero h1 { color: #f5f5f5; }
body[data-theme="dark"] .commission-hero p { color: #d9d9d9; }
body[data-theme="dark"] .commission-switcher { background: #262626; }
body[data-theme="dark"] .commission-switcher button { color: #bfbfbf; }
body[data-theme="dark"] .commission-switcher button.active { background: #1f1f1f; }
body[data-theme="dark"] .filter-chip { background: #262626; border-color: #404040; color: #d9d9d9; }
body[data-theme="dark"] .task-card h2,
body[data-theme="dark"] .submission-task-title { color: #f5f5f5; }
body[data-theme="dark"] .task-desc,
body[data-theme="dark"] .task-facts,
body[data-theme="dark"] .submission-meta,
body[data-theme="dark"] .task-card-bottom { color: #bfbfbf; }
body[data-theme="dark"] .task-card-bottom { border-top-color: #303030; }
body[data-theme="dark"] .eyebrow { background: rgba(255, 255, 255, 0.08); }

@media (max-width: 1024px) {
  .task-grid, .submission-grid { grid-template-columns: 1fr; }
  .commission-hero { grid-template-columns: 1fr; grid-template-areas: 'copy' 'stats'; }
  .hero-orbit { display: none; }
  .hero-stats { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .commission-hero {
    grid-template-columns: minmax(0, 1fr);
    grid-template-areas: 'copy' 'stats';
    padding: 24px 20px;
    border-radius: 20px;
  }
  .commission-hero h1 { font-size: 22px; }
  .hero-orbit { display: none; }
  .hero-stats {
    display: flex;
    grid-template-columns: none;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
  }
  .hero-stats::-webkit-scrollbar { display: none; }
  .hero-stats > div {
    flex: 0 0 132px;
    min-width: 132px;
    padding: 14px 16px;
  }
  .task-grid,
  .submission-grid { grid-template-columns: minmax(0, 1fr); }
  .task-card,
  .submission-card { padding: 16px; }
  .task-card-top {
    min-height: 32px;
    padding-right: 118px;
  }
  .task-reward {
    position: absolute;
    top: 16px;
    right: 16px;
    max-width: 116px;
    text-align: right;
  }
  .task-desc {
    -webkit-line-clamp: 2;
    line-clamp: 2;
  }
  .commission-switcher button { padding: 7px 16px; font-size: 13px; }
}
</style>