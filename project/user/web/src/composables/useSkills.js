import { ref } from 'vue'
import { getMySkills, createSkill, updateSkill, deleteSkill, getSystemSkills, analyzeSkill } from '@/api/skill'
import { message } from 'ant-design-vue'

export const systemSkills = ref([])

export const mySkills = ref([])
export const currentSkill = ref(null)

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
export async function loadSystemSkills() {
  try {
    const res = await getSystemSkills()
    const list = res.data || res || []
    systemSkills.value = list.map(s => ({
      bizNo: s.bizNo,
      name: s.name,
      desc: s.description,
      promptSummary: s.promptSummary,
      prompt: s.prompt,
      scope: s.scope
    }))
    if (systemSkills.value.length > 0 && !currentSkill.value) {
      currentSkill.value = systemSkills.value[0]
    }
  } catch (e) {
    console.warn('[loadSystemSkills]', errMsg(e))
  }
}

/**
 * 加载当前用户的自定义风格列表。
 * @returns {Promise<void>}
 */
export async function loadMySkills() {
  try {
    const res = await getMySkills()
    const list = res.data || res || []
    mySkills.value = list.map(s => ({
      bizNo: s.bizNo,
      name: s.skillName,
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

export const applySkill = (style) => {
  currentSkill.value = style
}

export const addCustomSkill = async (style) => {
  const trimmed = {
    skillName: style.name.trim(),
    prompt: style.prompt.trim(),
    scope: (style.scope || '').trim()
  }
  try {
    await createSkill(trimmed)
    await loadMySkills()
    message.success('风格已保存')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export const updateCustomSkill = async (oldName, style) => {
  const target = mySkills.value.find(x => x.name === oldName)
  if (!target) return
  const trimmed = {
    skillName: style.name.trim(),
    prompt: style.prompt.trim(),
    scope: (style.scope || '').trim()
  }
  try {
    await updateSkill(target.bizNo, trimmed)
    await loadMySkills()
    if (currentSkill.value && currentSkill.value.name === oldName) {
      const updated = mySkills.value.find(s => s.name === trimmed.skillName)
      if (updated) currentSkill.value = updated
    }
    message.success('风格已更新')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export const removeCustomSkill = async (name) => {
  const target = mySkills.value.find(x => x.name === name)
  if (!target) return
  try {
    await deleteSkill(target.bizNo)
    await loadMySkills()
    if (currentSkill.value && currentSkill.value.name === name) {
      currentSkill.value = systemSkills.value[0] || null
    }
    message.success('风格已删除')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export const isSkillNameExists = (name, excludeName = null) => {
  const target = name.trim().toLowerCase()
  if (!target) return false
  if (excludeName && target === excludeName.trim().toLowerCase()) return false
  const inSystem = systemSkills.value.some(s => s.name.trim().toLowerCase() === target)
  const inCustom = mySkills.value.some(s => s.name.trim().toLowerCase() === target)
  return inSystem || inCustom
}

// ============ 学习的风格（后端 u_user_style source_type=2） ============

export const learnedSkills = ref([])
export const isLearning = ref(false)

/** 加载当前用户的学习风格列表（sourceType=2）。 */
export async function loadLearnedSkills() {
  try {
    const res = await getMySkills(2)
    const list = res.data || res || []
    learnedSkills.value = list.map(s => ({
      bizNo: s.bizNo,
      name: s.skillName,
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
export async function analyzeArticleSkill(text, meta) {
  isLearning.value = true
  try {
    const res = await analyzeSkill(text)
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

// 命名去重（在学习风格列表内检查；与 mySkills 共用 isSkillNameExists）
export function isLearnedSkillNameExists(name, excludeName = null) {
  const target = name.trim().toLowerCase()
  if (!target) return false
  if (excludeName && target === excludeName.trim().toLowerCase()) return false
  return learnedSkills.value.some(s => s.name.trim().toLowerCase() === target)
}

export async function addLearnedSkill(style) {
  try {
    await createSkill({
      skillName: style.name.trim(),
      prompt: style.prompt.trim(),
      scope: (style.scope || '').trim(),
      sourceType: 2
    })
    await loadLearnedSkills()
    message.success('风格已保存')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export async function updateLearnedSkill(bizNo, style) {
  try {
    await updateSkill(bizNo, {
      skillName: style.name.trim(),
      prompt: style.prompt.trim(),
      scope: (style.scope || '').trim()
    })
    await loadLearnedSkills()
    message.success('风格已更新')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export async function removeLearnedSkill(bizNo) {
  try {
    await deleteSkill(bizNo)
    await loadLearnedSkills()
    message.success('风格已删除')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}
