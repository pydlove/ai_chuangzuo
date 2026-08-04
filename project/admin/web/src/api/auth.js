import request from '@/utils/request.js'

export function adminAuthLogin(data) {
  return request.post('/auth/login', data)
}

export function adminAuthRefreshToken(data) {
  return request.post('/auth/refresh-token', data)
}

export function adminAuthLogout() {
  return request.post('/auth/logout')
}
