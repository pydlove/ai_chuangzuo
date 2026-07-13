const CURRENT_ARTICLE_KEY = 'aichuangzuo_current_article'

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

