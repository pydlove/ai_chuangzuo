import { defineStore } from 'pinia'

/**
 * 全局异步任务 store：跟踪正在运行的后台任务，页脚进度条组件订阅这个 store。
 *
 * 任务结构：
 * - key: `${taskType}:${taskId}`，避免不同任务类型 ID 撞车
 * - taskType: 'topic-title-generate' 等，未来扩展
 * - label: 进度条展示文案，如「AI 生成标题 30 条」
 * - status: 0=queued, 1=processing, 2=completed, 3=failed
 * - onComplete: 任务完成（success/failed）时回调，列表页用它来 reload
 */
export const useAsyncTasksStore = defineStore('adminAsyncTasks', {
  state: () => ({
    tasks: {}
  }),

  getters: {
    runningList: (state) =>
      Object.values(state.tasks).filter((t) => t.status === 0 || t.status === 1)
  },

  actions: {
    add(taskType, taskId, label, onComplete) {
      const key = `${taskType}:${taskId}`
      this.tasks[key] = {
        key,
        taskType,
        taskId,
        label,
        status: 0,
        generatedCount: 0,
        failedReason: null,
        onComplete,
        createdAt: Date.now()
      }
      return key
    },

    update(taskType, taskId, patch) {
      const key = `${taskType}:${taskId}`
      const t = this.tasks[key]
      if (!t) return
      Object.assign(t, patch)
    },

    remove(taskType, taskId) {
      const key = `${taskType}:${taskId}`
      const t = this.tasks[key]
      delete this.tasks[key]
      // 通知调用方（任务可能不是从 store 里查到的，比如状态查询兜底）
      if (t && typeof t.onComplete === 'function') {
        try {
          t.onComplete({
            status: t.status,
            generatedCount: t.generatedCount,
            failedReason: t.failedReason
          })
        } catch (e) {
          // 忽略回调异常，不影响 store 清理
        }
      }
    }
  }
})