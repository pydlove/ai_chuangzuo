import { api } from '@/api/auth'

/**
 * 获取邀请有礼统计。
 * @returns {Promise<{inviteCode:string, invitedCount:number, inviteCoinEarned:number, coinEarned:number, coinBalance:number, friends:Array}>}
 */
export function getInviteStats() {
  return api.get('/account/invite-stats').then((res) => res.data || {})
}
