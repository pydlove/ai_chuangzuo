import { ref } from 'vue'

// 模块级单例：引导模式与极简模式共享同一份创作配置（同 useStyles 模式）
const MODE_KEY = 'aichuangzuo_create_mode'

export const platforms = [
  { key: 'wechat', name: '公众号', desc: '深度长文，适合专业内容输出', recommendWords: 1500, trait: '长文深度阅读，段落完整，适合观点输出' },
  { key: 'xiaohongshu', name: '小红书', desc: '轻松图文，种草安利效果好', recommendWords: 800, trait: '短段落多 emoji，自动带话题标签' },
  { key: 'toutiao', name: '今日头条', desc: '算法分发，热点资讯类内容', recommendWords: 800, trait: '算法友好，热点资讯风格' },
  { key: 'baijiahao', name: '百家号', desc: '多平台分发，SEO友好', recommendWords: 1500, trait: 'SEO 友好，知识科普调性' },
  { key: 'douyin', name: '抖音图文', desc: '短视频+图文，流量大', recommendWords: 300, trait: '图配文短文案，金句为主' },
  { key: 'zhihu', name: '知乎', desc: '深度问答，专业知识分享', recommendWords: 1500, trait: '专业问答体，逻辑严谨' },
  { key: 'bilibili', name: 'B站', desc: '专栏图文，年轻兴趣社区', recommendWords: 1500, trait: '专栏图文，年轻社区语气' }
]

export const wordCountPresets = {
  platform: {
    wechat: [
      { count: 800, label: '早报 / 简评' },
      { count: 1500, label: '标准深度文' },
      { count: 2500, label: '专题报道' },
      { count: 3000, label: '行业研究（上限）' }
    ],
    xiaohongshu: [
      { count: 300, label: '标题种草' },
      { count: 500, label: '图文分享' },
      { count: 800, label: '详细测评' },
      { count: 1200, label: '步骤拆解教程' }
    ],
    toutiao: [
      { count: 400, label: '热点快讯' },
      { count: 800, label: '事件报道' },
      { count: 1500, label: '专题分析' },
      { count: 2000, label: '观点长文' }
    ],
    baijiahao: [
      { count: 1000, label: '知识科普' },
      { count: 1500, label: '生活攻略' },
      { count: 2000, label: '人文叙事' },
      { count: 2500, label: '行业洞察' }
    ],
    douyin: [
      { count: 150, label: '封面金句' },
      { count: 300, label: '图配文' },
      { count: 600, label: '情感短篇' }
    ],
    bilibili: [
      { count: 800, label: '动态短文' },
      { count: 1500, label: '科普专栏' },
      { count: 2500, label: '深度评测' },
      { count: 3000, label: '连载长文' }
    ],
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

const createMode = ref(localStorage.getItem(MODE_KEY) === 'minimal' ? 'minimal' : 'guided')
const customTitle = ref('')
const customRequirement = ref('')
const currentPlatform = ref(platforms[0])
const currentWordCount = ref({ count: 1500, label: '标准', desc: '深度阅读，8 分钟读完' })
const customWordCount = ref(1500)
const selectedTemplateKey = ref('wechat')
const platformVisible = ref(false)
const wordCountVisible = ref(false)
const styleVisible = ref(false)
const templateVisible = ref(false)

export function useCreateForm() {
  function setCreateMode(mode) {
    createMode.value = mode
    try { localStorage.setItem(MODE_KEY, mode) } catch { /* 隐私模式忽略 */ }
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
