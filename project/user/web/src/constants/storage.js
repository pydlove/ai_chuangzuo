/**
 * 本地存储 key 常量。
 *
 * 所有 localStorage key 统一在此维护，避免各文件硬编码字符串。
 * 用户相关 / 动态的 key 通过下方 helper 函数生成，保持原有格式不变。
 */

export const STORAGE_PREFIX = 'aichuangzuo'

export const STORAGE_KEYS = {
  // 认证
  ACCESS_TOKEN: `${STORAGE_PREFIX}_access_token`,
  REFRESH_TOKEN: `${STORAGE_PREFIX}_refresh_token`,
  REMEMBER_ME: `${STORAGE_PREFIX}_remember_me`,
  ACCESS_TOKEN_EXPIRES_AT: `${STORAGE_PREFIX}_access_token_expires_at`,

  // 用户
  USER_ID: `${STORAGE_PREFIX}_user_id`,
  MEMBERSHIP: `${STORAGE_PREFIX}_membership`,

  // 主题 / UI 状态
  THEME: `${STORAGE_PREFIX}_theme`,
  MINE_NAV_EXPANDED: `${STORAGE_PREFIX}_mine_nav_expanded`,

  // 创作
  CURRENT_ARTICLE: `${STORAGE_PREFIX}_current_article`,
  DRAFTS: `${STORAGE_PREFIX}_drafts`,
  CREATE_LAST_SKILL: `${STORAGE_PREFIX}_create_last_skill`,
  CREATE_MODE: `${STORAGE_PREFIX}_create_mode`,
  CREATE_FORM: `${STORAGE_PREFIX}_create_form`,

  // 账户 / 收益
  WITHDRAW_AGREEMENT_ACCEPTED: `${STORAGE_PREFIX}_withdraw_agreement_accepted`,

  // 账号检测
  ACCOUNT_CHECK_LAST: `${STORAGE_PREFIX}_account_check_last`,
  ACCOUNT_RECOMMEND_LAST: `${STORAGE_PREFIX}_account_recommend_last`,

  // 新手引导 / 弹窗
  NEWCOMER_BANNER_DISMISSED: `${STORAGE_PREFIX}_newcomer_banner_dismissed`,
  NEWCOMER_MODAL_DISMISSED: `${STORAGE_PREFIX}_newcomer_modal_dismissed`,
  INVITE_MODAL_DISMISSED: `${STORAGE_PREFIX}_invite_modal_dismissed`,

  // 兑换码
  REDEEM_CODES: `${STORAGE_PREFIX}_redeem_codes`,
  REDEEM_HISTORY: `${STORAGE_PREFIX}_redeem_history`,

  // 自媒体方案
  SELF_MEDIA_PLAN_MODAL_DISMISSED: `${STORAGE_PREFIX}_selfmedia_plan_modal_dismissed`,

  // 约稿中心引导
  ONBOARDING_DRAFT: `${STORAGE_PREFIX}_onboarding_draft`,
  ONBOARDING_DONE: `${STORAGE_PREFIX}_onboarding_done`
}

/**
 * 获取账号检测上次输入 key（按用户隔离）。
 */
export function getAccountCheckLastKey(userId) {
  return `${STORAGE_KEYS.ACCOUNT_CHECK_LAST}_${userId || 'anonymous'}`
}

/**
 * 获取账号检测推荐选项 key（按用户隔离）。
 */
export function getAccountRecommendLastKey(userId) {
  return `${STORAGE_KEYS.ACCOUNT_RECOMMEND_LAST}_${userId || 'anonymous'}`
}

/**
 * 获取今日已完成 key。
 */
export function getTodayDoneKey(date = new Date()) {
  return `${STORAGE_PREFIX}_today_done_${date.getFullYear()}_${date.getMonth() + 1}_${date.getDate()}`
}

/**
 * 获取优惠券过期提醒 key。
 */
export function getCouponWarnKey(couponId) {
  return `coupon_warn_${couponId}`
}

/**
 * 获取约稿中心引导草稿 key（按用户隔离）。
 */
export function getOnboardingDraftKey(userId) {
  return userId ? `${STORAGE_KEYS.ONBOARDING_DRAFT}:${userId}` : STORAGE_KEYS.ONBOARDING_DRAFT
}

/**
 * 获取约稿中心引导完成 key（按用户隔离）。
 */
export function getOnboardingDoneKey(userId) {
  return userId ? `${STORAGE_KEYS.ONBOARDING_DONE}:${userId}` : STORAGE_KEYS.ONBOARDING_DONE
}

/**
 * 用户切换时需要清空的一组 key（保留用户隔离的 key 格式前缀）。
 */
export const USER_SCOPED_STORAGE_KEYS = [
  STORAGE_KEYS.ACCESS_TOKEN,
  STORAGE_KEYS.REFRESH_TOKEN,
  STORAGE_KEYS.REMEMBER_ME,
  STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT,
  STORAGE_KEYS.USER_ID,
  STORAGE_KEYS.MEMBERSHIP,
  STORAGE_KEYS.CURRENT_ARTICLE,
  STORAGE_KEYS.DRAFTS,
  STORAGE_KEYS.CREATE_LAST_SKILL,
  STORAGE_KEYS.CREATE_MODE,
  STORAGE_KEYS.CREATE_FORM,
  STORAGE_KEYS.WITHDRAW_AGREEMENT_ACCEPTED,
  STORAGE_KEYS.ACCOUNT_CHECK_LAST,
  STORAGE_KEYS.ACCOUNT_RECOMMEND_LAST,
  STORAGE_KEYS.ONBOARDING_DRAFT,
  STORAGE_KEYS.ONBOARDING_DONE
]
