import request from '@/utils/request.js'

export function adminAuthLogin(data) {
  return request.post('/api/v1/admin/auth/login', data)
}

export function adminAuthRefreshToken(data) {
  return request.post('/api/v1/admin/auth/refresh-token', data)
}

export function adminAuthLogout() {
  return request.post('/api/v1/admin/auth/logout')
}
