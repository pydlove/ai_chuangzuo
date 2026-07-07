import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import * as api from '@/api/leaderboard.js'

export function useLeaderboardAward() {
  const state = reactive({
    leaderboardType: 1,
    periodMonth: '',
    top10: [],
    rewards: { items: [], total: 0, page: 1, size: 20 },
    loading: false
  })

  const fetchTop10 = async () => {
    if (!state.periodMonth) return
    state.loading = true
    try {
      state.top10 = await api.previewTop10({
        leaderboardType: state.leaderboardType,
        periodMonth: state.periodMonth
      })
    } finally {
      state.loading = false
    }
  }

  const grant = async () => {
    if (!state.periodMonth) {
      message.warning('请选择月份')
      return
    }
    state.loading = true
    try {
      const result = await api.grantRewards({
        leaderboardType: state.leaderboardType,
        periodMonth: state.periodMonth
      })
      message.success(`发放完成：成功 ${result.granted} 人，跳过 ${result.skipped} 人`)
      await fetchTop10()
      await fetchRewards()
    } finally {
      state.loading = false
    }
  }

  const fetchRewards = async (params = {}) => {
    state.loading = true
    try {
      state.rewards = await api.getRewards({
        leaderboardType: state.leaderboardType,
        periodMonth: state.periodMonth,
        ...params
      })
    } finally {
      state.loading = false
    }
  }

  return {
    state,
    fetchTop10,
    grant,
    fetchRewards
  }
}
