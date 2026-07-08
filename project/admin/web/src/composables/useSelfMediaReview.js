import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { getSubmissions, approveSubmission, rejectSubmission } from '@/api/leaderboard.js'

export function useSelfMediaReview() {
  const status = ref(0)
  const periodMonth = ref('')
  const submissions = ref([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(20)
  const loading = ref(false)
  const rejectVisible = ref(false)
  const rejectTarget = ref(null)
  const rejectReason = ref('')

  const fetchSubmissions = async () => {
    loading.value = true
    try {
      const res = await getSubmissions({
        status: status.value,
        periodMonth: periodMonth.value,
        page: page.value,
        size: size.value
      })
      submissions.value = res.records
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载申报列表失败')
    } finally {
      loading.value = false
    }
  }

  const approve = async (id) => {
    try {
      await approveSubmission(id)
      message.success('已通过')
      await fetchSubmissions()
    } catch (error) {
      message.error(error.message || '操作失败')
    }
  }

  const openReject = (record) => {
    rejectTarget.value = record
    rejectReason.value = ''
    rejectVisible.value = true
  }

  const confirmReject = async () => {
    if (!rejectReason.value.trim()) {
      message.warning('请输入拒绝原因')
      return
    }
    try {
      await rejectSubmission(rejectTarget.value.id, rejectReason.value)
      message.success('已拒绝')
      rejectVisible.value = false
      await fetchSubmissions()
    } catch (error) {
      message.error(error.message || '操作失败')
    }
  }

  const handlePageChange = (p, s) => {
    page.value = p
    size.value = s
    fetchSubmissions()
  }

  return {
    status, periodMonth, submissions, total, page, size, loading,
    rejectVisible, rejectTarget, rejectReason,
    fetchSubmissions, approve, openReject, confirmReject, handlePageChange
  }
}
