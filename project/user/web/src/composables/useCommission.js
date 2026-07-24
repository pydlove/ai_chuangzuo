import { ref, computed, watch } from 'vue'
import {
  genId,
  COMMISSION_CONFIG
} from '@/api/commission'
import { DEMO_USER_ID, DEMO_USER_NICKNAMES, SEED_TASKS, SEED_SUBMISSIONS } from '@/data/commissionSeed'
import { useInviteStats } from '@/composables/useInviteStats'

const TASKS_KEY = 'aichuangzuo_commission_tasks'
const SUBS_KEY = 'aichuangzuo_commission_submissions'
const USER_KEY = 'aichuangzuo_commission_current_user'
const SEED_FLAG_KEY = 'aichuangzuo_commission_seeded'

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    if (raw == null) return fallback
    return JSON.parse(raw)
  } catch {
    return fallback
  }
}

function write(key, value) {
  try { localStorage.setItem(key, JSON.stringify(value)) } catch { /* ignore */ }
}

function ensureSeed() {
  if (localStorage.getItem(SEED_FLAG_KEY)) return
  write(TASKS_KEY, SEED_TASKS)
  write(SUBS_KEY, SEED_SUBMISSIONS)
  localStorage.setItem(SEED_FLAG_KEY, '1')
}

// 模块级状态,跨组件共享(单例)
ensureSeed()
const tasks = ref(read(TASKS_KEY, []))
const submissions = ref(read(SUBS_KEY, []))
const currentUserId = ref(localStorage.getItem(USER_KEY) || DEMO_USER_ID)

watch(tasks, (v) => write(TASKS_KEY, v), { deep: true })
watch(submissions, (v) => write(SUBS_KEY, v), { deep: true })
watch(currentUserId, (v) => localStorage.setItem(USER_KEY, v))

const { coinBalance, setCoinBalance } = useInviteStats()

// 把过期 OPEN 任务刷成 EXPIRED 并退款
function reconcileExpired() {
  const now = Date.now()
  let changed = false
  for (const t of tasks.value) {
    if (t.status === 'OPEN' && new Date(t.graceDeadlineAt).getTime() <= now) {
      t.status = 'EXPIRED'
      t.settledAt = new Date(now).toISOString()
      setCoinBalance(coinBalance.value + t.rewardCoin)
      changed = true
    }
  }
  if (changed) tasks.value = [...tasks.value]
}

// 页面 mount 后调一次,后续由 setInterval 每分钟轮询
let reconcileTimer = null
function startReconcile() {
  reconcileExpired()
  if (reconcileTimer) return
  reconcileTimer = setInterval(reconcileExpired, 60 * 1000)
}

const myNickname = computed(() => DEMO_USER_NICKNAMES[currentUserId.value] || '匿名用户')

function setCurrentUserId(id) {
  if (!DEMO_USER_NICKNAMES[id]) return
  currentUserId.value = id
}

const myPublishedTasks = computed(() =>
  tasks.value.filter(t => t.publisherId === currentUserId.value)
)

const mySubmissions = computed(() =>
  submissions.value.filter(s => s.submitterId === currentUserId.value && !s.withdrawnAt)
)

function getTask(id) {
  return tasks.value.find(t => t.id === id) || null
}

function getSubmissionsOfTask(taskId) {
  return submissions.value.filter(s => s.taskId === taskId && !s.withdrawnAt)
}

function mySubmissionForTask(taskId) {
  return submissions.value.find(s => s.taskId === taskId && s.submitterId === currentUserId.value && !s.withdrawnAt) || null
}

function createTask({ title, description, requirements, rewardCoin, deadlineDays }) {
  if (!title?.trim() || title.trim().length > 30) {
    return { ok: false, error: '标题不能为空且不超过 30 字' }
  }
  if (!description?.trim() || description.trim().length > 500) {
    return { ok: false, error: '需求描述不能为空且不超过 500 字' }
  }
  const min = Math.floor(requirements.minWordCount || 0)
  const max = Math.floor(requirements.maxWordCount || 0)
  if (min < 100 || max > 5000 || min > max) {
    return { ok: false, error: '字数范围需 100-5000,且下限不超过上限' }
  }
  if (rewardCoin < COMMISSION_CONFIG.MIN_REWARD || rewardCoin > COMMISSION_CONFIG.MAX_REWARD) {
    return { ok: false, error: `奖励需在 ${COMMISSION_CONFIG.MIN_REWARD}-${COMMISSION_CONFIG.MAX_REWARD} 创作币之间` }
  }
  if (![3, 7, 15].includes(deadlineDays)) {
    return { ok: false, error: '截止时间选项无效' }
  }
  if (coinBalance.value < rewardCoin) {
    return { ok: false, error: '余额不足,请先提现或做任务赚币', insufficient: true }
  }
  const now = Date.now()
  const deadline = new Date(now + deadlineDays * 24 * 60 * 60 * 1000)
  const grace = new Date(deadline.getTime() + COMMISSION_CONFIG.GRACE_HOURS * 60 * 60 * 1000)
  const task = {
    id: genId('cmt'),
    publisherId: currentUserId.value,
    publisherNickname: myNickname.value,
    title: title.trim(),
    description: description.trim(),
    requirements: { minWordCount: min, maxWordCount: max, styleHint: requirements.styleHint?.trim() || '' },
    rewardCoin,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: deadline.toISOString(),
    graceDeadlineAt: grace.toISOString(),
    status: 'OPEN',
    winnerSubmissionId: null,
    settledAt: null,
    createdAt: new Date(now).toISOString()
  }
  tasks.value = [task, ...tasks.value]
  setCoinBalance(coinBalance.value - rewardCoin)
  return { ok: true, task }
}

function cancelTask(taskId) {
  const t = getTask(taskId)
  if (!t) return { ok: false, error: '任务不存在' }
  if (t.publisherId !== currentUserId.value) return { ok: false, error: '只能撤销自己的任务' }
  if (t.status !== 'OPEN') return { ok: false, error: '当前状态不允许撤销' }
  const subs = getSubmissionsOfTask(taskId)
  if (subs.length > 0) return { ok: false, error: '已有投稿,无法撤销' }
  t.status = 'CANCELLED'
  t.settledAt = new Date().toISOString()
  tasks.value = [...tasks.value]
  setCoinBalance(coinBalance.value + t.rewardCoin)
  return { ok: true }
}

function submitToTask(taskId, article) {
  const t = getTask(taskId)
  if (!t) return { ok: false, error: '任务不存在' }
  if (t.status !== 'OPEN') return { ok: false, error: '任务已截止' }
  if (t.publisherId === currentUserId.value) return { ok: false, error: '不能给自己的任务投稿' }
  if (mySubmissionForTask(taskId)) return { ok: false, error: '你已投递过此任务' }
  if (article.wordCount < t.requirements.minWordCount || article.wordCount > t.requirements.maxWordCount) {
    return { ok: false, error: `字数需在 ${t.requirements.minWordCount}-${t.requirements.maxWordCount}` }
  }
  const sub = {
    id: genId('cms'),
    taskId,
    submitterId: currentUserId.value,
    submitterNickname: myNickname.value,
    articleBizNo: article.bizNo,
    articleTitle: article.title,
    wordCount: article.wordCount,
    submittedAt: new Date().toISOString(),
    withdrawnAt: null
  }
  submissions.value = [sub, ...submissions.value]
  return { ok: true, submission: sub }
}

function withdrawSubmission(submissionId) {
  const s = submissions.value.find(x => x.id === submissionId)
  if (!s) return { ok: false, error: '投稿不存在' }
  if (s.submitterId !== currentUserId.value) return { ok: false, error: '只能撤回自己的投稿' }
  const t = getTask(s.taskId)
  if (!t || t.status !== 'OPEN') return { ok: false, error: '任务已截止,无法撤回' }
  s.withdrawnAt = new Date().toISOString()
  submissions.value = [...submissions.value]
  return { ok: true }
}

function pickWinner(taskId, submissionId) {
  const t = getTask(taskId)
  if (!t) return { ok: false, error: '任务不存在' }
  if (t.publisherId !== currentUserId.value) return { ok: false, error: '只有发布者可以选人' }
  if (t.status !== 'OPEN') return { ok: false, error: '当前状态不允许结算' }
  const subs = getSubmissionsOfTask(taskId)
  const target = subs.find(s => s.id === submissionId)
  if (!target) return { ok: false, error: '投稿不存在或已撤回' }
  const fee = Math.floor(t.rewardCoin * t.platformFeeRate)
  const payout = t.rewardCoin - fee
  t.status = 'SETTLED'
  t.winnerSubmissionId = submissionId
  t.settledAt = new Date().toISOString()
  tasks.value = [...tasks.value]
  return { ok: true, fee, payout }
}

export function useCommission() {
  return {
    // state
    tasks,
    submissions,
    currentUserId,
    myNickname,
    coinBalance,
    // computed
    myPublishedTasks,
    mySubmissions,
    // helpers
    setCurrentUserId,
    getTask,
    getSubmissionsOfTask,
    mySubmissionForTask,
    startReconcile,
    // actions
    createTask,
    cancelTask,
    submitToTask,
    withdrawSubmission,
    pickWinner
  }
}