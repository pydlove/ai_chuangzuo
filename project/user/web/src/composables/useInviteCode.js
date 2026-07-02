const INVITE_CODE_KEY = 'aichuangzuo_invite_code'
const INVITE_REF_KEY = 'aichuangzuo_invite_ref'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const COIN_BONUS_NEW_USER = 5

const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

function generateInviteCode() {
  let code = ''
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
}

/** 获取或生成当前用户的邀请码（持久化在 localStorage）。 */
export function getInviteCode() {
  let code = localStorage.getItem(INVITE_CODE_KEY)
  if (!code) {
    code = generateInviteCode()
    localStorage.setItem(INVITE_CODE_KEY, code)
  }
  return code
}

/** 读取邀请链接 ?ref= 参数；返回 trim+uppercase 后的码。 */
export function getRefFromUrl() {
  const params = new URLSearchParams(window.location.search)
  const ref = params.get('ref')
  return ref ? ref.trim().toUpperCase() : ''
}

/** 把 ref 写入 localStorage；空值则清除残留。 */
export function setStoredRef(ref) {
  if (ref) {
    localStorage.setItem(INVITE_REF_KEY, ref)
  } else {
    localStorage.removeItem(INVITE_REF_KEY)
  }
}

/** 读取持久化的 ref。 */
export function getStoredRef() {
  const raw = localStorage.getItem(INVITE_REF_KEY)
  return raw ? raw.trim().toUpperCase() : ''
}

/** 注册完成时调用：消费 ref，给当前用户发 5 创作币。返回发放数量。 */
export function awardNewUserCoins() {
  const ref = getStoredRef()
  if (!ref) return 0
  const raw = localStorage.getItem(COIN_BALANCE_KEY)
  const balance = raw ? parseInt(raw, 10) : 0
  localStorage.setItem(COIN_BALANCE_KEY, String(balance + COIN_BONUS_NEW_USER))
  localStorage.removeItem(INVITE_REF_KEY)
  return COIN_BONUS_NEW_USER
}