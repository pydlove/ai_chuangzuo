import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { listGlobalSkills, createGlobalSkill, updateGlobalSkill, deleteGlobalSkill, batchDeleteGlobalSkills } from '@/api/globalSkill.js'

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
      const res = await listGlobalSkills({
        keyword: keyword.value,
        pageNum: page.value,
        pageSize: pageSize.value,
        enableStatus: status.value === '' || status.value === null ? undefined : status.value
      })
      list.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载预设提示词失败')
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
      await createGlobalSkill(payload)
      message.success('预设提示词已创建')
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
      await updateGlobalSkill(bizNo, payload)
      message.success('预设提示词已更新')
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
      await deleteGlobalSkill(bizNo)
      message.success('已删除')
      await fetch()
      return true
    } catch (error) {
      message.error(error.message || '删除失败')
      return false
    }
  }

  const handleBatchDelete = async (bizNos) => {
    try {
      const count = await batchDeleteGlobalSkills(bizNos)
      message.success(`已批量删除 ${count} 条`)
      await fetch()
      return true
    } catch (error) {
      message.error(error.message || '批量删除失败')
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
    handleDelete,
    handleBatchDelete
  }
}