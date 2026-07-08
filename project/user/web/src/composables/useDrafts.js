import { ref } from 'vue'
import {
  listDrafts,
  getDraft as fetchDraft,
  saveDraft as saveDraftApi,
  updateDraft as updateDraftApi,
  deleteDraft as deleteDraftApi
} from '@/api/draft.js'

const drafts = ref([])
const total = ref(0)
const loading = ref(false)

function normalizeDraft(raw) {
  return {
    bizNo: raw.bizNo,
    title: raw.customTitle || '未命名草稿',
    customTitle: raw.customTitle,
    customRequirement: raw.customRequirement,
    platform: raw.platform || '',
    platformName: raw.platform || '未选择平台',
    style: raw.style || '',
    styleName: raw.style || '未选择',
    template: raw.template || '',
    wordCount: raw.wordCount || 0,
    savedAt: raw.savedAt,
    raw
  }
}

export function useDrafts() {
  const load = async (params = {}) => {
    loading.value = true
    try {
      const data = await listDrafts({
        page: 1,
        pageSize: 100,
        ...params
      })
      drafts.value = (data.list || []).map(normalizeDraft)
      total.value = data.total || 0
    } finally {
      loading.value = false
    }
  }

  const fetchOne = async (bizNo) => {
    return await fetchDraft(bizNo)
  }

  const save = async (payload) => {
    return await saveDraftApi(payload)
  }

  const update = async (bizNo, payload) => {
    return await updateDraftApi(bizNo, payload)
  }

  const remove = async (bizNo) => {
    await deleteDraftApi(bizNo)
    drafts.value = drafts.value.filter((item) => item.bizNo !== bizNo)
    total.value = Math.max(0, total.value - 1)
  }

  return {
    drafts,
    total,
    loading,
    load,
    fetchOne,
    save,
    update,
    remove
  }
}