import { ref } from 'vue'
import {
  getCoinLeaderboard as fetchCoinLeaderboard,
  getIncomeLeaderboard as fetchIncomeLeaderboard,
  submitIncomeSubmission as fetchSubmitIncomeSubmission,
  getMyIncomeSubmissions as fetchMyIncomeSubmissions
} from '@/api/leaderboard.js'

const STATUS_MAP = {
  0: 'pending',
  1: 'approved',
  2: 'rejected'
}

export const coinLeaderboard = ref([])
export const incomeLeaderboard = ref([])
export const mySubmissions = ref([])

function mergeTopListAndMe(vo) {
  const list = [...(vo?.topList || [])]
  const me = vo?.me
  if (me && !list.some(item => item.isMe)) {
    list.push(me)
  }
  return list
}

function normalizeSubmission(s) {
  return {
    id: s.bizNo,
    bizNo: s.bizNo,
    month: s.periodMonth,
    amount: s.amount,
    platform: s.platform,
    status: STATUS_MAP[s.auditStatus] || String(s.auditStatus),
    auditStatus: s.auditStatus,
    rejectReason: s.rejectReason,
    screenshotPaths: s.screenshotPaths || [],
    createdAt: s.createdAt
  }
}

export async function getCoinLeaderboard(month) {
  const res = await fetchCoinLeaderboard(month)
  coinLeaderboard.value = mergeTopListAndMe(res?.data)
  return coinLeaderboard.value
}

export async function getIncomeLeaderboard(periodType, periodValue) {
  const res = await fetchIncomeLeaderboard(periodType, periodValue)
  incomeLeaderboard.value = mergeTopListAndMe(res?.data)
  return incomeLeaderboard.value
}

export async function submitIncomeSubmission(formData) {
  const res = await fetchSubmitIncomeSubmission(formData)
  return res?.data
}

export async function getMyIncomeSubmissions() {
  const res = await fetchMyIncomeSubmissions()
  mySubmissions.value = (res?.data || []).map(normalizeSubmission)
  return mySubmissions.value
}

/**
 * 获取某用户在某周期是否已获奖。
 * 目前后端未提供该查询接口，先返回 null，后续接入管理端发奖记录后再实现。
 */
export function getRewardRecord(leaderboardType, periodValue, userId) {
  return null
}
