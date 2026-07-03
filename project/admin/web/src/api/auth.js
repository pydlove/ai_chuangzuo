import request from '@/utils/request.js'

export function adminAuthLogin(data) {
  return request.post('/admin/auth/login', data)
}
