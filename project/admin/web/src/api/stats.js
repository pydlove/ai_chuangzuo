import request from '@/utils/request.js'

export function getStatsOverview() {
  return request.get('/stats/overview').then((body) => body.data)
}
