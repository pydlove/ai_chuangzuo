import request from '@/utils/request'

const AI_TIMEOUT = 60000

export function getRecommendedCreationSession() {
  return request.get('/recommended-creation/session').then((res) => res.data)
}

export function generateRecommendedTopics() {
  return request.post('/recommended-creation/topics', {}, { timeout: AI_TIMEOUT }).then((res) => res.data)
}

export function generateRecommendedAngles(topicId) {
  return request.post('/recommended-creation/angles', { topicId }, { timeout: AI_TIMEOUT }).then((res) => res.data)
}

export function updateRecommendedSession(data) {
  return request.patch('/recommended-creation/session', data).then((res) => res.data)
}

export function submitRecommendedGeneration() {
  return request.post('/recommended-creation/submit').then((res) => res.data)
}

export function clearRecommendedSession() {
  return request.delete('/recommended-creation/session').then((res) => res.data)
}
