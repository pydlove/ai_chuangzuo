/**
 * 规范化消息跳转链接。
 * 历史数据或后端旧代码可能写入前端不存在的路由，在这里做兼容映射。
 */
export function normalizeMessageLink(link) {
  if (link === '/me/membership') return '/console/benefits'
  return link
}
