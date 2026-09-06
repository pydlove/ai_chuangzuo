import request from '@/utils/request.js'

export function listUserOptions(keyword = '', limit = 20) {
  return request.get('/users/options', { params: { keyword, limit } })
    .then((body) => body.data || [])
}

export function fetchUserOptionsPage(keyword = '', page = 1, pageSize = 10) {
  return request.get('/users/options/page', { params: { keyword, page, pageSize } })
    .then((body) => body.data || { list: [], total: 0 })
}
