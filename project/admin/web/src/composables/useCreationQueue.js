import { reactive, ref, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import {
  listGenerationTasks,
  manualRetryGenerationTask,
  releaseGenerationTaskLease,
  markGenerationTaskFailed
} from '@/api/creationQueue.js'

/**
 * 创作队列 composable。
 *
 * <p>提供 3 个 tab 共享的列表状态 + 5s 自动刷新。
 * status: 0=queued 1=processing 3=failed
 */
export function useCreationQueue() {
  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(20)
  const keyword = ref('')

  const STATUS_TAB = {
    processing: 1,
    queued: 0,
    failed: 3
  }
  const activeStatus = ref(STATUS_TAB.processing)

  const fetch = async () => {
    loading.value = true
    try {
      const res = await listGenerationTasks({
        status: activeStatus.value,
        keyword: keyword.value || undefined,
        page: page.value,
        pageSize: pageSize.value
      })
      list.value = res.list || []
      total.value = res.total || 0
    } catch (error) {
      message.error(error.message || '加载任务列表失败')
    } finally {
      loading.value = false
    }
  }

  const switchTab = (key) => {
    activeStatus.value = STATUS_TAB[key] ?? null
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

  const handlePageChange = (p, ps) => {
    page.value = p
    pageSize.value = ps
    fetch()
  }

  const refresh = () => fetch()

  const handleRetry = async (id) => {
    try {
      await manualRetryGenerationTask(id)
      message.success('已加入重试队列')
      fetch()
    } catch (e) {
      message.error(e.message || '操作失败')
    }
  }

  const handleRelease = async (id) => {
    try {
      await releaseGenerationTaskLease(id)
      message.success('已释放 lease')
      fetch()
    } catch (e) {
      message.error(e.message || '操作失败')
    }
  }

  const handleMarkFailed = async (id) => {
    let reason = ''
    try {
      // 使用一个临时 prompt 拿失败原因，简单直接
      // eslint-disable-next-line no-alert
      reason = window.prompt('请输入失败原因（可空）：', 'admin 手动标记失败') || 'admin 手动标记失败'
    } catch (e) {
      reason = 'admin 手动标记失败'
    }
    try {
      await markGenerationTaskFailed(id, reason)
      message.success('已标记为失败')
      fetch()
    } catch (e) {
      message.error(e.message || '操作失败')
    }
  }

  // 5s 自动刷新（页面可见时）
  const state = reactive({ timer: null })
  const startAutoRefresh = () => {
    stopAutoRefresh()
    state.timer = setInterval(() => {
      if (document.visibilityState === 'visible') fetch()
    }, 5000)
  }
  const stopAutoRefresh = () => {
    if (state.timer) {
      clearInterval(state.timer)
      state.timer = null
    }
  }
  onBeforeUnmount(stopAutoRefresh)

  return {
    list,
    total,
    loading,
    page,
    pageSize,
    keyword,
    activeStatus,
    STATUS_TAB,
    fetch,
    switchTab,
    handleSearch,
    handleReset,
    handlePageChange,
    refresh,
    handleRetry,
    handleRelease,
    handleMarkFailed,
    startAutoRefresh,
    stopAutoRefresh
  }
}
