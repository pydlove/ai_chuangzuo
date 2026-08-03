import { ref } from 'vue'
import {
  getAccountSummary,
  getMonthlySettlementList,
  getEarningsRecords
} from '@/api/earnings.js'

const summary = ref({
  coinBalance: 0,
  totalEarnings: 0
})

const monthlyList = ref([])
const records = ref([])
const recordsTotal = ref(0)
const loading = ref(false)

function toNumber(value) {
  const n = Number(value)
  return Number.isNaN(n) ? 0 : n
}

function normalizeRecord(raw) {
  return {
    id: raw.id,
    type: raw.type,
    typeLabel: raw.typeLabel || raw.type,
    title: raw.title || raw.description || '',
    description: raw.description || '',
    sourceType: raw.sourceType || '',
    sourceId: raw.sourceId || '',
    sourceLabel: raw.sourceLabel || '',
    planKey: raw.planKey || '',
    planName: raw.planName || '',
    cycle: raw.cycle || '',
    orderAmount: toNumber(raw.orderAmount),
    commissionRate: toNumber(raw.commissionRate),
    isFirstPurchase: raw.isFirstPurchase,
    amount: toNumber(raw.amount),
    createdAt: raw.createdAt
  }
}

export function useEarnings() {
  const loadSummary = async () => {
    const data = await getAccountSummary()
    summary.value = {
      coinBalance: toNumber(data.coinBalance),
      totalEarnings: toNumber(data.totalEarnings)
    }
  }

  const loadMonthlyList = async () => {
    const list = await getMonthlySettlementList()
    monthlyList.value = (list || []).map((item) => ({
      month: item.month,
      count: item.count || 0,
      total: toNumber(item.total)
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
    loadSummary,
    loadMonthlyList,
    loadRecords,
    refreshAll
  }
}
