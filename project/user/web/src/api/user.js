import { api } from '@/api/auth'

/**
 * 获取当前登录用户的个人资料。
 * @returns {Promise<{userId:string, nickname:string, email:string, avatarUrl:string|null, emailVerified:number, phoneVerified:number, inviterUserId:number|null, inviterNickname:string|null}>}
 */
export function getMyProfile() {
  return api.get('/me')
}

/**
 * 修改个人资料（昵称、简介、性别、生日、所在地）。
 * @param {{nickname?:string, bio?:string, gender?:number, birthday?:string, location?:string}} payload
 * @returns {Promise<{userId:string, nickname:string, email:string, avatarUrl:string|null, emailVerified:number, phoneVerified:number, inviterUserId:number|null, inviterNickname:string|null}>}
 */
export function updateProfile(payload) {
  return api.put('/me/profile', payload)
}

/**
 * 修改昵称。
 * @param {string} nickname 新昵称，1-20 字符
 * @returns {Promise<{userId:string, nickname:string, email:string, avatarUrl:string|null, emailVerified:number, phoneVerified:number, inviterUserId:number|null, inviterNickname:string|null}>}
 */
export function updateNickname(nickname) {
  return api.put('/me/nickname', { nickname })
}

/**
 * 修改邮箱（需要新邮箱收到的验证码）。
 * @param {string} newEmail
 * @param {string} emailCode 6 位验证码
 * @returns {Promise<{userId:string, nickname:string, email:string, avatarUrl:string|null, emailVerified:number, phoneVerified:number, inviterUserId:number|null, inviterNickname:string|null}>}
 */
export function updateEmail(newEmail, emailCode) {
  return api.put('/me/email', { newEmail, emailCode })
}

/**
 * 修改手机号（需要新手机号收到的短信验证码）。
 * @param {string} newPhone 新手机号
 * @param {string} smsCode 6 位短信验证码
 * @returns {Promise<{userId:string, nickname:string, email:string|null, phone:string|null, avatarUrl:string|null, emailVerified:number, phoneVerified:number, inviterUserId:number|null, inviterNickname:string|null}>}
 */
export function updatePhone(newPhone, smsCode) {
  return api.put('/me/phone', { newPhone, smsCode })
}

/**
 * 修改密码（已登录状态，需原密码）。
 * @param {{oldPassword:string, newPassword:string, confirmPassword:string}} payload
 */
export function changePassword(payload) {
  return api.put('/me/password', payload)
}

/**
 * 绑定邀请人（注册 7 天内且未绑定过邀请人时可补绑）。
 * @param {string} inviteCode 6 位邀请码
 */
export function bindInviteCode(inviteCode) {
  return api.post('/me/invite-binding', { inviteCode })
}

/**
 * 上传头像。
 * @param {File} file 压缩后的头像文件
 * @returns {Promise<{userId:string, nickname:string, email:string, avatarUrl:string|null, emailVerified:number, phoneVerified:number, inviterUserId:number|null, inviterNickname:string|null}>}
 */
export function updateAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/me/avatar', formData, {
    timeout: 30000,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
