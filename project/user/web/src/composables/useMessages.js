import { ref } from 'vue'
import { getMessages, getUnreadCount, markMessageRead, markAllMessagesRead } from '@/api/message'

// 模块级 ref：ConsoleLayout 的铃铛 / 手机端 TabBar 角标 与 MessagesIndex 的消息列表共享同一份数据，
// 保证任意一处标记已读后，另一处实时同步。
const messages = ref([])
const loading = ref(false)
// 角标数字单独存一份：轮询只拉 /messages/unread-count（不拉全量列表），
// 打开消息中心或消息页时再用全量列表覆盖。
const unreadCount = ref(0)

// 角标轮询间隔，消息源（生成完成/会员到期/奖励/公告）都是低频，30s 足够
const POLL_INTERVAL = 30000
let pollTimer = null
let pollRefCount = 0

async function loadMessages() {
  loading.value = true
  try {
    const res = await getMessages()
    messages.value = (res.data || []).map((n) => {
      // 兼容旧数据：后端早期使用 type='skill'，前端按 'style' 渲染
      if (n.type === 'skill') {
        n.type = 'style'
      }
      return n
    }).sort((a, b) => {
      // 未读置顶，同状态下按时间倒序
      if (a.read !== b.read) return a.read ? 1 : -1
      return new Date(b.createdAt) - new Date(a.createdAt)
    })
    unreadCount.value = messages.value.filter(m => !m.read).length
  } catch (e) {
    messages.value = []
  } finally {
    loading.value = false
  }
}

async function refreshUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch {
    // 轮询失败保持上一次的值，避免角标闪烁
  }
}

function resumePolling() {
  if (pollTimer) return
  refreshUnreadCount()
  if (document.hidden) return
  pollTimer = setInterval(refreshUnreadCount, POLL_INTERVAL)
}

function pausePolling() {
  clearInterval(pollTimer)
  pollTimer = null
}

// 页面切到后台停轮询，回到前台立刻补一次，避免无谓请求也避免角标过期
function handleVisibilityChange() {
  if (document.hidden) {
    pausePolling()
  } else {
    resumePolling()
  }
}

function startUnreadPolling() {
  pollRefCount += 1
  if (pollRefCount > 1) return
  document.addEventListener('visibilitychange', handleVisibilityChange)
  resumePolling()
}

function stopUnreadPolling() {
  pollRefCount = Math.max(0, pollRefCount - 1)
  if (pollRefCount > 0) return
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  pausePolling()
}

function unreadCountByType(type) {
  if (type === 'all') {
    return unreadCount.value
  }
  if (type === 'station') {
    return messages.value.filter(m => (m.type === 'membership' || m.type === 'reward') && !m.read).length
  }
  return messages.value.filter(m => m.type === type && !m.read).length
}

async function markRead(id) {
  try {
    await markMessageRead(id)
    const msg = messages.value.find(m => m.id === id)
    if (msg && !msg.read) {
      msg.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  } catch {
    // 即使接口失败也允许继续交互
  }
}

async function markAllRead() {
  try {
    await markAllMessagesRead()
    messages.value.forEach(m => { m.read = true })
    unreadCount.value = 0
    return true
  } catch {
    return false
  }
}

export function useMessages() {
  return {
    messages,
    loading,
    unreadCount,
    unreadCountByType,
    loadMessages,
    refreshUnreadCount,
    startUnreadPolling,
    stopUnreadPolling,
    markRead,
    markAllRead
  }
}
