import axios from 'axios'

// 开发环境：/api 由 Vite proxy 转发到后端（见 vite.config.js）
// 生产环境：通过 VITE_API_BASE_URL 指定后端域名
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1/user',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('aichuangzuo_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    // 统一响应包装：业务错误码也转成 rejected，方便调用方用 try/catch 处理
    if (payload && payload.code !== undefined && payload.code !== 0) {
      const status = response.status
      const code = payload.code
      // 401 或 token 过期/无效 → 清空本地登录态并跳转登录页
      if (status === 401 || code === 111010 || code === 111011) {
        localStorage.removeItem('aichuangzuo_access_token')
        localStorage.removeItem('aichuangzuo_refresh_token')
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
      return Promise.reject(payload)
    }
    return payload
  },
  (error) => {
    const status = error.response?.status
    const code = error.response?.data?.code
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

export default request
