import request from '@/utils/request.js'

export function getReminderConfig() {
  return request.get('/expire-reminder/config').then((res) => res.data)
}

export function saveReminderConfig(data) {
  return request.put('/expire-reminder/config', data).then((res) => res.data)
}

export function listExpiringUsers(params = {}) {
  return request.get('/expire-reminder/users', { params }).then((res) => res.data)
}

export function remindUser(userId) {
  return request.post(`/expire-reminder/users/${userId}/remind`).then((res) => res.data)
}
