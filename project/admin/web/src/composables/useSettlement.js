import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { getPendingSettlementSummary, getPendingSettlementUsers, settleAccounts } from '@/api/earnings.js'

function previousMonth() {
  const now = new Date()
  now.setMonth(now.getMonth() - 1)
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

export function useSettlement() {
  const month = ref(previousMonth())
  const summary = ref(null)
  const users = ref([])
  const loading = ref(false)
  const settling = ref(false)

  const fetchSummary = async () => {
    loading.value = true
    try {
      summary.value = await getPendingSettlementSummary(month.value)
      users.value = await getPendingSettlementUsers(month.value)
    } catch (error) {
      message.error(error.message || '加载待结算数据失败')
    } finally {
      loading.value = false
    }
  }

  const settleAll = async () => {
    settling.value = true
    try {
      await settleAccounts({ month: month.value, userIds: [] })
      message.success('结算成功')
      await fetchSummary()
    } catch (error) {
      message.error(error.message || '结算失败')
    } finally {
      settling.value = false
    }
  }

  const settleUser = async (userId) => {
    settling.value = true
    try {
      await settleAccounts({ month: month.value, userIds: [userId] })
      message.success('结算成功')
      await fetchSummary()
    } catch (error) {
      message.error(error.message || '结算失败')
    } finally {
      settling.value = false
    }
  }

  return {
    month, summary, users, loading, settling,
    fetchSummary, settleAll, settleUser
  }
}
