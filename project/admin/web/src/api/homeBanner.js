import request from '@/utils/request.js'

const BASE = '/api/v1/admin/home-banner'

export function fetchHomeBanners() {
  return request.get(BASE).then((res) => res.data || [])
}

export function createHomeBanner(data) {
  return request.post(BASE, data).then((res) => res.data)
}

export function updateHomeBanner(id, data) {
  return request.put(`${BASE}/${id}`, data)
}

export function deleteHomeBanner(id) {
  return request.delete(`${BASE}/${id}`)
}
