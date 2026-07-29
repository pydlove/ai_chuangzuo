import { ref } from 'vue'
import { getMySkills, createSkill, updateSkill, deleteSkill, getSystemSkills, analyzeSkill } from '@/api/skill'
import { marketSkills } from '@/composables/useSkillMarket.js'
import { message } from 'ant-design-vue'

export const systemSkills = ref([])

export const mySkills = ref([])
export const learnedSkills = ref([])
export const currentSkill = ref(null)

const DEFAULT_SKILL_KEY = 'aichuangzuo_default_skill'

export const defaultSkill = ref(null)

export function loadDefaultSkill() {
  try {
    const raw = localStorage.getItem(DEFAULT_SKILL_KEY)
    defaultSkill.value = raw ? JSON.parse(raw) : null
  } catch {
    defaultSkill.value = null
  }
}

export function saveDefaultSkill(skill) {
  defaultSkill.value = skill
  try {
    if (skill) {
      localStorage.setItem(DEFAULT_SKILL_KEY, JSON.stringify(skill))
    } else {
      localStorage.removeItem(DEFAULT_SKILL_KEY)
    }
  } catch {
    // ignore
  }
}

/** 生成默认 skill 的标识对象。 */
export function makeDefaultSkillRef(source, skill) {
  if (!skill) return null
  if (source === 'favorite') {
    return { source, id: skill.id, name: skill.name, prompt: skill.prompt, scope: skill.scope }
  }
  return {
    source,
    bizNo: skill.bizNo,
    name: skill.name,
    prompt: skill.prompt,
    scope: skill.scope
  }
}

/** 判断指定 skill 是否为当前默认 skill。 */
export function isDefaultSkill(source, skill) {
  if (!defaultSkill.value || !skill) return false
  const d = defaultSkill.value
  if (d.source !== source) return false
  if (source === 'favorite') return d.id === skill.id
  return d.bizNo === skill.bizNo
}

/** 尝试应用默认 skill；若未设置或找不到，回退到第一个系统预设。 */
export function applyDefaultSkill() {
  loadDefaultSkill()
  const d = defaultSkill.value
  if (!d) {
    if (systemSkills.value.length > 0 && !currentSkill.value) {
      currentSkill.value = systemSkills.value[0]
    }
    return
  }

  let target = null
  if (d.source === 'system') {
    target = systemSkills.value.find(s => s.bizNo === d.bizNo)
  } else if (d.source === 'my') {
    target = mySkills.value.find(s => s.bizNo === d.bizNo)
  } else if (d.source === 'learned') {
    target = learnedSkills.value.find(s => s.bizNo === d.bizNo)
  } else if (d.source === 'favorite') {
    target = marketSkills.value.find(s => s.id === d.id)
  }

  if (target) {
    currentSkill.value = target
  } else if (systemSkills.value.length > 0 && !currentSkill.value) {
    currentSkill.value = systemSkills.value[0]
  }
}

function errMsg(e) {
  if (!e) return '请求失败'
  if (typeof e === 'string') return e
  return e.message || e.msg || '请求失败'
}

/**
 * 加载系统预设 skills。应用启动时调用一次。
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
 * 加载当前用户的自定义 skills 列表。
 * @returns {Promise<void>}
 */
export async function loadMySkills() {
  try {
    const res = await getMySkills()
    const list = res.data || res || []
    mySkills.value = list.map(s => ({
      bizNo: s.bizNo,
      name: s.skillName,
      desc: '自定义 skills',
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
    message.success('skills 已保存')
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
    message.success('skills 已更新')
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
    if (isDefaultSkill('my', target)) {
      saveDefaultSkill(null)
    }
    message.success('skills 已删除')
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

// ============ 学习的 skills（后端 u_user_style source_type=2） ============

export const isLearning = ref(false)

/** 加载当前用户的学习 skills 列表（sourceType=2）。 */
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

// skills 分析（后端 AI 分析）
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

// 命名去重（在学习 skills 列表内检查；与 mySkills 共用 isSkillNameExists）
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
    message.success('skills 已保存')
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
    message.success('skills 已更新')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}

export async function removeLearnedSkill(bizNo) {
  const target = learnedSkills.value.find(s => s.bizNo === bizNo)
  try {
    await deleteSkill(bizNo)
    await loadLearnedSkills()
    if (target && isDefaultSkill('learned', target)) {
      saveDefaultSkill(null)
    }
    message.success('skills 已删除')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}
