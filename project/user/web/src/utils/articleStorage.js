const CURRENT_ARTICLE_KEY = 'aichuangzuo_current_article'
const QUEUE_KEY = 'aichuangzuo_generation_queue'

export function loadCurrentArticle() {
  try {
    const raw = localStorage.getItem(CURRENT_ARTICLE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    console.error('load current article failed', e)
    return null
  }
}

export function saveCurrentArticle(article) {
  try {
    localStorage.setItem(CURRENT_ARTICLE_KEY, JSON.stringify(article))
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
