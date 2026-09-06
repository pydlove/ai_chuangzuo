import request from '@/utils/request.js'

/** 获取微信公众号配置。 */
export const getWechatOfficialAccountConfig = () =>
  request.get('/settings/wechat-official-account-config').then((res) => res.data)

/** 更新微信公众号配置。 */
export const updateWechatOfficialAccountConfig = (payload) =>
  request.put('/settings/wechat-official-account-config', payload).then((res) => res.data)

/** 发布微信公众号自定义菜单。 */
export const publishWechatOfficialAccountMenu = () =>
  request.post('/settings/wechat-official-account-config/publish-menu').then((res) => res.data)
