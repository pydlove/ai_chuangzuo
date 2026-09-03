import axios from 'axios'
import { STORAGE_KEYS } from '@/constants/storage.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1/user'
const REFRESH_LEEWAY_MS = 5 * 60 * 1000

function getAccessToken() {
  return localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
}

function getRefreshToken() {
  return localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN)
}

function getExpiresAt() {
  const raw = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT)
  return raw ? Number(raw) : null
}

function setTokens(data) {
  localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, data.accessToken)
  localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, data.refreshToken)
  localStorage.setItem(STORAGE_KEYS.REMEMBER_ME, data.rememberMe ? 'true' : 'false')
  if (data.expiresIn != null) {
    localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT, String(Date.now() + data.expiresIn * 1000))
  }
}

function clearTokens() {
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
  localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN)
  localStorage.removeItem(STORAGE_KEYS.REMEMBER_ME)
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT)
}

function isAccessTokenEffectivelyExpired() {
  const token = getAccessToken()
  if (!token) return true
  const expiresAt = getExpiresAt()
  if (!expiresAt) return true
  return Date.now() >= expiresAt - REFRESH_LEEWAY_MS
}

/**
 * 应用启动时初始化认证状态。
 *
 * - 如果 localStorage 中有 refreshToken 且 accessToken 已过期或即将过期，
 *   主动调用 /auth/refresh-token 换取新 token，避免首个业务请求 401。
 * - 使用裸 axios 调用，不经过 request 拦截器，避免带上可能已过期的 Authorization 头。
 * - 如果 refresh 失败（token 过期、被吊销等），清理本地凭证，让路由守卫引导到登录页。
 */
export async function initAuth() {
  if (!isAccessTokenEffectivelyExpired()) {
    return
  }

  const refreshTokenValue = getRefreshToken()
  // 即使 localStorage 中的 refreshToken 被浏览器清理，HttpOnly Cookie 仍可能保留，
  // 所以 accessToken 过期/缺失时仍尝试刷新，由后端决定能否从 Cookie 兜底。
  const body = refreshTokenValue ? { refreshToken: refreshTokenValue } : {}

  try {
    const res = await axios.post(
      `${API_BASE_URL}/auth/refresh-token`,
      body,
      { withCredentials: true }
    )
    const payload = res.data
    if (payload && payload.code === 0 && payload.data) {
      setTokens(payload.data)
    } else {
      throw new Error(payload?.message || '刷新失败')
    }
  } catch (err) {
    clearTokens()
    throw err
  }
}
