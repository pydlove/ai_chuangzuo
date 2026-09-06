import request from '@/utils/request.js'

export function listSelfMediaPlans(params = {}) {
  return request.get('/self-media-plans', { params }).then((res) => res.data)
}
