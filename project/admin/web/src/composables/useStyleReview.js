import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { listStyles, rejectStyle, approveStyle, approveBatch } from '@/api/style.js'

export function useStyleReview() {
  const styles = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(20)
  const keyword = ref('')
  const activeTab = ref('pending')

  const fetchStyles = async () => {
    loading.value = true
    try {
      const isReviewed = activeTab.value === 'reviewed'
      const res = await listStyles({
        keyword: keyword.value,
        pageNum: page.value,
        pageSize: pageSize.value,
        status: isReviewed ? undefined : 0,
        reviewed: isReviewed ? true : undefined
      })
      styles.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载风格列表失败')
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    page.value = 1
    fetchStyles()
  }

  const handleReset = () => {
    keyword.value = ''
    page.value = 1
    fetchStyles()
  }

  const handlePageChange = (newPage, newPageSize) => {
    page.value = newPage
    pageSize.value = newPageSize
    fetchStyles()
  }

  const handleTabChange = () => {
    page.value = 1
    fetchStyles()
  }

  const handleApprove = async (style) => {
    try {
      await approveStyle(style.id)
      message.success('风格已通过')
      fetchStyles()
      return true
    } catch (error) {
      message.error(error.message || '通过失败')
      return false
    }
  }

  const handleReject = async (style, reason) => {
    try {
      await rejectStyle(style.id, reason)
      message.success('风格已打回')
      fetchStyles()
      return true
    } catch (error) {
      message.error(error.message || '打回失败')
      return false
    }
  }

  const handleApproveBatch = async (ids) => {
    try {
      const count = await approveBatch(ids)
      message.success(`批量通过 ${count} 条风格`)
      fetchStyles()
      return true
    } catch (error) {
      message.error(error.message || '批量通过失败')
      return false
    }
  }

  return {
    styles,
    total,
    loading,
    page,
    pageSize,
    keyword,
    activeTab,
    fetchStyles,
    handleSearch,
    handleReset,
    handlePageChange,
    handleTabChange,
    handleReject,
    handleApprove,
    handleApproveBatch
  }
}
