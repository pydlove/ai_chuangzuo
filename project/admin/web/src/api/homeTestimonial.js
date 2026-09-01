import request from '@/utils/request.js'

const BASE = '/testimonials'

export function listTestimonials(params = {}) {
  const { keyword = '', pageNum = 1, pageSize = 20 } = params
  return request
    .get(BASE, { params: { keyword, pageNum, pageSize } })
    .then((res) => {
      const data = res.data || {}
      const rows = data.records || data.list || []
      return {
        list: rows,
        total: data.total || 0
      }
    })
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

export function batchDeleteTestimonials(ids) {
  return request.post(`${BASE}/batch/delete`, { ids }).then((res) => res.data)
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

export function downloadTestimonialImportTemplate() {
  return request.get(`${BASE}/import-template`, {
    responseType: 'blob'
  })
}

export function importTestimonials(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request
    .post(`${BASE}/import-excel`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    .then((res) => res.data)
}
