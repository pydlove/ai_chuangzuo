import { api } from '@/api/auth'

/**
 * 生成公众号绑定二维码。
 * @returns {Promise<{qrCodeUrl:string, sceneStr:string, expireTime:number}>}
 */
export function generateWechatBindQrCode() {
  return api.get('/wechat/bind/qr-code')
}

/**
 * 生成公众号绑定码。
 * @returns {Promise<{bindCode:string, expireTime:number}>}
 */
export function generateWechatBindCode() {
  return api.post('/wechat/bind/code')
}

/**
 * 查询当前用户公众号绑定状态。
 * @returns {Promise<{bound:boolean, status:number|null, nickname:string|null, avatarUrl:string|null}>}
 */
export function getWechatBindStatus() {
  return api.get('/wechat/bind/status')
}

/**
 * 根据场景值查询绑定状态。
 * @param {string} sceneStr
 * @returns {Promise<{bound:boolean, status:number|null, nickname:string|null, avatarUrl:string|null}>}
 */
export function getWechatBindStatusByScene(sceneStr) {
  return api.get('/wechat/bind/status-by-scene', { params: { sceneStr } })
}
