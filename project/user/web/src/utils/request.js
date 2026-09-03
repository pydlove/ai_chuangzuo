import axios from 'axios'
import { STORAGE_KEYS } from '@/constants/storage.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1/user'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  withCredentials: true
})

let refreshingPromise = null
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

function setTokens(accessToken, refreshToken, expiresIn) {
  localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken)
  if (refreshToken) {
    localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken)
  }
  if (expiresIn != null) {
    localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT, String(Date.now() + expiresIn * 1000))
  }
}

function clearTokens() {
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
  localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN)
  localStorage.removeItem(STORAGE_KEYS.REMEMBER_ME)
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT)
}

function parseJwtExpiresIn(token) {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(atob(base64))
    if (payload.exp) {
      const expMs = payload.exp * 1000
      const remaining = Math.max(0, Math.floor((expMs - Date.now()) / 1000))
      return remaining > 0 ? remaining : null
    }
  } catch {
    // ignore
  }
  return null
}

function isTokenExpiredError(status, code) {
  return status === 401 || code === 111010
}

function isTokenInvalidError(status, code) {
  return code === 111011 || code === 111022 || code === 111023
}

function logoutAndRedirect() {
  clearTokens()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

function shouldRefreshBeforeRequest() {
  const token = getAccessToken()
  if (!token) return false
  const expiresAt = getExpiresAt()
  if (!expiresAt) {
    // 没有记录过期时间时，尝试从 token 中解析
    const expiresIn = parseJwtExpiresIn(token)
    if (expiresIn != null) {
      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT, String(Date.now() + expiresIn * 1000))
      return expiresIn * 1000 <= REFRESH_LEEWAY_MS
    }
    return false
  }
  return Date.now() >= expiresAt - REFRESH_LEEWAY_MS
}

function doRefresh() {
  const refreshToken = getRefreshToken()
  // localStorage 中的 refreshToken 可能被浏览器清理，HttpOnly Cookie 仍可能保留，
  // 因此没有本地 refreshToken 时仍发一次刷新请求，让后端从 Cookie 兜底。
  const body = refreshToken ? { refreshToken } : {}
  // 使用裸 axios 实例，避免循环进入当前拦截器；不带旧的 Authorization 头
  return axios.post(`${API_BASE_URL}/auth/refresh-token`, body, { withCredentials: true })
}

function refreshAccessToken() {
  if (refreshingPromise) {
    return refreshingPromise
  }

  refreshingPromise = doRefresh()
    .then((res) => {
      const payload = res.data
      if (payload && payload.code === 0) {
        const data = payload.data
        setTokens(data.accessToken, data.refreshToken, data.expiresIn)
        return data.accessToken
      }
      throw new Error(payload?.message || 'Refresh failed')
    })
    .catch((err) => {
      logoutAndRedirect()
      throw err
    })
    .finally(() => {
      refreshingPromise = null
    })

  return refreshingPromise
}

function handleRefresh(originalRequest) {
  if (originalRequest._alreadyRetried) {
    logoutAndRedirect()
    return Promise.reject(new Error('Already retried'))
  }

  return refreshAccessToken()
    .then((newToken) => {
      originalRequest.headers.Authorization = `Bearer ${newToken}`
      originalRequest._alreadyRetried = true
      return request(originalRequest)
    })
    .catch((err) => {
      return Promise.reject(err)
    })
}

request.interceptors.request.use(async (config) => {
  // 在 accessToken 即将过期前主动刷新，避免业务请求 401 中断
  if (shouldRefreshBeforeRequest() && !config.url?.includes('/auth/refresh-token')) {
    try {
      const newToken = await refreshAccessToken()
      config.headers.Authorization = `Bearer ${newToken}`
      return config
    } catch {
      // 刷新失败时仍继续发送原请求，让响应拦截器统一处理跳转
    }
  }

  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && payload.code !== undefined && payload.code !== 0) {
      const status = response.status
      const code = payload.code

      if (isTokenInvalidError(status, code)) {
        logoutAndRedirect()
        return Promise.reject(payload)
      }

      if (isTokenExpiredError(status, code)) {
        return handleRefresh(response.config)
      }

      return Promise.reject(payload)
    }
    return payload
  },
  (error) => {
    const response = error.response
    const status = response?.status
    const code = response?.data?.code

    if (isTokenInvalidError(status, code)) {
      logoutAndRedirect()
      return Promise.reject(response?.data || error)
    }

    if (isTokenExpiredError(status, code)) {
      return handleRefresh(error.config)
    }

    return Promise.reject(response?.data || error)
  }
)

export default request
