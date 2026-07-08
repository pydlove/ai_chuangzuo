import { ref } from 'vue'
import { getMyStyles, createStyle, updateStyle, deleteStyle, getSystemStyles } from '@/api/style'
import { message } from 'ant-design-vue'

export const systemStyles = ref([])

export const myStyles = ref([])
export const currentStyle = ref(null)

/** 从异常负载里取出可读 message；兼容多种错误结构。 */
function errMsg(e) {
  if (!e) return '请求失败'
  if (typeof e === 'string') return e
  return e.message || e.msg || '请求失败'
}

/**
 * 加载系统预设风格。应用启动时调用一次。
 * @returns {Promise<void>}
 */
export async function loadSystemStyles() {
  try {
    const res = await getSystemStyles()
    const list = res.data || res || []
    systemStyles.value = list.map(s => ({
      bizNo: s.bizNo,
      name: s.name,
      desc: s.description,
      promptSummary: s.promptSummary,
      prompt: s.prompt,
      scope: s.scope
    }))
    if (systemStyles.value.length > 0 && !currentStyle.value) {
      currentStyle.value = systemStyles.value[0]
    }
  } catch (e) {
    console.warn('[loadSystemStyles]', errMsg(e))
  }
}

/**
 * 加载当前用户的自定义风格列表。
 * @returns {Promise<void>}
 */
export async function loadMyStyles() {
  try {
    const res = await getMyStyles()
    const list = res.data || res || []
    myStyles.value = list.map(s => ({
      bizNo: s.bizNo,
      name: s.styleName,
      desc: '自定义风格',
      prompt: s.prompt,
      scope: s.scope,
      count: s.useCount || 0
    }))
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export const applyStyle = (style) => {
  currentStyle.value = style
}

export const addCustomStyle = async (style) => {
  const trimmed = {
    styleName: style.name.trim(),
    prompt: style.prompt.trim(),
    scope: (style.scope || '').trim()
  }
  try {
    await createStyle(trimmed)
    await loadMyStyles()
    message.success('风格已保存')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export const updateCustomStyle = async (oldName, style) => {
  const target = myStyles.value.find(x => x.name === oldName)
  if (!target) return
  const trimmed = {
    styleName: style.name.trim(),
    prompt: style.prompt.trim(),
    scope: (style.scope || '').trim()
  }
  try {
    await updateStyle(target.bizNo, trimmed)
    await loadMyStyles()
    if (currentStyle.value && currentStyle.value.name === oldName) {
      const updated = myStyles.value.find(s => s.name === trimmed.styleName)
      if (updated) currentStyle.value = updated
    }
    message.success('风格已更新')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export const removeCustomStyle = async (name) => {
  const target = myStyles.value.find(x => x.name === name)
  if (!target) return
  try {
    await deleteStyle(target.bizNo)
    await loadMyStyles()
    if (currentStyle.value && currentStyle.value.name === name) {
      currentStyle.value = systemStyles.value[0] || null
    }
    message.success('风格已删除')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export const isStyleNameExists = (name, excludeName = null) => {
  const target = name.trim().toLowerCase()
  if (!target) return false
  if (excludeName && target === excludeName.trim().toLowerCase()) return false
  const inSystem = systemStyles.value.some(s => s.name.trim().toLowerCase() === target)
  const inCustom = myStyles.value.some(s => s.name.trim().toLowerCase() === target)
  return inSystem || inCustom
}

// ============ 文章风格学习（前端 mock，后端替换点） ============

const LEARNED_STORAGE_KEY = 'aichuangzuo_learned_styles'

function loadLearnedStyles() {
  try {
    const raw = localStorage.getItem(LEARNED_STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveLearnedStyles() {
  localStorage.setItem(LEARNED_STORAGE_KEY, JSON.stringify(learnedStyles.value))
}

async function simpleHash(text) {
  const sample = text.slice(0, 1000) + '|' + text.length
  const bytes = new TextEncoder().encode(sample)
  const buffer = await crypto.subtle.digest('SHA-1', bytes)
  const hex = Array.from(new Uint8Array(buffer))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
  return hex.slice(0, 16)
}

export const learnedStyles = ref(loadLearnedStyles())
export const isLearning = ref(false)

export function readFileAsText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target.result)
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsText(file)
  })
}

export async function readDocxAsText(file) {
  if (!window.mammoth) throw new Error('mammoth.js 未加载')
  const buffer = await file.arrayBuffer()
  const result = await window.mammoth.extractRawText({ arrayBuffer: buffer })
  return result.value
}

// 风格分析（前端 mock，async 接口为后端预留）
export async function analyzeArticleStyle(text, meta) {
  isLearning.value = true
  try {
    const fileHash = await simpleHash(text)
    const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim().length > 20)
    const first = paragraphs[0]?.trim() || ''
    const mid = paragraphs[Math.floor(paragraphs.length / 2)]?.trim() || ''
    const sentences = text.split(/[。！？\n]/).filter(s => s.trim().length > 10)
    const longest = sentences.sort((a, b) => b.length - a.length)[0]?.trim().slice(0, 80) || ''

    const prompt = `你是一位中文写手，请模仿以下参考文章的写作风格：

【语气】克制、文学化，善用短句与留白
【词汇】避免网络用语，偏书面表达
【句式】长短句交替，节奏感强
【结构】起承转合清晰，结尾有余味

请在生成新内容时参考以下片段的风格特征。`

    // mock 延迟 1.5 秒
    await new Promise(r => setTimeout(r, 1500))

    return {
      sourceType: meta.sourceType,
      excerpt1: (first || mid).slice(0, 120),
      excerpt2: longest,
      prompt,
      scope: '',     // 适用范围，由用户在结果页手填
      fileHash,
      createdAt: new Date().toISOString()
    }
  } finally {
    isLearning.value = false
  }
}

// 命名去重（仅在学习风格之间检查；与 myStyles 共用 isStyleNameExists）
export function isLearnedStyleNameExists(name, excludeName = null) {
  const target = name.trim().toLowerCase()
  if (!target) return false
  if (excludeName && target === excludeName.trim().toLowerCase()) return false
  return learnedStyles.value.some(s => s.name.trim().toLowerCase() === target)
}

export function addLearnedStyle(style) {
  learnedStyles.value.unshift({
    name: style.name.trim(),
    sourceType: style.sourceType,
    excerpt1: style.excerpt1,
    excerpt2: style.excerpt2,
    prompt: style.prompt.trim(),
    scope: (style.scope || '').trim(),
    fileHash: style.fileHash,
    createdAt: style.createdAt
  })
  saveLearnedStyles()
}

export function removeLearnedStyle(name) {
  const idx = learnedStyles.value.findIndex(s => s.name === name)
  if (idx > -1) learnedStyles.value.splice(idx, 1)
  saveLearnedStyles()
}

export function updateLearnedStyle(oldName, style) {
  const idx = learnedStyles.value.findIndex(s => s.name === oldName)
  if (idx > -1) {
    const updated = {
      ...learnedStyles.value[idx],
      name: style.name.trim(),
      prompt: style.prompt.trim(),
      scope: (style.scope || '').trim()
    }
    learnedStyles.value[idx] = updated
    if (currentStyle.value && currentStyle.value.name === oldName) {
      currentStyle.value = updated
    }
    saveLearnedStyles()
  }
}

export function findLearnedStyleByHash(hash) {
  return learnedStyles.value.find(s => s.fileHash === hash)
}
