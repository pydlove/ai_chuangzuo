import request from '@/utils/request'

export function subscribe(data) {
  return request.post('/membership/subscribe', data)
}

export function getMyMembership() {
  return request.get('/membership/me')
}
