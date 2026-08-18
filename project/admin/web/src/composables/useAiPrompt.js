import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listAiPrompts,
  getAiPrompt,
  createAiPrompt,
  updateAiPrompt,
  deleteAiPrompt,
  enableAiPrompt,
  disableAiPrompt
} from '@/api/aiPrompt.js'

export function useAiPrompt() {
  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(20)

  const fetchList = async (params = {}) => {
    loading.value = true
    try {
      const res = await listAiPrompts({
        page: page.value,
        pageSize: pageSize.value,
        ...params
      })
      list.value = res.list || []
      total.value = res.total || 0
    } catch (e) {
      message.error(e.message || '加载失败')
    } finally {
      loading.value = false
    }
  }

  const getDetail = async (id) => {
    return await getAiPrompt(id)
  }

  const save = async (id, data) => {
    if (id) {
      await updateAiPrompt(id, data)
      message.success('更新成功')
    } else {
      await createAiPrompt(data)
      message.success('创建成功')
    }
  }

  const remove = async (id) => {
    await deleteAiPrompt(id)
    message.success('删除成功')
  }

  const toggleStatus = async (record) => {
    if (record.status === 1) {
      await disableAiPrompt(record.id)
      message.success('已停用')
    } else {
      await enableAiPrompt(record.id)
      message.success('已启用')
    }
  }

  return {
    list,
    total,
    loading,
    page,
    pageSize,
    fetchList,
    getDetail,
    save,
    remove,
    toggleStatus
  }
}
