import request from '@/utils/request.js'

export function listUserOptions(keyword = '', limit = 20) {
  return request.get('/api/v1/admin/users/options', { params: { keyword, limit } })
    .then((body) => body.data || [])
}
