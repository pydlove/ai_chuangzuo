<template>
  <div class="onboarding-index">
    <div class="onboarding-header">
      <h2 class="onboarding-title">定制你的自媒体方案</h2>
      <p class="onboarding-subtitle">别再瞎折腾了！只要设置一次，让你拥有一个24小时在线的自媒体运营总监，你不再关心明天要发什么，怎么写，几点发，发到哪，怎么蹭热点……我们都会给你安排好。</p>
    </div>

    <!-- 手机端宣传头图 -->
    <div class="onboarding-hero-mobile">
      <div class="onboarding-hero-mobile__text">
        <img
          class="onboarding-hero-mobile__logo"
          :src="'/assets/images/运营方案logo-v1.png'"
          alt="定制你的自媒体方案"
        />
        <p class="onboarding-hero-mobile__desc">别再瞎折腾了！只要设置一次，让你拥有一个24小时在线的自媒体运营总监，你不再关心明天要发什么，怎么写，几点发，发到哪，怎么蹭热点……我们都会给你安排好。</p>
      </div>
    </div>

    <div class="onboarding-body">
      <div class="onboarding-steps">
      <div class="step-bar">
        <div
          v-for="(s, idx) in steps"
          :key="idx"
          :class="['step-item', { active: step >= idx + 1, done: step > idx + 1 }]"
        >
          <div class="step-circle">{{ idx + 1 }}</div>
          <div class="step-label">{{ s }}</div>
        </div>
      </div>
    </div>

    <div class="onboarding-card" :class="{ 'onboarding-card--platform': step === 1, 'onboarding-card--niche': step === 3, 'onboarding-card--persona': step === 4, 'onboarding-card--summary': step === 5 }">
      <!-- Step 1: 选平台 -->
      <div v-if="step === 1" class="step-panel step-panel--platform">
        <h3 class="step-title">你想在哪个平台做自媒体？</h3>
        <p class="step-desc">每个平台的内容形式、用户群体、变现方式和收入空间都不同，了解清楚再选。</p>
        <!-- 桌面端：网格卡片 -->
        <div class="platform-grid platform-grid--desktop">
          <div
            v-for="p in platforms"
            :key="p.key"
            :class="['platform-card', { selected: selectedPlatform === p.key }]"
            @click="selectPlatform(p.key)"
          >
            <div class="platform-card-header">
              <div class="platform-icon">
                <img :src="p.iconImg" :alt="p.name" />
              </div>
              <div class="platform-title">
                <div class="platform-name">{{ p.name }}</div>
                <a-tag :class="difficultyClass(p.difficulty)">{{ p.difficulty }}难度</a-tag>
              </div>
            </div>
            <div class="platform-tagline">{{ p.tagline }}</div>
            <div class="platform-meta">
              <div class="meta-row">
                <span class="meta-label">内容形式</span>
                <span class="meta-tags">
                  <a-tag v-for="cf in p.contentForm" :key="cf" size="small">{{ cf }}</a-tag>
                </span>
              </div>
              <div class="meta-row">
                <span class="meta-label">主要收益</span>
                <span class="meta-tags">
                  <a-tag v-for="m in p.monetization" :key="m" size="small">{{ m }}</a-tag>
                </span>
              </div>
              <div class="meta-row">
                <span class="meta-label">变现门槛</span>
                <span class="meta-value">{{ p.threshold }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">适合谁</span>
                <span class="meta-value">{{ p.bestFor }}</span>
              </div>
            </div>
            <div class="platform-reason">
              <BulbOutlined /> {{ p.reason }}
            </div>
            <div class="platform-earn">
              <div class="earn-title">变现评估</div>
              <div class="earn-metrics">
                <div class="earn-metric">
                  <div class="earn-metric-label">变现难度</div>
                  <div class="earn-metric-value">
                    <a-tag :class="earnClass(p.monetizationEase)">{{ p.monetizationEase }}</a-tag>
                  </div>
                </div>
                <div class="earn-metric">
                  <div class="earn-metric-label">预计周期</div>
                  <div class="earn-metric-value">{{ p.timeToIncome }}</div>
                </div>
                <div class="earn-metric">
                  <div class="earn-metric-label">收入空间</div>
                  <div class="earn-metric-value">{{ p.incomeRange }}</div>
                </div>
              </div>
            </div>
            <div v-if="selectedPlatform === p.key" class="selected-check">
              <CheckOutlined />
            </div>
          </div>
        </div>

        <!-- 手机端：京东风格横向滑动卡片 -->
        <div class="platform-swiper platform-swiper--mobile">
          <div ref="trackRef" class="platform-swiper-track">
            <div
              v-for="p in platforms"
              :key="p.key"
              :class="['platform-swiper-card', { selected: selectedPlatform === p.key }]"
              @click="selectPlatform(p.key)"
            >
              <div class="platform-swiper-card__header">
                <div class="platform-icon">
                  <img :src="p.iconImg" :alt="p.name" />
                </div>
                <div class="platform-title">
                  <div class="platform-name">{{ p.name }}</div>
                  <a-tag :class="difficultyClass(p.difficulty)">{{ p.difficulty }}难度</a-tag>
                </div>
              </div>
              <div class="platform-swiper-card__tagline">{{ p.tagline }}</div>
              <div class="platform-swiper-card__info">
                <div class="info-row">
                  <span class="info-label">内容形式</span>
                  <span class="info-value">{{ p.contentForm.join('、') }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">主要收益</span>
                  <span class="info-value">{{ p.monetization.join('、') }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">变现门槛</span>
                  <span class="info-value">{{ p.threshold }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">适合谁</span>
                  <span class="info-value">{{ p.bestFor }}</span>
                </div>
              </div>
              <div class="platform-swiper-card__metrics">
                <div class="platform-metric">
                  <div class="platform-metric__label">变现难度</div>
                  <div class="platform-metric__value" :class="earnClass(p.monetizationEase)">{{ p.monetizationEase }}</div>
                </div>
                <div class="platform-metric">
                  <div class="platform-metric__label">预计周期</div>
                  <div class="platform-metric__value">{{ p.timeToIncome }}</div>
                </div>
                <div class="platform-metric">
                  <div class="platform-metric__label">收入空间</div>
                  <div class="platform-metric__value">{{ p.incomeRange }}</div>
                </div>
              </div>
              <div v-if="selectedPlatform === p.key" class="selected-check">
                <CheckOutlined />
              </div>
            </div>
          </div>
          <div class="platform-swiper-dots">
            <span
              v-for="p in platforms"
              :key="p.key"
              :class="['swiper-dot', { active: visiblePlatformKey === p.key }]"
            />
          </div>
        </div>

        <!-- 手机端选中平台提示 -->
        <div class="platform-reason-mobile">
          <BulbOutlined />
          <span>{{ selectedPlatformDetail?.reason || '左右滑动查看各平台特点，点击卡片选择你想做的平台' }}</span>
        </div>
      </div>

      <!-- Step 2: 回答问题 -->
      <div v-if="step === 2" class="step-panel">
        <h3 class="step-title">回答几个关于你的问题</h3>
        <p class="step-desc">请根据你的实际情况回答以下问题，AI 将结合平台特征为你推荐更合适的赛道方向。</p>
        <div v-if="isLoadingQuestions" class="wizard-loading">
          <a-spin size="large" />
          <div class="wizard-loading-text">
            <div class="wizard-loading-title">正在为您定制方案</div>
            <div class="wizard-loading-subtitle">先准备一些问题需要您回答</div>
          </div>
        </div>
        <div v-else>
          <div v-for="q in questions" :key="q.key" class="form-block">
            <div class="form-label">
              {{ q.text }}
              <span v-if="q.isRequired" class="required-mark">*</span>
            </div>
            <div class="option-group">
              <button
                v-for="opt in q.options"
                :key="opt.key"
                :class="['option-btn', { selected: answers[q.key] === opt.key }]"
                @click="answers[q.key] = opt.key"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 3: 选赛道 -->
      <div v-if="step === 3" class="step-panel">
        <h3 class="step-title">推荐你尝试这些细分赛道</h3>
        <p class="step-desc">基于你选择的平台和回答，我们找出了需求真实、竞争度尚可的赛道。</p>
        <div v-if="isLoadingNiches" class="wizard-loading">
          <a-spin size="large" />
          <div class="wizard-loading-text">
            <div class="wizard-loading-title">正在为您定制方案</div>
            <div class="wizard-loading-subtitle">根据您的回答推荐适合的赛道</div>
          </div>
        </div>
        <div v-else class="niche-list">
          <div
            v-for="n in nicheOptions"
            :key="n.key"
            :class="['niche-card', { selected: selectedNiche === n.key }]"
            @click="selectedNiche = n.key"
          >
            <div class="niche-header">
              <div class="niche-name">{{ n.name }}</div>
              <a-tag :color="n.riskColor">{{ n.riskLabel }}</a-tag>
            </div>
            <div class="niche-meta">
              <span>目标人群：{{ n.audience }}</span>
              <span>变现：{{ n.monetization }}</span>
            </div>
            <div class="niche-evidence">
              <FireOutlined /> 近7天低粉高赞案例 {{ n.caseCount }} 篇
            </div>
            <div class="niche-reason">{{ n.reason }}</div>
            <div v-if="selectedNiche === n.key" class="selected-check">
              <CheckOutlined />
            </div>
          </div>
        </div>
      </div>

      <!-- Step 4: 选人设 -->
      <div v-if="step === 4" class="step-panel">
        <h3 class="step-title">选择你的人设和内容支柱</h3>
        <p class="step-desc">人设决定用户怎么记住你，内容支柱保证你持续有得写。</p>
        <div v-if="isLoadingPersonas" class="wizard-loading">
          <a-spin size="large" />
          <div class="wizard-loading-text">
            <div class="wizard-loading-title">正在为您定制方案</div>
            <div class="wizard-loading-subtitle">根据赛道推荐适合的人设与内容支柱</div>
          </div>
        </div>
        <template v-else>
          <div class="form-block">
            <div class="form-label">你想以什么身份出现？</div>
          <div class="persona-grid">
            <div
              v-for="p in personaOptions"
              :key="p.key"
              :class="['persona-card', { selected: selectedPersona === p.key }]"
              @click="selectedPersona = p.key"
            >
              <div class="persona-name">{{ p.name }}</div>
              <div class="persona-desc">{{ p.desc }}</div>
              <div v-if="selectedPersona === p.key" class="selected-check">
                <CheckOutlined />
              </div>
            </div>
          </div>
        </div>
        <div class="form-block">
          <div class="form-label">内容支柱比例（默认推荐）</div>
          <div class="pillars-input">
            <div v-for="pillar in pillars" :key="pillar.name" class="pillar-card">
              <div class="pillar-card-header">
                <span class="pillar-name">{{ pillar.name }}</span>
                <span class="pillar-percent">{{ pillar.percent }}%</span>
              </div>
              <a-slider v-model:value="pillar.percent" :min="0" :max="100" />
            </div>
          </div>
          <div v-if="pillarTotal !== 100" class="pillar-warning">三项比例之和需等于 100%</div>
        </div>
        </template>
      </div>

      <!-- Step 5: 方案汇总 -->
      <div v-if="step === 5" class="step-panel">
        <h3 class="step-title">你的自媒体运营方案</h3>
        <p class="step-desc">确认后就可以进入工作台，按推荐选题开始创作。</p>
        <div class="summary-card">
          <div class="summary-row">
            <span class="summary-label">主攻平台</span>
            <span class="summary-value">{{ platformLabels }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">细分赛道</span>
            <span class="summary-value">{{ selectedNicheName }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">人设定位</span>
            <span class="summary-value">{{ selectedPersonaName }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">内容支柱</span>
            <div class="summary-pillars">
              <a-tag v-for="p in pillars" :key="p.name">{{ p.name }} {{ p.percent }}%</a-tag>
            </div>
          </div>
          <div class="summary-row">
            <span class="summary-label">平台问答</span>
            <div class="summary-answers">
              <div v-for="a in answerSummary" :key="a.key" class="summary-answer">
                <span class="answer-question">{{ a.text }}</span>
                <span class="answer-value">{{ a.answerLabel }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="summary-tip">
          <BulbOutlined /> 系统会基于这个方案，每周给你推荐选题和角度，降低同质化风险。
        </div>
      </div>
    </div>

    <div class="onboarding-actions">
      <a-button v-if="step > 1 && step !== 2" size="large" @click="prev">上一步</a-button>
      <a-button v-if="step < 5" type="primary" size="large" :disabled="!canNext" :loading="isLoadingNext" @click="next">下一步</a-button>
      <a-button v-if="step === 5" type="primary" size="large" @click="confirm">确认方案，进入工作台</a-button>
    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  CheckOutlined,
  BulbOutlined,
  FireOutlined
} from '@ant-design/icons-vue'
import { platforms as backendPlatforms, loadPlatforms } from '@/composables/usePlatforms.js'
import {
  fetchCurrentPlan,
  fetchPlatformQuestions,
  recommendNiches,
  recommendPersonas,
  savePlan as apiSavePlan
} from '@/api/selfMediaPlan.js'
import { getMyProfile } from '@/api/user.js'

const router = useRouter()
const route = useRoute()
const step = ref(1)
const steps = ['选平台', '答问题', '选赛道', '做人设', '出方案']
let DRAFT_KEY = 'aichuangzuo_onboarding_draft'
let DONE_KEY = 'aichuangzuo_onboarding_done'
const isRestoringDraft = ref(false)
const trackRef = ref(null)
const visiblePlatformKey = ref('')

function platformIconUrl(key) {
  return `/platforms/${key}.png`
}

const PLATFORM_DETAILS = {
  xiaohongshu: {
    name: '小红书',
    tagline: '图文种草社区，女性用户多，适合分享生活经验',
    contentForm: ['图文笔记', '短视频'],
    monetization: ['品牌广告', '带货分佣', '私域引流'],
    monetizationEase: '中等',
    timeToIncome: '2-4个月',
    incomeRange: '几千~几万/月',
    threshold: '0粉可带货，1000粉可接蒲公英商单',
    difficulty: '中',
    bestFor: '有生活经验、愿意分享好物/干货的人',
    reason: '种草转化率高，起号相对快，但对封面和标题要求高'
  },
  wechat: {
    name: '微信公众号',
    tagline: '深度长文平台，粉丝价值高，适合做私域沉淀',
    contentForm: ['长文章'],
    monetization: ['流量主广告', '赞赏', '付费阅读', '私域转化'],
    monetizationEase: '较慢',
    timeToIncome: '6个月以上',
    incomeRange: '几千~几万/月',
    threshold: '100粉丝可开通流量主',
    difficulty: '高',
    bestFor: '有专业积累、能持续输出深度内容的人',
    reason: '粉丝粘性最强，变现路径稳定，但需要长期坚持'
  },
  toutiao: {
    name: '今日头条',
    tagline: '算法推荐资讯平台，流量大，变现门槛低',
    contentForm: ['文章', '微头条', '视频'],
    monetization: ['流量分成', '青云计划', '带货'],
    monetizationEase: '容易',
    timeToIncome: '1-2个月',
    incomeRange: '几百~几千/月',
    threshold: '0粉即可参与流量分成',
    difficulty: '低',
    bestFor: '想快速获得流量、擅长追热点的人',
    reason: '发文即有推荐流量，新手容易看到正反馈'
  },
  baijiahao: {
    name: '百家号',
    tagline: '百度生态内容平台，搜索流量长尾稳定',
    contentForm: ['文章', '短视频', '动态'],
    monetization: ['广告分成', '带货', '付费专栏'],
    monetizationEase: '容易',
    timeToIncome: '2-3个月',
    incomeRange: '几百~几千/月',
    threshold: '转正后（持续发文）可开广告分成',
    difficulty: '低',
    bestFor: '擅长图文、希望内容长期被搜索到的人',
    reason: '百度搜索能带来长期被动流量，适合干货类内容'
  },
  zhihu: {
    name: '知乎',
    tagline: '专业问答社区，长尾流量强，适合建立专业信任',
    contentForm: ['问答', '文章', '视频'],
    monetization: ['好物推荐', '付费咨询', '品牌任务', '盐选专栏'],
    monetizationEase: '中等',
    timeToIncome: '3-6个月',
    incomeRange: '几千~几万/月',
    threshold: '创作者等级4级可开好物推荐',
    difficulty: '中',
    bestFor: '有专业知识、能解答具体问题的人',
    reason: '一个问题可能持续带来流量，适合专业型 IP'
  },
  douyin: {
    name: '抖音',
    tagline: '短视频头部平台，流量天花板高，适合快速起量',
    contentForm: ['短视频', '直播'],
    monetization: ['广告分成', '带货', '直播打赏', '本地生活'],
    monetizationEase: '容易',
    timeToIncome: '1-3个月',
    incomeRange: '几千~几十万/月',
    threshold: '0粉可开橱窗，1000粉可直播带货',
    difficulty: '高',
    bestFor: '愿意出镜、能做短视频的人',
    reason: '流量最大，但竞争激烈，对视频生产能力要求高'
  },
  bilibili: {
    name: 'B站',
    tagline: '年轻兴趣社区，专栏+视频结合，适合圈层内容',
    contentForm: ['专栏图文', '视频'],
    monetization: ['充电计划', '广告分成', '带货', '商单'],
    monetizationEase: '中等',
    timeToIncome: '3-6个月',
    incomeRange: '几百~几万/月',
    threshold: '创作激励需电磁力等级',
    difficulty: '中',
    bestFor: '熟悉年轻文化、能持续产出内容的人',
    reason: '社区氛围好，适合兴趣圈层和系列化内容'
  }
}

function getPlatformDetail(key) {
  return PLATFORM_DETAILS[key] || {
    name: key,
    tagline: '',
    contentForm: ['图文', '视频'],
    monetization: ['广告分成'],
    monetizationEase: '中等',
    timeToIncome: '待定',
    incomeRange: '待定',
    threshold: '无门槛',
    difficulty: '中',
    bestFor: '内容创作者',
    reason: '该平台暂未配置详情'
  }
}

const platforms = computed(() => {
  const list = backendPlatforms.value.length ? backendPlatforms.value : Object.keys(PLATFORM_DETAILS).map(k => ({ key: k }))
  return list.map(p => {
    const detail = getPlatformDetail(p.key)
    return {
      ...detail,
      ...p,
      key: p.key,
      name: p.name || detail.name,
      iconImg: p.iconUrl || platformIconUrl(p.key) || '',
      tagline: p.tagline || detail.tagline,
      contentForm: p.contentForm?.length ? p.contentForm : detail.contentForm,
      monetization: p.monetization?.length ? p.monetization : detail.monetization,
      threshold: p.threshold || detail.threshold,
      bestFor: p.bestFor || detail.bestFor,
      reason: p.reason || detail.reason,
      monetizationEase: p.monetizationEase || detail.monetizationEase,
      timeToIncome: p.timeToIncome || detail.timeToIncome,
      incomeRange: p.incomeRange || detail.incomeRange,
      difficulty: p.difficulty || detail.difficulty
    }
  })
})

onMounted(async () => {
  await initUserKeys()
  const hasDraft = loadDraft()
  if (route.query.reset === '1') {
    if (!hasDraft) {
      clearDraft()
      step.value = 1
    }
    await loadPlatforms()
    if (step.value >= 2 && selectedPlatform.value) await loadQuestions()
    if (step.value >= 3) await loadNicheOptions()
    if (step.value >= 4) await loadPersonaOptions()
    router.replace({ query: {} })
    await nextTick(initTrackScroll)
    return
  }
  if (hasDraft) {
    await loadPlatforms()
    if (step.value >= 2 && selectedPlatform.value) await loadQuestions()
    if (step.value >= 3) await loadNicheOptions()
    if (step.value >= 4) await loadPersonaOptions()
    await nextTick(initTrackScroll)
    return
  }
  await checkExistingPlan()
  if (step.value !== 5) {
    await loadPlatforms()
  }
  await nextTick(initTrackScroll)
})
async function checkExistingPlan() {
  try {
    const res = await fetchCurrentPlan()
    const data = res?.data ?? null
    if (!data || !data.platformKey) return

    localStorage.setItem(DONE_KEY, '1')
    selectedPlatform.value = data.platformKey
    await loadQuestions()

    if (Array.isArray(data.answers)) {
      data.answers.forEach((a) => {
        answers[a.questionKey] = a.answer
      })
    }

    nicheOptions.value = data.nicheKey
      ? [{ key: data.nicheKey, name: data.nicheName || data.nicheKey }]
      : []
    selectedNiche.value = data.nicheKey || ''

    personaOptions.value = data.personaKey
      ? [{ key: data.personaKey, name: data.personaName || data.personaKey, desc: '' }]
      : []
    selectedPersona.value = data.personaKey || ''

    if (Array.isArray(data.pillars) && data.pillars.length) {
      pillars.splice(0, pillars.length, ...data.pillars.map((p) => ({ name: p.name, percent: p.percent })))
    }

    step.value = 5
  } catch (e) {
    console.warn('加载已有方案失败', e)
  }
}

function difficultyClass(difficulty) {
  if (difficulty === '低') return 'tag-easy'
  if (difficulty === '中') return 'tag-medium'
  return 'tag-hard'
}

function earnClass(ease) {
  if (ease === '容易') return 'tag-easy'
  if (ease === '中等') return 'tag-medium'
  return 'tag-hard'
}

const selectedPlatform = ref('')
const questions = ref([])
const answers = reactive({})
const nicheOptions = ref([])
const selectedNiche = ref('')
const personaOptions = ref([])
const selectedPersona = ref('')
const pillars = reactive([])

const isLoadingQuestions = ref(false)
const isLoadingNiches = ref(false)
const isLoadingPersonas = ref(false)
const isLoadingNext = ref(false)

const selectedNicheName = computed(() => nicheOptions.value.find((n) => n.key === selectedNiche.value)?.name || '')
const selectedPersonaName = computed(() => personaOptions.value.find((p) => p.key === selectedPersona.value)?.name || '')
const platformLabels = computed(() => platforms.value.find((p) => p.key === selectedPlatform.value)?.name || '')
const selectedPlatformDetail = computed(() => platforms.value.find((p) => p.key === selectedPlatform.value))
const pillarTotal = computed(() => pillars.reduce((sum, p) => sum + p.percent, 0))

const answerList = computed(() => {
  return questions.value.map(q => ({
    questionKey: q.key,
    answer: answers[q.key] || ''
  })).filter(a => a.answer)
})

const answerSummary = computed(() => {
  return questions.value.map(q => {
    const opt = q.options.find(o => o.key === answers[q.key])
    return {
      key: q.key,
      text: q.text,
      answerLabel: opt?.label || answers[q.key] || '未回答'
    }
  })
})

function selectPlatform(key) {
  selectedPlatform.value = selectedPlatform.value === key ? '' : key
  scrollTrackToPlatform(key)
}

function scrollTrackToPlatform(key) {
  const track = trackRef.value
  if (!track) return
  const idx = platforms.value.findIndex(p => p.key === key)
  if (idx < 0) return
  const cardWidth = track.clientWidth - 32
  const gap = 12
  track.scrollTo({ left: idx * (cardWidth + gap), behavior: 'smooth' })
}

function updateVisibleFromScroll() {
  const track = trackRef.value
  if (!track) return
  const cardWidth = track.clientWidth - 32
  const gap = 12
  const index = Math.max(0, Math.min(platforms.value.length - 1, Math.round(track.scrollLeft / (cardWidth + gap))))
  const p = platforms.value[index]
  if (p) visiblePlatformKey.value = p.key
}

function initTrackScroll() {
  const track = trackRef.value
  if (!track) return
  if (selectedPlatform.value) {
    const idx = platforms.value.findIndex(p => p.key === selectedPlatform.value)
    if (idx >= 0) {
      const cardWidth = track.clientWidth - 32
      const gap = 12
      track.scrollTo({ left: idx * (cardWidth + gap), behavior: 'instant' })
    }
  }
  updateVisibleFromScroll()
  track.addEventListener('scroll', updateVisibleFromScroll, { passive: true })
}

onBeforeUnmount(() => {
  trackRef.value?.removeEventListener('scroll', updateVisibleFromScroll)
})

function saveDraft() {
  const draft = {
    step: step.value,
    selectedPlatform: selectedPlatform.value,
    answers: { ...answers },
    selectedNiche: selectedNiche.value,
    selectedPersona: selectedPersona.value,
    pillars: pillars.map((p) => ({ name: p.name, percent: p.percent }))
  }
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}

function loadDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return false
    const draft = JSON.parse(raw)
    if (!draft || typeof draft.step !== 'number') return false

    isRestoringDraft.value = true
    step.value = draft.step
    selectedPlatform.value = draft.selectedPlatform || ''
    Object.keys(answers).forEach((k) => delete answers[k])
    Object.assign(answers, draft.answers || {})
    selectedNiche.value = draft.selectedNiche || ''
    selectedPersona.value = draft.selectedPersona || ''
    pillars.splice(
      0,
      pillars.length,
      ...(draft.pillars || []).map((p) => ({ name: p.name, percent: p.percent }))
    )
    return true
  } catch (e) {
    console.warn('加载方案草稿失败', e)
    return false
  } finally {
    isRestoringDraft.value = false
  }
}

function clearDraft() {
  localStorage.removeItem(DRAFT_KEY)
}

async function initUserKeys() {
  try {
    const res = await getMyProfile()
    const userId = res?.data?.userId || res?.userId
    if (userId) {
      DRAFT_KEY = `aichuangzuo_onboarding_draft:${userId}`
      DONE_KEY = `aichuangzuo_onboarding_done:${userId}`
    }
  } catch (e) {
    // 未登录或获取失败时继续使用通用 key
  }
}

watch(step, saveDraft)
watch(selectedPlatform, saveDraft)
watch(answers, saveDraft, { deep: true })
watch(selectedNiche, saveDraft)
watch(selectedPersona, saveDraft)
watch(pillars, saveDraft, { deep: true })

function resetAfterPlatformChange() {
  questions.value = []
  Object.keys(answers).forEach(k => delete answers[k])
  nicheOptions.value = []
  selectedNiche.value = ''
  personaOptions.value = []
  selectedPersona.value = ''
  pillars.splice(0, pillars.length)
}

async function loadQuestions() {
  if (!selectedPlatform.value) return
  isLoadingQuestions.value = true
  try {
    const res = await fetchPlatformQuestions(selectedPlatform.value)
    const data = res?.data ?? null
    questions.value = Array.isArray(data) ? data : []
    if (!isRestoringDraft.value) {
      Object.keys(answers).forEach((k) => delete answers[k])
    }
  } catch (e) {
    message.error('问题生成失败，请重试')
  } finally {
    isLoadingQuestions.value = false
  }
}

async function loadNicheOptions() {
  if (!selectedPlatform.value || answerList.value.length === 0) return
  isLoadingNiches.value = true
  try {
    const res = await recommendNiches({
      platformKey: selectedPlatform.value,
      answers: answerList.value
    })
    const data = res?.data ?? null
    nicheOptions.value = Array.isArray(data) ? data : []
    if (!isRestoringDraft.value) {
      selectedNiche.value = nicheOptions.value[0]?.key || ''
    }
  } catch (e) {
    message.error('赛道推荐失败，请重试')
  } finally {
    isLoadingNiches.value = false
  }
}

async function loadPersonaOptions() {
  if (!selectedPlatform.value || !selectedNiche.value) return
  isLoadingPersonas.value = true
  try {
    const res = await recommendPersonas({
      platformKey: selectedPlatform.value,
      nicheKey: selectedNiche.value,
      answers: answerList.value
    })
    const result = res?.data ?? {}
    personaOptions.value = Array.isArray(result.personas) ? result.personas : []
    if (!isRestoringDraft.value) {
      if (Array.isArray(result.defaultPillars) && result.defaultPillars.length) {
        pillars.splice(0, pillars.length, ...result.defaultPillars.map((p) => ({ name: p.name, percent: p.percent })))
      } else {
        pillars.splice(0, pillars.length,
          { name: '干货复盘', percent: 60 },
          { name: '个人故事', percent: 20 },
          { name: '热点解读', percent: 20 }
        )
      }
      selectedPersona.value = personaOptions.value[0]?.key || ''
    }
  } catch (e) {
    message.error('人设推荐失败，请重试')
  } finally {
    isLoadingPersonas.value = false
  }
}

const canNext = computed(() => {
  if (isLoadingNext.value) return false
  if (step.value === 1) return !!selectedPlatform.value
  if (step.value === 2) {
    return questions.value.every(q => !q.isRequired || !!answers[q.key])
  }
  if (step.value === 3) return !!selectedNiche.value
  if (step.value === 4) return !!selectedPersona.value && pillarTotal.value === 100
  return true
})

async function next() {
  if (!canNext.value) {
    message.warning('请先完成当前步骤的选择')
    return
  }
  if (step.value === 1) {
    Modal.confirm({
      title: '确认平台',
      content: '您是否确认选择此平台？确认后将根据您的情况和平台特性为您定制运营方案',
      okText: '确认',
      cancelText: '取消',
      centered: true,
      okButtonProps: { danger: true },
      onOk: () => proceedNext()
    })
    return
  }
  await proceedNext()
}

async function proceedNext() {
  isLoadingNext.value = true
  try {
    if (step.value === 1) {
      resetAfterPlatformChange()
      step.value = 2
      await loadQuestions()
    } else if (step.value === 2) {
      step.value = 3
      await loadNicheOptions()
    } else if (step.value === 3) {
      step.value = 4
      await loadPersonaOptions()
    } else if (step.value === 4) {
      step.value = 5
    }
  } finally {
    isLoadingNext.value = false
  }
}

function prev() {
  step.value--
}

async function confirm() {
  const platform = platforms.value.find((p) => p.key === selectedPlatform.value)
  const niche = nicheOptions.value.find((n) => n.key === selectedNiche.value)
  const persona = personaOptions.value.find((p) => p.key === selectedPersona.value)
  try {
    await apiSavePlan({
      platformKey: selectedPlatform.value,
      platformName: platform?.name || '',
      nicheKey: niche?.key || '',
      nicheName: niche?.name || '',
      personaKey: persona?.key || '',
      personaName: persona?.name || '',
      pillars: pillars.map((p) => ({ name: p.name, percent: p.percent })),
      answers: answerList.value
    })
    message.success('自媒体方案已生成')
    localStorage.setItem(DONE_KEY, '1')
    clearDraft()
    router.push('/console/workbench')
  } catch (e) {
    message.error('保存方案失败，请重试')
  }
}
</script>

<style scoped>
.onboarding-index {
  padding: 32px 24px;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  min-height: 100%;
  color: #1a1a1a;
}
.onboarding-header {
  text-align: center;
  margin-bottom: 32px;
}
.onboarding-title {
  color: #1a1a1a;
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
}
.onboarding-subtitle {
  color: #8c8c8c;
  font-size: 15px;
  margin: 0;
}

/* 手机端宣传头图（默认隐藏） */
.onboarding-hero-mobile {
  display: none;
}
.onboarding-steps {
  margin-bottom: 24px;
}
.step-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
}
.step-bar::before {
  content: '';
  position: absolute;
  top: 14px;
  left: 10%;
  right: 10%;
  height: 2px;
  background: #f0f0f0;
  z-index: 0;
}
.step-item {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.step-circle {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s;
  color: #1a1a1a;
}
.step-item.active .step-circle {
  background: var(--color-primary, #FF2442);
  color: #fff;
}
.step-item.done .step-circle {
  background: var(--color-primary, #07c160);
  color: #fff;
}
.step-label {
  font-size: 13px;
  color: #8c8c8c;
}
.step-item.active .step-label,
.step-item.done .step-label {
  font-weight: 500;
  color: var(--color-primary, #FF2442);
}
.onboarding-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}
.step-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #1a1a1a;
}
.step-desc {
  font-size: 14px;
  margin: 0 0 24px;
  color: #595959;
}
.platform-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 16px;
}

/* 手机端平台选择（京东风格）默认隐藏 */
.platform-swiper--mobile,
.platform-reason-mobile {
  display: none;
}

.platform-card {
  position: relative;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.platform-card:hover {
  border-color: var(--color-primary, #FF2442);
}
.platform-card.selected {
  border-color: var(--color-primary, #FF2442);
  background: rgba(255, 36, 66, 0.04);
}
.platform-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.platform-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}
.platform-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.platform-title {
  flex: 1;
  min-width: 0;
}
.platform-name {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
  color: #1a1a1a;
}

.tag-easy,
.tag-medium,
.tag-hard {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}
.tag-easy {
  background: #D9F7BE;
  border-color: #B7EB8F;
  color: #237804;
}
.tag-medium {
  background: #FA8C16;
  border-color: #FA8C16;
}
.tag-hard {
  background: #FF6B6B;
  border-color: #FF6B6B;
}
.platform-tagline {
  color: #595959;
  font-size: 13px;
  line-height: 1.6;
}
.platform-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
}
.meta-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
}
.meta-label {
  width: 56px;
  flex-shrink: 0;
  color: #8c8c8c;
}
.meta-tags {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.meta-value {
  flex: 1;
  line-height: 1.5;
  color: #1a1a1a;
}
.platform-reason {
  font-size: 12px;
  line-height: 1.5;
  padding: 10px 12px;
  background: rgba(255, 36, 66, 0.06);
  border: 1px solid rgba(255, 36, 66, 0.12);
  border-radius: 8px;
  margin-top: auto;
  color: #595959;
}

.platform-earn {
  background: rgba(255, 36, 66, 0.06);
  border: 1px solid rgba(255, 36, 66, 0.12);
  border-radius: 8px;
  padding: 12px;
}

.earn-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #1a1a1a;
}

.earn-metrics {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.earn-metric {
  flex: 1;
  text-align: center;
}

.earn-metric-label {
  font-size: 12px;
  margin-bottom: 4px;
  color: #8c8c8c;
}

.earn-metric-value {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.form-block {
  margin-bottom: 24px;
}
.form-label {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1a1a1a;
}
.required-mark {
  color: var(--color-primary, #FF2442);
  margin-left: 4px;
}
.option-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.option-btn {
  padding: 10px 18px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  color: #1a1a1a;
}
.option-btn:hover {
  border-color: var(--color-primary, #FF2442);
}
.option-btn.selected {
  border-color: var(--color-primary, #FF2442);
  background: var(--color-primary, #FF2442);
  color: #fff;
}
.question-loading,
.niche-loading,
.persona-loading {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}
.wizard-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 64px 0;
}
.wizard-loading-text {
  text-align: center;
}
.wizard-loading-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
}
.wizard-loading-subtitle {
  font-size: 14px;
  color: #8c8c8c;
}
.wizard-loading :deep(.ant-spin-dot-item) {
  background: var(--color-primary, #07c160);
}
.niche-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.niche-card {
  position: relative;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.niche-card:hover {
  border-color: var(--color-primary, #FF2442);
}
.niche-card.selected {
  border-color: var(--color-primary, #FF2442);
  background: rgba(255, 36, 66, 0.04);
}
.niche-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.niche-name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.niche-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  margin-bottom: 8px;
  flex-wrap: wrap;
  color: #595959;
}
.niche-evidence {
  font-size: 13px;
  margin-bottom: 8px;
  color: var(--color-primary, #FF2442);
}
.niche-reason {
  font-size: 13px;
  line-height: 1.6;
  color: #595959;
}
.persona-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.persona-card {
  position: relative;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.persona-card:hover {
  border-color: var(--color-primary, #FF2442);
}
.persona-card.selected {
  border-color: var(--color-primary, #FF2442);
  background: rgba(255, 36, 66, 0.04);
}
.persona-name {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 6px;
  color: #1a1a1a;
}
.persona-desc {
  font-size: 13px;
  line-height: 1.5;
  color: #595959;
}
.pillars-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.pillar-card {
  background: #fff;
  border: 1px solid #f5f5f5;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}
.pillar-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  gap: 12px;
}
.pillar-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  flex-shrink: 0;
}
.pillar-percent {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary, #FF2442);
  white-space: nowrap;
  flex-shrink: 0;
}
.pillar-card :deep(.ant-slider) {
  margin: 0;
}
.pillar-card :deep(.ant-slider-track) {
  background: var(--color-primary, #FF2442);
}
.pillar-card :deep(.ant-slider-handle) {
  border-color: var(--color-primary, #FF2442);
}
.pillar-card :deep(.ant-slider-handle:focus),
.pillar-card :deep(.ant-slider-handle.ant-tooltip-open) {
  border-color: var(--color-primary, #FF2442);
  box-shadow: 0 0 0 4px rgba(255, 36, 66, 0.2);
}
.pillar-card :deep(.ant-slider-dot-active) {
  border-color: var(--color-primary, #FF2442);
}
.pillar-warning {
  margin-top: 8px;
  font-size: 13px;
  color: var(--color-primary, #FF2442);
}
.selected-check {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 16px;
  color: var(--color-primary, #FF2442);
}
.summary-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}
.summary-row {
  display: flex;
  padding: 10px 0;
}
.summary-label {
  width: 100px;
  font-size: 14px;
  flex-shrink: 0;
  color: #8c8c8c;
}
.summary-value {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.summary-pillars {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.summary-answers {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.summary-answer {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 8px;
}
.answer-question {
  color: #595959;
}
.answer-value {
  color: #1a1a1a;
  font-weight: 500;
}
.summary-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  background: #fff7e6;
  padding: 12px 16px;
  border-radius: 8px;
  color: #d46b08;
}
.onboarding-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-bottom: 48px;
}
.onboarding-actions .ant-btn-primary:not(.ant-btn-disabled) {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}
.onboarding-actions .ant-btn-primary:not(.ant-btn-disabled):hover,
.onboarding-actions .ant-btn-primary:not(.ant-btn-disabled):focus {
  background: var(--color-primary-hover, #E61E3A);
  border-color: var(--color-primary-hover, #E61E3A);
  color: #fff;
}
.onboarding-actions .ant-btn-primary:disabled {
  opacity: 0.6;
}

@media (max-width: 768px) {
  .onboarding-index {
    padding: 0 0 calc(24px + env(safe-area-inset-bottom));
    background:
      radial-gradient(140% 90% at 50% -8%, var(--color-primary-bg) 0%, transparent 55%),
      var(--color-bg-page);
  }
  .onboarding-header {
    display: none;
  }

  .onboarding-hero-mobile {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    height: 170px;
    margin: 0 0 12px;
    padding: 0 20px 0 22px;
    border-radius: 0;
    background: linear-gradient(45deg, #ffe8ec 0%, #fff0f3 40%, #fff5f7 70%, #fffafc 100%);
    overflow: hidden;
    position: relative;
  }

  .onboarding-hero-mobile::before,
  .onboarding-hero-mobile::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    border: 18px solid rgba(255, 36, 66, 0.08);
    pointer-events: none;
  }

  .onboarding-hero-mobile::before {
    width: 120px;
    height: 120px;
    bottom: -30px;
    left: -30px;
  }

  .onboarding-hero-mobile::after {
    width: 80px;
    height: 80px;
    top: -20px;
    left: 60px;
    border-width: 14px;
    border-color: rgba(255, 36, 66, 0.06);
  }

  .onboarding-hero-mobile__text {
    position: relative;
    z-index: 1;
    flex: 1;
    min-width: 0;
  }

  .onboarding-hero-mobile__logo {
    height: 48px;
    width: auto;
    display: block;
  }

  .onboarding-hero-mobile__desc {
    margin: 8px 0 0;
    font-size: 12px;
    color: #595959;
  }

  .onboarding-title {
    font-size: 24px;
    font-weight: 800;
    letter-spacing: -0.5px;
    margin-bottom: 8px;
  }
  .onboarding-subtitle {
    font-size: 13px;
    line-height: 1.6;
    color: #8c8c8c;
  }

  .onboarding-body {
    padding: 0 14px;
  }

  .onboarding-steps {
    margin-bottom: 16px;
  }
  .step-bar::before {
    left: 12%;
    right: 12%;
    top: 16px;
    height: 2px;
    background: #f0f0f0;
  }
  .step-item {
    gap: 6px;
  }
  .step-circle {
    width: 34px;
    height: 34px;
    font-size: 13px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
  }
  .step-label {
    display: block;
    font-size: 10px;
    white-space: nowrap;
    color: #8c8c8c;
  }
  .step-item.active .step-label {
    color: var(--color-primary);
    font-weight: 600;
  }

  .onboarding-card {
    border-radius: 20px;
    padding: 18px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
    border: 1px solid rgba(0, 0, 0, 0.03);
    margin-bottom: 16px;
  }

  /* 平台选择步骤：去掉外层 onboarding-card 卡片 */
  .onboarding-card--platform {
    background: transparent;
    box-shadow: none;
    border: none;
    padding: 0;
    border-radius: 0;
    margin: 0;
  }

  .onboarding-card--niche {
    background: transparent;
    box-shadow: none;
    border: none;
    padding: 0;
    border-radius: 0;
    margin: 0;
  }

  .onboarding-card--persona {
    background: transparent;
    box-shadow: none;
    border: none;
    padding: 0;
    border-radius: 0;
    margin: 0;
  }

  .onboarding-card--summary {
    background: transparent;
    box-shadow: none;
    border: none;
    padding: 0;
    border-radius: 0;
    margin: 0;
  }

  .step-panel--platform .step-title,
  .step-panel--platform .step-desc {
    padding: 0;
  }

  .step-title {
    font-size: 18px;
    font-weight: 700;
    margin-bottom: 6px;
  }
  .step-desc {
    font-size: 13px;
    color: #8c8c8c;
    margin-bottom: 18px;
    line-height: 1.55;
  }

  /* 平台选择：桌面端网格隐藏，手机端京东风格横向滑动卡片 */
  .platform-grid--desktop {
    display: none;
  }

  .platform-swiper--mobile {
    display: block;
  }

  .platform-swiper-track {
    display: flex;
    gap: 12px;
    overflow-x: auto;
    scroll-snap-type: x mandatory;
    scrollbar-width: none;
    padding: 4px 0 16px;
  }

  .platform-swiper-track::-webkit-scrollbar {
    display: none;
  }

  .platform-swiper-card {
    flex: 0 0 calc(100% - 32px);
    max-width: 320px;
    min-height: 240px;
    scroll-snap-align: start;
    position: relative;
    border-radius: 20px;
    padding: 18px;
    background: #fff;
    border: 2px solid transparent;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    cursor: pointer;
    transition: all 0.25s ease;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .platform-swiper-card.selected {
    border-color: var(--color-primary, #FF2442);
    box-shadow: 0 6px 24px rgba(255, 36, 66, 0.12);
  }

  .platform-swiper-card__header {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 12px;
    text-align: left;
  }

  .platform-swiper-card .platform-icon {
    width: 48px;
    height: 48px;
    border-radius: 14px;
    flex-shrink: 0;
  }

  .platform-swiper-card .platform-title {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .platform-swiper-card .platform-name {
    font-size: 18px;
    font-weight: 700;
  }

  .platform-swiper-card__tagline {
    font-size: 12px;
    color: #595959;
    line-height: 1.5;
    text-align: left;
  }

  .platform-swiper-card__info {
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 12px;
    line-height: 1.4;
  }

  .info-row {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  .info-label {
    font-size: 11px;
    color: #8c8c8c;
    white-space: nowrap;
  }

  .info-value {
    color: #262626;
    overflow-wrap: break-word;
  }

  .platform-swiper-card__metrics {
    display: flex;
    gap: 8px;
    margin-top: auto;
    padding-top: 10px;
    border-top: 1px solid #f5f5f5;
  }

  .platform-metric {
    flex: 1;
    text-align: center;
  }

  .platform-metric__label {
    font-size: 11px;
    color: #8c8c8c;
    margin-bottom: 6px;
  }

  .platform-metric__value {
    font-size: 13px;
    font-weight: 600;
    color: #1a1a1a;
    white-space: nowrap;
  }

  .platform-metric__value.tag-easy {
    color: #237804;
    background: transparent;
    border-color: transparent;
  }
  .platform-metric__value.tag-medium {
    color: #fa8c16;
    background: transparent;
    border-color: transparent;
  }
  .platform-metric__value.tag-hard {
    color: #ff6b6b;
    background: transparent;
    border-color: transparent;
  }

  .platform-swiper-card .selected-check {
    position: absolute;
    top: 14px;
    right: 14px;
    width: 26px;
    height: 26px;
    border-radius: 50%;
    background: var(--color-primary, #FF2442);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
  }

  .platform-swiper-dots {
    display: flex;
    justify-content: center;
    gap: 6px;
    margin-top: 2px;
  }

  .swiper-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #d9d9d9;
    transition: all 0.2s;
  }

  .swiper-dot.active {
    width: 18px;
    border-radius: 3px;
    background: var(--color-primary, #FF2442);
  }

  .platform-reason-mobile {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    margin: 16px 0;
    padding: 14px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    font-size: 13px;
    line-height: 1.55;
    color: #595959;
  }

  .platform-reason-mobile .anticon {
    flex-shrink: 0;
    margin-top: 2px;
    color: var(--color-primary, #FF2442);
  }

  /* 问题选项 */
  .form-block {
    margin-bottom: 18px;
  }
  .form-label {
    font-size: 14px;
    margin-bottom: 10px;
  }
  .option-group {
    flex-direction: column;
    gap: 8px;
  }
  .option-btn {
    width: 100%;
    text-align: left;
    padding: 12px 14px;
    border-radius: 12px;
    font-size: 14px;
    border-color: #eeeeee;
  }
  .option-btn.selected {
    background: var(--color-primary-bg);
    border-color: var(--color-primary);
    color: var(--color-primary);
  }

  /* 赛道 */
  .niche-list {
    gap: 10px;
  }
  .niche-card {
    padding: 14px;
    border-radius: 14px;
    background: #fff;
    border: 1px solid #f5f5f5;
  }
  .niche-card.selected {
    background: var(--color-primary-bg);
    border-color: var(--color-primary-light);
    box-shadow: 0 4px 14px rgba(255, 36, 66, 0.08);
  }
  .niche-header {
    margin-bottom: 8px;
  }
  .niche-name {
    font-size: 16px;
  }
  .niche-meta {
    flex-direction: row;
    gap: 12px;
    font-size: 12px;
  }
  .niche-evidence {
    font-size: 12px;
    margin-bottom: 6px;
  }
  .niche-reason {
    font-size: 12px;
    line-height: 1.55;
  }

  /* 人设 */
  .persona-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .persona-card {
    padding: 14px;
    border-radius: 14px;
    background: #fff;
    border: 1px solid #f5f5f5;
  }
  .persona-card.selected {
    background: var(--color-primary-bg);
    border-color: var(--color-primary-light);
    box-shadow: 0 4px 14px rgba(255, 36, 66, 0.08);
  }
  .persona-name {
    font-size: 15px;
  }
  .persona-desc {
    font-size: 12px;
    line-height: 1.55;
  }

  /* 内容支柱 */
  .pillars-input {
    gap: 10px;
  }
  .pillar-card {
    padding: 14px;
    border-radius: 12px;
  }
  .pillar-card-header {
    margin-bottom: 8px;
    gap: 8px;
  }
  .pillar-name {
    font-size: 14px;
  }
  .pillar-percent {
    font-size: 14px;
  }
  .pillar-warning {
    font-size: 12px;
  }

  /* 方案汇总 */
  .summary-card {
    background: #fff;
    border: 1px solid #f5f5f5;
    border-radius: 16px;
    padding: 14px 16px;
  }
  .summary-row {
    flex-direction: row;
    align-items: flex-start;
    padding: 10px 0;
    gap: 10px;
  }
  .summary-label {
    width: 76px;
    font-size: 13px;
  }
  .summary-value {
    font-size: 14px;
  }
  .summary-pillars {
    gap: 6px;
  }
  .summary-pillars :deep(.ant-tag) {
    margin: 0;
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 999px;
    background: var(--color-primary-bg);
    border-color: var(--color-primary-light);
    color: var(--color-primary);
  }
  .summary-answer {
    flex-direction: row;
    align-items: center;
    padding: 6px 10px;
    gap: 8px;
  }
  .answer-question {
    font-size: 12px;
  }
  .answer-value {
    font-size: 12px;
  }
  .summary-tip {
    font-size: 12px;
    padding: 10px 12px;
    border-radius: 12px;
    margin-bottom: 16px;
  }

  /* 底部操作 */
  .onboarding-actions {
    gap: 10px;
    padding-bottom: 0;
  }
  .onboarding-actions .ant-btn {
    flex: 1;
    height: 46px;
    border-radius: 12px;
    font-weight: 600;
    font-size: 15px;
  }
  .onboarding-actions .ant-btn-primary {
    flex: 2;
  }

  /* 手机端暗色主题 */
  body[data-theme="dark"] .onboarding-hero-mobile {
    background: linear-gradient(45deg, #2a181b 0%, #221416 40%, #1a1113 70%, #150f10 100%);
  }
  body[data-theme="dark"] .onboarding-hero-mobile::before,
  body[data-theme="dark"] .onboarding-hero-mobile::after {
    border-color: rgba(255, 77, 111, 0.1);
  }
  body[data-theme="dark"] .onboarding-hero-mobile__desc {
    color: #8c8c8c;
  }
  body[data-theme="dark"] .platform-swiper-card {
    background: #1f1f1f;
    border-color: transparent;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
  }
  body[data-theme="dark"] .platform-swiper-card.selected {
    border-color: rgba(255, 77, 111, 0.5);
    box-shadow: 0 6px 24px rgba(255, 36, 66, 0.18);
  }
  body[data-theme="dark"] .platform-swiper-card__metrics {
    border-top-color: #333;
  }
  body[data-theme="dark"] .platform-swiper-card__tagline,
  body[data-theme="dark"] .platform-reason-mobile {
    color: rgba(255, 255, 255, 0.55);
  }
  body[data-theme="dark"] .platform-swiper-card__info {
    color: rgba(255, 255, 255, 0.55);
  }
  body[data-theme="dark"] .info-label {
    color: rgba(255, 255, 255, 0.45);
  }
  body[data-theme="dark"] .info-value {
    color: rgba(255, 255, 255, 0.75);
  }
  body[data-theme="dark"] .platform-reason-mobile {
    background: #1f1f1f;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  }
  body[data-theme="dark"] .platform-metric__label {
    color: rgba(255, 255, 255, 0.45);
  }
  body[data-theme="dark"] .platform-metric__value {
    color: rgba(255, 255, 255, 0.9);
  }
  body[data-theme="dark"] .swiper-dot {
    background: #595959;
  }
  body[data-theme="dark"] .swiper-dot.active {
    background: var(--color-primary, #FF2442);
  }
}
</style>
