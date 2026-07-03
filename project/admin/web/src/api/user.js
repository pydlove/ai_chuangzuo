// 内置 50 条 mock 数据，含中文昵称、随机邮箱、状态、邀请码
// 使用 setTimeout 模拟 300~600ms 延迟

const NICKNAMES = ['夜雨微凉', '山间清风', '云端之上', '月下独酌', '林深时见鹿', '海蓝时见鲸', '梦里花落', '半夏微凉', '浅笑安然', '一纸荒年']
const STATUS_LIST = ['enabled', 'enabled', 'enabled', 'enabled', 'disabled']

function randomDate(daysAgo) {
  const date = new Date(Date.now() - Math.floor(Math.random() * daysAgo * 86400000))
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function generateInviteCode() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let result = ''
  for (let i = 0; i < 6; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

const MOCK_USERS = Array.from({ length: 50 }, (_, i) => ({
  id: i + 1,
  account: `aichuang_${String(i + 1).padStart(3, '0')}`,
  email: `user${i + 1}@example.com`,
  nickname: NICKNAMES[i % NICKNAMES.length],
  status: STATUS_LIST[i % STATUS_LIST.length],
  inviteCode: generateInviteCode(),
  createdAt: randomDate(180),
  lastLoginAt: Math.random() > 0.2 ? randomDate(30) : null
}))

function delay(ms = 300 + Math.floor(Math.random() * 300)) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function listUsers(params = {}) {
  const { keyword = '', page = 1, pageSize = 10 } = params
  return delay().then(() => {
    const filtered = MOCK_USERS.filter((u) => {
      if (!keyword) return true
      const kw = keyword.toLowerCase()
      return u.account.toLowerCase().includes(kw) || u.email.toLowerCase().includes(kw)
    })
    const start = (page - 1) * pageSize
    return {
      list: filtered.slice(start, start + pageSize),
      total: filtered.length
    }
  })
}

export function getUser(id) {
  return delay().then(() => {
    const user = MOCK_USERS.find((u) => u.id === id)
    if (!user) throw new Error('用户不存在')
    return user
  })
}

export function updateUserStatus(id, status) {
  return delay().then(() => {
    const user = MOCK_USERS.find((u) => u.id === id)
    if (!user) throw new Error('用户不存在')
    user.status = status
    return user
  })
}

export function resetUserPassword(id) {
  return delay().then(() => {
    const user = MOCK_USERS.find((u) => u.id === id)
    if (!user) throw new Error('用户不存在')
    return { id, newPassword: '123456' }
  })
}
