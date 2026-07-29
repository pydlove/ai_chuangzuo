import { onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'

/**
 * 通用异步任务轮询 composable。
 *
 * - 启动后按 intervalMs 调 fetcher(taskId)，直到 status 进入终态（2=completed / 3=failed）
 * - 中途 fetcher 异常不停止轮询，只记 warn 日志（瞬时网络抖动）
 * - 组件卸载时自动清理定时器
 *
 * @param {object} opts
 * @param {(taskId: number) => Promise<{status:number, generatedCount?:number, failedReason?:string}>} opts.fetcher
 * @param {(snapshot: object) => void} opts.onUpdate 每次轮询回调（含中间状态）
 * @param {(snapshot: object) => void} opts.onComplete 终态回调
 * @param {number} [opts.intervalMs=1500]
 */
export function useTaskPolling(opts) {
  const { fetcher, onUpdate, onComplete, intervalMs = 1500 } = opts
  let timer = null
  let stopped = false

  const stop = () => {
    stopped = true
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  const tick = async (taskId) => {
    if (stopped) return
    let snapshot
    try {
      snapshot = await fetcher(taskId)
    } catch (e) {
      // 瞬时网络错误不停止，给后台重试机会
      // eslint-disable-next-line no-console
      console.warn(`task ${taskId} 轮询失败: ${e?.message || e}`)
      if (!stopped) timer = setTimeout(() => tick(taskId), intervalMs)
      return
    }

    if (typeof onUpdate === 'function') {
      try {
        onUpdate(snapshot)
      } catch (e) {
        // ignore
      }
    }

    const done = snapshot.status === 2 || snapshot.status === 3
    if (done) {
      if (typeof onComplete === 'function') {
        try {
          onComplete(snapshot)
        } catch (e) {
          // ignore
        }
      }
      stop()
      return
    }
    if (!stopped) timer = setTimeout(() => tick(taskId), intervalMs)
  }

  const start = (taskId) => {
    stopped = false
    // 立即跑一次，避免 1.5s 空白
    tick(taskId)
  }

  onBeforeUnmount(stop)

  return { start, stop }
}

/**
 * 终态提示消息：success / failed 各自一条 message。
 */
export function notifyTaskResult(taskType, snapshot) {
  if (snapshot.status === 2) {
    message.success(`${taskTypeLabel(taskType)} 完成，新增 ${snapshot.generatedCount ?? 0} 条`)
  } else if (snapshot.status === 3) {
    message.error(`${taskTypeLabel(taskType)} 失败：${snapshot.failedReason || '未知错误'}`)
  }
}

function taskTypeLabel(t) {
  if (t === 'topic-title-generate') return 'AI 生成标题'
  return t
}