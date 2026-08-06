import { ref, computed } from 'vue'
import { listGenerationTasks } from '@/api/generation.js'
import { useMessages } from '@/composables/useMessages'

// 模块级单例：队列数据 + 轮询，两模式共用（引导模式进度卡、极简模式抽屉、徽章）
const queueList = ref([])
const queueOpen = ref(false)
let timer = null

export const mapStatus = (code, failedReason) => {
  if (code === 3 && failedReason === '用户手动停止') return 'cancelled'
  return code === 0 ? 'queued' : code === 1 ? 'generating' : code === 2 ? 'completed' : code === 3 ? 'failed' : 'queued'
}

export const statusText = (status) =>
  ({ generating: '生成中', queued: '排队中', completed: '已完成', failed: '失败', cancelled: '已停止' }[status] || status)

export function useGenerationQueue() {
  const { refreshUnreadCount } = useMessages()

  const activeCount = computed(
    () => queueList.value.filter(t => t.status === 'queued' || t.status === 'generating').length
  )

  async function loadQueue() {
    try {
      const data = await listGenerationTasks({ page: 1, pageSize: 20 })
      const prevStatus = new Map(queueList.value.map(t => [t.id, t.status]))
      queueList.value = (data.list || []).map(t => ({
        id: t.id,
        title: t.title || t.inputParam?.title || '未命名',
        platform: t.inputParam?.platform || '未选择',
        wordCount: t.wordLimitTarget || 0,
        status: mapStatus(t.status, t.failedReason),
        progress: t.progressPct || 0,
        createdAt: t.createdAt,
        completedAt: t.completedAt
      }))
      // 有任务刚跑完 → 后端此时才推「创作完成」消息，立刻刷角标，
      // 不用等消息中心 30s 的轮询周期
      const justFinished = queueList.value.some(
        t => (t.status === 'completed' || t.status === 'failed') &&
          prevStatus.has(t.id) && prevStatus.get(t.id) !== t.status
      )
      if (justFinished) {
        refreshUnreadCount()
      }
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
