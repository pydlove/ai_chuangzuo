import request from '@/utils/request.js'

const BASE = '/testimonials'

export function listTestimonials() {
  return request.get(BASE).then((res) => res.data || [])
}

export function createTestimonial(data) {
  return request.post(BASE, data).then((res) => res.data)
}

export function updateTestimonial(id, data) {
  return request.put(`${BASE}/${id}`, data)
}

export function deleteTestimonial(id) {
  return request.delete(`${BASE}/${id}`)
}

export function updateTestimonialStatus(id, data) {
  return request.patch(`${BASE}/${id}/status`, data)
}

export function uploadTestimonialAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request
    .post(`${BASE}/upload-avatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    .then((res) => res.data)
}
