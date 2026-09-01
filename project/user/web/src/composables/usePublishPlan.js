import { reactive } from 'vue'
import {
  getPublishPlan as apiGetPublishPlan,
  generatePublishPlan as apiGeneratePublishPlan
} from '@/api/selfMediaPlan.js'

/**
 * 发布计划（发布建议 + 一文多发）的跨页面共享缓存。
 *
 * 后端已按 (userId, mainPlatform) 在 u_self_media_plan_publish_guide 持久化缓存；
 * 这里再做一层前端内存缓存，避免同一浏览器会话内重复请求。
 */
const publishPlanCache = reactive(new Map())
const publishPlanInflight = reactive(new Map())

function cacheKey(mainPlatform) {
  return mainPlatform?.trim() || ''
}

function unwrap(res) {
  return res?.data ?? null
}

/**
 * 只读查询已缓存的发布计划，不触发 AI 生成。
 * 优先读前端内存缓存，未命中再调后端只读接口。
 *
 * @param {string} mainPlatform 主发平台名称（如"小红书"）
 * @returns {Promise<object|null>} PublishPlanGuideVO 或 null
 */
export async function getCachedPublishPlan(mainPlatform) {
  const key = cacheKey(mainPlatform)
  if (!key) return null

  while (true) {
    const inflight = publishPlanInflight.get(key)
    if (inflight) {
      return await inflight
    }

    const cached = publishPlanCache.get(key)
    if (cached) return cached

    const promise = apiGetPublishPlan(key)
      .then(res => unwrap(res))
      .finally(() => {
        publishPlanInflight.delete(key)
      })
    publishPlanInflight.set(key, promise)

    const data = await promise
    if (data) {
      publishPlanCache.set(key, data)
    }
    return data
  }
}

/**
 * 生成/获取发布计划。后端若有缓存直接返回，否则触发 AI 生成并缓存。
 * 写前端内存缓存，供跨页面共享。
 *
 * @param {string} mainPlatform 主发平台名称（如"小红书"）
 * @returns {Promise<object|null>} PublishPlanGuideVO 或 null
 */
export async function generatePublishPlan(mainPlatform) {
  const key = cacheKey(mainPlatform)
  if (!key) return null

  while (true) {
    const inflight = publishPlanInflight.get(key)
    if (inflight) {
      const data = await inflight
      if (data) return data
      // 如果是只读查询未命中返回了 null，则继续生成
      continue
    }

    const cached = publishPlanCache.get(key)
    if (cached) return cached

    const promise = apiGeneratePublishPlan({ mainPlatform: key })
      .then(res => unwrap(res))
      .finally(() => {
        publishPlanInflight.delete(key)
      })
    publishPlanInflight.set(key, promise)

    const data = await promise
    if (data) {
      publishPlanCache.set(key, data)
    }
    return data
  }
}

export function clearPublishPlanCache() {
  publishPlanCache.clear()
  publishPlanInflight.clear()
}

export function usePublishPlan() {
  return {
    getCachedPublishPlan,
    generatePublishPlan,
    clearPublishPlanCache
  }
}
