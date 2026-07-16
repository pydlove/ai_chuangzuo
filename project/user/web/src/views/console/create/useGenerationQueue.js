import { ref, computed } from 'vue'
import { listGenerationTasks } from '@/api/generation.js'

// 模块级单例：队列数据 + 轮询，两模式共用（引导模式进度卡、极简模式抽屉、徽章）
const queueList = ref([])
const queueOpen = ref(false)
let timer = null

export const mapStatus = (code) =>
  code === 0 ? 'queued' : code === 1 ? 'generating' : code === 2 ? 'completed' : code === 3 ? 'failed' : 'queued'

export const statusText = (status) =>
  ({ generating: '生成中', queued: '排队中', completed: '已完成', failed: '失败' }[status] || status)

export function useGenerationQueue() {
  const activeCount = computed(
    () => queueList.value.filter(t => t.status === 'queued' || t.status === 'generating').length
  )

  async function loadQueue() {
    try {
      const data = await listGenerationTasks({ page: 1, pageSize: 20 })
      queueList.value = (data.list || []).map(t => ({
        id: t.id,
        title: t.title || t.inputParam?.title || '未命名',
        platform: t.inputParam?.platform || '未选择',
        wordCount: t.wordLimitTarget || 0,
        status: mapStatus(t.status),
        progress: t.progressPct || 0,
        createdAt: t.createdAt,
        completedAt: t.completedAt
      }))
    } catch {
      queueList.value = []
    }
  }

  function startPolling() {
    if (timer) return
    loadQueue()
    timer = setInterval(loadQueue, 5000)
  }

  function stopPolling() {
    clearInterval(timer)
    timer = null
  }

  return { queueList, queueOpen, activeCount, loadQueue, startPolling, stopPolling }
}
