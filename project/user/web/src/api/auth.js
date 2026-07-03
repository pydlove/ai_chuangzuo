import axios from 'axios'

// 开发环境：/api 由 Vite proxy 转发到后端（见 vite.config.js）
// 生产环境：通过 VITE_API_BASE_URL 指定后端域名
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1/user',
  timeout: 10000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('aichuangzuo_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const code = error.response?.data?.code
    // 401 或 token 过期/无效 → 清空本地登录态并跳转登录页
    if (status === 401 || code === 111010 || code === 111011) {
      localStorage.removeItem('aichuangzuo_access_token')
      localStorage.removeItem('aichuangzuo_refresh_token')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error.response?.data || error)
  }
)

export function getCaptcha() {
  return api.get('/auth/captcha')
}

export function sendEmailCode(data) {
  return api.post('/auth/email-codes', data)
}

export function register(data) {
  return api.post('/auth/register', data)
}

export function login(data) {
  return api.post('/auth/login', data)
}

export function refreshToken(data) {
  return api.post('/auth/refresh-token', data)
}

export function logout() {
  return api.post('/auth/logout')
}