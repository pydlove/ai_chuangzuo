import request from '@/utils/request.js'

export function listUserArticles(params = {}) {
  return request.get('/articles', { params }).then((res) => res.data)
}

export function getArticleDetail(bizNo) {
  return request.get(`/articles/${bizNo}`).then((res) => res.data)
}
