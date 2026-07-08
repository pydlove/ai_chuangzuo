import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listMessages,
  getMessageDetail,
  createMessage,
  updateMessage
} from '@/api/message.js'

export const TAB_ITEMS = [
  { key: 'announcement', label: '公告' },
  { key: 'feature', label: '新功能' },
  { key: 'promotion', label: '优惠活动' }
]

export function useMessageManagement() {
  const activeTab = ref('announcement')
  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const submitting = ref(false)
  const page = ref(1)
  const pageSize = ref(20)
  const keyword = ref('')

  const fetch = async () => {
    loading.value = true
    try {
      const res = await listMessages({
        msgType: activeTab.value,
        keyword: keyword.value,
        page: page.value,
        size: pageSize.value
      })
      list.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载消息列表失败')
    } finally {
      loading.value = false
    }
  }

  const switchTab = (key) => {
    activeTab.value = key
    page.value = 1
    fetch()
  }

  const handleSearch = () => {
    page.value = 1
    fetch()
  }

  const handleReset = () => {
    keyword.value = ''
    page.value = 1
    fetch()
  }

  const handlePageChange = (newPage, newSize) => {
    page.value = newPage
    pageSize.value = newSize
    fetch()
  }

  const loadDetail = async (id) => {
    return await getMessageDetail(id)
  }

  const handleCreate = async (payload) => {
    submitting.value = true
    try {
      await createMessage({ ...payload, msgType: activeTab.value })
      message.success('消息已发布')
      page.value = 1
      await fetch()
      return true
    } catch (error) {
      message.error(error.message || '发布失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  const handleUpdate = async (id, payload) => {
    submitting.value = true
    try {
      await updateMessage(id, payload)
      message.success('消息已更新')
      await fetch()
      return true
    } catch (error) {
      message.error(error.message || '更新失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  return {
    activeTab,
    TAB_ITEMS,
    list,
    total,
    loading,
    submitting,
    page,
    pageSize,
    keyword,
    fetch,
    switchTab,
    handleSearch,
    handleReset,
    handlePageChange,
    loadDetail,
    handleCreate,
    handleUpdate
  }
}
