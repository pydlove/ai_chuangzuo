import request from '@/utils/request'

export const api = request

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

/**
 * 重置密码（公开接口）
 * @param {object} data
 * @param {string} data.email
 * @param {string} data.emailCode      6 位邮箱验证码
 * @param {string} data.password       新密码
 * @param {string} data.confirmPassword
 */
export function resetPassword(data) {
  return api.post('/auth/reset-password', data)
}
