import { ref, watch } from 'vue'
import {
  getMySkills,
  createSkill,
  updateSkill,
  deleteSkill,
  getSystemSkills,
  analyzeSkill
} from '@/api/skill'
import { message } from 'ant-design-vue'

export const systemSkills = ref([])

export const mySkills = ref([])
export const learnedSkills = ref([])
export const currentSkill = ref(null)

// 记住最近一次选择的 skill key：模块级 watch，不依赖组件挂载顺序，
// 避免 main.js 提前 auto-pick 时覆盖掉用户上次保存的选择。
const LAST_SKILL_KEY = 'aichuangzuo_create_last_skill'

function saveLastSkill(s) {
  try {
    if (s) localStorage.setItem(LAST_SKILL_KEY, JSON.stringify(s))
  } catch {
    // 隐私模式忽略
  }
}

watch(currentSkill, saveLastSkill, { deep: true })

function errMsg(e) {
  if (!e) return '请求失败'
  if (typeof e === 'string') return e
  return e.message || e.msg || '请求失败'
}

/**
 * 加载系统预设 skills。应用启动时调用一次。
 * 仅负责拉取列表，不再自动选中 —— 选中态由 restoreLastSkill 统一恢复，
 * 避免 main.js 抢先 auto-pick 覆盖掉 localStorage 中用户上次的选择。
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
  } catch (e) {
    console.warn('[loadSystemSkills]', errMsg(e))
  }
}

/**
 * 恢复上次本地记住的 skill。需要在所有 skill 列表（系统/我的/学习/市场）加载完之后调用。
 * - 命中缓存且当前列表里仍存在 → 还原为缓存的那条
 * - 没缓存 / 缓存的 skill 已下架 → 回退到第一个系统提示词
 * - 系统提示词也为空 → 置空，让上层 UI 提示用户去选
 * @param {Array} marketSkillsList 已加载的市场提示词列表（从 useSkillMarket 传入，避免循环依赖）
 */
export function restoreLastSkill(marketSkillsList = []) {
  let saved = null
  try {
    const raw = localStorage.getItem(LAST_SKILL_KEY)
    if (raw) saved = JSON.parse(raw)
  } catch {
    // ignore
  }

  const found = saved && (
    systemSkills.value.find(s => s.bizNo === saved.bizNo)
    || mySkills.value.find(s => s.bizNo === saved.bizNo)
    || learnedSkills.value.find(s => s.bizNo === saved.bizNo)
    || (saved.id && marketSkillsList.find(s => s.id === saved.id))
  )
  if (found) {
    currentSkill.value = found
    return
  }

  if (systemSkills.value.length > 0) {
    currentSkill.value = systemSkills.value[0]
  } else {
    currentSkill.value = null
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
      desc: s.description || '自定义提示词',
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
    scope: (style.scope || '').trim(),
    description: (style.description || '').trim() || null
  }
  try {
    await createSkill(trimmed)
    await loadMySkills()
    message.success('提示词已保存')
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
    scope: (style.scope || '').trim(),
    description: (style.description || '').trim() || null
  }
  try {
    await updateSkill(target.bizNo, trimmed)
    await loadMySkills()
    if (currentSkill.value && currentSkill.value.name === oldName) {
      const updated = mySkills.value.find(s => s.name === trimmed.skillName)
      if (updated) currentSkill.value = updated
    }
    message.success('提示词已更新')
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
    message.success('提示词已删除')
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
  const inLearned = learnedSkills.value.some(s => s.name.trim().toLowerCase() === target)
  return inSystem || inCustom || inLearned
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
      desc: s.description || '',
      prompt: s.prompt,
      excerpt1: s.excerpt1 || '',
      excerpt2: s.excerpt2 || '',
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
      desc: data.description || '',
      scope: '',     // 适用范围，由用户在结果页手填
      createdAt: new Date().toISOString()
    }
  } finally {
    isLearning.value = false
  }
}

export async function addLearnedSkill(style) {
  try {
    await createSkill({
      skillName: style.name.trim(),
      prompt: style.prompt.trim(),
      scope: (style.scope || '').trim(),
      description: (style.desc || '').trim() || null,
      excerpt1: (style.excerpt1 || '').trim(),
      excerpt2: (style.excerpt2 || '').trim(),
      sourceType: 2
    })
    await loadLearnedSkills()
    message.success('提示词已保存')
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
      scope: (style.scope || '').trim(),
      description: (style.desc || '').trim() || null,
      excerpt1: (style.excerpt1 || '').trim(),
      excerpt2: (style.excerpt2 || '').trim()
    })
    await loadLearnedSkills()
    message.success('提示词已更新')
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
    message.success('提示词已删除')
  } catch (e) {
    message.error(errMsg(e))
    throw e
  }
}
