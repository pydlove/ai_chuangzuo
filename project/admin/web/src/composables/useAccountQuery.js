import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listAccounts,
  getAccountDetail,
  listUserCoinRecords,
  listUserEarningsRecords,
  listUserRewardRecords
} from '@/api/earnings.js'

export function useAccountQuery() {
  const accounts = ref([])
  const total = ref(0)
  const loading = ref(false)
  const query = ref({
    userId: null,
    nickname: '',
    phone: '',
    email: '',
    userType: null,
    sortBy: null,
    page: 1,
    size: 20
  })

  const detail = ref(null)
  const detailVisible = ref(false)
  const detailLoading = ref(false)
  const detailActiveKey = ref('coin')
  const detailUserId = ref(null)
  const viewMode = ref('all')

  const coinRecords = ref([])
  const coinTotal = ref(0)
  const coinLoading = ref(false)
  const coinQuery = ref({
    direction: null,
    startTime: null,
    endTime: null,
    page: 1,
    size: 10
  })

  const earningsRecords = ref([])
  const earningsTotal = ref(0)
  const earningsLoading = ref(false)
  const earningsQuery = ref({
    type: '',
    startTime: null,
    endTime: null,
    page: 1,
    size: 10
  })

  const rewardRecords = ref([])
  const rewardTotal = ref(0)
  const rewardLoading = ref(false)
  const rewardQuery = ref({
    leaderboardType: null,
    startTime: null,
    endTime: null,
    page: 1,
    size: 10
  })

  const fetchAccounts = async () => {
    loading.value = true
    try {
      const params = { ...query.value }
      if (!params.sortBy) {
        delete params.sortBy
      }
      if (params.userType === null || params.userType === undefined) {
        delete params.userType
      }
      const res = await listAccounts(params)
      accounts.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载账户列表失败')
    } finally {
      loading.value = false
    }
  }

  const openDetail = async (userId) => {
    detailUserId.value = userId
    detailVisible.value = true
    detailLoading.value = true
    try {
      detail.value = await getAccountDetail(userId)
      detailActiveKey.value = 'coin'
      await loadAllDetailRecords()
    } catch (error) {
      message.error(error.message || '加载账户详情失败')
    } finally {
      detailLoading.value = false
    }
  }

  const loadAllDetailRecords = async () => {
    await Promise.all([
      fetchCoinRecords(),
      fetchEarningsRecords(),
      fetchRewardRecords()
    ])
  }

  const fetchCoinRecords = async () => {
    coinLoading.value = true
    try {
      const res = await listUserCoinRecords(detailUserId.value, buildQuery(coinQuery.value))
      coinRecords.value = res.items || []
      coinTotal.value = res.total || 0
    } catch (error) {
      message.error(error.message || '加载创作币明细失败')
    } finally {
      coinLoading.value = false
    }
  }

  const fetchEarningsRecords = async () => {
    earningsLoading.value = true
    try {
      const res = await listUserEarningsRecords(detailUserId.value, buildQuery(earningsQuery.value))
      earningsRecords.value = res.items || []
      earningsTotal.value = res.total || 0
    } catch (error) {
      message.error(error.message || '加载收益明细失败')
    } finally {
      earningsLoading.value = false
    }
  }

  const fetchRewardRecords = async () => {
    rewardLoading.value = true
    try {
      const res = await listUserRewardRecords(detailUserId.value, buildQuery(rewardQuery.value))
      rewardRecords.value = res.items || []
      rewardTotal.value = res.total || 0
    } catch (error) {
      message.error(error.message || '加载奖励明细失败')
    } finally {
      rewardLoading.value = false
    }
  }

  const buildQuery = (q) => {
    const params = { page: q.page, size: q.size }
    if (q.direction !== null && q.direction !== undefined) params.direction = q.direction
    if (q.type) params.type = q.type
    if (q.leaderboardType !== null && q.leaderboardType !== undefined) params.leaderboardType = q.leaderboardType
    if (q.startTime) params.startTime = q.startTime.format('YYYY-MM-DDTHH:mm:ss')
    if (q.endTime) params.endTime = q.endTime.format('YYYY-MM-DDTHH:mm:ss')
    return params
  }

  const onViewModeChange = (mode) => {
    viewMode.value = mode
    query.value.page = 1
    query.value.sortBy = mode === 'heavyEarnings' ? 'totalEarnings' : mode === 'heavyCoin' ? 'coinBalance' : null
    fetchAccounts()
  }

  const handlePageChange = (page, size) => {
    query.value.page = page
    query.value.size = size
    fetchAccounts()
  }

  const handleCoinPageChange = (pagination) => {
    coinQuery.value.page = pagination.current
    coinQuery.value.size = pagination.pageSize
    fetchCoinRecords()
  }

  const handleEarningsPageChange = (pagination) => {
    earningsQuery.value.page = pagination.current
    earningsQuery.value.size = pagination.pageSize
    fetchEarningsRecords()
  }

  const handleRewardPageChange = (pagination) => {
    rewardQuery.value.page = pagination.current
    rewardQuery.value.size = pagination.pageSize
    fetchRewardRecords()
  }

  const searchCoinRecords = () => {
    coinQuery.value.page = 1
    fetchCoinRecords()
  }

  const resetCoinQuery = () => {
    coinQuery.value = {
      direction: null,
      startTime: null,
      endTime: null,
      page: 1,
      size: 10
    }
    fetchCoinRecords()
  }

  const searchEarningsRecords = () => {
    earningsQuery.value.page = 1
    fetchEarningsRecords()
  }

  const resetEarningsQuery = () => {
    earningsQuery.value = {
      type: '',
      startTime: null,
      endTime: null,
      page: 1,
      size: 10
    }
    fetchEarningsRecords()
  }

  const searchRewardRecords = () => {
    rewardQuery.value.page = 1
    fetchRewardRecords()
  }

  const resetRewardQuery = () => {
    rewardQuery.value = {
      leaderboardType: null,
      startTime: null,
      endTime: null,
      page: 1,
      size: 10
    }
    fetchRewardRecords()
  }

  return {
    accounts, total, loading, query,
    detail, detailVisible, detailLoading, detailActiveKey, detailUserId,
    coinRecords, coinTotal, coinLoading, coinQuery,
    earningsRecords, earningsTotal, earningsLoading, earningsQuery,
    rewardRecords, rewardTotal, rewardLoading, rewardQuery,
    viewMode,
    fetchAccounts, openDetail, onViewModeChange,
    handlePageChange, handleCoinPageChange, handleEarningsPageChange, handleRewardPageChange,
    fetchCoinRecords, fetchEarningsRecords, fetchRewardRecords,
    searchCoinRecords, resetCoinQuery,
    searchEarningsRecords, resetEarningsQuery,
    searchRewardRecords, resetRewardQuery
  }
}
