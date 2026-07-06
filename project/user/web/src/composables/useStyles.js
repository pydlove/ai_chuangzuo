import { ref } from 'vue'
import { getMyStyles, createStyle, updateStyle, deleteStyle } from '@/api/style'
import { message } from 'ant-design-vue'

export const systemStyles = [
  {
    name: '年度总结',
    desc: '回顾、复盘、展望',
    promptSummary: '语气：回顾性、感恩 + 数据自省\n结构：成绩 + 反思 + 明年目标\n长度：1500-2500 字，带小标题分章',
    prompt: '你是一位擅长年度复盘与展望的写手。文章语气应回顾性、感恩且带数据自省。结构分为：成绩回顾、深度反思、明年目标。长度 1500-2500 字，使用小标题分章。'
  },
  {
    name: '产品评测',
    desc: '客观、数据驱动、多角度对比',
    promptSummary: '语气：客观中立、有理有据\n结构：外观 + 性能 + 体验 + 总结\n要素：必带参数对比表 + 优缺点',
    prompt: '你是一位客观中立的产品评测作者。文章需数据驱动、多角度对比，结构分为外观、性能、体验、总结，必须包含参数对比表和优缺点分析。'
  },
  {
    name: '情感散文',
    desc: '细腻、共情、个人化表达',
    promptSummary: '语气：细腻、温暖、第一人称\n修辞：善用比喻、意象、留白\n结构：场景 + 情绪 + 升华',
    prompt: '你擅长写情感散文。使用细腻温暖的第一人称，善用比喻、意象和留白。结构为：场景描写、情绪铺陈、主题升华。'
  },
  {
    name: '职场干货',
    desc: '实操性强、结构清晰',
    promptSummary: '语气：专业务实、老板视角\n结构：痛点 + 方案 + 步骤 + 案例\n要素：可执行的 checklist',
    prompt: '你是一位专业务实的职场作者。从老板视角出发，结构为痛点、方案、步骤、案例，必须提供可执行的 checklist。'
  },
  {
    name: '热点评论',
    desc: '观点鲜明、论据紧凑',
    promptSummary: '语气：犀利、有态度\n结构：事件概述 + 观点 + 论据 + 结论\n要素：引用数据或权威观点',
    prompt: '你是一位观点鲜明的热点评论员。语气犀利有态度，结构为事件概述、核心观点、论据支撑、结论，需引用数据或权威观点。'
  },
  {
    name: '知识科普',
    desc: '深入浅出、逻辑清晰',
    promptSummary: '语气：亲和、易懂\n结构：问题 + 原理 + 案例 + 总结\n要素：避免术语堆砌，善用类比',
    prompt: '你是一位知识科普作者。语气亲和易懂，结构为提出问题、解释原理、给出案例、总结要点。避免术语堆砌，善用类比。'
  },
  {
    name: '营销转化',
    desc: '引导行动、强说服',
    promptSummary: '语气：紧迫感 + 利益点突出\n结构：痛点共鸣 + 方案 + 案例 + CTA\n要素：必带限时/优惠/倒计时',
    prompt: '你是一位营销转化写手。语气紧迫、利益点突出，结构为痛点共鸣、解决方案、案例证明、行动号召（CTA），必须包含限时/优惠/倒计时要素。'
  },
  {
    name: '故事叙事',
    desc: '沉浸感、有冲突与转折',
    promptSummary: '语气：克制、文学化\n结构：起承转合 + 人物对话\n要素：场景细节 + 心理活动',
    prompt: '你是一位故事叙事作者。语气克制文学化，结构为起承转合，包含人物对话，注重场景细节和心理活动描写。'
  }
]

export const myStyles = ref([])
export const currentStyle = ref(systemStyles[0])

/** 从异常负载里取出可读 message；兼容多种错误结构。 */
function errMsg(e) {
  if (!e) return '请求失败'
  if (typeof e === 'string') return e
  return e.message || e.msg || '请求失败'
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
      currentStyle.value = systemStyles[0]
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
  const inSystem = systemStyles.some(s => s.name.trim().toLowerCase() === target)
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
