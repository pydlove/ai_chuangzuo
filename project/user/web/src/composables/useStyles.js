import { ref } from 'vue'
import { getMyStyles, createStyle, updateStyle, deleteStyle, getSystemStyles, analyzeStyle } from '@/api/style'
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
      count: s.useCount || 0,
      auditStatus: s.auditStatus
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

// ============ 学习的风格（后端 u_user_style source_type=2） ============

export const learnedStyles = ref([])
export const isLearning = ref(false)

/** 加载当前用户的学习风格列表（sourceType=2）。 */
export async function loadLearnedStyles() {
  try {
    const res = await getMyStyles(2)
    const list = res.data || res || []
    learnedStyles.value = list.map(s => ({
      bizNo: s.bizNo,
      name: s.styleName,
      prompt: s.prompt,
      scope: s.scope,
      count: s.useCount || 0,
      createdAt: s.createdAt,
      auditStatus: s.auditStatus
    }))
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

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

// 风格分析（后端 AI 分析）
export async function analyzeArticleStyle(text, meta) {
  isLearning.value = true
  try {
    const res = await analyzeStyle(text)
    const data = res.data || res || {}
    return {
      sourceType: meta.sourceType,
      excerpt1: data.excerpt1 || '',
      excerpt2: data.excerpt2 || '',
      prompt: data.prompt || '',
      scope: '',     // 适用范围，由用户在结果页手填
      createdAt: new Date().toISOString()
    }
  } finally {
    isLearning.value = false
  }
}

// 命名去重（在学习风格列表内检查；与 myStyles 共用 isStyleNameExists）
export function isLearnedStyleNameExists(name, excludeName = null) {
  const target = name.trim().toLowerCase()
  if (!target) return false
  if (excludeName && target === excludeName.trim().toLowerCase()) return false
  return learnedStyles.value.some(s => s.name.trim().toLowerCase() === target)
}

export async function addLearnedStyle(style) {
  try {
    await createStyle({
      styleName: style.name.trim(),
      prompt: style.prompt.trim(),
      scope: (style.scope || '').trim(),
      sourceType: 2
    })
    await loadLearnedStyles()
    message.success('风格已保存')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export async function updateLearnedStyle(bizNo, style) {
  try {
    await updateStyle(bizNo, {
      styleName: style.name.trim(),
      prompt: style.prompt.trim(),
      scope: (style.scope || '').trim()
    })
    await loadLearnedStyles()
    message.success('风格已更新')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export async function removeLearnedStyle(bizNo) {
  try {
    await deleteStyle(bizNo)
    await loadLearnedStyles()
    message.success('风格已删除')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}
