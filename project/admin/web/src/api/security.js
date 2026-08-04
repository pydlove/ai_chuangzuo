import request from '@/utils/request.js'

/** 拉取 AI 提示词分析安全配置。 */
export const getSkillAnalyzeConfig = () =>
  request.get('/security/skill-analyze-config').then((res) => res.data)

/** 更新 AI 提示词分析安全配置。 */
export const updateSkillAnalyzeConfig = (payload) =>
  request.put('/security/skill-analyze-config', payload).then((res) => res.data)
