import { ref } from 'vue'
import {
  fetchCurrentPlan as apiFetchCurrentPlan,
  savePlan as apiSavePlan,
  fetchPlatformQuestions as apiFetchPlatformQuestions,
  recommendNiches as apiRecommendNiches,
  recommendPersonas as apiRecommendPersonas
} from '@/api/selfMediaPlan'
import { clearPublishPlanCache } from '@/composables/usePublishPlan.js'
import { message } from 'ant-design-vue'

export const currentPlan = ref(null)
export const isLoadingPlan = ref(false)

function errMsg(e) {
  if (!e) return '请求失败'
  if (typeof e === 'string') return e
  return e.message || e.msg || '请求失败'
}

function unwrap(res) {
  return res?.data ?? null
}

export async function fetchCurrentPlan() {
  isLoadingPlan.value = true
  try {
    const res = await apiFetchCurrentPlan()
    const plan = unwrap(res)
    currentPlan.value = plan
    return plan
  } catch (e) {
    return null
  } finally {
    isLoadingPlan.value = false
  }
}

export async function fetchPlatformQuestions(platformKey) {
  try {
    const res = await apiFetchPlatformQuestions(platformKey)
    return unwrap(res)
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export async function recommendNiches(data) {
  try {
    const res = await apiRecommendNiches(data)
    return unwrap(res)
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export async function recommendPersonas(data) {
  try {
    const res = await apiRecommendPersonas(data)
    return unwrap(res)
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export async function savePlan(data) {
  try {
    const res = await apiSavePlan(data)
    const plan = unwrap(res)
    currentPlan.value = plan
    clearPublishPlanCache()
    return plan
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export function useSelfMediaPlan() {
  return {
    currentPlan,
    isLoadingPlan,
    fetchCurrentPlan,
    fetchPlatformQuestions,
    recommendNiches,
    recommendPersonas,
    savePlan
  }
}
