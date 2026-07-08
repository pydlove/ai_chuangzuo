import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { listGlobalStyles, createGlobalStyle, updateGlobalStyle, deleteGlobalStyle } from '@/api/globalStyle.js'

export function useGlobalStyleManagement() {
  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const submitting = ref(false)
  const page = ref(1)
  const pageSize = ref(20)
  const keyword = ref('')
  const status = ref('')

  const fetch = async () => {
    loading.value = true
    try {
      const res = await listGlobalStyles({
        keyword: keyword.value,
        pageNum: page.value,
        pageSize: pageSize.value,
        enableStatus: status.value === '' || status.value === null ? undefined : status.value
      })
      list.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载预设风格失败')
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    page.value = 1
    fetch()
  }

  const handleReset = () => {
    keyword.value = ''
    status.value = ''
    page.value = 1
    fetch()
  }

  const handlePageChange = (newPage, newPageSize) => {
    page.value = newPage
    pageSize.value = newPageSize
    fetch()
  }

  const handleCreate = async (payload) => {
    submitting.value = true
    try {
      await createGlobalStyle(payload)
      message.success('预设风格已创建')
      await fetch()
      return true
    } catch (error) {
      message.error(error.message || '创建失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  const handleUpdate = async (bizNo, payload) => {
    submitting.value = true
    try {
      await updateGlobalStyle(bizNo, payload)
      message.success('预设风格已更新')
      await fetch()
      return true
    } catch (error) {
      message.error(error.message || '更新失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  const handleDelete = async (bizNo) => {
    try {
      await deleteGlobalStyle(bizNo)
      message.success('已删除')
      await fetch()
      return true
    } catch (error) {
      message.error(error.message || '删除失败')
      return false
    }
  }

  return {
    list,
    total,
    loading,
    submitting,
    page,
    pageSize,
    keyword,
    status,
    fetch,
    handleSearch,
    handleReset,
    handlePageChange,
    handleCreate,
    handleUpdate,
    handleDelete
  }
}