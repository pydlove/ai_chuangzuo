import request from '@/utils/request.js'

const BASE = '/platforms'

/** 获取所有自媒体平台配置。 */
export function fetchPlatforms() {
  return request.get(BASE).then((res) => res.data || [])
}

/** 新增平台。 */
export function createPlatform(data) {
  return request.post(BASE, data).then((res) => res.data)
}

/** 更新平台。 */
export function updatePlatform(id, data) {
  return request.put(`${BASE}/${id}`, data).then((res) => res.data)
}

/** 删除平台。 */
export function deletePlatform(id) {
  return request.delete(`${BASE}/${id}`).then((res) => res.data)
}
