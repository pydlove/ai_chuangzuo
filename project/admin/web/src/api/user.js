import request from '@/utils/request.js'

export function listUsers(params = {}) {
  return request.get('/api/v1/admin/users', { params }).then((res) => res.data)
}

export function getUser(id) {
  return request.get(`/api/v1/admin/users/${id}`).then((res) => res.data)
}

export function createUser(data) {
  return request.post('/api/v1/admin/users', data).then((res) => res.data)
}

export function updateUserStatus(id, status) {
  return request.patch(`/api/v1/admin/users/${id}/status`, { status })
}

export function resetUserPassword(id) {
  return request.post(`/api/v1/admin/users/${id}/reset-password`).then((res) => res.data)
}

export function updateUserMembership(id, expireDate) {
  return request.patch(`/api/v1/admin/users/${id}/membership`, { expireDate }).then((res) => res.data)
}

export function updateUser(id, data) {
  return request.put(`/api/v1/admin/users/${id}`, data).then((res) => res.data)
}
