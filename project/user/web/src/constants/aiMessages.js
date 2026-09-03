/**
 * AI 各业务场景的等待提示话术。
 *
 * 规则：
 * 1. 语气亲切，统一用「小爱正在…」。
 * 2. 明确给出预计等待时间，降低用户焦虑。
 * 3. 不同场景根据实际耗时写不同的提示。
 * 4. 全部导出为纯函数或常量，便于复用和单测。
 */

const TEMPLATES = {
  titleOptimize: '小爱正在为你优化标题，预计需要 30 秒~1 分钟，请稍候',
  publishPlan: '小爱正在为你定制发布计划，预计需要 30 秒~1 分钟',
  repostPlan: '小爱正在为你准备多平台发布方案，预计需要 30 秒~1 分钟',
  recommendedTopics: '小爱正在为你挑选今日创作选题，预计需要 30 秒~1 分钟',
  recommendedAngles: '小爱正在为你生成文章观点，预计需要 30 秒~1 分钟',
  submitGeneration: '已加入生成队列，小爱正在创作，预计需要 30 秒~1 分钟',
  nicheRecommend: '小爱正在为你分析赛道方向，预计需要 30 秒~1 分钟',
  personaRecommend: '小爱正在为你设计人设与内容支柱，预计需要 30 秒~1 分钟',
  platformQuestions: '小爱正在为你准备平台问题，预计需要 30 秒左右'
}

/**
 * 获取等待提示文案。
 * @param {keyof TEMPLATES} scene
 * @returns {string}
 */
export function getAiWaitingText(scene) {
  return TEMPLATES[scene] || '小爱正在处理中，预计需要 30 秒~1 分钟'
}

/**
 * 生成提交成功后的轻提示，提醒用户可离开等待。
 * @param {string} [fallback]
 * @returns {string}
 */
export function getAsyncSubmittedText(fallback = '已提交') {
  return `${fallback}，小爱会在后台完成，预计 30 秒~1 分钟`
}
