import request from '@/utils/request.js'

export function listUsers(params = {}) {
  return request.get('/users', { params }).then((res) => res.data)
}

export function getUser(id) {
  return request.get(`/users/${id}`).then((res) => res.data)
}

export function getUserInvites(id) {
  return request.get(`/users/${id}/invites`).then((res) => res.data)
}

export function listUserSkills(id, sourceType = 1) {
  return request.get(`/users/${id}/skills`, { params: { sourceType } }).then((res) => res.data)
}

export function listUserPublishedSkills(id) {
  return request.get(`/users/${id}/published-skills`).then((res) => res.data)
}

export function listUserFavoriteSkills(id) {
  return request.get(`/users/${id}/favorite-skills`).then((res) => res.data)
}

export function listUserLearnedSkillsByMonth(id) {
  return request.get(`/users/${id}/learned-skills`).then((res) => res.data)
}

export function resetLearnedSkillQuota(id, period) {
  return request.post(`/users/${id}/learned-skills/reset`, null, { params: { period } }).then((res) => res.data)
}

export function releaseCustomSkillQuota(id, count) {
  return request.post(`/users/${id}/custom-skill-quota/release`, { count }).then((res) => res.data)
}

export function createUser(data) {
  return request.post('/users', data).then((res) => res.data)
}

export function updateUserStatus(id, status) {
  return request.patch(`/users/${id}/status`, { status })
}

export function resetUserPassword(id) {
  return request.post(`/users/${id}/reset-password`).then((res) => res.data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data).then((res) => res.data)
}

export function importUsers(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request
    .post('/users/import-excel', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((res) => res.data)
}

export function downloadUserImportTemplate() {
  return request.get('/users/import-template', {
    responseType: 'blob'
  })
}

export function deleteUser(id) {
  return request.delete(`/users/${id}`).then((res) => res.data)
}
