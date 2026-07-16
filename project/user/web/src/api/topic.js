import request from '@/utils/request'

/** 随机拉取 N 条选题标题（排除我已用过 + 已删除）。 */
export function fetchRandomTopics(count = 6) {
  return request.get('/topics/random', { params: { count } }).then((res) => res.data)
}

/** 上报使用：幂等，重复调用不报错。 */
export function markTopicUsed(id) {
  return request.post(`/topics/${id}/use`).then((res) => res.data)
}
