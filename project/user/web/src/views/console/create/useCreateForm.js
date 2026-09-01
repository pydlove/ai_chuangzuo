import { ref, watch } from 'vue'
import { STORAGE_KEYS } from '@/constants/storage.js'
import { platforms } from '@/composables/usePlatforms.js'

// 模块级单例：熟手模式创作配置

// 平台列表已从后端动态加载，见 composables/usePlatforms.js

export const wordCountPresets = {
  platform: {
    general: [
      { count: 500, label: '短文' },
      { count: 1000, label: '中等' },
      { count: 1500, label: '标准' },
      { count: 2500, label: '长文' }
    ]
  },
  scenario: [
    { count: 1200, label: '教程 / 步骤', desc: '操作步骤详细说明，适合图文对照' },
    { count: 1000, label: '测评 / 对比', desc: '优缺点详细对比，附评分' },
    { count: 500, label: '清单 / 种草', desc: '快速清单 + 标签，重点突出' },
    { count: 1800, label: '故事 / 叙事', desc: '沉浸式叙事，节奏完整' }
  ],
  tier: [
    { count: 500, label: '短文', desc: '速读，3 分钟读完' },
    { count: 1000, label: '中等', desc: '快速浏览，5 分钟读完' },
    { count: 1500, label: '标准', desc: '深度阅读，8 分钟读完' },
    { count: 2500, label: '长文', desc: '完整报告，12 分钟读完' }
  ]
}

const createMode = ref('minimal')
const customTitle = ref('')
const customRequirement = ref('')
const currentPlatform = ref({ key: '', name: '选择平台', desc: '', recommendWords: 0, trait: '', wordCountPresets: [] })
const currentWordCount = ref({ count: 1500, label: '标准', desc: '深度阅读，8 分钟读完' })
const customWordCount = ref(1500)
const selectedTemplateKey = ref('wechat')
const platformVisible = ref(false)
const wordCountVisible = ref(false)
const styleVisible = ref(false)
const templateVisible = ref(false)

function setDefaultPlatform() {
  const p = platforms.value.find(x => x.isDefault) || platforms.value[0]
  if (p) currentPlatform.value = p
}

export function loadForm() {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.CREATE_FORM)
    if (!raw) {
      setDefaultPlatform()
      return
    }
    const data = JSON.parse(raw)
    if (data.platformKey) {
      const p = platforms.value.find(x => x.key === data.platformKey)
      if (p) currentPlatform.value = p
    }
    if (!currentPlatform.value?.key) {
      setDefaultPlatform()
    }
    if (data.wordCount && typeof data.wordCount.count === 'number') {
      currentWordCount.value = data.wordCount
    }
    if (data.templateKey) {
      selectedTemplateKey.value = data.templateKey
    }
  } catch {
    setDefaultPlatform()
  }
}

function saveForm() {
  try {
    localStorage.setItem(STORAGE_KEYS.CREATE_FORM, JSON.stringify({
      platformKey: currentPlatform.value?.key,
      wordCount: currentWordCount.value,
      templateKey: selectedTemplateKey.value
    }))
  } catch {
    // 隐私模式忽略
  }
}


export function useCreateForm() {
  watch(currentPlatform, saveForm, { deep: true })
  watch(currentWordCount, saveForm, { deep: true })
  watch(selectedTemplateKey, saveForm)

  function setCreateMode(mode) {
    createMode.value = mode
    try { localStorage.setItem(STORAGE_KEYS.CREATE_MODE, mode) } catch { /* 隐私模式忽略 */ }
  }
  function clearForm() {
    customTitle.value = ''
    customRequirement.value = ''
  }
  return {
    createMode, setCreateMode,
    customTitle, customRequirement,
    currentPlatform, currentWordCount, customWordCount, selectedTemplateKey,
    platformVisible, wordCountVisible, styleVisible, templateVisible,
    clearForm
  }
}
