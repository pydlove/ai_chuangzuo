export function isWechatBrowser() {
  return /MicroMessenger/i.test(navigator.userAgent)
}
