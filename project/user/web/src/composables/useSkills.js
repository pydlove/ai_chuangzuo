import { ref, watch } from 'vue'
import { STORAGE_KEYS } from '@/constants/storage.js'
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
export const mySkillsTotal = ref(0)
export const learnedSkills = ref([])
export const currentSkill = ref(null)

// 记住最近一次选择的 skill key：模块级 watch，不依赖组件挂载顺序，
// 避免 main.js 提前 auto-pick 时覆盖掉用户上次保存的选择。
function saveLastSkill(s) {
  try {
    if (s) localStorage.setItem(STORAGE_KEYS.CREATE_LAST_SKILL, JSON.stringify(s))
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
 * @param {string} [keyword]
 * @param {number} [page]
 * @param {number} [pageSize]
 * @returns {Promise<{list: Array, total: number}>}
 */
export async function loadSystemSkills(keyword = '', page = 1, pageSize = 999) {
  try {
    const res = await getSystemSkills(keyword, page, pageSize)
    const data = res.data || res || {}
    const list = Array.isArray(data) ? data : (data.records || [])
    const total = Array.isArray(data) ? list.length : (data.total || 0)
    const mapped = list.map(s => ({
      bizNo: s.bizNo,
      name: s.name,
      desc: s.description,
      promptSummary: s.promptSummary,
      prompt: s.prompt,
      scope: s.scope
    }))
    // 仅全量加载时同步全局 ref，分页调用由调用方自行维护局部状态
    if (page === 1 && pageSize >= 999) {
      systemSkills.value = mapped
    }
    return { list: mapped, total }
  } catch (e) {
    return { list: [], total: 0 }
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
    const raw = localStorage.getItem(STORAGE_KEYS.CREATE_LAST_SKILL)
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
 * 加载当前用户的自定义 skills 列表（分页）。
 * @param {string} [keyword]
 * @param {number} [page]
 * @param {number} [pageSize]
 * @param {boolean} [updateGlobal=true] 是否同步到全局 mySkills/mySkillsTotal ref
 * @returns {Promise<{total:number, list:Array}>}
 */
export async function loadMySkills(keyword = '', page = 1, pageSize = 12, updateGlobal = true) {
  try {
    const res = await getMySkills(1, keyword, page, pageSize)
    const data = res.data || res || {}
    const list = Array.isArray(data) ? data : (data.records || [])
    const total = Array.isArray(data) ? list.length : (data.total || 0)
    const mapped = list.map(s => ({
      bizNo: s.bizNo,
      name: s.skillName,
      desc: s.description || '自定义提示词',
      prompt: s.prompt,
      scope: s.scope,
      promptExtra: s.promptExtra || null,
      count: s.useCount || 0,
      auditStatus: s.auditStatus
    }))
    if (updateGlobal) {
      mySkills.value = mapped
      mySkillsTotal.value = total
    }
    return { total, list: mapped }
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
    description: (style.description || '').trim() || null,
    promptExtra: style.promptExtra || null
  }
  try {
    await createSkill(trimmed)
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
    description: (style.description || '').trim() || null,
    promptExtra: style.promptExtra || null
  }
  try {
    await updateSkill(target.bizNo, trimmed)
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

/** 加载当前用户的学习 skills 列表（sourceType=2），支持分页。
 * @param {string} [keyword]
 * @param {number} [page]
 * @param {number} [pageSize]
 * @returns {Promise<{list: Array, total: number}>}
 */
export async function loadLearnedSkills(keyword = '', page = 1, pageSize = 999) {
  try {
    const res = await getMySkills(2, keyword, page, pageSize)
    const data = res.data || res || {}
    const list = Array.isArray(data) ? data : (data.records || [])
    const total = Array.isArray(data) ? list.length : (data.total || 0)
    const mapped = list.map(s => ({
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
    // 仅全量加载时同步全局 ref，分页调用由调用方自行维护局部状态
    if (page === 1 && pageSize >= 999) {
      learnedSkills.value = mapped
    }
    return { list: mapped, total }
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
      promptExtra: style.promptExtra || null,
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
      excerpt2: (style.excerpt2 || '').trim(),
      promptExtra: style.promptExtra || null
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
