<template>
  <div class="onboarding-index">
    <div class="onboarding-header">
      <h2 class="onboarding-title">制定你的自媒体方案</h2>
      <p class="onboarding-subtitle">别再瞎折腾了！只要设置一次，让你拥有一个24小时在线的自媒体运营总监，你不再关心明天要发什么，怎么写，几点发，发到哪，怎么蹭热点……我们都会给你安排好。</p>
    </div>

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

    <div class="onboarding-card">
     <div v-if="step === 1" class="step-panel">
       <h3 class="step-title">你想在哪个平台做自媒体？</h3>
        <p class="step-desc">每个平台的内容形式、用户群体、变现方式和收入空间都不同，了解清楚再选。</p>
       <div class="recommend-bar">
          <div class="recommend-card" @click="openRecommendModal">
           <div class="recommend-card-text">
             <BulbOutlined class="recommend-icon" />
             <span>纠结选哪个平台？<strong>AI 帮你推荐最合适的</strong></span>
           </div>
            <button class="recommend-card-btn">AI 推荐</button>
         </div>
        </div>
        <div class="platform-grid">
          <div
            v-for="p in platforms"
            :key="p.key"
            :class="['platform-card', { selected: form.platform === p.key }]"
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
            <div v-if="form.platform === p.key" class="selected-check">
              <CheckOutlined />
            </div>
          </div>
        </div>
      </div>

      <div v-if="step === 2" class="step-panel">
        <h3 class="step-title">你更适合哪种变现方式？</h3>
        <p class="step-desc">根据你的优势选择变现路径，后面会推荐对应的赛道和玩法。</p>
        <div class="form-block">
          <div class="form-label">你更适合哪种变现方式？</div>
          <a-button type="primary" size="small" :loading="isLoadingGoals" :disabled="isLoadingGoals || !form.platform" @click="loadGoalOptions">
            <BulbOutlined /> AI 推荐目标
          </a-button>
          <div class="option-group" style="margin-top: 12px;">
            <button
              v-for="g in goalOptions"
              :key="g.key"
              :class="['option-btn', { selected: form.goal === g.name }]"
              @click="form.goal = g.name"
            >
              {{ g.name }}
            </button>
          </div>
        </div>
        <div class="form-block">
          <div class="form-label">你的职业/经验领域是？</div>
          <div class="option-group">
            <button
              v-for="b in backgrounds"
              :key="b"
              :class="['option-btn', { selected: form.background === b }]"
              @click="form.background = b"
            >
              {{ b }}
            </button>
          </div>
        </div>
        <div v-if="showProductInput" class="form-block">
          <div class="form-label">你是否有可变现的产品、服务或技能？</div>
          <div class="option-group">
            <button :class="['option-btn', { selected: form.hasProduct === true }]" @click="form.hasProduct = true">
              有
            </button>
            <button :class="['option-btn', { selected: form.hasProduct === false }]" @click="form.hasProduct = false">
              没有，想先跑流量
            </button>
          </div>
          <a-input
            v-if="form.hasProduct"
            v-model:value="form.productDesc"
            placeholder="简单描述一下，比如：职场咨询、育儿课程、电商货源"
            class="product-input"
          />
        </div>
      </div>

      <div v-if="step === 3" class="step-panel">
        <h3 class="step-title">推荐你尝试这些细分赛道</h3>
        <p class="step-desc">基于你选择的平台、背景和目标，我们找出了需求真实、竞争度尚可的赛道。</p>
        <div v-if="isLoadingNiches" class="niche-loading">
          <a-spin tip="AI 正在推荐赛道..." />
        </div>
        <div class="niche-list">
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

      <div v-if="step === 4" class="step-panel">
        <h3 class="step-title">选择你的人设和内容支柱</h3>
        <p class="step-desc">人设决定用户怎么记住你，内容支柱保证你持续有得写。</p>
        <div v-if="isLoadingPersonas" class="persona-loading">
          <a-spin tip="AI 正在推荐人设..." />
        </div>
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
            <div v-for="pillar in pillars" :key="pillar.name" class="pillar-row">
              <span class="pillar-name">{{ pillar.name }}</span>
              <a-slider v-model:value="pillar.percent" :min="0" :max="100" />
              <span class="pillar-percent">{{ pillar.percent }}%</span>
            </div>
          </div>
          <div v-if="pillarTotal !== 100" class="pillar-warning">三项比例之和需等于 100%</div>
        </div>
      </div>

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
            <span class="summary-label">核心目标</span>
            <span class="summary-value">{{ form.goal }}</span>
          </div>
        </div>
        <div class="summary-tip">
          <BulbOutlined /> 系统会基于这个方案，每周给你推荐选题和角度，降低同质化风险。
        </div>
      </div>
    </div>

   <div class="onboarding-actions">
     <a-button v-if="step > 1" size="large" @click="prev">上一步</a-button>
     <a-button v-if="step < 5" type="primary" size="large" :disabled="!canNext" @click="next">下一步</a-button>
     <a-button v-if="step === 5" type="primary" size="large" @click="confirm">确认方案，进入工作台</a-button>
   </div>
   <a-modal
     v-model:open="recommendModalOpen"
     title="让 AI 推荐最适合你的平台"
      width="760px"
     :footer="null"
     @cancel="resetRecommend"
   >
      <div class="recommend-form">
        <div class="form-block">
          <div class="form-label">你做自媒体是主业还是副业？</div>
          <div class="option-group">
            <button
              v-for="w in workTypes"
              :key="w"
              :class="['option-btn', { selected: recommendForm.workType === w }]"
              @click="recommendForm.workType = w"
            >{{ w }}</button>
          </div>
        </div>
        <div class="form-block">
          <div class="form-label">你每周能投入多少时间？</div>
          <div class="option-group">
            <button
              v-for="t in timeOptions"
              :key="t"
              :class="['option-btn', { selected: recommendForm.timePerWeek === t }]"
              @click="recommendForm.timePerWeek = t"
            >{{ t }}</button>
          </div>
        </div>
        <div class="form-block">
          <div class="form-label">你期望月收入达到多少？</div>
          <div class="option-group">
            <button
              v-for="i in incomeGoals"
              :key="i"
              :class="['option-btn', { selected: recommendForm.incomeGoal === i }]"
              @click="recommendForm.incomeGoal = i"
            >{{ i }}</button>
          </div>
        </div>
        <div class="form-block">
          <div class="form-label">你能接受多久不盈利？</div>
          <div class="option-group">
            <button
              v-for="b in breakEvenPeriods"
              :key="b"
              :class="['option-btn', { selected: recommendForm.breakEvenPeriod === b }]"
              @click="recommendForm.breakEvenPeriod = b"
            >{{ b }}</button>
          </div>
        </div>
        <div class="form-block">
          <div class="form-label">你倾向于做哪种内容？</div>
          <div class="option-group">
            <button
              v-for="c in contentTypes"
              :key="c"
              :class="['option-btn', { selected: recommendForm.contentType === c }]"
              @click="recommendForm.contentType = c"
            >{{ c }}</button>
          </div>
        </div>
        <div class="form-block">
          <div class="form-label">你的目标受众是哪类人？</div>
          <div class="option-group">
            <button
              v-for="a in audiences"
              :key="a"
              :class="['option-btn', { selected: recommendForm.audience === a }]"
              @click="recommendForm.audience = a"
            >{{ a }}</button>
          </div>
        </div>
        <div class="form-block">
          <div class="form-label">你更符合哪种身份？</div>
          <div class="option-group">
            <button
              v-for="i in identities"
              :key="i"
              :class="['option-btn', { selected: recommendForm.identity === i }]"
              @click="recommendForm.identity = i"
            >{{ i }}</button>
          </div>
        </div>
        <div class="form-block">
          <div class="form-label">你是否愿意出镜或做视频？</div>
          <div class="option-group">
            <button
              v-for="o in onCameraOptions"
              :key="o"
              :class="['option-btn', { selected: recommendForm.onCamera === o }]"
              @click="recommendForm.onCamera = o"
           >{{ o }}</button>
         </div>
       </div>
       <div class="form-block">
         <div class="form-label">其他补充（可选）</div>
          <a-textarea v-model:value="recommendForm.note" :rows="3" placeholder="比如：我想做短视频但不敢出镜、有育儿经验但不知道写什么..." />
        </div>
        <div class="recommend-actions">
          <a-button type="primary" class="recommend-modal-btn" size="large" :disabled="!canRecommend" :loading="recommendLoading" @click="runRecommend">获取推荐</a-button>
        </div>
        <div v-if="recommendResult" class="recommend-result">
          <div class="recommend-result-title">推荐平台：{{ recommendResult.platformName }}</div>
          <div class="recommend-result-reason">{{ recommendResult.reason }}</div>
          <a-button type="primary" class="recommend-modal-btn" @click="applyRecommend">选择这个平台</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CheckOutlined,
  BulbOutlined,
  FireOutlined
} from '@ant-design/icons-vue'
import { platforms as backendPlatforms, loadPlatforms } from '@/composables/usePlatforms.js'
import {
  recommendPlatform as apiRecommendPlatform,
  recommendGoals as apiRecommendGoals,
  recommendNiches as apiRecommendNiches,
  recommendPersonas as apiRecommendPersonas,
  savePlan as apiSavePlan
} from '@/api/selfMediaPlan.js'

const router = useRouter()
const step = ref(1)
const steps = ['选平台', '定目标', '选赛道', '做人设', '出方案']

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

onMounted(loadPlatforms)

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

const backgrounds = ['职场/管理', 'IT/互联网', '育儿/教育', '健康/养生', '金融/理财', '电商/创业', '生活方式', '其他']
const timeOptions = ['小于 3 小时', '3 - 10 小时', '大于 10 小时']
const workTypes = ['主业', '副业', '想转主业', '不明确']
const incomeGoals = ['几千零花钱', '月入过万', '月入几万', '没具体目标', '不明确']
const breakEvenPeriods = ['1 个月', '3 个月', '6 个月', '1 年以上', '不明确']
const contentTypes = ['长图文', '短视频', '图文笔记', '直播', '问答', '不明确']
const audiences = ['年轻人', '职场人', '宝妈家庭', '中老年人', '专业人士', '不明确']
const identities = ['宝妈', '职场人', '创业者/老板', '自由职业', '学生', '其他', '不明确']
const onCameraOptions = ['愿意出镜', '做视频但不出镜', '不想做视频', '不明确']

const form = reactive({
  platform: '',
  goal: '',
  background: '',
  timePerWeek: '',
  hasProduct: null,
  productDesc: '',
  recommendedByAI: false
})

const selectedNiche = ref('')
const selectedPersona = ref('')
const pillars = reactive([
  { name: '干货复盘', percent: 60 },
  { name: '个人故事', percent: 20 },
  { name: '热点解读', percent: 20 }
])

// AI 推荐结果
const goalOptions = ref([])
const nicheOptions = ref([])
const personaOptions = ref([])
const isLoadingGoals = ref(false)
const isLoadingNiches = ref(false)
const isLoadingPersonas = ref(false)

const selectedNicheName = computed(() => nicheOptions.value.find((n) => n.key === selectedNiche.value)?.name || '')
const selectedPersonaName = computed(() => personaOptions.value.find((p) => p.key === selectedPersona.value)?.name || '')
const platformLabels = computed(() => platforms.value.find((p) => p.key === form.platform)?.name || '')
const pillarTotal = computed(() => pillars.reduce((sum, p) => sum + p.percent, 0))

const showProductInput = computed(() => {
  const g = form.goal || ''
  return g.includes('产品') || g.includes('服务') || g.includes('资源') || g.includes('信息差')
})

watch(() => form.platform, () => {
  form.goal = ''
  goalOptions.value = []
})

const canNext = computed(() => {
  if (step.value === 1) return !!form.platform
  if (step.value === 2) {
    if (!form.goal || !form.background) return false
    if (showProductInput.value) {
      if (form.hasProduct === null || form.hasProduct === undefined) return false
      if (form.hasProduct === true && !form.productDesc.trim()) return false
    }
    return true
  }
  if (step.value === 3) return !!selectedNiche.value
  if (step.value === 4) return !!selectedPersona.value && pillarTotal.value === 100
  return true
})

function selectPlatform(key) {
  form.platform = form.platform === key ? '' : key
  form.recommendedByAI = false
}

async function loadGoalOptions() {
  if (!form.platform) return
  isLoadingGoals.value = true
  try {
    const platform = platforms.value.find((p) => p.key === form.platform)
    const data = await apiRecommendGoals({
      platformKey: form.platform,
      background: form.background || '',
      context: buildContext(),
      platformName: platform?.name || '',
      platformTagline: platform?.tagline || '',
      platformContentForm: (platform?.contentForm || []).join('、'),
      platformMonetization: (platform?.monetization || []).join('、'),
      platformBestFor: platform?.bestFor || ''
    })
    goalOptions.value = Array.isArray(data) ? data : []
    if (goalOptions.value.length && !form.goal) {
      form.goal = goalOptions.value[0].name
    }
  } catch (e) {
    message.error('目标推荐失败，请重试')
  } finally {
    isLoadingGoals.value = false
  }
}

async function loadNicheOptions() {
  if (!form.platform || !form.goal) return
  isLoadingNiches.value = true
  try {
    const platform = platforms.value.find((p) => p.key === form.platform)
    const data = await apiRecommendNiches({
      platformKey: form.platform,
      goal: form.goal,
      background: form.background || '',
      hasProduct: form.hasProduct === true,
      productDesc: form.productDesc || '',
      context: buildContext(),
      platformName: platform?.name || '',
      platformTagline: platform?.tagline || '',
      platformContentForm: (platform?.contentForm || []).join('、'),
      platformMonetization: (platform?.monetization || []).join('、'),
      platformBestFor: platform?.bestFor || ''
    })
    nicheOptions.value = Array.isArray(data) ? data : []
    selectedNiche.value = nicheOptions.value[0]?.key || ''
  } catch (e) {
    message.error('赛道推荐失败，请重试')
  } finally {
    isLoadingNiches.value = false
  }
}

async function loadPersonaOptions() {
  if (!form.platform || !form.goal || !selectedNiche.value) return
  isLoadingPersonas.value = true
  try {
    const platform = platforms.value.find((p) => p.key === form.platform)
    const niche = nicheOptions.value.find((n) => n.key === selectedNiche.value)
    const data = await apiRecommendPersonas({
      platformKey: form.platform,
      goal: form.goal,
      background: form.background || '',
      nicheKey: niche?.key || '',
      nicheName: niche?.name || '',
      context: buildContext(),
      platformName: platform?.name || '',
      platformTagline: platform?.tagline || '',
      platformContentForm: (platform?.contentForm || []).join('、'),
      platformMonetization: (platform?.monetization || []).join('、'),
      platformBestFor: platform?.bestFor || ''
    })
    const result = data || {}
    personaOptions.value = Array.isArray(result.personas) ? result.personas : []
    if (Array.isArray(result.defaultPillars) && result.defaultPillars.length) {
      pillars.splice(0, pillars.length, ...result.defaultPillars.map((p) => ({ name: p.name, percent: p.percent })))
    }
    selectedPersona.value = personaOptions.value[0]?.key || ''
  } catch (e) {
    message.error('人设推荐失败，请重试')
  } finally {
    isLoadingPersonas.value = false
  }
}

function buildContext() {
  return {
    workType: recommendForm.workType || '',
    timePerWeek: recommendForm.timePerWeek || '',
    incomeGoal: recommendForm.incomeGoal || '',
    breakEvenPeriod: recommendForm.breakEvenPeriod || '',
    contentType: recommendForm.contentType || '',
    audience: recommendForm.audience || '',
    identity: recommendForm.identity || '',
    onCamera: recommendForm.onCamera || '',
    note: recommendForm.note || ''
  }
}

function next() {
  if (!canNext.value) {
    message.warning('请先完成当前步骤的选择')
    return
  }
  if (step.value === 2) {
    loadNicheOptions()
  }
  if (step.value === 3) {
    loadPersonaOptions()
  }
  if (step.value === 1 && form.recommendedByAI) {
    step.value = 3
    loadNicheOptions()
    return
  }
  step.value++
}

function prev() {
  step.value--
}

async function confirm() {
  const platform = platforms.value.find((p) => p.key === form.platform)
  const niche = nicheOptions.value.find((n) => n.key === selectedNiche.value)
  const persona = personaOptions.value.find((p) => p.key === selectedPersona.value)
  try {
    await apiSavePlan({
      platformKey: form.platform,
      platformName: platform?.name || '',
      goal: form.goal,
      background: form.background,
      hasProduct: form.hasProduct === true,
      productDesc: form.productDesc || '',
      nicheKey: niche?.key || '',
      nicheName: niche?.name || '',
      personaKey: persona?.key || '',
      personaName: persona?.name || '',
      isRecommendedByAI: form.recommendedByAI,
      pillars: pillars.map((p) => ({ name: p.name, percent: p.percent })),
      recommendationContext: buildContext()
    })
    message.success('自媒体方案已生成')
    localStorage.setItem('aichuangzuo_onboarding_done', '1')
    router.push('/console/workbench')
  } catch (e) {
    message.error('保存方案失败，请重试')
  }
}

// AI 平台推荐（前端规则模拟，后续可替换为 LLM 接口）
const recommendModalOpen = ref(false)
const recommendLoading = ref(false)
const recommendResult = ref(null)
const recommendForm = reactive({
  workType: '',
  timePerWeek: '',
  incomeGoal: '',
  breakEvenPeriod: '',
  contentType: '',
  audience: '',
  identity: '',
  onCamera: '',
  note: ''
})

const canRecommend = computed(() => {
  return !!recommendForm.contentType &&
         !!recommendForm.timePerWeek &&
         !!recommendForm.onCamera &&
         !!recommendForm.incomeGoal &&
         !!recommendForm.workType
})

function openRecommendModal() {
  recommendForm.workType = ''
  recommendForm.timePerWeek = form.timePerWeek || ''
  recommendForm.incomeGoal = ''
  recommendForm.breakEvenPeriod = ''
  recommendForm.contentType = ''
  recommendForm.audience = ''
  recommendForm.identity = ''
  recommendForm.onCamera = ''
  recommendForm.note = ''
  recommendResult.value = null
  recommendModalOpen.value = true
}

function resetRecommend() {
  recommendResult.value = null
  recommendLoading.value = false
}

function applyRecommend() {
  if (recommendResult.value) {
    form.platform = recommendResult.value.platformKey
    if (recommendForm.timePerWeek) form.timePerWeek = recommendForm.timePerWeek
    form.recommendedByAI = true
    form.background = identityBackgroundMap[recommendForm.identity] || '其他'
    recommendModalOpen.value = false
    resetRecommend()
    // 提前拉取 AI 目标推荐，方便用户进入下一步直接看到选项
    loadGoalOptions()
  }
}

async function runRecommend() {
  if (!canRecommend.value) return
  recommendLoading.value = true
  recommendResult.value = null
  try {
    recommendResult.value = await apiRecommendPlatform({ context: buildContext() })
  } catch (e) {
    message.error('平台推荐失败，请重试')
  } finally {
    recommendLoading.value = false
  }
}

const identityBackgroundMap = {
  '职场人': '职场/管理',
  '创业者/老板': '电商/创业',
  '宝妈': '育儿/教育',
  '学生': '其他',
  '自由职业': '其他',
  '其他': '其他',
  '不明确': '其他'
}
</script>

<style scoped>
.onboarding-index {
  padding: 32px 24px;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  min-height: 100%;
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
}
.step-item.active .step-circle {
  background: var(--color-primary, #FF2442);
}
.step-item.done .step-circle {
  background: var(--color-primary, #07c160);
}
.step-label {
  font-size: 13px;
}
.step-item.active .step-label,
.step-item.done .step-label {
  font-weight: 500;
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
}
.step-desc {
  font-size: 14px;
  margin: 0 0 24px;
}
.platform-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 16px;
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
}
.platform-reason {
  font-size: 12px;
  line-height: 1.5;
  padding: 10px 12px;
  background: rgba(255, 36, 66, 0.06);
  border: 1px solid rgba(255, 36, 66, 0.12);
  border-radius: 8px;
  margin-top: auto;
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
}

.earn-metric-value {
  font-size: 13px;
  font-weight: 600;
}

.form-block {
  margin-bottom: 24px;
}
.form-label {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
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
}
.option-btn:hover {
  border-color: var(--color-primary, #FF2442);
}
.option-btn.selected {
  border-color: var(--color-primary, #FF2442);
  background: var(--color-primary, #FF2442);
}
.product-input {
  margin-top: 12px;
  max-width: 420px;
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
}
.niche-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.niche-evidence {
  font-size: 13px;
  margin-bottom: 8px;
}
.niche-reason {
  font-size: 13px;
  line-height: 1.6;
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
}
.persona-desc {
  font-size: 13px;
  line-height: 1.5;
}
.pillars-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.pillar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pillar-name {
  width: 80px;
  font-size: 14px;
}
.pillar-row :deep(.ant-slider) {
  flex: 1;
}
.pillar-row :deep(.ant-slider-track) {
  background: var(--color-primary, #07c160);
}
.pillar-row :deep(.ant-slider-handle) {
  border-color: var(--color-primary, #07c160);
}
.pillar-row :deep(.ant-slider-handle:focus),
.pillar-row :deep(.ant-slider-handle.ant-tooltip-open) {
  border-color: var(--color-primary, #07c160);
  box-shadow: 0 0 0 4px rgba(7, 193, 96, 0.2);
}
.pillar-row :deep(.ant-slider-dot-active) {
  border-color: var(--color-primary, #07c160);
}
.pillar-percent {
  width: 48px;
  text-align: right;
  font-size: 14px;
}
.pillar-warning {
  margin-top: 8px;
  font-size: 13px;
}
.selected-check {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 16px;
}
.summary-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}
.summary-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.summary-row:last-child {
  border-bottom: none;
}
.summary-label {
  width: 100px;
  font-size: 14px;
  flex-shrink: 0;
}
.summary-value {
  font-size: 15px;
  font-weight: 600;
}
.summary-pillars {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.summary-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  background: #fff7e6;
  padding: 12px 16px;
  border-radius: 8px;
}
.onboarding-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}
.onboarding-actions .ant-btn-primary:not(.ant-btn-disabled) {
  background: var(--color-primary, #07c160);
  border-color: var(--color-primary, #07c160);
  color: #fff;
}
.onboarding-actions .ant-btn-primary:not(.ant-btn-disabled):hover,
.onboarding-actions .ant-btn-primary:not(.ant-btn-disabled):focus {
  background: var(--color-primary-hover, #06ad56);
  border-color: var(--color-primary-hover, #06ad56);
  color: #fff;
}
.onboarding-actions .ant-btn-primary:disabled {
  opacity: 0.6;
}
/* Restore text colors stripped by the last edit */
.onboarding-index {
  color: #1a1a1a;
}
.step-circle {
  color: #1a1a1a;
}
.step-item.active .step-circle,
.step-item.done .step-circle {
  color: #fff;
}
.step-label {
  color: #8c8c8c;
}
.step-item.active .step-label,
.step-item.done .step-label {
  color: var(--color-primary, #FF2442);
}
.step-title,
.form-label,
.platform-name,
.meta-value,
.earn-title,
.earn-metric-value,
.niche-name,
.persona-name,
.pillar-name,
.pillar-percent,
.summary-value {
  color: #1a1a1a;
}
.step-desc,
.platform-reason,
.platform-earn,
.niche-meta,
.niche-reason,
.persona-desc {
  color: #595959;
}
.meta-label,
.earn-metric-label,
.summary-label {
  color: #8c8c8c;
}
.niche-evidence,
.pillar-warning,
.selected-check {
  color: var(--color-primary, #FF2442);
}
.option-btn.selected {
  color: #fff;
}
.summary-tip {
  color: #d46b08;
}

@media (max-width: 768px) {
  .onboarding-index {
    padding: 20px 16px;
  }
  .onboarding-title {
    font-size: 22px;
  }
  .step-bar::before {
    display: none;
  }
  .step-label {
    display: none;
  }
  .onboarding-card {
    padding: 20px;
  }
   .platform-grid {
    grid-template-columns: 1fr;
  }

  .persona-grid {
    grid-template-columns: 1fr 1fr;
  }

  .earn-metrics {
    flex-direction: column;
    gap: 8px;
  }
  .niche-meta {
    flex-direction: column;
    gap: 4px;
  }
}

.recommend-bar {
  margin-bottom: 20px;
}
.recommend-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fff1f2;
  border: 1px solid #ffccd0;
  border-radius: 12px;
  padding: 14px 18px;
  cursor: pointer;
  transition: all 0.2s;
}
.recommend-card:hover {
  background: #ffe6e8;
  border-color: #ffaeb4;
}
.recommend-card-text {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #820014;
  font-size: 14px;
  line-height: 1.5;
}
.recommend-card-text strong {
  color: var(--color-primary, #FF2442);
}
.recommend-icon {
  font-size: 22px;
  color: var(--color-primary, #FF2442);
  flex-shrink: 0;
}
.recommend-card-btn {
  flex-shrink: 0;
  background: #fff;
  color: var(--color-primary, #FF2442);
  border: 1px solid var(--color-primary, #FF2442);
  border-radius: 8px;
  padding: 0 18px;
  height: 34px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.recommend-card:hover .recommend-card-btn {
  background: var(--color-primary, #FF2442);
  color: #fff;
}
.recommend-form {
  padding: 8px 4px;
}
.recommend-form .ant-input:focus,
.recommend-form .ant-input-focused {
  border-color: var(--color-primary, #FF2442) !important;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.2) !important;
}
.recommend-actions {
  display: flex;
  justify-content: center;
  margin: 24px 0 16px;
}
.recommend-result {
  background: #fff1f2;
  border: 1px solid #ffccd0;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}
.recommend-result-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #820014;
}
.recommend-modal-btn.ant-btn-primary:not(.ant-btn-disabled) {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}
.recommend-modal-btn.ant-btn-primary:not(.ant-btn-disabled):hover,
.recommend-modal-btn.ant-btn-primary:not(.ant-btn-disabled):focus {
  background: #cf1322;
  border-color: #cf1322;
  color: #fff;
}
.recommend-result-reason {
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
  margin-bottom: 12px;
}

</style>
