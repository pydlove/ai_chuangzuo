import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1/user'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000
})

const ACCESS_TOKEN_KEY = 'aichuangzuo_access_token'
const REFRESH_TOKEN_KEY = 'aichuangzuo_refresh_token'

let refreshingPromise = null

function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

function setTokens(accessToken, refreshToken) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  }
}

function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem('aichuangzuo_remember_me')
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

function doRefresh() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return Promise.reject(new Error('No refresh token'))
  }
  // 使用裸 axios 实例，避免循环进入当前拦截器
  return axios.post(`${API_BASE_URL}/auth/refresh-token`, { refreshToken })
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
        setTokens(data.accessToken, data.refreshToken)
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
  const refreshToken = getRefreshToken()
  if (!refreshToken || originalRequest._alreadyRetried) {
    logoutAndRedirect()
    return Promise.reject(new Error('No refresh token or already retried'))
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

request.interceptors.request.use((config) => {
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
