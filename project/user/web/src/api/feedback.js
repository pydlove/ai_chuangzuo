import request from '@/utils/request'

export function submitFeedback(data) {
  return request.post('/feedback/submit', data)
}

export function pageMyFeedbacks(params) {
  return request.get('/feedback/mine', { params })
}
