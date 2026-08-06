export const copyToClipboard = async (text) => {
  if (text === undefined || text === null || text === '') {
    return Promise.reject(new Error('复制内容为空'))
  }

  const textValue = String(text)

  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(textValue)
  }

  const textarea = document.createElement('textarea')
  textarea.value = textValue
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  textarea.style.top = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()

  try {
    const success = document.execCommand('copy')
    if (!success) {
      return Promise.reject(new Error('复制命令执行失败'))
    }
  } catch (err) {
    return Promise.reject(err)
  } finally {
    document.body.removeChild(textarea)
  }
}
