import { ref } from 'vue'
import { getInviteStats } from '@/api/invite'

const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const DEFAULT_COIN_BALANCE = 100

// 模块级 ref：ConsoleLayout 和 MineIndex 共享同一份邀请统计。
const inviteStats = ref({
  invitedCount: 0,
  membershipDaysEarned: 0,
  coinEarned: 0,
  friends: []
})
const coinBalance = ref(readCoinBalance())
const loading = ref(false)

function readCoinBalance() {
  try {
    const raw = localStorage.getItem(COIN_BALANCE_KEY)
    if (raw == null) return DEFAULT_COIN_BALANCE
    const n = Number(raw)
    return Number.isFinite(n) && n >= 0 ? n : DEFAULT_COIN_BALANCE
  } catch {
    return DEFAULT_COIN_BALANCE
  }
}

function writeCoinBalance(n) {
  try { localStorage.setItem(COIN_BALANCE_KEY, String(n)) } catch { /* ignore */ }
}

function setCoinBalance(n) {
  const v = Math.max(0, Math.floor(Number(n) || 0))
  coinBalance.value = v
  writeCoinBalance(v)
}

function adjustCoinBalance(delta) {
  setCoinBalance(coinBalance.value + delta)
}

function toNumber(value) {
  const n = Number(value)
  return Number.isNaN(n) ? 0 : n
}

export function useInviteStats() {
  async function loadInviteStats() {
    loading.value = true
    try {
      const data = await getInviteStats()
      inviteStats.value = {
        invitedCount: toNumber(data.invitedCount),
        membershipDaysEarned: toNumber(data.membershipDaysEarned),
        coinEarned: toNumber(data.coinEarned),
        friends: Array.isArray(data.friends) ? data.friends : []
      }
      // 后端返回的余额视为权威值,覆盖本地缓存
      setCoinBalance(toNumber(data.coinBalance))
    } finally {
      loading.value = false
    }
  }

  return {
    inviteStats,
    coinBalance,
    loading,
    loadInviteStats,
    setCoinBalance,
    adjustCoinBalance
  }
}