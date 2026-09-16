import request from '@/utils/request.js'

const BASE = '/simulation/batches'

export function createBatch(data) {
  return request.post(BASE, data).then((res) => res.data)
}

export function listBatches(params = {}) {
  return request.get(BASE, { params }).then((res) => res.data)
}

export function getBatch(id) {
  return request.get(`${BASE}/${id}`).then((res) => res.data)
}

export function listBatchLogs(id, params = {}) {
  return request.get(`${BASE}/${id}/logs`, { params }).then((res) => res.data)
}

export function cancelBatch(id) {
  return request.post(`${BASE}/${id}/cancel`).then((res) => res.data)
}
