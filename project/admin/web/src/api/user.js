import request from '@/utils/request.js'

export function listUsers(params = {}) {
  return request.get('/api/v1/admin/users', { params }).then((res) => res.data)
}

export function getUser(id) {
  return request.get(`/api/v1/admin/users/${id}`).then((res) => res.data)
}

export function getUserInvites(id) {
  return request.get(`/api/v1/admin/users/${id}/invites`).then((res) => res.data)
}

export function listUserSkills(id, sourceType = 1) {
  return request.get(`/api/v1/admin/users/${id}/skills`, { params: { sourceType } }).then((res) => res.data)
}

export function listUserLearnedSkillsByMonth(id) {
  return request.get(`/api/v1/admin/users/${id}/learned-skills`).then((res) => res.data)
}

export function resetLearnedSkillQuota(id, period) {
  return request.post(`/api/v1/admin/users/${id}/learned-skills/reset`, null, { params: { period } }).then((res) => res.data)
}

export function releaseCustomSkillQuota(id, count) {
  return request.post(`/api/v1/admin/users/${id}/custom-skill-quota/release`, { count }).then((res) => res.data)
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

export function updateUser(id, data) {
  return request.put(`/api/v1/admin/users/${id}`, data).then((res) => res.data)
}

export function importUsers(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request
    .post('/api/v1/admin/users/import-excel', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((res) => res.data)
}

export function downloadUserImportTemplate() {
  return request.get('/api/v1/admin/users/import-template', {
    responseType: 'blob'
  })
}

export function deleteUser(id) {
  return request.delete(`/api/v1/admin/users/${id}`).then((res) => res.data)
}
