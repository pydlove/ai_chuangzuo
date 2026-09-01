/**
 * 将 ISO 日期字符串格式化为 "M月D日 HH:MM"
 * 示例：2026-07-15T10:30:00 → "7月15日 14:30"
 * @param {string} dateStr
 * @returns {string}
 */
export function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return ''
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hour = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hour}:${min}`
}

/**
 * 将 Date 或日期字符串格式化为 "YYYY-MM-DD"
 * @param {Date|string} date
 * @returns {string}
 */
export function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
