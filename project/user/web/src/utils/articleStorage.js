import { STORAGE_KEYS } from '@/constants/storage.js'

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
    const raw = localStorage.getItem(STORAGE_KEYS.CURRENT_ARTICLE)
    if (!raw) return null
    const article = JSON.parse(raw)
    if (article && typeof article === 'object') {
      article.styleOverrides = normalizeStyleOverrides(article.styleOverrides)
    }
    return article
  } catch (e) {
    return null
  }
}

export function saveCurrentArticle(article) {
  try {
    const safe = {
      ...article,
      styleOverrides: normalizeStyleOverrides(article && article.styleOverrides)
    }
    localStorage.setItem(STORAGE_KEYS.CURRENT_ARTICLE, JSON.stringify(safe))
    return true
  } catch (e) {
    return false
  }
}

