import { ref } from 'vue'
import { earningsRecords } from '@/composables/useStyleMarket.js'

const INCOME_SUBMISSIONS_KEY = 'aichuangzuo_leaderboard_income_submissions'
const REWARD_RECORDS_KEY = 'aichuangzuo_leaderboard_rewards'
const USER_ID_KEY = 'aichuangzuo_user_id'

function load(key, fallback = []) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function save(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

export function getUserId() {
  let id = localStorage.getItem(USER_ID_KEY)
  if (!id) {
    id = 'u_' + Math.random().toString(36).slice(2, 10)
    localStorage.setItem(USER_ID_KEY, id)
  }
  return id
}

export const incomeSubmissions = ref(load(INCOME_SUBMISSIONS_KEY))
export const rewardRecords = ref(load(REWARD_RECORDS_KEY))

const MOCK_NICKNAMES = [
  '创作者小王', '文案阿杰', '自媒体老李', '写作喵', '内容工匠',
  '运营小周', '爆款制造机', '深夜写手', '追光者', '灵感捕手',
  '笔尖流浪', '文章猎手', '流量玩家', '创作旅人', '码字工',
  '新媒体小白', '观点输出机', '故事银行', '热榜观察员'
]

function hashString(str) {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (h << 5) - h + str.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

function seededRandom(seed) {
  const x = Math.sin(seed) * 10000
  return x - Math.floor(x)
}

function generateMockUser(index, month) {
  const userId = 'mock_' + index
  const nickname = MOCK_NICKNAMES[index % MOCK_NICKNAMES.length]
  const seed = hashString(`${month}-${userId}`)
  const amount = Number((seededRandom(seed) * 50 + 0.5).toFixed(2))
  return { userId, nickname, amount }
}

export function getCoinLeaderboard(month) {
  const currentUserId = getUserId()
  const start = new Date(`${month}-01T00:00:00`)
  const end = new Date(start.getFullYear(), start.getMonth() + 1, 1)

  const currentUserAmount = earningsRecords.value
    .filter(r => {
      if (!r.createdAt) return false
      const d = new Date(r.createdAt)
      return r.fromUserId === currentUserId && r.amount > 0 && d >= start && d < end
    })
    .reduce((sum, r) => sum + r.amount, 0)

  const list = [
    { userId: currentUserId, nickname: '我', amount: Number(currentUserAmount.toFixed(2)), isMe: true },
    ...MOCK_NICKNAMES.map((_, i) => generateMockUser(i, month))
  ]

  list.sort((a, b) => b.amount - a.amount)
  return list.map((item, index) => ({ ...item, rank: index + 1 }))
}
