import request from '@/utils/request.js'

export function getDashboard() {
  return request.get('/stats/dashboard').then((body) => body.data)
}

export function getDashboardTrend(days = 30) {
  return request.get('/stats/dashboard/trend', { params: { days } }).then((body) => body.data)
}

export function getDashboardDistribution() {
  return request.get('/stats/dashboard/distribution').then((body) => body.data)
}
