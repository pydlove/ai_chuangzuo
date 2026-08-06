import { ref } from 'vue'
import {
  getRealName,
  submitRealName as submitRealNameApi,
  listWithdrawals,
  applyWithdraw as applyWithdrawApi
} from '@/api/withdraw.js'
import { useInviteStats } from '@/composables/useInviteStats.js'

const realNameInfo = ref({
  realName: '',
  idCard: '',
  verified: false
})
const withdrawRecords = ref([])
const loading = ref(false)

function toNumber(value) {
  const n = Number(value)
  return Number.isNaN(n) ? 0 : n
}

function normalizeRecord(raw) {
  return {
    id: raw.bizNo || raw.id,
    bizNo: raw.bizNo || '',
    amount: toNumber(raw.amount),
    account: raw.account || '',
    name: raw.name || '',
    status: raw.status || 'pending',
    createdAt: raw.createdAt
  }
}

export function useWithdraw() {
  const { loadInviteStats } = useInviteStats()

  const loadRealName = async () => {
    const data = await getRealName()
    if (data && data.verified) {
      realNameInfo.value = {
        realName: data.realName || '',
        idCard: data.idCard || '',
        verified: true
      }
    } else {
      realNameInfo.value = { realName: '', idCard: '', verified: false }
    }
  }

  const submitRealName = async (data) => {
    await submitRealNameApi(data)
    await loadRealName()
  }

  const loadWithdrawals = async () => {
    const list = await listWithdrawals()
    withdrawRecords.value = (list || []).map(normalizeRecord)
  }

  const applyWithdraw = async (data) => {
    try {
      loading.value = true
      const bizNo = await applyWithdrawApi(data)
      await Promise.all([loadWithdrawals(), loadInviteStats()])
      return bizNo
    } finally {
      loading.value = false
    }
  }

  return {
    realNameInfo,
    withdrawRecords,
    loading,
    loadRealName,
    submitRealName,
    loadWithdrawals,
    applyWithdraw
  }
}
