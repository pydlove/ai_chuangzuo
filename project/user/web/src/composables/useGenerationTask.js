import { onBeforeUnmount, ref } from 'vue'
import { getGenerationTask, retryGenerationTask, submitGeneration } from '@/api/generation'

const POLL_INTERVAL_MS = 1500
const TERMINAL_STATUSES = new Set([2, 3]) // COMPLETED=2, FAILED=3

/**
 * 创作任务轮询 composable：
 * - submit(payload) → 后端返回 taskId，开始轮询
 * - retry(taskId) → 后端返回新 taskId，开始轮询
 * - task 当前状态在 task.value，loading / submitting 供 UI 反馈
 * - onDone(task) 回调：终端状态触发（成功/失败都触发）
 *
 * 由于不能引出 work 内部触发（worker 在 admin-api），
 * 通过 1.5s 轮询 GET 进度。
 */
export function useGenerationTask() {
  const task = ref(null)
  const loading = ref(false)    // 初始 submit/retry loading
  const polling = ref(false)    // 轮询中标志
  let timer = null
  let onDoneCb = null

  const stop = () => {
    polling.value = false
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  const startPoll = (id) => {
    polling.value = true
    const tick = async () => {
      try {
        const data = await getGenerationTask(id)
        task.value = data
        if (TERMINAL_STATUSES.has(data.status)) {
          stop()
          onDoneCb && onDoneCb(data)
          return
        }
      } catch (e) {
        // 单次失败不停止，静默重试
      }
      if (polling.value) {
        timer = setTimeout(tick, POLL_INTERVAL_MS)
      }
    }
    timer = setTimeout(tick, POLL_INTERVAL_MS)
  }

  const submit = async (payload) => {
    loading.value = true
    try {
      const data = await submitGeneration(payload)
      task.value = data
      startPoll(data.id)
      return data
    } finally {
      loading.value = false
    }
  }

  const retry = async (id) => {
    loading.value = true
    try {
      const data = await retryGenerationTask(id)
      task.value = data
      startPoll(data.id)
      return data
    } finally {
      loading.value = false
    }
  }

  const setOnDone = (cb) => { onDoneCb = cb }

  onBeforeUnmount(stop)

  return {
    task,
    loading,
    polling,
    submit,
    retry,
    setOnDone,
    stop
  }
}
