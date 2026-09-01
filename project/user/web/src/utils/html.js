/**
 * 去除 HTML 标签，返回纯文本
 * @param {string} html
 * @returns {string}
 */
export function stripHtml(html) {
  if (html == null) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}
