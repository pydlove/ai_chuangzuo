// 用户端资源 URL 解析。
// 用户上传的头像/图片存于 user-api 本地磁盘，管理端页面需要通过用户端域名访问：
// 生产环境使用 VITE_USER_WEB_URL（如 https://www.ichuang.top），由 Nginx 转发 /api/v1/user 到 user-api；
// 开发环境 VITE_USER_WEB_URL 为空，返回相对路径，由 vite.config.js 代理到本地 user-api。
const USER_WEB_URL = (import.meta.env.VITE_USER_WEB_URL || '').replace(/\/+$/, '')

export function resolveUserAssetUrl(url) {
  if (!url || !url.startsWith('/api/v1/user/')) return url
  return USER_WEB_URL ? `${USER_WEB_URL}${url}` : url
}
