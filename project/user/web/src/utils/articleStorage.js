const CURRENT_ARTICLE_KEY = 'aichuangzuo_current_article'
const QUEUE_KEY = 'aichuangzuo_generation_queue'

function normalizeStyleOverrides(value) {
  const empty = { blocks: {}, inlines: [] }
  if (!value || typeof value !== 'object') return empty
  const blocks = (value.blocks && typeof value.blocks === 'object') ? value.blocks : {}
  const inlines = Array.isArray(value.inlines) ? value.inlines.filter(i =>
    i && typeof i.block === 'number' && typeof i.start === 'number' && typeof i.end === 'number' && i.styles && typeof i.styles === 'object'
  ) : []
  return { blocks, inlines }
}

export function loadCurrentArticle() {
  try {
    const raw = localStorage.getItem(CURRENT_ARTICLE_KEY)
    if (!raw) return null
    const article = JSON.parse(raw)
    if (article && typeof article === 'object') {
      article.styleOverrides = normalizeStyleOverrides(article.styleOverrides)
    }
    return article
  } catch (e) {
    console.error('load current article failed', e)
    return null
  }
}

export function saveCurrentArticle(article) {
  try {
    const safe = {
      ...article,
      styleOverrides: normalizeStyleOverrides(article && article.styleOverrides)
    }
    localStorage.setItem(CURRENT_ARTICLE_KEY, JSON.stringify(safe))
    return true
  } catch (e) {
    console.error('save current article failed', e)
    return false
  }
}

export function syncArticleToQueue(article) {
  if (!article || !article.id) return false
  try {
    const raw = localStorage.getItem(QUEUE_KEY) || '[]'
    const queue = JSON.parse(raw)
    const idx = queue.findIndex(item => item.id === article.id)
    if (idx >= 0) {
      queue[idx].content = { title: article.title, body: article.body }
      queue[idx].title = article.title
      queue[idx].wordCount = estimateWordCount(article.body)
      localStorage.setItem(QUEUE_KEY, JSON.stringify(queue))
      return true
    }
    return false
  } catch (e) {
    console.error('sync article to queue failed', e)
    return false
  }
}

function estimateWordCount(body) {
  if (!body) return 0
  return String(body).replace(/\s/g, '').length
}
