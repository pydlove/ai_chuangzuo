/**
 * 会员等级 → 生成队列最大任务数
 * 与后端 MembershipPlan(BASIC=basic / PRO=pro / FLAGSHIP=flagship) 保持一致。
 *
 * 规则:
 *   - free(未开通或已过期): 0,不能提交任何任务
 *   - basic 基础版:           1
 *   - pro 专业版:             5
 *   - flagship 旗舰版:        10
 */
export const MEMBERSHIP_QUEUE_LIMITS = {
  free: 0,
  basic: 1,
  pro: 5,
  flagship: 10,
}

/**
 * 会员等级 → 「发布到风格市场」月度额度
 * 与后端 u_plan_benefit 中 style_market_publish 的值保持一致。
 *   - basic 基础版:           0 (禁止发布)
 *   - pro 专业版:             1
 *   - flagship 旗舰版:        2
 */
export const MEMBERSHIP_STYLE_PUBLISH_QUOTA = {
  free: 0,
  basic: 0,
  pro: 1,
  flagship: 2,
}

/**
 * 会员等级 → 「学习我的风格」月度次数
 * 与后端 u_plan_benefit 中 style_learn_analyze 的值保持一致。
 *   - basic 基础版:           0 (禁止使用)
 *   - pro 专业版:             1
 *   - flagship 旗舰版:        2
 */
export const MEMBERSHIP_STYLE_LEARN_QUOTA = {
  free: 0,
  basic: 0,
  pro: 1,
  flagship: 2,
}

/**
 * 会员等级 → 「我的风格」保存数量上限
 * 与后端 u_plan_benefit 中 style_custom 的值保持一致。
 *   - basic 基础版:           1
 *   - pro 专业版:             2
 *   - flagship 旗舰版:        4
 */
export const MEMBERSHIP_CUSTOM_STYLE_LIMIT = {
  free: 0,
  basic: 1,
  pro: 2,
  flagship: 4,
}

/**
 * 当前档位「我的风格」保存数量上限。未开通或已过期 → 0。
 */
export function getCustomStyleLimit() {
  return MEMBERSHIP_CUSTOM_STYLE_LIMIT[getCurrentPlanKey()] ?? 0
}

export const PLAN_KEY_TO_NAME = {
  basic: '基础版',
  pro: '专业版',
  flagship: '旗舰版',
}

export const PLAN_NAME_TO_KEY = {
  '基础版': 'basic',
  '专业版': 'pro',
  '旗舰版': 'flagship',
}

const MEMBERSHIP_KEY = 'aichuangzuo_membership'

/**
 * 读 localStorage,返回当前档位 key (free / basic / pro / flagship)。
 * - 未登录 / 未开通 → 'free'
 * - 已过期 → 降级为 'free'
 * - 旧 string 格式(早期直接存 '基础版' 等)也能解析
 */
export function getCurrentPlanKey() {
  let raw
  try {
    raw = localStorage.getItem(MEMBERSHIP_KEY)
  } catch {
    return 'free'
  }
  if (!raw) return 'free'

  let level = null
  let expiresAt = null
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') {
      level = parsed.level
      expiresAt = parsed.expiresAt
    } else {
      level = raw
    }
  } catch {
    level = raw
  }

  if (!level) return 'free'

  if (expiresAt) {
    const expiry = new Date(expiresAt)
    if (!isNaN(expiry.getTime()) && expiry.getTime() < Date.now()) {
      return 'free'
    }
  }

  return PLAN_NAME_TO_KEY[level] || 'free'
}

/**
 * 当前用户队列上限。未开通或已过期 → 0。
 */
export function getQueueLimit() {
  return MEMBERSHIP_QUEUE_LIMITS[getCurrentPlanKey()] ?? 0
}

/**
 * 当前档位的中文显示名。free → '免费用户'。
 */
export function getCurrentPlanName() {
  const key = getCurrentPlanKey()
  return key === 'free' ? '免费用户' : (PLAN_KEY_TO_NAME[key] || '免费用户')
}

/**
 * 当前档位「发布到风格市场」月度额度。0 = 禁止发布。
 */
export function getStylePublishQuota() {
  return MEMBERSHIP_STYLE_PUBLISH_QUOTA[getCurrentPlanKey()] ?? 0
}

/**
 * 当前档位「学习我的风格」月度次数。0 = 禁止使用。
 */
export function getStyleLearnQuota() {
  return MEMBERSHIP_STYLE_LEARN_QUOTA[getCurrentPlanKey()] ?? 0
}
