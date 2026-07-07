import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { listAccounts, getAccountDetail } from '@/api/earnings.js'

export function useAccountQuery() {
  const accounts = ref([])
  const total = ref(0)
  const loading = ref(false)
  const query = ref({
    userId: null,
    nickname: '',
    phone: '',
    email: '',
    page: 1,
    size: 20
  })
  const detail = ref(null)
  const detailVisible = ref(false)

  const fetchAccounts = async () => {
    loading.value = true
    try {
      const res = await listAccounts(query.value)
      accounts.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载账户列表失败')
    } finally {
      loading.value = false
    }
  }

  const openDetail = async (userId) => {
    try {
      detail.value = await getAccountDetail(userId)
      detailVisible.value = true
    } catch (error) {
      message.error(error.message || '加载账户详情失败')
    }
  }

  const handlePageChange = (page, size) => {
    query.value.page = page
    query.value.size = size
    fetchAccounts()
  }

  return {
    accounts, total, loading, query, detail, detailVisible,
    fetchAccounts, openDetail, handlePageChange
  }
}
