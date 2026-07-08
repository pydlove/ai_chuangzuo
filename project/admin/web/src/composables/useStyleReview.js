import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { listStyles, rejectStyle } from '@/api/style.js'

export function useStyleReview() {
  const styles = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(20)
  const keyword = ref('')
  const status = ref(0)

  const fetchStyles = async () => {
    loading.value = true
    try {
      const res = await listStyles({
        keyword: keyword.value,
        pageNum: page.value,
        pageSize: pageSize.value,
        status: status.value === '' || status.value === null ? undefined : status.value
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

  return {
    styles,
    total,
    loading,
    page,
    pageSize,
    keyword,
    status,
    fetchStyles,
    handleSearch,
    handleReset,
    handlePageChange,
    handleReject
  }
}