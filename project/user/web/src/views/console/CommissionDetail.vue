<template>
  <div class="detail-page" v-if="task">
    <button class="back-btn" @click="goBack">← 返回列表</button>

    <!-- 任务信息卡 -->
    <div class="task-info-card">
      <div class="task-info-head">
        <span :class="['task-status-tag', `status-${task.status.toLowerCase()}`]">
          {{ statusLabel(task.status) }}
        </span>
        <h2 class="task-info-title">{{ task.title }}</h2>
        <div class="task-info-meta">
          <span class="meta-publisher">👤 {{ task.publisherNickname }}</span>
          <span class="meta-dot">·</span>
          <span class="meta-time">{{ formatTime(task.createdAt) }}发布</span>
        </div>
      </div>

      <div class="task-info-grid">
        <div class="info-cell">
          <span class="info-label">奖励</span>
          <span class="info-value reward">{{ task.rewardCoin }} 创作币</span>
        </div>
        <div class="info-cell">
          <span class="info-label">字数范围</span>
          <span class="info-value">{{ task.requirements.minWordCount }}~{{ task.requirements.maxWordCount }} 字</span>
        </div>
        <div class="info-cell">
          <span class="info-label">风格</span>
          <span class="info-value">{{ task.requirements.styleHint || '不限' }}</span>
        </div>
        <div class="info-cell">
          <span class="info-label">{{ task.status === 'OPEN' ? '剩余时间' : '已截止' }}</span>
          <span :class="['info-value', { 'deadline-danger': deadlineInfo.danger }]">
            {{ deadlineInfo.text }}
          </span>
        </div>
      </div>

      <div class="task-info-desc">
        <div class="desc-label">需求描述</div>
        <p class="desc-content">{{ task.description }}</p>
      </div>
    </div>

    <!-- ============ 发布者视角 ============ -->
    <template v-if="isPublisher">
      <div class="action-bar">
        <h3 class="section-title">
          投稿列表 ({{ taskSubmissions.length }}人)
        </h3>
        <div class="action-buttons">
          <button v-if="canCancel" class="btn-secondary" @click="onCancelTask">撤回任务</button>
        </div>
      </div>

      <div v-if="taskSubmissions.length === 0" class="empty-block">
        <p>暂无投稿,耐心等待</p>
        <p v-if="canCancel" class="empty-hint">没有投稿时可以撤回任务,奖励会全额退回</p>
      </div>

      <div v-else class="submission-list">
        <div
          v-for="s in taskSubmissions"
          :key="s.id"
          :class="['submission-row', { 'submission-row-winner': task.winnerSubmissionId === s.id }]"
        >
          <div class="avatar" :style="{ background: avatarColor(s.submitterNickname) }">
            {{ s.submitterNickname.charAt(0) }}
          </div>
          <div class="submission-meta">
            <div class="submission-line1">
              <span class="submission-nick">{{ s.submitterNickname }}</span>
              <span v-if="task.winnerSubmissionId === s.id" class="winner-tag">🏆 中标</span>
            </div>
            <div class="submission-line2">
              <span class="submission-title">《{{ s.articleTitle }}》</span>
              <span class="submission-wordcount">{{ s.wordCount }} 字</span>
            </div>
            <div class="submission-line3">{{ formatTime(s.submittedAt) }}投递</div>
          </div>
          <div class="submission-actions">
            <button v-if="task.status === 'OPEN'" class="btn-primary" @click="onPickWinner(s.id, s.submitterNickname)">
              选用 TA
            </button>
            <span v-else-if="task.winnerSubmissionId === s.id" class="winner-text">
              实得 {{ winnerPayout }} 币
            </span>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 投稿人/旁观者视角 ============ -->
    <template v-else>
      <div v-if="mySub" class="my-sub-card">
        <div class="my-sub-header">
          <span class="my-sub-label">我的投递</span>
          <span v-if="task.winnerSubmissionId === mySub.id" class="winner-tag large">🏆 已中标</span>
        </div>
        <div class="my-sub-title">《{{ mySub.articleTitle }}》</div>
        <div class="my-sub-meta">{{ mySub.wordCount }} 字 · {{ formatTime(mySub.submittedAt) }}投递</div>

        <div v-if="task.winnerSubmissionId === mySub.id" class="my-sub-reward">
          实得 <b>{{ winnerPayout }}</b> 创作币(已扣除平台抽成 10%)
        </div>

        <div class="my-sub-actions">
          <button v-if="canWithdraw" class="btn-secondary" @click="onWithdraw">撤回投递</button>
          <button v-if="canSubmit" class="btn-primary" @click="openPicker">立即投稿</button>
        </div>
      </div>

      <div v-else-if="canSubmit" class="empty-block">
        <p>你还没有投递此任务</p>
        <button class="btn-primary" @click="openPicker">立即投稿</button>
      </div>

      <div v-else-if="task.status !== 'OPEN'" class="empty-block">
        <p>{{ task.status === 'SETTLED' ? '任务已结算' : '任务已结束' }}</p>
      </div>

      <div class="other-subs-card">
        <div class="other-subs-header" @click="showOthers = !showOthers">
          <span>查看其他 {{ taskSubmissions.length }} 位投稿人</span>
          <span class="expand-arrow">{{ showOthers ? '▾' : '▸' }}</span>
        </div>
        <div v-if="showOthers" class="other-subs-list">
          <div v-if="taskSubmissions.length === 0" class="empty-block mini">
            <p>暂无其他投稿人</p>
          </div>
          <div
            v-for="s in taskSubmissions"
            :key="s.id"
            class="other-sub-row"
          >
            <div class="avatar small" :style="{ background: avatarColor(s.submitterNickname) }">
              {{ s.submitterNickname.charAt(0) }}
            </div>
            <span class="other-sub-nick">{{ s.submitterNickname }}</span>
            <span class="other-sub-words">{{ s.wordCount }} 字</span>
            <span v-if="task.winnerSubmissionId === s.id" class="winner-tag mini">🏆</span>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 投稿选择器 ============ -->
    <a-modal
      v-model:open="pickerVisible"
      :footer="null"
      :width="560"
      centered
      class="picker-modal"
      title="选择要投递的文章"
    >
      <div v-if="myArticles.length === 0" class="picker-empty">
        <p>还没有已生成的文章</p>
        <button class="btn-primary" @click="goCreate">去创作</button>
      </div>

      <div v-else class="picker-list">
        <div
          v-for="a in myArticles"
          :key="a.bizNo"
          :class="[
            'picker-item',
            {
              active: selectedBizNo === a.bizNo,
              disabled: !isArticleInRange(a)
            }
          ]"
          @click="isArticleInRange(a) && (selectedBizNo = a.bizNo)"
        >
          <div class="picker-item-title">{{ a.title }}</div>
          <div class="picker-item-meta">
            <span class="picker-item-platform">{{ a.platformName }}</span>
            <span class="picker-item-dot">·</span>
            <span class="picker-item-words">{{ a.wordCount }} 字</span>
            <span class="picker-item-dot">·</span>
            <span class="picker-item-time">{{ formatTime(a.completedAt) }}</span>
          </div>
          <div v-if="!isArticleInRange(a)" class="picker-item-warn">
            字数不符(要求 {{ task.requirements.minWordCount }}-{{ task.requirements.maxWordCount }})
          </div>
        </div>
      </div>

      <div v-if="myArticles.length > 0" class="picker-actions">
        <button class="btn-secondary" @click="pickerVisible = false">取消</button>
        <button class="btn-primary" :disabled="!canConfirmPicker" @click="confirmSubmit">
          确认投递
        </button>
      </div>
    </a-modal>
  </div>

  <div v-else class="detail-missing">
    <p>任务不存在或已删除</p>
    <button class="btn-primary" @click="goBack">返回列表</button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { useCommission } from '@/composables/useCommission'
import { useWorks } from '@/composables/useWorks'
import { COMMISSION_CONFIG } from '@/api/commission'

const route = useRoute()
const router = useRouter()
const {
  currentUserId, coinBalance,
  getTask, getSubmissionsOfTask, mySubmissionForTask,
  startReconcile,
  cancelTask, submitToTask, withdrawSubmission, pickWinner
} = useCommission()
const { articles, load: loadWorks } = useWorks()

const taskId = computed(() => route.params.id)
const now = ref(Date.now())
let tick = null

onMounted(async () => {
  startReconcile()
  tick = setInterval(() => { now.value = Date.now() }, 60_000)
  try {
    await loadWorks({ page: 1, pageSize: 50 })
  } catch {
    // 后端未实现时 useWorks 内部已处理,这里静默
  }
})
onUnmounted(() => { if (tick) clearInterval(tick) })

const task = computed(() => getTask(taskId.value))
const taskSubmissions = computed(() => task.value ? getSubmissionsOfTask(task.value.id) : [])
const mySub = computed(() => task.value ? mySubmissionForTask(task.value.id) : null)

const isPublisher = computed(() => task.value?.publisherId === currentUserId.value)

const canSubmit = computed(() => {
  if (!task.value) return false
  if (task.value.status !== 'OPEN') return false
  if (isPublisher.value) return false
  if (mySub.value) return false
  return true
})

const canWithdraw = computed(() => {
  if (!mySub.value) return false
  if (task.value?.status !== 'OPEN') return false
  return true
})

const canCancel = computed(() => {
  if (!task.value || !isPublisher.value) return false
  if (task.value.status !== 'OPEN') return false
  if (taskSubmissions.value.length > 0) return false
  return true
})

const winnerPayout = computed(() => {
  if (!task.value) return 0
  const fee = Math.floor(task.value.rewardCoin * task.value.platformFeeRate)
  return task.value.rewardCoin - fee
})

const deadlineInfo = computed(() => {
  const t = task.value
  if (!t) return { text: '', danger: false }
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
})

function statusLabel(s) {
  return { OPEN: '进行中', SETTLED: '已结算', EXPIRED: '已流局', CANCELLED: '已撤销' }[s] || s
}

function avatarColor(nick) {
  // 简单 hash 选色
  const colors = ['#FF6B8A', '#FFB86C', '#FFD93D', '#6BCB77', '#4D96FF', '#9D6BFF', '#FF8FB1']
  let hash = 0
  for (let i = 0; i < nick.length; i++) hash = (hash * 31 + nick.charCodeAt(i)) | 0
  return colors[Math.abs(hash) % colors.length]
}

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const nowD = new Date()
  const diff = Math.floor((nowD - d) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 86400 * 7) return Math.floor(diff / 86400) + ' 天前'
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// ========== 发布者操作 ==========
function onCancelTask() {
  if (!task.value) return
  Modal.confirm({
    title: '确认撤回任务?',
    content: `撤回后将退还 ${task.value.rewardCoin} 创作币,任务变为"已撤销"。`,
    okText: '确认撤回',
    cancelText: '取消',
    centered: true,
    okType: 'danger',
    onOk: () => {
      const r = cancelTask(task.value.id)
      if (!r.ok) return message.warning(r.error)
      message.success(`已撤销,退还 ${task.value.rewardCoin} 创作币`)
    }
  })
}

function onPickWinner(submissionId, nick) {
  if (!task.value) return
  Modal.confirm({
    title: '确认选用此稿件?',
    content: `将结算给 ${nick},实得 ${winnerPayout.value} 创作币,平台抽成 ${Math.floor(task.value.rewardCoin * task.value.platformFeeRate)} 创作币。结算后不可撤销。`,
    okText: '确认结算',
    cancelText: '取消',
    centered: true,
    onOk: () => {
      const r = pickWinner(task.value.id, submissionId)
      if (!r.ok) return message.warning(r.error)
      message.success(`已结算,${nick} 获得 ${r.payout} 创作币`)
    }
  })
}

// ========== 投稿人操作 ==========
function onWithdraw() {
  if (!mySub.value) return
  Modal.confirm({
    title: '确认撤回投递?',
    content: '撤回后可以重新投递其他文章。',
    okText: '确认撤回',
    cancelText: '取消',
    centered: true,
    onOk: () => {
      const r = withdrawSubmission(mySub.value.id)
      if (!r.ok) return message.warning(r.error)
      message.success('已撤回投递')
    }
  })
}

// ========== 投稿选择器 ==========
const pickerVisible = ref(false)
const selectedBizNo = ref(null)
const showOthers = ref(false)

const myArticles = computed(() => articles.value || [])

function isArticleInRange(a) {
  if (!task.value) return false
  return a.wordCount >= task.value.requirements.minWordCount &&
         a.wordCount <= task.value.requirements.maxWordCount
}

const canConfirmPicker = computed(() => {
  if (!selectedBizNo.value) return false
  const a = myArticles.value.find(x => x.bizNo === selectedBizNo.value)
  return a && isArticleInRange(a)
})

function openPicker() {
  selectedBizNo.value = null
  pickerVisible.value = true
}

function confirmSubmit() {
  if (!task.value || !selectedBizNo.value) return
  const a = myArticles.value.find(x => x.bizNo === selectedBizNo.value)
  if (!a) return
  const r = submitToTask(task.value.id, {
    bizNo: a.bizNo,
    title: a.title,
    wordCount: a.wordCount
  })
  if (!r.ok) {
    message.warning(r.error)
    return
  }
  message.success('投递成功,等待发布者选择')
  pickerVisible.value = false
}

function goCreate() {
  pickerVisible.value = false
  router.push('/console/create')
}

function goBack() {
  router.push('/console/commission')
}
</script>

<style scoped>
.detail-page {
  max-width: 880px;
  margin: 0 auto;
}

.back-btn {
  display: inline-block;
  margin-bottom: 16px;
  padding: 6px 12px;
  background: none;
  border: none;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--color-primary-light, rgba(255, 36, 66, 0.08));
  color: var(--color-primary, #FF2442);
}

.task-info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.task-info-head {
  margin-bottom: 16px;
}

.task-status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  margin-bottom: 8px;
}

.task-status-tag.status-open {
  background: #e6f4ff;
  color: #1677ff;
}

.task-status-tag.status-settled {
  background: #e6f7ed;
  color: #07c160;
}

.task-status-tag.status-expired {
  background: #f5f5f5;
  color: #8c8c8c;
}

.task-status-tag.status-cancelled {
  background: #fff7e6;
  color: #fa8c16;
}

.task-info-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.task-info-meta {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 6px;
}

.task-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
}

.info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 11px;
  color: #8c8c8c;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.info-value.reward {
  color: var(--color-primary, #FF2442);
  font-size: 16px;
}

.info-value.deadline-danger {
  color: #fa8c16;
}

.task-info-desc {
  border-top: 1px dashed #f0f0f0;
  padding-top: 16px;
}

.desc-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.desc-content {
  font-size: 14px;
  color: #262626;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.btn-secondary,
.btn-primary {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-secondary {
  background: #fff;
  color: #595959;
  border: 1px solid #d9d9d9;
}

.btn-secondary:hover:not(:disabled) {
  border-color: var(--color-primary, #FF2442);
  color: var(--color-primary, #FF2442);
}

.btn-primary {
  background: var(--color-primary, #FF2442);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #e0203b;
}

.btn-primary:disabled,
.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-block {
  background: #fff;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
  margin-bottom: 20px;
}

.empty-block p {
  margin: 0 0 12px;
}

.empty-hint {
  font-size: 12px;
  color: #bfbfbf;
}

.empty-block.mini {
  padding: 20px;
  font-size: 13px;
}

.submission-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.submission-row {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.submission-row-winner {
  background: linear-gradient(135deg, #fff5f7, #fff0f2);
  border: 1px solid rgba(255, 36, 66, 0.2);
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.avatar.small {
  width: 28px;
  height: 28px;
  font-size: 13px;
}

.submission-meta {
  flex: 1;
  min-width: 0;
}

.submission-line1 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.submission-nick {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.winner-tag {
  background: #fff7e6;
  color: #fa8c16;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.winner-tag.large {
  padding: 3px 12px;
  font-size: 12px;
}

.winner-tag.mini {
  padding: 1px 6px;
}

.submission-line2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 13px;
  color: #262626;
}

.submission-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.submission-wordcount {
  flex-shrink: 0;
  color: #8c8c8c;
  font-size: 12px;
}

.submission-line3 {
  font-size: 11px;
  color: #8c8c8c;
}

.submission-actions {
  flex-shrink: 0;
}

.winner-text {
  font-size: 13px;
  color: var(--color-primary, #FF2442);
  font-weight: 600;
}

.my-sub-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.my-sub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.my-sub-label {
  font-size: 13px;
  color: #8c8c8c;
  font-weight: 500;
}

.my-sub-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.my-sub-meta {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.my-sub-reward {
  background: linear-gradient(135deg, #e6f7ed, #d9f7be);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 14px;
  color: #07c160;
  margin-bottom: 12px;
}

.my-sub-reward b {
  font-size: 18px;
  font-weight: 700;
}

.my-sub-actions {
  display: flex;
  gap: 10px;
}

.other-subs-card {
  background: #fff;
  border-radius: 12px;
  margin-top: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.other-subs-header {
  padding: 14px 20px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: background 0.15s;
}

.other-subs-header:hover {
  background: #fafafa;
}

.expand-arrow {
  font-size: 12px;
  color: #8c8c8c;
}

.other-subs-list {
  border-top: 1px solid #f0f0f0;
  padding: 4px 0;
}

.other-sub-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  font-size: 13px;
}

.other-sub-nick {
  flex: 1;
  color: #262626;
}

.other-sub-words {
  color: #8c8c8c;
  font-size: 12px;
}

.detail-missing {
  max-width: 480px;
  margin: 80px auto;
  text-align: center;
  background: #fff;
  border-radius: 12px;
  padding: 60px 20px;
}

.detail-missing p {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 16px;
}

/* ============ 投稿选择器 ============ */
.picker-modal .ant-modal-body {
  padding: 16px 24px 20px;
}

.picker-empty {
  text-align: center;
  padding: 40px 0;
  color: #8c8c8c;
}

.picker-empty p {
  margin-bottom: 16px;
}

.picker-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 50vh;
  overflow-y: auto;
}

.picker-item {
  padding: 12px 14px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.picker-item:hover:not(.disabled) {
  border-color: var(--color-primary, #FF2442);
  background: var(--color-primary-light, rgba(255, 36, 66, 0.04));
}

.picker-item.active {
  border-color: var(--color-primary, #FF2442);
  background: var(--color-primary-light, rgba(255, 36, 66, 0.08));
}

.picker-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.picker-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.picker-item-meta {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.picker-item-dot {
  color: #d9d9d9;
}

.picker-item-warn {
  font-size: 11px;
  color: #fa8c16;
  margin-top: 4px;
}

.picker-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .task-info-card {
  background: #1f1f1f;
}

body[data-theme="dark"] .task-info-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .task-info-grid {
  background: #262626;
}

body[data-theme="dark"] .info-value {
  color: #e0e0e0;
}

body[data-theme="dark"] .desc-content {
  color: #d9d9d9;
}

body[data-theme="dark"] .section-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .empty-block {
  background: #1f1f1f;
}

body[data-theme="dark"] .submission-row {
  background: #1f1f1f;
}

body[data-theme="dark"] .submission-row-winner {
  background: linear-gradient(135deg, #2a1015, #1f0d12);
  border-color: rgba(255, 36, 66, 0.4);
}

body[data-theme="dark"] .submission-nick,
body[data-theme="dark"] .submission-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .my-sub-card,
body[data-theme="dark"] .other-subs-card {
  background: #1f1f1f;
}

body[data-theme="dark"] .my-sub-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .my-sub-reward {
  background: linear-gradient(135deg, rgba(7, 193, 96, 0.18), rgba(7, 193, 96, 0.08));
  color: #36cfc9;
}

body[data-theme="dark"] .other-subs-header:hover {
  background: #262626;
}

body[data-theme="dark"] .other-subs-list {
  border-top-color: #303030;
}

body[data-theme="dark"] .other-sub-nick {
  color: #e0e0e0;
}

body[data-theme="dark"] .detail-missing {
  background: #1f1f1f;
}

body[data-theme="dark"] .picker-item {
  background: #262626;
  border-color: #404040;
}

body[data-theme="dark"] .picker-item-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .picker-actions {
  border-top-color: #303030;
}

body[data-theme="dark"] .btn-secondary {
  background: #262626;
  border-color: #404040;
  color: #a6a6a6;
}

body[data-theme="dark"] .btn-secondary:hover:not(:disabled) {
  border-color: #ff4d6f;
  color: #ff4d6f;
}

/* ========== 移动端 ========== */
@media (max-width: 768px) {
  .task-info-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .task-info-card {
    padding: 18px 16px;
  }

  .submission-row {
    flex-wrap: wrap;
  }

  .submission-actions {
    width: 100%;
    margin-top: 8px;
    display: flex;
    justify-content: flex-end;
  }

  .my-sub-actions {
    flex-direction: column;
  }

  .btn-secondary,
  .btn-primary {
    width: 100%;
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}
</style>