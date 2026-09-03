import { ref } from 'vue'

/**
 * 全局 inflight Promise 缓存，用于同一浏览器会话内 AI 请求的异步去重。
 *
 * 同一 key 的请求在尚未完成时，再次调用会复用已有 Promise，避免重复提交或重复生成。
 * 适合弹框/页面关闭后再次打开的场景：只要页面没有刷新，进行中的请求会被复用。
 */
const inflight = new Map()

/**
 * 执行一个异步请求，并按 key 做全局 inflight 去重。
 *
 * @template T
 * @param {string} key 去重键，通常用业务标识 + 参数组成
 * @param {() => Promise<T>} factory 真正发起请求的工厂函数
 * @returns {Promise<T>}
 */
export async function runWithDedupe(key, factory) {
  const existing = inflight.get(key)
  if (existing) {
    return existing
  }

  const promise = factory().finally(() => {
    inflight.delete(key)
  })
  inflight.set(key, promise)
  return promise
}

/**
 * 检查某个 key 是否正在请求中。
 *
 * @param {string} key
 * @returns {boolean}
 */
export function isTaskRunning(key) {
  return inflight.has(key)
}

/**
 * 取消/移除某个 key 的 inflight 缓存（不取消实际 HTTP 请求）。
 * 用于明确知道请求已完成或需要重试时。
 *
 * @param {string} key
 */
export function clearInflight(key) {
  inflight.delete(key)
}

/**
 * Vue 组件内使用的异步任务封装。
 *
 * 提供组件级 loading/ref，同时享受全局 inflight 去重。
 *
 * @template T
 * @param {object} options
 * @param {string} options.key 去重键
 * @param {() => Promise<T>} options.task 请求工厂
 * @param {import('vue').Ref<boolean>} [options.loadingRef] 外部 loading ref（可选）
 * @returns {{ loading: import('vue').Ref<boolean>, run: () => Promise<T> }}
 */
export function useAsyncTask({ key, task, loadingRef } = {}) {
  const internalLoading = ref(false)
  const loading = loadingRef || internalLoading

  const run = async () => {
    if (isTaskRunning(key)) {
      loading.value = true
      try {
        return await inflight.get(key)
      } finally {
        loading.value = false
      }
    }

    loading.value = true
    try {
      return await runWithDedupe(key, task)
    } finally {
      loading.value = false
    }
  }

  return { loading, run }
}
