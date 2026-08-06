import { api } from '@/api/auth'

/**
 * 获取用户实名信息。
 * @returns {Promise<{realName:string, idCard:string, verified:boolean}|null>}
 */
export function getRealName() {
  return api.get('/account/real-name').then((res) => res.data || null)
}

/**
 * 提交实名认证。
 * @param {{realName:string, idCard:string}} data
 * @returns {Promise<void>}
 */
export function submitRealName(data) {
  return api.post('/account/real-name', data).then((res) => res.data)
}

/**
 * 查询提现记录列表。
 * @returns {Promise<Array<{bizNo:string, amount:number, account:string, name:string, status:string, createdAt:string}>>}
 */
export function listWithdrawals() {
  return api.get('/account/withdrawals').then((res) => res.data || [])
}

/**
 * 申请提现。
 * @param {{amount:number, account:string}} data
 * @returns {Promise<string>} 提现业务编号
 */
export function applyWithdraw(data) {
  return api.post('/account/withdrawals', data).then((res) => res.data)
}
