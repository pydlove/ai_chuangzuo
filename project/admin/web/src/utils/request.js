import axios from 'axios'
import storage from '@/utils/storage.js'
import { useUserStore } from '@/stores/user.js'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = storage.get('admin_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 登录失效统一处理：清 token + 跳登录页
// 后端 Spring Security 默认 token 失效返回 403（不是 401），两个状态码都要拦截。
// 用 window.location.replace 硬跳（而不是 router.push），强制清空所有内存状态，
// 避免富文本编辑器等有内部状态的组件在数据 undefined 时白屏。
function redirectToLogin() {
  const userStore = useUserStore()
  userStore.clearToken()
  localStorage.removeItem('admin_refresh_token')
  // 已在登录页就不重复跳
  if (window.location.pathname !== '/login') {
    window.location.replace('/login')
  }
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && typeof body.code === 'number' && body.code !== 0) {
      // 业务错误码 401/403（HTTP 200 但 code 表示登录失效）也要拦截
      if (body.code === 401 || body.code === 403) {
        redirectToLogin()
        return Promise.reject(new Error(body.message || '登录已过期，请重新登录'))
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      redirectToLogin()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    const message = error.response?.data?.message || error.message || '请求失败，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

export default request
