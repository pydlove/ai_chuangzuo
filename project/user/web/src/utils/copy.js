export async function copyToClipboard(text) {
  if (text == null || text === '') return

  // 优先使用现代 Clipboard API（需要 HTTPS 或 localhost）
  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(String(text))
      return
    } catch {
      // 失败时回退到 execCommand，兼容微信内置浏览器等环境
    }
  }

  const textarea = document.createElement('textarea')
  textarea.value = String(text)
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '0'
  textarea.style.left = '0'
  textarea.style.width = '1px'
  textarea.style.height = '1px'
  textarea.style.padding = '0'
  textarea.style.border = '0'
  textarea.style.outline = 'none'
  textarea.style.opacity = '0'
  textarea.style.zIndex = '-1'
  document.body.appendChild(textarea)

  textarea.focus()
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)

  try {
    const ok = document.execCommand('copy')
    if (!ok) {
      throw new Error('execCommand copy returned false')
    }
  } finally {
    document.body.removeChild(textarea)
  }
}
