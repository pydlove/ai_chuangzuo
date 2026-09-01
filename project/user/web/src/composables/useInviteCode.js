/** 读取邀请链接 ?ref= 参数；返回 trim+uppercase 后的码。 */
export function getRefFromUrl() {
  const params = new URLSearchParams(window.location.search)
  const ref = params.get('ref')
  return ref ? ref.trim().toUpperCase() : ''
}

/** 读取体验会员链接 ?experience= 参数；返回 trim 后的令牌。 */
export function getExperienceTokenFromUrl() {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('experience')
  return token ? token.trim() : ''
}
