import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import * as api from '@/api/leaderboard.js'

export function useLeaderboardReview() {
  const state = reactive({
    submissions: { items: [], total: 0, page: 1, size: 20 },
    loading: false
  })

  const fetchSubmissions = async (params = {}) => {
    state.loading = true
    try {
      state.submissions = await api.getSubmissions(params)
    } finally {
      state.loading = false
    }
  }

  const approve = async (id) => {
    await api.approveSubmission(id)
    message.success('已通过')
    await fetchSubmissions({ page: state.submissions.page, size: state.submissions.size })
  }

  const reject = async (id, reason) => {
    await api.rejectSubmission(id, reason)
    message.success('已拒绝')
    await fetchSubmissions({ page: state.submissions.page, size: state.submissions.size })
  }

  return {
    state,
    fetchSubmissions,
    approve,
    reject
  }
}
