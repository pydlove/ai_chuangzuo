import { ref } from 'vue'
import { getInviteStats } from '@/api/invite'

// 模块级 ref：ConsoleLayout 和 MineIndex 共享同一份邀请统计。
const inviteStats = ref({
  invitedCount: 0,
  membershipDaysEarned: 0,
  coinEarned: 0,
  friends: []
})
const coinBalance = ref(0)
const loading = ref(false)

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
      coinBalance.value = toNumber(data.coinBalance)
    } finally {
      loading.value = false
    }
  }

  return {
    inviteStats,
    coinBalance,
    loading,
    loadInviteStats
  }
}
