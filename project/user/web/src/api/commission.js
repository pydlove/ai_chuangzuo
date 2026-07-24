import { api } from '@/api/auth'

// TODO 后端实现:以下是预留接口,前端暂走 localStorage。
// 真实接入时,把这层 throw 的 Error 移除,实现真正的 HTTP 调用即可。
// composable (useCommission) 内部对每个 action 都做了 try/catch,
// 调用方可以安全调用,失败会自然回落到 localStorage 路径。

function notImplemented(name) {
  return () => Promise.reject(new Error(`commission API ${name} not implemented`))
}

export const listCommissionTasks = notImplemented('listCommissionTasks')
export const getCommissionTask = notImplemented('getCommissionTask')
export const createCommissionTask = notImplemented('createCommissionTask')
export const cancelCommissionTask = notImplemented('cancelCommissionTask')
export const listSubmissions = notImplemented('listSubmissions')
export const submitCommissionArticle = notImplemented('submitCommissionArticle')
export const withdrawSubmission = notImplemented('withdrawSubmission')
export const pickWinner = notImplemented('pickWinner')

// 工具方法,前端自行实现(用于 ID 生成、昵称截断等)
export function genId(prefix) {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

export const COMMISSION_CONFIG = Object.freeze({
  MIN_REWARD: 5,
  MAX_REWARD: 10000,
  PLATFORM_FEE_RATE: 0.10,
  DEADLINE_OPTIONS: [
    { label: '3 天', days: 3 },
    { label: '7 天', days: 7 },
    { label: '15 天', days: 15 }
  ],
  GRACE_HOURS: 24
})