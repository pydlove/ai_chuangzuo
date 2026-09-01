import request from '@/utils/request.js'

export function batchGenerateExperienceTokens(data) {
  return request.post('/experience-tokens/batch-generate', data).then((res) => res.data)
}

export function listExperienceTokens(params = {}) {
  return request.get('/experience-tokens', { params }).then((res) => res.data)
}
