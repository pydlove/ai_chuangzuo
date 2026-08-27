import request from '@/utils/request.js'

export function createQrLogin() {
  return request.post('/auth/qr-login/create')
}

export function getQrLoginStatus(qrCode) {
  return request.get('/auth/qr-login/status', { params: { qrCode } })
}

export function scanQrLogin(qrCode) {
  return request.post('/auth/qr-login/scan', { qrCode })
}

export function authorizeQrLogin(qrCode) {
  return request.post('/auth/qr-login/authorize', { qrCode })
}

export function cancelQrLogin(qrCode) {
  return request.post('/auth/qr-login/cancel', null, { params: { qrCode } })
}
