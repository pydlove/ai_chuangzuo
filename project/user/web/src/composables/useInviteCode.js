/** 读取邀请链接 ?ref= 参数；返回 trim+uppercase 后的码。 */
export function getRefFromUrl() {
  const params = new URLSearchParams(window.location.search)
  const ref = params.get('ref')
  return ref ? ref.trim().toUpperCase() : ''
}
