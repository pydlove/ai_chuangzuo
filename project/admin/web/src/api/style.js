import storage from '@/utils/storage.js'

const STYLE_KEY = 'aichuangzuo_admin_style_review'

const SOURCE_TYPES = ['my', 'learned']
const STATUS_LIST = ['pending', 'pending', 'pending', 'pending', 'rejected']

const NAMES = ['娱乐至死', '温柔治愈', '犀利吐槽', '文艺清新', '专业严谨', '轻松俏皮', '热血激昂', '冷静客观', '幽默风趣', '唯美浪漫']
const SCOPES = ['公众号情感文', '小红书种草', '知乎回答', '今日头条', '百家号', '抖音脚本', '通用文案']
const PROMPTS = [
  '轻松幽默、网络热梗、短句为主，适合年轻读者',
  '温柔细腻、情感共鸣、用词优美，适合情感类内容',
  '犀利直接、观点鲜明、节奏快，适合评论类文章',
  '文艺清新、段落优美、引用诗句，适合生活方式',
  '专业严谨、数据支撑、逻辑清晰，适合行业分析',
  '轻松俏皮、表情丰富、互动性强，适合社交媒体',
  '热血激昂、排比有力、情绪饱满，适合励志内容',
  '冷静客观、事实陈述、不偏不倚，适合新闻报道',
  '幽默风趣、包袱不断、反转巧妙，适合娱乐内容',
  '唯美浪漫、画面感强、修辞丰富，适合故事散文'
]

function randomDate(daysAgo) {
  const date = new Date(Date.now() - Math.floor(Math.random() * daysAgo * 86400000))
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function loadStyles() {
  try {
    const raw = storage.get(STYLE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function saveStyles(styles) {
  storage.set(STYLE_KEY, JSON.stringify(styles))
}

function generateMockStyles() {
  return Array.from({ length: 30 }, (_, i) => ({
    id: `market-${String(i + 1).padStart(4, '0')}`,
    name: NAMES[i % NAMES.length],
    sourceType: SOURCE_TYPES[i % SOURCE_TYPES.length],
    creatorName: `用户${String(i + 1).padStart(3, '0')}`,
    prompt: PROMPTS[i % PROMPTS.length],
    scope: SCOPES[i % SCOPES.length],
    status: STATUS_LIST[i % STATUS_LIST.length],
    rejectReason: i % 5 === 4 ? '示例：风格描述过于宽泛，请补充具体写作要求' : '',
    createdAt: randomDate(30)
  }))
}

const MOCK_STYLES = loadStyles() || generateMockStyles()
if (!loadStyles()) {
  saveStyles(MOCK_STYLES)
}

function delay(ms = 300 + Math.floor(Math.random() * 300)) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function listStyles(params = {}) {
  const { keyword = '', page = 1, pageSize = 10 } = params
  return delay().then(() => {
    const styles = loadStyles() || MOCK_STYLES
    const filtered = styles.filter((s) => {
      if (!keyword) return true
      const kw = keyword.toLowerCase()
      return s.name.toLowerCase().includes(kw) || s.creatorName.toLowerCase().includes(kw)
    })
    const start = (page - 1) * pageSize
    return {
      list: filtered.slice(start, start + pageSize),
      total: filtered.length
    }
  })
}

export function rejectStyle(id, reason) {
  return delay().then(() => {
    const styles = loadStyles() || MOCK_STYLES
    const style = styles.find((s) => s.id === id)
    if (!style) throw new Error('风格不存在')
    if (style.status === 'rejected') throw new Error('该风格已被打回')
    if (!reason || !reason.trim()) throw new Error('请输入打回原因')
    style.status = 'rejected'
    style.rejectReason = reason.trim()
    saveStyles(styles)
    return style
  })
}
