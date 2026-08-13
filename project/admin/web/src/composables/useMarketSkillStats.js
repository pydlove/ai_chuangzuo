import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { getMarketSkillStats, getMarketSkillUsageRecords } from '@/api/marketSkill.js'

export function useMarketSkillStats() {
  const stats = ref(null)
  const statsLoading = ref(false)
  const usageRecords = ref([])
  const usageTotal = ref(0)
  const usagePage = ref(1)
  const usagePageSize = ref(20)
  const usageLoading = ref(false)

  const fetchStats = async () => {
    statsLoading.value = true
    try {
      stats.value = await getMarketSkillStats()
    } catch (error) {
      message.error(error.message || '加载统计概览失败')
    } finally {
      statsLoading.value = false
    }
  }

  const fetchUsageRecords = async (bizNo) => {
    usageLoading.value = true
    try {
      const res = await getMarketSkillUsageRecords(bizNo, {
        pageNum: usagePage.value,
        pageSize: usagePageSize.value
      })
      usageRecords.value = res.list
      usageTotal.value = res.total
      usagePage.value = res.page
      usagePageSize.value = res.size
    } catch (error) {
      message.error(error.message || '加载使用记录失败')
    } finally {
      usageLoading.value = false
    }
  }

  const handleUsagePageChange = (newPage, newPageSize) => {
    usagePage.value = newPage
    usagePageSize.value = newPageSize
  }

  const resetUsageRecords = () => {
    usageRecords.value = []
    usageTotal.value = 0
    usagePage.value = 1
    usagePageSize.value = 20
  }

  return {
    stats,
    statsLoading,
    fetchStats,
    usageRecords,
    usageTotal,
    usagePage,
    usagePageSize,
    usageLoading,
    fetchUsageRecords,
    handleUsagePageChange,
    resetUsageRecords
  }
}
