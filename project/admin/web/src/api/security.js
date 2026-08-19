import request from '@/utils/request.js'

/** 拉取 AI 提示词分析安全配置。 */
export const getSkillAnalyzeConfig = () =>
  request.get('/security/skill-analyze-config').then((res) => res.data)

/** 更新 AI 提示词分析安全配置。 */
export const updateSkillAnalyzeConfig = (payload) =>
  request.put('/security/skill-analyze-config', payload).then((res) => res.data)

/** 拉取登录限流配置。 */
export const getRateLimitConfig = () =>
  request.get('/security/rate-limit-config').then((res) => res.data)

/** 更新登录限流配置。 */
export const updateRateLimitConfig = (payload) =>
  request.put('/security/rate-limit-config', payload).then((res) => res.data)

/** 拉取访问控制规则列表。 */
export const fetchAccessControlRules = (params = {}) =>
  request.get('/security/access-control/rules', { params }).then((res) => res.data)

/** 创建访问控制规则。 */
export const createAccessControlRule = (payload) =>
  request.post('/security/access-control/rules', payload).then((res) => res.data)

/** 更新访问控制规则。 */
export const updateAccessControlRule = (id, payload) =>
  request.put(`/security/access-control/rules/${id}`, payload).then((res) => res.data)

/** 删除访问控制规则。 */
export const deleteAccessControlRule = (id) =>
  request.delete(`/security/access-control/rules/${id}`).then((res) => res.data)

/** 获取短信配置。 */
export const getSmsConfig = () =>
  request.get('/security/sms-config').then((res) => res.data)

/** 更新短信配置。 */
export const updateSmsConfig = (payload) =>
  request.put('/security/sms-config', payload).then((res) => res.data)
