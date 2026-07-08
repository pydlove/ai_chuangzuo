import { ref, computed } from 'vue'
import {
  getAccountSummary,
  getMonthlySettlementList,
  getEarningsRecords,
  settleLastMonth as settleLastMonthApi
} from '@/api/earnings.js'

const summary = ref({
  coinBalance: 0,
  totalEarnings: 0,
  settledEarnings: 0,
  unsettledEarnings: 0
})

const monthlyList = ref([])
const records = ref([])
const recordsTotal = ref(0)
const loading = ref(false)

const previousMonth = computed(() => {
  const now = new Date()
  now.setMonth(now.getMonth() - 1)
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
})

function toNumber(value) {
  const n = Number(value)
  return Number.isNaN(n) ? 0 : n
}

function normalizeRecord(raw) {
  const statusCode = raw.status
  const status = statusCode === 1 ? 'settled' : 'unsettled'
  return {
    id: raw.id,
    type: raw.type,
    typeLabel: raw.typeLabel || raw.type,
    title: raw.title || raw.description || '',
    description: raw.description || '',
    amount: toNumber(raw.amount),
    status,
    statusLabel: raw.statusLabel || (status === 'settled' ? '已结算' : '未结算'),
    settlementMonth: raw.settlementMonth,
    createdAt: raw.createdAt
  }
}

export function useEarnings() {
  const loadSummary = async () => {
    const data = await getAccountSummary()
    summary.value = {
      coinBalance: toNumber(data.coinBalance),
      totalEarnings: toNumber(data.totalEarnings),
      settledEarnings: toNumber(data.settledEarnings),
      unsettledEarnings: toNumber(data.unsettledEarnings)
    }
  }

  const loadMonthlyList = async () => {
    const list = await getMonthlySettlementList()
    monthlyList.value = (list || []).map((item) => ({
      month: item.month,
      count: item.count || 0,
      total: toNumber(item.total),
      settled: toNumber(item.settled),
      unsettled: toNumber(item.unsettled)
    }))
  }

  const loadRecords = async (params = {}) => {
    const data = await getEarningsRecords({
      page: 1,
      pageSize: 100,
      ...params
    })
    records.value = (data.list || []).map(normalizeRecord)
    recordsTotal.value = data.total || 0
  }

  const settle = async () => {
    const result = await settleLastMonthApi()
    await Promise.all([loadSummary(), loadMonthlyList(), loadRecords()])
    return result
  }

  const refreshAll = async () => {
    loading.value = true
    try {
      await Promise.all([loadSummary(), loadMonthlyList(), loadRecords()])
    } finally {
      loading.value = false
    }
  }

  return {
    summary,
    monthlyList,
    records,
    recordsTotal,
    loading,
    previousMonth,
    loadSummary,
    loadMonthlyList,
    loadRecords,
    settle,
    refreshAll
  }
}
